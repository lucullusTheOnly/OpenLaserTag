package de.c_ebberg.openlasertag;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Created by christian on 16.02.17.
 * Thread for doing the game logic, so that the UI Thread can do UI things
 */

public class GameLogicThread extends Thread {
    // reference to mainHandler from the mainThread
    private Handler parentHandler;
    private Handler serial_handler;
    private Context context;
    private String LOG_TAG = "GameLogicThread";
    private static final boolean DEBUG = false;
    private boolean serial_running = false;
    private boolean thread_running = true;
    private GameFileLoader.GameInformation game_info = null;
    private GameFileLoader loader;
    public HashMap<String, Boolean> running_items = new HashMap<>();

    private List<ParallelGameExecutionThread> parallel_exe_threads;
    // local Handler manages messages for GameLogicThread
    // received from other Threads
    private Handler myThreadHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0: // abort Thread
                    interrupt();
                    break;
                case 1: // serial thread state has changed
                    serial_running = (msg.arg1 == 1);
                    break;
                case 3: // send game_info to designated handler
                    if (msg.obj == null) break;
                    Message gi_msg = new Message();
                    gi_msg.what = 15;
                    gi_msg.obj = game_info;
                    ((Handler) msg.obj).sendMessage(gi_msg);
                    break;
                case 4: // command to load XML Game File at location of "Path"
                    try {
                        game_info = loader.load_xml_game_file(msg.getData().getString("Path"));
                        if(DEBUG) Log.i(LOG_TAG, "Loaded GameFile: " + msg.getData().getString("Path"));
                        Message text_msg = new Message();
                        text_msg.what = 2;
                        Bundle text_msg_data = new Bundle();
                        text_msg_data.putInt("Type", 20);
                        text_msg_data.putString("String", game_info.toString());
                        text_msg.setData(text_msg_data);
                        text_msg.obj = game_info;
                        parentHandler.sendMessage(text_msg);
                        Singleton.getInstance().setGameInfo(game_info);
                        running_items = new HashMap<>();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "IOException during XML-File Load - " + e);
                    } catch (XmlPullParserException e) {
                        Log.e(LOG_TAG, "Parser Exception: File not valid - (" + e.getLineNumber() + "," + e.getColumnNumber() + ") " + e.getDetail());
                    }
                    break;
                case 5: // Serial-Command from Tagger-System
                    if (game_info == null) break;
                    if (!game_info.game_running) break;
                    byte[] cmd_array = msg.getData().getByteArray("ByteData");
                    try {
                        switch (cmd_array[0]) {
                            case 's': // Shot received
                                if(DEBUG) Log.i(LOG_TAG, "Handler: SHOT received");
                                QueueSignal(Signal.SHOT_RECEIVED, process_shot_info(cmd_array[1], cmd_array[2], cmd_array[3]));
                                break;
                            case 'r': // Recharge
                                if(DEBUG) Log.i(LOG_TAG, "Handler: Recharge received");
                                QueueSignal(Signal.RECHARGE_BUTTON, new HashMap<String, GameFileLoader.GameVariable>());
                                break;
                            case 't': // Trigger Pulled
                                if(DEBUG) Log.i(LOG_TAG, "Handler: Trigger pulled");
                                QueueSignal(Signal.TRIGGER_PULLED, new HashMap<String, GameFileLoader.GameVariable>());
                                break;
                            case 'g': // Game Command
                                if(DEBUG) Log.i(LOG_TAG, "Handler: Game Command received");
                                Map<String, GameFileLoader.GameVariable> mp = process_game_command(cmd_array[1], cmd_array[2]);
                                switch((String)mp.get("Signal").value){
                                    case "GET_ITEM":
                                        QueueSignal(Signal.GET_ITEM, mp);
                                        break;
                                    case "FLAG_PICKUP":
                                        QueueSignal(Signal.FLAG_PICKUP, mp);
                                        break;
                                    case "FLAG_LOST":
                                        QueueSignal(Signal.FLAG_LOST, mp);
                                        break;
                                    case "PLAYER_BASE_SIGNAL":
                                        QueueSignal(Signal.PLAYER_BASE_SIGNAL, mp);
                                        break;
                                    case "GAME_OVER":
                                        QueueSignal(Signal.GAME_OVER,mp);
                                        break;
                                    default:
                                        QueueSignal(Signal.GAME_COMMAND, mp);
                                        break;
                                }
                                break;
                            case 'c': { // get IDs of connected receivers
                                String receivers = msg.getData().getByteArray("ByteData").toString().substring(1);
                                Message rec_msg = new Message();
                                rec_msg.what = 2;
                                Bundle data = new Bundle();
                                data.putInt("Type", 1);
                                data.putString("ReceiverIDs", receivers);
                                rec_msg.setData(data);
                                parentHandler.sendMessage(rec_msg);
                                break;
                            }
                        }
                    } catch (NullPointerException e) {
                        Log.e(LOG_TAG, "Serial-Command: ByteData not big enough! Data lost?");
                    }
                    break;
                case 6: // Parallel Execution Thread delete
                    int thread_number = msg.arg1;
                    if (thread_number >= parallel_exe_threads.size()) {
                        Log.e(LOG_TAG, "Abort Parallel Exe Thread: Index out of bounds");
                        break;
                    }
                    parallel_exe_threads.remove(thread_number);
                    break;
                case 7: // Queue Timer Tick or Finish
                    if (game_info == null) break;
                    if (!game_info.game_running) break;
                    if (msg.getData().getString("type").equals("tick")) {
                        Map<String, GameFileLoader.GameVariable> mp = new HashMap<String, GameFileLoader.GameVariable>();
                        GameFileLoader.GameVariable v = loader.new_game_variable();
                        v.type = "string";
                        v.value = msg.getData().getString("name");
                        mp.put("name", v);
                        v = loader.new_game_variable();
                        v.type = "double";
                        v.value = Double.parseDouble(((Long) msg.getData().getLong("millisleft")).toString());
                        mp.put("millisleft", v);
                        QueueSignal(Signal.TIMER_TICK, mp);
                        if(msg.getData().getString("name").equals("GameTimer")){
                            game_info.game_time = ((Long)msg.getData().getLong("millisleft")).intValue()/1000;
                        }
                    } else if (msg.getData().getString("type").equals("finish")) {
                        Map<String, GameFileLoader.GameVariable> mp = new HashMap<String, GameFileLoader.GameVariable>();
                        GameFileLoader.GameVariable v = loader.new_game_variable();
                        v.type = "string";
                        v.value = msg.getData().getString("name");
                        mp.put("name", v);
                        QueueSignal(Signal.TIMER_OVER, mp);
                    }
                    break;
                case 8: // Queue FlagLost signal
                    if (game_info == null) break;
                    if (!game_info.game_running) break;
                    QueueSignal(Signal.FLAG_LOST, new HashMap<String, GameFileLoader.GameVariable>());
                    break;
                case 9: // Message from GameActivity
                    switch (msg.getData().getInt("Type")) {
                        case 0: // Start game
                            if (game_info != null && !game_info.game_running) {
                                game_info.game_running = true;
                                QueueSignal(Signal.GAME_STARTED, new HashMap<String, GameFileLoader.GameVariable>());
                                ((GameFileLoader.Timer) game_info.definitions.get("Timer_GameTimer").value).running = true;
                                ((GameFileLoader.Timer) game_info.definitions.get("Timer_GameTimer").value).timer
                                        = new CountDownTimer(((GameFileLoader.Timer) game_info.definitions.get("Timer_GameTimer").value).duration * 1000,
                                        1000) {
                                    @Override
                                    public void onTick(long millisUntilFinished) {
                                        Message msg = new Message();
                                        msg.what = 7;
                                        Bundle data = new Bundle();
                                        data.putString("type", "tick");
                                        data.putString("name", ((GameFileLoader.Timer) game_info.definitions.get("Timer_GameTimer").value).name);
                                        data.putLong("millisleft", millisUntilFinished);
                                        msg.setData(data);
                                        Singleton.getInstance().getGameLogicHandler().sendMessage(msg);
                                        Message n_msg = new Message();
                                        n_msg.what = 2;
                                        Bundle n_data = new Bundle();
                                        String new_game_time = String.format("%d:%02d:%02d", millisUntilFinished / 1000 / 3600,
                                                (millisUntilFinished / 1000 % 3600) / 60,
                                                millisUntilFinished / 1000 % 60);
                                        n_data.putString("GameTime", new_game_time);

                                        n_msg.setData(n_data);
                                        Singleton.getInstance().getGameActivityHandler().sendMessage(n_msg);
                                    }

                                    @Override
                                    public void onFinish() {
                                        /*Message msg = new Message();
                                        msg.what = 7;
                                        Bundle data = new Bundle();
                                        data.putString("type", "finish");
                                        data.putString("name", ((GameFileLoader.Timer) game_info.definitions.get("Timer_GameTimer").value).name);
                                        msg.setData(data);
                                        Singleton.getInstance().getGameLogicHandler().sendMessage(msg);*/ // the GameTimer shouldn't queue the TIMER_OVER signal
                                        game_info.game_running = false;
                                        ((GameFileLoader.Timer)game_info.definitions.get("Timer_GameTimer").value).running = false;
                                        QueueSignal(Signal.GAME_OVER, new HashMap<String, GameFileLoader.GameVariable>());
                                    }
                                }.start();
                            }
                            break;
                        case 1:{ // Halt complete game
                            if (game_info != null && game_info.game_running) {
                                Iterator it = game_info.definitions.entrySet().iterator();
                                while (it.hasNext()) {
                                    Map.Entry pair = (Map.Entry) it.next();
                                    if (pair.getKey().toString().startsWith("Timer_")) {
                                        if (!((GameFileLoader.Timer) ((GameFileLoader.Definition) pair.getValue()).value).running)
                                            continue;
                                        ((GameFileLoader.Timer) ((GameFileLoader.Definition) pair.getValue()).value).timer.cancel();
                                        ((GameFileLoader.Timer) ((GameFileLoader.Definition) pair.getValue()).value).running = false;
                                    }
                                }
                                Message smsg = new Message();
                                smsg.what=2;
                                Bundle msg_data = new Bundle();
                                String color_str="fblack\n";
                                msg_data.putByteArray("WriteData",color_str.getBytes());
                                smsg.setData(msg_data);
                                serial_handler.sendMessage(smsg);
                                /*for(ParallelGameExecutionThread thread : parallel_exe_threads){
                                    thread.interrupt();
                                }
                                parallel_exe_threads.clear();*/
                                game_info.game_running = false;
                            }
                            break;}
                        case 2:{ // Queue game initiated
                            if(game_info != null){
                                QueueSignal(Signal.GAME_INITIATED,new HashMap<String, GameFileLoader.GameVariable>());
                            }
                            break;}
                    }
                    break;
                case 10: {// invoke item
                    HashMap<String, GameFileLoader.GameVariable> dat = new HashMap<>();
                    if (running_items.containsKey(Integer.valueOf(msg.getData().getInt("ItemID")).toString() + "," + Integer.valueOf(msg.getData().getInt("ItemData")).toString())) {
                        if(DEBUG) Log.i(LOG_TAG, "Item (ID=" + msg.getData().getInt("ItemID") + ") is already running, break");
                        break;
                    }
                    GameFileLoader.GameVariable var = loader.new_game_variable();
                    var.type = "int";
                    var.value = msg.getData().getInt("ItemID");
                    dat.put("ItemID", var);
                    var = loader.new_game_variable();
                    var.type = "int";
                    var.value = msg.getData().getInt("ItemData");
                    dat.put("ItemData", var);
                    if (msg.getData().containsKey("DirectInvoke")) {
                        var = loader.new_game_variable();
                        var.type = "boolean";
                        var.value = msg.getData().getBoolean("DirectInvoke");
                        dat.put("DirectInvoke", var);
                    }
                    if (msg.getData().containsKey("ItemListID")) {
                        var = loader.new_game_variable();
                        var.type = "int";
                        var.value = msg.getData().getInt("ItemListID");
                        dat.put("ItemListID", var);
                    }
                    if (msg.getData().containsKey("CodeInvoke")) {
                        var = loader.new_game_variable();
                        var.type = "boolean";
                        var.value = msg.getData().getBoolean("CodeInvoke");
                        dat.put("CodeInvoke", var);
                    }
                    QueueSignal(Signal.INVOKE_ITEM, dat);
                    break;
                }
                case 11: { // get item
                    HashMap<String, GameFileLoader.GameVariable> dat = new HashMap<>();
                    GameFileLoader.GameVariable var = loader.new_game_variable();
                    var.type = "int";
                    var.value = msg.getData().getInt("ItemID");
                    dat.put("ItemID", var);
                    var = loader.new_game_variable();
                    var.type = "int";
                    var.value = msg.getData().getInt("ItemData");
                    dat.put("ItemData", var);
                    QueueSignal(Signal.GET_ITEM, dat);
                    break;
                }
                case 12: { // Queue Signal per Command
                    try {
                        QueueSignal(Signal.valueOf(msg.getData().getString("Signal")), new HashMap<String, GameFileLoader.GameVariable>());
                    } catch (IllegalArgumentException e) {
                        Log.e(LOG_TAG, "Command QUEUESIGNAL: No existing signal named '" + msg.getData().getString("Signal") + "'");
                    }
                    break;
                }
                case 13: { // Set weapon type from UI
                    game_info.game_variables.get("WeaponType").write_value(msg.getData().getInt("WeaponType"));
                    if(DEBUG) Log.i(LOG_TAG, "new WeaponType set: " + game_info.game_variables.get("WeaponType").read_value().toString());
                    break;
                }
                case 14: {// add weapon from GameConfigActivity
                    Integer index = msg.getData().getInt("Index");
                    if (game_info.definitions.containsKey("Weapon" + index.toString())) {
                        Log.e(LOG_TAG, "Tried adding a weapon with an index, that already exists. Quitting");
                        break;
                    }
                    String name = msg.getData().getString("Name");
                    Double frequency = msg.getData().getDouble("Frequency");
                    Integer range = msg.getData().getInt("Range");
                    Boolean allowed = msg.getData().getBoolean("Allowed");
                    Integer damagesign = msg.getData().getInt("DamageSign");
                    GameFileLoader.WeaponInfo weapon = loader.new_weapon_instance();
                    weapon.index = index;
                    weapon.name = name;
                    weapon.shot_frequency = frequency;
                    weapon.range = range;
                    weapon.allowed = allowed;
                    weapon.damage_sign = damagesign;

                    GameFileLoader.Definition def = loader.new_definition_instance();
                    def.type = "weapon";
                    def.value = weapon;
                    game_info.definitions.put("Weapon" + index.toString(), def);
                    break;
                }
                case 15: { // add team from GameConfigActivity
                    Integer index = msg.getData().getInt("Index");
                    if (game_info.definitions.containsKey("Team_" + index.toString())) {
                        Log.e(LOG_TAG, "Tried adding a team with an index, that already exists. Quitting");
                        break;
                    }
                    String name = msg.getData().getString("Name");
                    String color = msg.getData().getString("Color");
                    GameFileLoader.Team team = loader.new_team_instance();
                    team.name = name;
                    team.index = index;
                    team.color = color;

                    GameFileLoader.Definition def = loader.new_definition_instance();
                    def.type = "team";
                    def.value = team;
                    game_info.definitions.put("Team_" + index.toString(), def);
                    break;
                }
                case 16: { // Queue Player dead Signal
                    QueueSignal(Signal.PLAYER_DEAD, new HashMap<String,GameFileLoader.GameVariable>());
                    break;}
                case 17: { // Queue Flag_PICKUP
                    Integer teamid= msg.getData().getInt("TeamID");
                    if(teamid<0 || teamid > GameFileLoader.max_team_number){
                        Log.e(LOG_TAG,"Tried Queue FLAG_PICKUP signal with invalid TeamID!");
                        break;
                    }
                    HashMap<String,GameFileLoader.GameVariable> map = new HashMap<>();
                    GameFileLoader.GameVariable var = loader.new_game_variable();
                    var.type = "int";
                    var.value = teamid;
                    map.put("TeamID",var);
                    QueueSignal(Signal.FLAG_PICKUP,map);
                    break;}
            }
        }
    };

    //constructor
    public GameLogicThread(Handler parentHandler, Handler serialhandler) {
        // initialize instance vars
        this.parentHandler = parentHandler;
        this.serial_handler = serialhandler;
        loader.set_game_logic_handler(myThreadHandler);
    }


    public GameLogicThread(Context _context, Handler parentHandler) {
        this.parentHandler = parentHandler;
        this.context = _context;
        loader = new GameFileLoader(context);
    }

    @Override
    public void run() {
        super.run();
        try {
            // Thread loop
            while (thread_running) {
                SignalHandler();
            }
        } catch (Exception e) {
            // Logging exception
            Log.e(LOG_TAG, "Main loop exception - " + e);
        }
    }

    // getter for local Handler
    public Handler getHandler() {
        return myThreadHandler;
    }

    public void setSerialHandler(Handler serialhandler) {
        this.serial_handler = serialhandler;
        loader.set_serial_handler(this.serial_handler);
    }

    /**************************************************
     * ************************************************
     *  Signal Handler and corresponding functions  ***
     *************************************************/
    /**
     * Signals:
     * Shot received                    --> SHOT_RECEIVED
     * Trigger pulled                   --> TRIGGER_PULLED
     * Invoke Item                      --> INVOKE_ITEM
     * Game over                        --> GAME_OVER
     * Flag Pickup/Stronghold Owned     --> FLAG_PICKUP
     * Player Dead                      --> PLAYER_DEAD
     * Player Base Signal               --> PLAYER_BASE_SIGNAL
     * Recharge Button pressed          --> RECHARGE_BUTTON
     * Flag Lost/Stronghold abandoned   --> FLAG_LOST
     * Game Started                     --> GAME_STARTED
     * Game initiated(before game starts)-> GAME_INITIATED
     */
    public enum Signal {
        SHOT_RECEIVED, TRIGGER_PULLED, GET_ITEM, INVOKE_ITEM, GAME_OVER,
        FLAG_PICKUP, PLAYER_DEAD, PLAYER_RESURRECTED, PLAYER_BASE_SIGNAL, RECHARGE_BUTTON,
        FLAG_LOST, GAME_STARTED, GAME_INITIATED, TIMER_OVER, TIMER_TICK, GAME_COMMAND
    }

    private class SIGNAL_CLASS {
        Signal signal;
        Map<String, GameFileLoader.GameVariable> data;

        public SIGNAL_CLASS(Signal sig, Map<String, GameFileLoader.GameVariable> dat) {
            this.signal = sig;
            this.data = dat;
        }
    }

    private List<SIGNAL_CLASS> queued_signals = new ArrayList<SIGNAL_CLASS>();


    private Bundle GameVariables;

    private void SignalHandler() {
        try {
            if (queued_signals.size() > 0) {
                SIGNAL_CLASS s = queued_signals.get(0);
                if (!game_info.signal_code.containsKey(s.signal.toString())) {
                    if(DEBUG) Log.i(LOG_TAG, "Unregistered Signal " + s.signal.toString() + " queued --> doing nothing");
                    queued_signals.remove(0);
                    return;
                }
                if(!game_info.game_running && s.signal!=Signal.GAME_INITIATED && s.signal!=Signal.GAME_STARTED){
                    queued_signals.remove(0);
                    return;
                }
                if (game_info.signal_code.get(s.signal.toString())._static
                        && game_info.signal_code.get(s.signal.toString()).running) {
                    queued_signals.remove(0);
                    return;
                } else {
                    game_info.signal_code.get(s.signal.toString()).running = true;
                }
                if (s.signal.toString().equals("GET_ITEM")) {
                    Message msg = new Message();
                    msg.what = 4;
                    msg.obj = game_info;
                    Bundle data = new Bundle();
                    data.putInt("Type", 0);
                    data.putInt("ItemID", (Integer) s.data.get("ItemID").value);
                    data.putInt("ItemData", (Integer) s.data.get("ItemData").value);
                    msg.setData(data);
                    Singleton.getInstance().getGameActivityHandler().sendMessage(msg);
                } else if (s.signal.toString().equals("INVOKE_ITEM")) {
                    running_items.put(s.data.get("ItemID").value.toString() + "," + s.data.get("ItemData").value.toString(), true);
                    if (s.data.containsKey("CodeInvoke")) {
                        Message ci_msg = new Message();
                        ci_msg.what = 4;
                        ci_msg.obj = game_info;
                        Bundle ci_data = new Bundle();
                        ci_data.putInt("Type", 2);
                        ci_data.putInt("ItemID", (Integer) s.data.get("ItemID").value);
                        ci_data.putInt("ItemData", (Integer) s.data.get("ItemData").value);
                        ci_data.putInt("ItemListID", -1);
                        ci_msg.setData(ci_data);
                        Singleton.getInstance().getGameActivityHandler().sendMessage(ci_msg);
                    }
                    invoke_default_item_behavior((Integer) s.data.get("ItemID").value, (Integer) s.data.get("ItemData").value);
                } else if(s.signal.toString().equals("FLAG_PICKUP")){
                    if(game_info.flag_team_ID!=-1) {
                        queued_signals.remove(0);
                        return;
                    }
                    game_info.flag_team_ID = (Integer)s.data.get("TeamID").value;
                    Message f_msg = new Message();
                    f_msg.what = 6;
                    Bundle f_data = new Bundle();
                    Integer teamID = (Integer)s.data.get("TeamID").value;
                    game_info.flag_team_ID = teamID;
                    f_data.putString("Color",((GameFileLoader.Team)game_info.definitions.get("Team_"+teamID.toString()).value).color);
                    f_msg.setData(f_data);
                    Singleton.getInstance().getGameActivityHandler().sendMessage(f_msg);
                } else if(s.signal.toString().equals("FLAG_LOST")){
                    if(game_info.flag_team_ID==-1){
                        queued_signals.remove(0);
                        return;
                    }
                    game_info.flag_team_ID = -1;
                    Message f_msg = new Message();
                    f_msg.what = 7;
                    Singleton.getInstance().getGameActivityHandler().sendMessage(f_msg);
                }

                UpdateStats(s.signal);

                if (game_info.signal_code.get(s.signal.toString()).parallel) {
                    ParallelGameExecutionThread temp_thread =
                            new ParallelGameExecutionThread(game_info.signal_code.get(s.signal.toString()), s.data, s.signal.toString(), myThreadHandler);
                    temp_thread.start();
                    //execute_code(game_info.signal_code.get(s.signal.toString()).commands,s.data);
                } else {
                    execute_code(game_info.signal_code.get(s.signal.toString()).commands, s.data);
                    game_info.signal_code.get(s.signal.toString()).running = false;
                    Message b_msg = new Message();
                    b_msg.what = 1;
                    b_msg.obj = game_info;
                    Singleton.getInstance().getGameActivityHandler().sendMessage(b_msg);
                }
                queued_signals.remove(0);
            }
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "SignalHandler - " + e);
            queued_signals.remove(0);
        }
    }

    private void QueueSignal(Signal type, Map<String, GameFileLoader.GameVariable> data) {
        SIGNAL_CLASS temp_signal = new SIGNAL_CLASS(type, data);
        queued_signals.add(temp_signal);
    }

    private void execute_code(List<GameFileLoader.Code> code, Map<String, GameFileLoader.GameVariable> data) {
        for (GameFileLoader.Code cmd : code) {
            if (!cmd.execute(data)) return;
        }
    }

    private Map<String, GameFileLoader.GameVariable> process_shot_info(int receiver_ID, int high_byte, int low_byte) {
        int team = (high_byte & 0b01100000) >> 5;
        int damage = (high_byte & 0b00011110) >> 1;
        int player_id = (low_byte & 0b11111000) >> 3;
        int weapon_type = (low_byte & 0b00000110) >> 1;

        for(int i=0;i<GameFileLoader.mapping_array_size-1;i++){
            if(damage>=game_info.damage_mapping.get(i) && damage<=game_info.damage_mapping.get(i+1)){
                if(damage>=game_info.damage_mapping.get(i) && damage<game_info.damage_mapping.get(i)+0.5*(game_info.damage_mapping.get(i+1)-game_info.damage_mapping.get(i))){
                    damage = game_info.damage_mapping.get(i);
                    break;
                } else {
                    damage = game_info.damage_mapping.get(i+1);
                    break;
                }
            }
        }
        //Log.i(LOG_TAG,"Shot: Team "+String.valueOf(team)+"  Damage "+String.valueOf(damage)+"  ID "+String.valueOf(player_id)+"  Weapon "+String.valueOf(weapon_type));
        Map<String, GameFileLoader.GameVariable> mp = new HashMap<String, GameFileLoader.GameVariable>();
        GameFileLoader.GameVariable var = loader.new_game_variable();
        var.type = "int";
        var.value = team;
        mp.put("shot_team", var);
        var = loader.new_game_variable();
        var.type = "int";
        var.value = damage;
        mp.put("shot_damage", var);
        var = loader.new_game_variable();
        var.type = "int";
        var.value = player_id;
        mp.put("shot_playerID", var);
        var = loader.new_game_variable();
        var.type = "int";
        var.value = weapon_type;
        mp.put("shot_weapon_type", var);
        var = loader.new_game_variable();
        var.type = "int";
        var.value = receiver_ID;
        mp.put("receiver_ID", var);
        return mp;
    }

    private Map<String, GameFileLoader.GameVariable> process_game_command(int high_byte, int low_byte) {
        Map<String, GameFileLoader.GameVariable> mp = new HashMap<String, GameFileLoader.GameVariable>();
        int command_ID = (high_byte & 0b01111100) >> 3;
        int command_data = ((high_byte & 0b00000010) << 6) | (low_byte & 0b11111110) >> 1;
        GameFileLoader.GameVariable var = loader.new_game_variable();
        var.type = "int";
        var.value = command_ID;
        mp.put("CommandID", var);
        var = loader.new_game_variable();
        var.type = "int";
        var.value = command_data;
        mp.put("CommandData", var);
        switch (command_ID) {
            case 1: { // Game Over Signal
                var = loader.new_game_variable();
                var.type = "string";
                var.value = "GAME_OVER";
                mp.put("Signal",var);
                break;}
            case 2: { // Game Started Signal
                var = loader.new_game_variable();
                var.type = "string";
                var.value = "GAME_STARTED";
                mp.put("Signal",var);
                break;}
            case 3: { // Player Base Signal
                int team_ID = (command_data & 0b0000000111100000) >> 5; // 4 bit team ID for better compatibility with possible extensions of team capability
                int base_ID = (command_data & 0b0000000000011110) >> 1;
                var = loader.new_game_variable();
                var.type = "string";
                var.value = "PLAYER_BASE_SIGNAL";
                mp.put("Signal", var);
                var = loader.new_game_variable();
                var.type = "int";
                var.value = team_ID;
                mp.put("BaseTeamID", var);
                var = loader.new_game_variable();
                var.type = "int";
                var.value = base_ID;
                mp.put("BaseID", var);
            }
            case 4: { // Get Item
                int item_id = (command_data & 0b0000000111110000) >> 4;
                int item_data = command_data & 0b0000000000001111;
                var = loader.new_game_variable();
                var.type = "string";
                var.value = "GET_ITEM";
                mp.put("Signal", var);
                var = loader.new_game_variable();
                var.type = "int";
                var.value = item_id;
                mp.put("ItemID", var);
                var = loader.new_game_variable();
                var.type = "int";
                var.value = item_data;
                mp.put("ItemData", var);
            }
            case 5: { // Flag Pickup
                Integer team_ID = (command_data & 0b0000000111100000) >> 5;
                var = loader.new_game_variable();
                var.type = "string";
                var.value = "FLAG_PICKUP";
                mp.put("Signal", var);
                var = loader.new_game_variable();
                var.type = "int";
                var.value = team_ID;
                mp.put("TeamID", var);
            }
            case 6: { // Flag Lost
                int team_ID = (command_data & 0b0000000111100000) >> 5;
                int all_lost = (command_data & 0b0000000000010000) >> 4;
                var = loader.new_game_variable();
                var.type = "string";
                var.value = "FLAG_LOST";
                mp.put("Signal", var);
                if (all_lost == 1) {
                    var = loader.new_game_variable();
                    var.type = "int";
                    var.value = 0;
                    mp.put("FlagNumber", var);
                } else {
                    var = loader.new_game_variable();
                    var.type = "int";
                    var.value = team_ID;
                    mp.put("FlagTeamID0", var);
                    var = loader.new_game_variable();
                    var.type = "int";
                    var.value = 1;
                    mp.put("FlagNumber", var);
                }
            }
            default: {
                var = loader.new_game_variable();
                var.type = "string";
                var.value = "GAME_COMMAND";
                mp.put("Signal", var);
            }
        }
        return mp;
    }

    public void invoke_default_item_behavior(Integer item_ID, Integer item_data) {
        switch (item_ID) {
            case 0: //ExtraLife
                if ((Integer) game_info.game_variables.get("ExtraLifes").read_value() >= (Integer) game_info.game_variables.get("max_ExtraLifes").read_value())
                    break;
                game_info.game_variables.get("ExtraLifes").write_value((Integer) game_info.game_variables.get("ExtraLifes").read_value() + 1);
                break;
            case 1: // Health
                game_info.game_variables.get("LifePoints").write_value((Integer) game_info.game_variables.get("LifePoints").read_value() + game_info.damage_mapping.get(item_data));
                if ((Integer) game_info.game_variables.get("LifePoints").read_value() > (Integer) game_info.game_variables.get("max_LifePoints").read_value()) {
                    game_info.game_variables.get("LifePoints").write_value(game_info.game_variables.get("max_LifePoints").read_value());
                }
                break;
            case 2: // Shield
                game_info.game_variables.get("ShieldPoints").write_value((Integer) game_info.game_variables.get("ShieldPoints").read_value() + game_info.damage_mapping.get(item_data));
                if ((Integer) game_info.game_variables.get("ShieldPoints").read_value() > (Integer) game_info.game_variables.get("max_ShieldPoints").read_value()) {
                    game_info.game_variables.get("ShieldPoints").write_value(game_info.game_variables.get("max_ShieldPoints").read_value());
                }
                break;
            case 3: // AmmoPack
                game_info.game_variables.get("AmmoPacks").write_value((Integer) game_info.game_variables.get("AmmoPacks").read_value() + 1);
                if ((Integer) game_info.game_variables.get("AmmoPacks").read_value() > (Integer) game_info.game_variables.get("AmmoPacks").read_value()) {
                    game_info.game_variables.get("AmmoPacks").write_value(game_info.game_variables.get("AmmoPacks").read_value());
                }
                break;
            case 4: // WeaponFound
                if (!game_info.definitions.containsKey("Weapon" + item_data.toString())) break;
                ((GameFileLoader.WeaponInfo) game_info.definitions.get("Weapon" + item_data.toString()).value).allowed = true;
                break;
            case 5: // Resurrection
                if ((Integer) game_info.game_variables.get("LifePoints").read_value() == 0) {
                    game_info.game_variables.get("LifePoints").write_value(game_info.game_variables.get("max_LifePoints").read_value());
                    game_info.game_variables.get("ShieldPoints").write_value(game_info.game_variables.get("max_ShieldPoints").read_value());
                    QueueSignal(Signal.PLAYER_RESURRECTED, new HashMap<String, GameFileLoader.GameVariable>());
                }
                break;
            case 6: // Invincible
                game_info.invincible = true;
                new CountDownTimer(game_info.duration_mapping.get(item_data) * 1000, game_info.duration_mapping.get(item_data) * 1000) {

                    public void onTick(long millisUntilFinished) {
                    }

                    public void onFinish() {
                        game_info.invincible = false;
                    }
                }.start();
                break;
            case 7: // Trap
                if (game_info.invincible) break;

                Integer Shield_rest;
                if (game_info.damage_mapping.get(item_data) <= (Integer) game_info.game_variables.get("ShieldPoints").read_value()) {
                    game_info.game_variables.get("ShieldPoints").write_value((Integer) game_info.game_variables.get("ShieldPoints").read_value() - game_info.damage_mapping.get(item_data));
                } else {
                    Shield_rest = (Integer) game_info.game_variables.get("ShieldPoints").read_value();
                    game_info.game_variables.get("ShieldPoints").write_value(0);
                    if (game_info.damage_mapping.get(item_data) - Shield_rest <= (Integer) game_info.game_variables.get("LifePoints").read_value()) {
                        game_info.game_variables.get("LifePoints").write_value((Integer) game_info.game_variables.get("LifePoints").read_value() - game_info.damage_mapping.get(item_data) + Shield_rest);
                    } else {
                        game_info.game_variables.get("LifePoints").write_value(0);
                        QueueSignal(Signal.PLAYER_DEAD, new HashMap<String, GameFileLoader.GameVariable>());
                    }
                }
                StatsManager.getInstance().AddPointToCurrentStat("Damage",game_info.damage_mapping.get(item_data).doubleValue(),game_info.game_time);
                break;
            case 8: // TrapForShield
                if (game_info.invincible) break;
                if ((Integer) game_info.game_variables.get("ShieldPoints").read_value() > game_info.damage_mapping.get(item_data)) {
                    game_info.game_variables.get("ShieldPoints").write_value((Integer) game_info.game_variables.get("ShieldPoints").read_value() - game_info.damage_mapping.get(item_data));
                } else {
                    game_info.game_variables.get("ShieldPoints").write_value(0);
                }
                StatsManager.getInstance().AddPointToCurrentStat("Damage",game_info.damage_mapping.get(item_data).doubleValue(),game_info.game_time);
                break;
            case 9: // TrapForHealth
                if (game_info.invincible) break;
                if ((Integer) game_info.game_variables.get("LifePoints").read_value() > game_info.damage_mapping.get(item_data)) {
                    game_info.game_variables.get("LifePoints").write_value((Integer) game_info.game_variables.get("LifePoints").read_value() - game_info.damage_mapping.get(item_data));
                } else {
                    game_info.game_variables.get("LifePoints").write_value(0);
                    QueueSignal(Signal.PLAYER_DEAD, new HashMap<String, GameFileLoader.GameVariable>());
                }
                StatsManager.getInstance().AddPointToCurrentStat("Damage",game_info.damage_mapping.get(item_data).doubleValue(),game_info.game_time);
                break;
            case 10: { // Spy 30s
                if (!game_info.definitions.containsKey("Team_" + item_data.toString())) break;

                Message msg = new Message();
                msg.what = 2;
                Bundle msg_data = new Bundle();
                String color_str = "f" + ((GameFileLoader.Team) game_info.definitions.get("Team_" + item_data.toString()).value).color + "\n";
                msg_data.putByteArray("WriteData", color_str.getBytes());
                msg.setData(msg_data);
                serial_handler.sendMessage(msg);
                new CountDownTimer(30 * 1000, 30 * 1000) {

                    public void onTick(long millisUntilFinished) {
                    }

                    public void onFinish() {
                        Message msg = new Message();
                        msg.what = 2;
                        Bundle msg_data = new Bundle();
                        String color_str = "f" + ((GameFileLoader.Team) game_info.definitions.get("Team_" + game_info.game_variables.get("TeamID").read_value().toString()).value).color + "\n";
                        msg_data.putByteArray("WriteData", color_str.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                    }
                }.start();
                break;
            }
            case 11: { // Camouflage (duration from item data)
                Message msg = new Message();
                msg.what = 2;
                Bundle msg_data = new Bundle();
                String color_str = "fblack\n";
                msg_data.putByteArray("WriteData", color_str.getBytes());
                msg.setData(msg_data);
                serial_handler.sendMessage(msg);
                new CountDownTimer(game_info.duration_mapping.get(item_data) * 1000, game_info.duration_mapping.get(item_data) * 1000) {

                    public void onTick(long millisUntilFinished) {
                    }

                    public void onFinish() {
                        Message msg = new Message();
                        msg.what = 2;
                        Bundle msg_data = new Bundle();
                        String color_str = "f" + ((GameFileLoader.Team) game_info.definitions.get("Team_" + game_info.game_variables.get("TeamID").read_value().toString()).value).color + "\n";
                        msg_data.putByteArray("WriteData", color_str.getBytes());
                        msg.setData(msg_data);
                        serial_handler.sendMessage(msg);
                    }
                }.start();
                break;
            }
        }
    }


    /**
     * ***********************************
     * ParallelGameExecutionThread
     **************************************/
    private class ParallelGameExecutionThread extends Thread {
        private String LOG_TAG = "ParallelExeThread";
        private GameFileLoader.SignalCode code;
        private Map<String, GameFileLoader.GameVariable> signal_data;
        private Handler parent_Handler;
        private String signal_name;

        @Override
        public void run() {
            super.run();
            try {
                execute_code(code.commands, signal_data);
                code.running = false;
                Message b_msg = new Message();
                b_msg.what = 1;
                b_msg.obj = game_info;
                Singleton.getInstance().getGameActivityHandler().sendMessage(b_msg);
            } catch (Exception e) {
                // Logging exception
                Log.e(LOG_TAG, "Execute parallel code exception - " + e);
            }
        }

        public ParallelGameExecutionThread(GameFileLoader.SignalCode _signal_code, Map<String, GameFileLoader.GameVariable> _signal_data, String signal, Handler _parent_handler) {
            this.code = _signal_code;
            this.signal_data = _signal_data;
            this.parent_Handler = _parent_handler;
            this.signal_name = signal;
        }
    }


    /****************************************
     * Stat Measuring Code
     *************************************/

    // Method Definitions
    private void UpdateStats(Signal signal){
        switch (signal){
            case SHOT_RECEIVED:
                StatsManager.getInstance().AddPointToCurrentStat("ReceivedShots",1.0,game_info.game_time);
                break;
            case INVOKE_ITEM:
                StatsManager.getInstance().AddPointToCurrentStat("InvokedItems",1.0,game_info.game_time);
                break;
            case TRIGGER_PULLED:
                if(game_info.tagger_enabled){
                    StatsManager.getInstance().AddPointToCurrentStat("FiredShots",1.0,game_info.game_time);
                }
                break;
            case PLAYER_DEAD:
                StatsManager.getInstance().AddPointToCurrentStat("Deaths",1.0,game_info.game_time);
                break;
            case GAME_OVER:
                Message msg = new Message();
                msg.what = 3;
                Singleton.getInstance().getGameActivityHandler().sendMessage(msg);
        }
    }
}
