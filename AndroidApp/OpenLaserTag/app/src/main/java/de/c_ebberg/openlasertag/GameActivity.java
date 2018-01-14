package de.c_ebberg.openlasertag;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class GameActivity extends BaseActivity implements AdapterView.OnItemSelectedListener {
    private static final String LOG_TAG="GameActivity";
    private CountDownTimer pre_countdown_timer=null;
    private ItemList invoked_items = new ItemList();
    private ArrayList<String> allowed_weapons = new ArrayList<>();
    private ArrayAdapter<String> allowed_weapons_adapter;
    private MessageList message_list;
    private MessageAdapter message_adapter;
    private ArrayList<EndStatsItem> endstatslist;
    private EndStatsAdapter endstatsadapter;
    private GameFileLoader.GameInformation game_info=null;

    public class ItemList extends ArrayList<Invoked_Item>{
        private Integer id_counter=0;

        public Integer get_new_ID(){
            return id_counter++;
        }
    }

    public class Invoked_Item{
        Integer show_duration=2;
        Boolean invoked=false;
        Integer item_ID;
        Integer item_data;
        Integer item_number=1;
        ItemStackView host=null;
        GridLayout grid;
        CountDownTimer timer=null;

        Integer list_ID;
    }

    class EndStatsItem{
        String name;
        Double value;
        EndStatsItem(String _name, Double _value){name=_name;value=_value;}
    }

    class EndStatsAdapter extends ArrayAdapter<EndStatsItem>{
        ArrayList<EndStatsItem> modelsArrayList;
        Context context;
        public EndStatsAdapter(Context context, ArrayList<EndStatsItem> modelsArrayList) {
            super(context, R.layout.list_item_endgame_stats, modelsArrayList);

            this.context = context;
            this.modelsArrayList = modelsArrayList;
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final StatsViewHolder mViewHolder;
            View v = convertView;

            if(v==null) {
                mViewHolder = new StatsViewHolder();
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                v = inflater.inflate(R.layout.list_item_endgame_stats, parent, false);
                mViewHolder.value = (TextView) v.findViewById(R.id.list_item_endgame_stats_value);
                mViewHolder.name = (TextView) v.findViewById(R.id.list_item_endgame_stats_name);

                v.setTag(mViewHolder);
            } else {
                mViewHolder = (StatsViewHolder) v.getTag();
            }

            mViewHolder.value.setText(String.format(Locale.getDefault(),"%f",modelsArrayList.get(position).value));
            mViewHolder.name.setText(modelsArrayList.get(position).name);

            // 5. return rowView
            return v;
        }
    }

    static class StatsViewHolder{
        private TextView name;
        private TextView value;
    }

    public class MessageList extends ArrayList<GameMessage>{
        Integer ID_counter=0;

        public Integer createNewID(){
            return ID_counter++;
        }
    }

    private enum GameMessageType{NORMAL,WARNING,INFO}

    public class GameMessage {
        Integer ID=-1;
        String text;
        GameMessageType type;
        Integer duration;

        public GameMessage(String _text, GameMessageType _type, Integer _duration, Integer _ID){
            text = _text;
            type = _type;
            duration = _duration;
            ID=_ID;
        }
    }

    public class MessageAdapter extends ArrayAdapter<GameMessage>{
        ArrayList<GameMessage> modelsArrayList;
        Context context;
        public MessageAdapter(Context context, ArrayList<GameMessage> modelsArrayList) {
            super(context, R.layout.list_item_file_list, modelsArrayList);

            this.context = context;
            this.modelsArrayList = modelsArrayList;
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final ViewHolder mViewHolder;
            View v = convertView;

            if(v==null) {
                mViewHolder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                v = inflater.inflate(R.layout.list_item_message_list, parent, false);
                mViewHolder.textview = (TextView) v.findViewById(R.id.game_messages_item_textview);
                mViewHolder.iconview = (ImageView) v.findViewById(R.id.game_messages_item_icon);

                v.setTag(mViewHolder);
            } else {
                mViewHolder = (ViewHolder) v.getTag();
            }
            // 3. Get icon,title & counter views from the rowView
            //ImageView imgView = (ImageView) rowView.findViewById(R.id.item_icon);
            //TextView titleView = (TextView) rowView.findViewById(R.id.item_textview);

            // 4. Set the text for textView
            switch (modelsArrayList.get(position).type){
                default:
                case NORMAL:
                    mViewHolder.iconview.setImageResource(R.drawable.ic_messages);
                    break;
                case WARNING:
                    mViewHolder.iconview.setImageResource(R.drawable.ic_warning);
                    break;
                case INFO:
                    mViewHolder.iconview.setImageResource(R.drawable.ic_info);
                    break;
            }
            mViewHolder.textview.setText(modelsArrayList.get(position).text);

            // 5. return rowView
            return v;
        }
    }

    static class ViewHolder{
        private TextView textview;
        private ImageView iconview;
    }

    public Handler mainHandler = new Handler() {
        private CustomProcessBar points_bars=null;
        private TextView extra_lifes_view=null;
        private TextView ammo_packs_view=null;
        private TextView game_timer_view=null;

        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 0:{ // Update complete game info UI representation
                    if(msg.obj==null) break;
                    GameFileLoader.GameInformation gi = (GameFileLoader.GameInformation) msg.obj;

                    allowed_weapons.clear();
                    for(Integer i=0;i<4;i++){
                        if(gi.definitions.containsKey("Weapon"+i.toString())){
                            if(((GameFileLoader.WeaponInfo)gi.definitions.get("Weapon"+i.toString()).value).allowed) {
                                allowed_weapons.add("("+((GameFileLoader.WeaponInfo) gi.definitions.get("Weapon" + i.toString()).value).index.toString()+") "+((GameFileLoader.WeaponInfo)gi.definitions.get("Weapon"+i.toString()).value).name);
                            }
                        }
                    }
                    allowed_weapons_adapter.notifyDataSetChanged();
                    Integer weapon_pos =0;
                    for(Integer i=0;i<allowed_weapons.size();i++){
                        if(Integer.valueOf(Integer.parseInt(allowed_weapons.get(i).substring(allowed_weapons.get(i).indexOf("(")+1,allowed_weapons.get(i).indexOf(")")))).equals((Integer)gi.game_variables.get("WeaponType").read_value())){
                            weapon_pos = i;
                            break;
                        }
                    }
                    ((Spinner)findViewById(R.id.game_weapon_select_spinner)).setSelection(weapon_pos);

                    break;}
                case 1: { // update simple stats UI representation
                    if (msg.obj == null) break;
                    GameFileLoader.GameInformation info = (GameFileLoader.GameInformation) msg.obj;
                    if(points_bars==null)
                        points_bars = (CustomProcessBar)findViewById(R.id.game_points_bar);
                    if(ammo_packs_view==null)
                        ammo_packs_view = (TextView)findViewById(R.id.ammo_packs_text);
                    if(extra_lifes_view==null)
                        extra_lifes_view = (TextView)findViewById(R.id.extra_lifes_text);

                    points_bars.setValue(0,((Integer)info.game_variables.get("LifePoints").read_value()).doubleValue());
                    points_bars.setValue(1,((Integer)info.game_variables.get("ShieldPoints").read_value()).doubleValue());
                    points_bars.setValue(2,((Integer)info.game_variables.get("AmmoPoints").read_value()).doubleValue());
                    extra_lifes_view.setText(info.game_variables.get("ExtraLifes").read_value().toString());
                    ammo_packs_view.setText(info.game_variables.get("AmmoPacks").read_value().toString());
                    break;}
                case 2: { // Update Game Timer UI
                    if(game_timer_view==null)
                        game_timer_view = (TextView)findViewById(R.id.game_time_text);

                    game_timer_view.setText(msg.getData().getString("GameTime"));
                    break;}
                case 3: { // Show Game Over overlay
                    ((TextView)findViewById(R.id.countdown_overlay_text)).setText(getResources().getString(R.string.GameOver));
                    ((TextView)findViewById(R.id.countdown_overlay_text)).setTextColor(Color.RED);
                    ((RelativeLayout)findViewById(R.id.countdown_overlay_layout)).setVisibility(View.VISIBLE);
                    try {
                        StatsManager.getInstance().CommitCurrentGameStats(GameActivity.this);
                    } catch(IOException e){
                        Log.e(LOG_TAG,"IOException during Stats Commit!");
                    }
                    new CountDownTimer(3000, 3000) {

                        public void onTick(long millisUntilFinished) {

                        }

                        public void onFinish() {
                            if(Singleton.getInstance().getGameActivityStatus()== null) return;
                            ((RelativeLayout)findViewById(R.id.countdown_overlay_layout)).setVisibility(View.GONE);
                            ((TextView)findViewById(R.id.game_time_text)).setText(getResources().getString(R.string.GameOver));
                            Singleton.getInstance().getGameActivityStatus().game_over = true;

                            if(!game_info.player_stats.isEmpty()) {
                                findViewById(R.id.game_item_scroll_view).setVisibility(View.GONE);
                                findViewById(R.id.game_messages_listview).setVisibility(View.GONE);
                                findViewById(R.id.game_weapon_select_label).setVisibility(View.GONE);
                                findViewById(R.id.game_weapon_select_spinner).setVisibility(View.GONE);
                                findViewById(R.id.game_endstats_layout).setVisibility(View.VISIBLE);
                                fill_end_stats_list();
                            }
                        }
                    }.start();

                    break;}
                case 4: { // item icon update
                    switch(msg.getData().getInt("Type")){
                        case 0: { // add new Item
                            final GameFileLoader.GameInformation gi = (GameFileLoader.GameInformation) msg.obj;
                            Integer item_id=msg.getData().getInt("ItemID");
                            Integer item_data=msg.getData().getInt("ItemData");
                            Integer item_list_id=-1;
                            Integer item_list_pos =-1;
                            for(Integer i=0;i<invoked_items.size();i++){
                                if(invoked_items.get(i).item_ID.equals(item_id)
                                        && invoked_items.get(i).item_data.equals(item_data)){
                                    item_list_id = invoked_items.get(i).list_ID;
                                    item_list_pos = i;
                                    break;
                                }
                            }
                            if(item_list_id.equals(-1)) {
                                Invoked_Item item = new Invoked_Item();
                                item.item_ID = item_id;
                                item.item_data = item_data;
                                item.list_ID = invoked_items.get_new_ID();
                                item_list_id = item.list_ID;
                                invoked_items.add(item);
                                item_list_pos = invoked_items.size()-1;
                            }

                            if(invoked_items.get(item_list_pos).host!=null){
                                invoked_items.get(item_list_pos).host.increaseNumber(1);
                                invoked_items.get(item_list_pos).item_number++;
                                break;
                            }

                            ItemStackView item_button = new ItemStackView(GameActivity.this);
                            Resources.Theme themes = getTheme();
                            TypedValue storedValueInTheme = new TypedValue();
                            if (themes.resolveAttribute(R.attr.colorForegroundPrimary, storedValueInTheme, true)) {
                                item_button.setValueColor(storedValueInTheme.data);
                            }
                            storedValueInTheme = new TypedValue();
                            if(themes.resolveAttribute(R.attr.colorBackground1, storedValueInTheme, true)){
                                item_button.setBackgroundColor(storedValueInTheme.data);
                            }
                            item_button.setValueText(getItemValueText(gi,item_id, item_data));
                            item_button.setItemID(item_id);
                            item_button.setItemListID(item_list_id);

                            if(gi.definitions.containsKey("Item_"+msg.getData().getInt("ItemID"))){ // user defined
                                Glide.with(GameActivity.this).load(Singleton.getInstance().getUserFilesPath()+ File.separator+"UserIcons"+File.separator+((GameFileLoader.Item)gi.definitions.get("Item_"+msg.getData().getInt("ItemID")).value).icon_path).into(item_button);
                                invoked_items.get(item_list_pos).show_duration = ((GameFileLoader.Item)gi.definitions.get("Item_"+item_id.toString()).value).invoke_duration;
                            } else { // predefined
                                int res_id=0;
                                switch(msg.getData().getInt("ItemID")){
                                    case 0:
                                        res_id = R.drawable.ic_1up;
                                        break;
                                    case 1:
                                        res_id = R.drawable.ic_heart;
                                        break;
                                    case 2:
                                        res_id = R.drawable.ic_shield;
                                        break;
                                    case 3:
                                        res_id = R.drawable.ic_ammo_pack;
                                        break;
                                    case 4:
                                        res_id = R.drawable.ic_lasergun_plus;
                                        break;
                                    case 5:
                                        res_id = R.drawable.ic_phoenix;
                                        break;
                                    case 6:
                                        res_id = R.drawable.ic_invincible;
                                        break;
                                    case 7:
                                    case 8:
                                    case 9:
                                        res_id = R.drawable.ic_trap;
                                        break;
                                    case 10:
                                        res_id = R.drawable.ic_spy;
                                        break;
                                    case 11:
                                        res_id = R.drawable.ic_cloak;
                                        break;
                                    default:
                                        res_id = R.drawable.ic_red_cross;
                                        break;
                                }
                                //item_button.setImageResource(res_id);
                                item_button.setImageDrawable(VectorDrawableCompat.create(getResources(),res_id,getTheme()));
                                invoked_items.get(item_list_pos).show_duration = getItemDuration(item_id, item_data,gi);
                            }
                            item_button.setPadding(0,0,0,0);
                            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                            params.height = 120;
                            params.width = 120;
                            item_button.setLayoutParams(params);
                            item_button.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            GridLayout grid = (GridLayout)findViewById(R.id.game_item_layout);
                            grid.addView(item_button);
                            invoked_items.get(item_list_pos).grid = grid;
                            invoked_items.get(item_list_pos).host = item_button;

                            item_button.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if(Singleton.getInstance().getGameActivityStatus().game_over) return;
                                    ItemStackView stack = (ItemStackView) v;
                                    Message msg = new Message();
                                    msg.what = 10;
                                    Bundle dat = new Bundle();
                                    Integer pos=0;
                                    for(Integer i=0;i<invoked_items.size();i++){
                                        if(invoked_items.get(i).list_ID.equals(stack.getItemListID())){
                                            pos = i;
                                            break;
                                        }
                                    }
                                    if(Singleton.getInstance().getGameLogicThread().running_items
                                            .containsKey(invoked_items.get(pos).item_ID.toString()+","+invoked_items.get(pos).item_data.toString())){
                                        return;
                                    }
                                    dat.putInt("ItemID",invoked_items.get(pos).item_ID);
                                    dat.putInt("ItemData",invoked_items.get(pos).item_data);
                                    msg.setData(dat);
                                    Singleton.getInstance().getGameLogicHandler().sendMessage(msg);
                                    Message a_msg = new Message();
                                    a_msg.what = 4;
                                    a_msg.obj = gi;
                                    Bundle a_data = new Bundle();
                                    a_data.putInt("Type",2);
                                    a_data.putBoolean("DirectInvoke",true);
                                    a_data.putInt("ItemID",invoked_items.get(pos).item_ID);
                                    a_data.putInt("ItemData",invoked_items.get(pos).item_data);
                                    a_data.putInt("ItemListID",invoked_items.get(pos).list_ID);
                                    a_msg.setData(a_data);
                                    Singleton.getInstance().getGameActivityHandler().sendMessage(a_msg);
                                }
                            });

                            if(invoked_items.size()>12){
                                grid.removeView(invoked_items.get(0).host);
                                invoked_items.remove(invoked_items.get(0));
                            }
                            break;}
                        case 1:{ // remove Item
                            GameFileLoader.GameInformation gi = (GameFileLoader.GameInformation) msg.obj;
                            GridLayout grid = (GridLayout)findViewById(R.id.game_item_layout);
                            Integer item_list_id = msg.getData().getInt("ItemListID");
                            Integer item_list_pos=-1;
                            if(!item_list_id.equals(-1)) {
                                for (Integer i = 0; i < invoked_items.size(); i++) {
                                    if (invoked_items.get(i).list_ID.equals(item_list_id)) {
                                        item_list_pos = i;
                                        if(invoked_items.get(item_list_pos).host==null){ // if host==null due to conf change, queue new remove message
                                            Message r_msg = new Message();
                                            r_msg.what = 4;
                                            r_msg.obj = gi;
                                            Bundle r_data = new Bundle();
                                            r_data.putInt("Type",1);
                                            r_data.putInt("ItemListID",item_list_id);
                                            r_msg.setData(r_data);
                                            Singleton.getInstance().getGameActivityHandler().sendMessage(r_msg);
                                            return;
                                        }
                                        break;
                                    }
                                }
                            } else {
                                break;
                            }

                            if(item_list_pos.equals(-1)) break;

                            if(invoked_items.get(item_list_pos).host.getNumber()>1){
                                invoked_items.get(item_list_pos).host.decreaseNumber(1);
                                invoked_items.get(item_list_pos).item_number--;
                            } else {
                                grid.removeView(invoked_items.get(item_list_pos).host);
                                invoked_items.remove(invoked_items.get(item_list_pos));
                            }
                            break;}
                        case 2:{ // invoked Item
                            final GameFileLoader.GameInformation gi = (GameFileLoader.GameInformation) msg.obj;
                            GridLayout grid = (GridLayout)findViewById(R.id.game_item_layout);
                            Integer item_list_id = msg.getData().getInt("ItemListID");
                            Integer item_pos = 0;
                            Integer item_ID = msg.getData().getInt("ItemID");
                            Integer item_data = msg.getData().getInt("ItemData");
                            if(item_list_id.equals(-1)){
                                item_list_id = 0;
                                for(Integer i=0;i< invoked_items.size();i++){
                                    if(invoked_items.get(i).item_ID.equals(item_ID) && invoked_items.get(i).item_data.equals(item_data)){
                                        item_pos = i;
                                        item_list_id = invoked_items.get(i).list_ID;
                                        break;
                                    }
                                }
                            } else {
                                for (Integer i = 0; i < invoked_items.size(); i++) {
                                    if (invoked_items.get(i).list_ID.equals(item_list_id)) {
                                        item_pos = i;
                                        break;
                                    }
                                }
                            }
                            invoked_items.get(item_pos).invoked = true;
                            if(msg.getData().containsKey("DirectInvoke")){
                                if(invoked_items.get(item_pos).host.getNumber()>1){
                                    invoked_items.get(item_pos).host.decreaseNumber(1);
                                    invoked_items.get(item_pos).item_number--;
                                } else {
                                    grid.removeView(invoked_items.get(item_pos).host);
                                    invoked_items.remove(invoked_items.get(item_pos));
                                }
                            } else {
                                final Integer final_list_id = item_list_id;
                                final Integer final_list_pos = item_pos;
                                invoked_items.get(item_pos).timer = new CountDownTimer(invoked_items.get(item_pos).show_duration*1000, 100) {

                                    public void onTick(long millisUntilFinished) {
                                        Integer item_pos=0;
                                        for(Integer i=0;i<invoked_items.size();i++){
                                            if(invoked_items.get(i).list_ID.equals(final_list_id) && invoked_items.get(item_pos).host!=null){
                                                item_pos=i;
                                                invoked_items.get(item_pos).host.setGrayOutRatio(
                                                        millisUntilFinished/(invoked_items.get(item_pos).show_duration*1000f));
                                                break;
                                            }
                                        }

                                    }

                                    public void onFinish() {
                                        Integer item_pos=0;
                                        for(Integer i=0;i<invoked_items.size();i++){
                                            if(invoked_items.get(i).list_ID.equals(final_list_id)){
                                                item_pos=i;
                                                break;
                                            }
                                        }
                                        invoked_items.get(item_pos).invoked=false;
                                        if(Singleton.getInstance().getGameLogicThread().running_items.containsKey(invoked_items.get(item_pos).item_ID.toString()+","+invoked_items.get(item_pos).item_data.toString())){
                                            Singleton.getInstance().getGameLogicThread().running_items.remove(invoked_items.get(item_pos).item_ID.toString()+","+invoked_items.get(item_pos).item_data.toString());
                                        }
                                        Message m = new Message();
                                        m.what = 4;
                                        m.obj = gi;
                                        Bundle d = new Bundle();
                                        d.putInt("Type", 1); // remove item
                                        d.putInt("ItemListID", final_list_id);
                                        m.setData(d);
                                        Singleton.getInstance().getGameActivityHandler().sendMessage(m);
                                    }
                                }.start();
                            }
                            break;}
                    }
                    break;}
                case 5: { // add new Message
                    String type_str = msg.getData().getString("MessageType");
                    GameMessageType type=GameMessageType.NORMAL;
                    if(type_str.toLowerCase().equals("warning")){
                        type = GameMessageType.WARNING;
                    } else if(type_str.toLowerCase().equals("info")){
                        type = GameMessageType.INFO;
                    }
                    final Integer ID=message_list.createNewID();
                    message_list.add(0,new GameMessage(msg.getData().getString("MessageText"),type,msg.getData().getInt("ShowDuration"),ID));
                    message_adapter.notifyDataSetChanged();
                    new CountDownTimer(msg.getData().getInt("ShowDuration")*1000, msg.getData().getInt("ShowDuration")*1000) {

                        public void onTick(long millisUntilFinished) {
                        }

                        public void onFinish() {
                            for(Integer i=0;i<message_list.size();i++){
                                if(message_list.get(i).ID.equals(ID)){
                                    message_list.remove(message_list.get(i));
                                    message_adapter.notifyDataSetChanged();
                                    break;
                                }
                            }
                        }
                    }.start();

                    break;}
                case 6:{ // show flag, that was picked up
                    String team_color = msg.getData().getString("Color");
                    ArrayList<String> colortexts = new ArrayList<>();
                    colortexts.addAll(Arrays.asList(getResources().getStringArray(R.array.team_color_names)));
                    ArrayList<Integer> color_values = new ArrayList<>();
                    int[] colors = getResources().getIntArray(R.array.team_color_values);
                    for(Integer i=0;i<colors.length;i++) color_values.add(colors[i]);
                    for(Integer i=0;i<colortexts.size();i++){
                        if(colortexts.get(i).equals(team_color)){
                            ImageView v = (ImageView)findViewById(R.id.game_flag_view);
                            v.setColorFilter(color_values.get(i),PorterDuff.Mode.SRC_ATOP);
                            v.setVisibility(View.VISIBLE);
                            break;
                        }
                    }
                }
                case 7: { // hide lost flag
                    ((ImageView)findViewById(R.id.game_flag_view)).setVisibility(View.GONE);
                }
                case 15:{ // received requested game information from game logic
                    final GameFileLoader.GameInformation gi = (GameFileLoader.GameInformation) msg.obj;
                    game_info = gi;
                    if(gi==null) break;
                    extra_lifes_view = (TextView)findViewById(R.id.extra_lifes_text);
                    ammo_packs_view = (TextView)findViewById(R.id.ammo_packs_text);
                    points_bars = (CustomProcessBar)findViewById(R.id.game_points_bar);
                    game_timer_view = (TextView)findViewById(R.id.game_time_text);
                    ((TextView)findViewById(R.id.game_name_text)).setText(gi.game_name);
                    ((TextView)findViewById(R.id.player_name_id_text)).setText(gi.player_name+" ("+gi.game_variables.get("PlayerID").read_value().toString()+")");
                    ((TextView)findViewById(R.id.team_text)).setText(((GameFileLoader.Team)gi.definitions.get("Team_" + gi.game_variables.get("TeamID").read_value().toString()).value).name + " (" + gi.game_variables.get("TeamID").read_value().toString() + ")");
                    ((TextView)findViewById(R.id.game_time_text)).setText(
                            String.format("%d:%02d:%02d",
                                    ((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration/3600,
                                    (((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration % 3600)/60,
                                    ((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration % 60));
                    if(Singleton.getInstance().getGameActivityStatus()!=null && Singleton.getInstance().getGameActivityStatus().game_over){
                        ((TextView)findViewById(R.id.game_time_text)).setText(getResources().getString(R.string.GameOver));
                    }
                    ((CustomProcessBar)findViewById(R.id.game_points_bar)).setMaxValue(0,((Integer)gi.game_variables.get("max_LifePoints").read_value()).doubleValue());
                    ((CustomProcessBar)findViewById(R.id.game_points_bar)).setValue(0,((Integer)gi.game_variables.get("LifePoints").read_value()).doubleValue());
                    ((CustomProcessBar)findViewById(R.id.game_points_bar)).setMaxValue(1,((Integer)gi.game_variables.get("max_ShieldPoints").read_value()).doubleValue());
                    ((CustomProcessBar)findViewById(R.id.game_points_bar)).setValue(1,((Integer)gi.game_variables.get("ShieldPoints").read_value()).doubleValue());
                    ((CustomProcessBar)findViewById(R.id.game_points_bar)).setMaxValue(2,((Integer)gi.game_variables.get("max_AmmoPoints").read_value()).doubleValue());
                    ((CustomProcessBar)findViewById(R.id.game_points_bar)).setValue(2,((Integer)gi.game_variables.get("AmmoPoints").read_value()).doubleValue());
                    ((TextView)findViewById(R.id.extra_lifes_text)).setText(gi.game_variables.get("ExtraLifes").read_value().toString());
                    ((TextView)findViewById(R.id.ammo_packs_text)).setText(gi.game_variables.get("AmmoPacks").read_value().toString());
                    Spinner weapon_spinner = (Spinner)findViewById(R.id.game_weapon_select_spinner);
                    for(Integer i=0;i<4;i++){
                        if(gi.definitions.containsKey("Weapon"+i.toString())){
                            if(((GameFileLoader.WeaponInfo)gi.definitions.get("Weapon"+i.toString()).value).allowed) {
                                allowed_weapons.add("("+((GameFileLoader.WeaponInfo) gi.definitions.get("Weapon" + i.toString()).value).index.toString()+") "+((GameFileLoader.WeaponInfo)gi.definitions.get("Weapon"+i.toString()).value).name);
                            }
                        }
                    }
                    allowed_weapons_adapter = new ArrayAdapter<String>(GameActivity.this,android.R.layout.simple_list_item_1,allowed_weapons);
                    weapon_spinner.setAdapter(allowed_weapons_adapter);
                    Integer weapon_pos =0;
                    for(Integer i=0;i<allowed_weapons.size();i++){
                        if(Integer.valueOf(Integer.parseInt(allowed_weapons.get(i).substring(allowed_weapons.get(i).indexOf("(")+1,allowed_weapons.get(i).indexOf(")")))).equals((Integer)gi.game_variables.get("WeaponType").read_value())){
                            weapon_pos = i;
                            break;
                        }
                    }
                    weapon_spinner.setSelection(weapon_pos);
                    weapon_spinner.setOnItemSelectedListener(GameActivity.this);

                    for(Integer i=0;i<invoked_items.size();i++){
                        ItemStackView item_button = new ItemStackView(GameActivity.this);
                        Resources.Theme themes = getTheme();
                        TypedValue storedValueInTheme = new TypedValue();
                        if (themes.resolveAttribute(R.attr.colorForegroundPrimary, storedValueInTheme, true)) {
                            item_button.setValueColor(storedValueInTheme.data);
                        }
                        storedValueInTheme = new TypedValue();
                        if(themes.resolveAttribute(R.attr.colorBackground1, storedValueInTheme, true)){
                            item_button.setBackgroundColor(storedValueInTheme.data);
                        }
                        item_button.setValueText(getItemValueText(gi,invoked_items.get(i).item_ID, invoked_items.get(i).item_data));
                        item_button.setItemID(invoked_items.get(i).item_ID);
                        item_button.setItemListID(invoked_items.get(i).list_ID);
                        if(gi.definitions.containsKey("Item_"+invoked_items.get(i).item_ID)){ // user defined
                            Glide.with(GameActivity.this).load(Singleton.getInstance().getUserFilesPath()+ File.separator+"UserIcons"+File.separator+((GameFileLoader.Item)gi.definitions.get("Item_"+invoked_items.get(i).item_ID).value).icon_path).into(item_button);
                            invoked_items.get(i).show_duration = ((GameFileLoader.Item)gi.definitions.get("Item_"+invoked_items.get(i).item_ID.toString()).value).invoke_duration;
                        } else { // predefined
                            int res_id=0;
                            switch(invoked_items.get(i).item_ID){
                                case 0:
                                    res_id = R.drawable.ic_1up;
                                    break;
                                case 1:
                                    res_id = R.drawable.ic_heart;
                                    break;
                                case 2:
                                    res_id = R.drawable.ic_shield;
                                    break;
                                case 3:
                                    res_id = R.drawable.ic_ammo_pack;
                                    break;
                                case 4:
                                    res_id = R.drawable.ic_lasergun_plus;
                                    break;
                                case 5:
                                    res_id = R.drawable.ic_phoenix;
                                    break;
                                case 6:
                                    res_id = R.drawable.ic_invincible;
                                    break;
                                case 7:
                                case 8:
                                case 9:
                                    res_id = R.drawable.ic_trap;
                                    break;
                                case 10:
                                    res_id = R.drawable.ic_spy;
                                    break;
                                case 11:
                                    res_id = R.drawable.ic_cloak;
                                    break;
                                default:
                                    res_id = R.drawable.ic_red_cross;
                                    break;
                            }
                            //item_button.setImageResource(res_id);
                            item_button.setImageDrawable(VectorDrawableCompat.create(getResources(),res_id,getTheme()));
                            invoked_items.get(i).show_duration = getItemDuration(invoked_items.get(i).item_ID, invoked_items.get(i).item_data,gi);
                        }
                        item_button.setPadding(0,0,0,0);
                        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                        params.height = 120;
                        params.width = 120;
                        item_button.setLayoutParams(params);
                        item_button.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        item_button.setNumber(invoked_items.get(i).item_number);
                        GridLayout grid = (GridLayout)findViewById(R.id.game_item_layout);
                        grid.addView(item_button);
                        invoked_items.get(i).grid = grid;
                        invoked_items.get(i).host = item_button;

                        item_button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if(Singleton.getInstance().getGameActivityStatus().game_over) return;
                                ItemStackView stack = (ItemStackView) v;
                                Message msg = new Message();
                                msg.what = 10;
                                Bundle dat = new Bundle();
                                Integer pos=0;
                                for(Integer i=0;i<invoked_items.size();i++){
                                    if(invoked_items.get(i).list_ID.equals(stack.getItemListID())){
                                        pos = i;
                                        break;
                                    }
                                }
                                if(Singleton.getInstance().getGameLogicThread().running_items
                                        .containsKey(invoked_items.get(pos).item_ID.toString()+","+invoked_items.get(pos).item_data.toString())){
                                    return;
                                }
                                dat.putInt("ItemID",invoked_items.get(pos).item_ID);
                                dat.putInt("ItemData",invoked_items.get(pos).item_data);
                                msg.setData(dat);
                                Singleton.getInstance().getGameLogicHandler().sendMessage(msg);
                                Message a_msg = new Message();
                                a_msg.what = 4;
                                a_msg.obj = gi;
                                Bundle a_data = new Bundle();
                                a_data.putInt("Type",2);
                                a_data.putBoolean("DirectInvoke",true);
                                a_data.putInt("ItemID",invoked_items.get(pos).item_ID);
                                a_data.putInt("ItemData",invoked_items.get(pos).item_data);
                                a_data.putInt("ItemListID",invoked_items.get(pos).list_ID);
                                a_msg.setData(a_data);
                                Singleton.getInstance().getGameActivityHandler().sendMessage(a_msg);
                            }
                        });
                    }
                    break;}
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //set content view AFTER ABOVE sequence (to avoid crash)
        this.setContentView(R.layout.activity_game);

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPref.getBoolean("game_wake_lock",true)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if(sharedPref.getBoolean("SoundOn",true)){
            ((ImageButton)findViewById(R.id.game_sound_button)).setImageResource(R.drawable.ic_sound);
            Singleton.getInstance().setSoundOn(true);
        } else {
            Singleton.getInstance().setSoundOn(false);
            ((ImageButton)findViewById(R.id.game_sound_button)).setImageResource(R.drawable.ic_sound_off);
        }

        ((ImageButton)findViewById(R.id.game_sound_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sharedPref.getBoolean("SoundOn",true)){
                    sharedPref.edit().putBoolean("SoundOn",false).apply();
                    ((ImageButton)findViewById(R.id.game_sound_button)).setImageResource(R.drawable.ic_sound_off);
                    Singleton.getInstance().setSoundOn(false);
                } else {
                    sharedPref.edit().putBoolean("SoundOn",true).apply();
                    ((ImageButton)findViewById(R.id.game_sound_button)).setImageResource(R.drawable.ic_sound);
                    Singleton.getInstance().setSoundOn(true);
                }
            }
        });

        Singleton.getInstance().setGameActivityHandler(mainHandler);

        Message g_msg = new Message();
        g_msg.what=3;
        g_msg.obj = mainHandler;
        Singleton.getInstance().getGameLogicHandler().sendMessage(g_msg);

        ((Button)findViewById(R.id.exit_game_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GameActivity.this.onBackPressed();
            }
        });

        if(Singleton.getInstance().getGameActivityStatus()==null) {
            message_list = new MessageList();
            message_adapter = new MessageAdapter(this,message_list);

            String[] countdown_texts = getResources().getStringArray(R.array.preference_game_countdown_entries);
            int[] countdown_values = getResources().getIntArray(R.array.preference_game_countdown_values);
            String countdown_setting = sharedPref.getString("start_game_countdown","10s");
            Integer countdown_value=10;
            for(Integer i=0;i<countdown_texts.length;i++){
                if(countdown_texts[i].equals(countdown_setting)){
                    countdown_value = countdown_values[i];
                    break;
                }
            }

            Message init_msg = new Message(); // send game initiate message
            init_msg.what=9;
            Bundle init_data = new Bundle();
            init_data.putInt("Type",2);
            init_msg.setData(init_data);
            Singleton.getInstance().getGameLogicHandler().sendMessage(init_msg);

            ((RelativeLayout) findViewById(R.id.countdown_overlay_layout)).setVisibility(View.VISIBLE);
            pre_countdown_timer = new CountDownTimer(countdown_value * 1000, 1000) {

                public void onTick(long millisUntilFinished) {
                    ((TextView) findViewById(R.id.countdown_overlay_text)).setText(Long.valueOf(millisUntilFinished / 1000).toString());
                    Singleton.getInstance().getGameActivityStatus().pre_countdown_status = Long.valueOf(millisUntilFinished/1000).intValue();
                }

                public void onFinish() {
                    ((RelativeLayout) findViewById(R.id.countdown_overlay_layout)).setVisibility(View.GONE);
                    Message msg = new Message(); // send game start message
                    msg.what = 9;
                    Bundle data = new Bundle();
                    data.putInt("Type", 0);
                    msg.setData(data);
                    Singleton.getInstance().getGameLogicHandler().sendMessage(msg);
                    Singleton.getInstance().getGameActivityStatus().pre_countdown_status=-1;

                }
            }.start();
            Singleton.getInstance().setGameActivityStatus(Singleton.getInstance().createNewGameActivityStatus(true,message_list,message_adapter,invoked_items,10));

        } else {
            message_list = Singleton.getInstance().getGameActivityStatus().message_list;
            message_adapter = Singleton.getInstance().getGameActivityStatus().message_adapter;

            invoked_items = Singleton.getInstance().getGameActivityStatus().item_list;
            if(!Singleton.getInstance().getGameActivityStatus().pre_countdown_status.equals(-1)){
                ((RelativeLayout) findViewById(R.id.countdown_overlay_layout)).setVisibility(View.VISIBLE);
                pre_countdown_timer = new CountDownTimer(Singleton.getInstance().getGameActivityStatus().pre_countdown_status * 1000, 1000) {

                    public void onTick(long millisUntilFinished) {
                        ((TextView) findViewById(R.id.countdown_overlay_text)).setText(Long.valueOf(millisUntilFinished / 1000).toString());
                        Singleton.getInstance().getGameActivityStatus().pre_countdown_status = Long.valueOf(millisUntilFinished/1000).intValue();
                    }

                    public void onFinish() {
                        ((RelativeLayout) findViewById(R.id.countdown_overlay_layout)).setVisibility(View.GONE);
                        Message msg = new Message(); // send game start message
                        msg.what = 9;
                        Bundle data = new Bundle();
                        data.putInt("Type", 0);
                        msg.setData(data);
                        Singleton.getInstance().getGameLogicHandler().sendMessage(msg);
                        Singleton.getInstance().getGameActivityStatus().pre_countdown_status=-1;

                    }
                }.start();
            }
        }

        ListView lv = ((ListView)findViewById(R.id.game_messages_listview));
        lv.setAdapter(message_adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                message_list.remove(message_list.get(position));
                message_adapter.notifyDataSetChanged();
            }
        });
        message_adapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed(){
        if(Singleton.getInstance().getGameActivityStatus().game_over){
            super.onBackPressed();
            Singleton.getInstance().setGameActivityStatus(null);
            Message msg = new Message();
            msg.what=9;
            Bundle data = new Bundle();
            data.putInt("Type",1);
            msg.setData(data);
            Singleton.getInstance().getGameLogicHandler().sendMessage(msg);
            Message reload_msg = new Message();
            reload_msg.what = 4;
            Bundle reload_data = new Bundle();
            reload_data.putString("Path",game_info.file_name);
            reload_msg.setData(reload_data);
            Singleton.getInstance().getGameLogicHandler().sendMessage(reload_msg);
            return;
        }
        new AlertDialog.Builder(GameActivity.this)
                .setTitle("Quit Game?")
                .setMessage("Are you sure you want to quit the game?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Message msg = new Message();
                        msg.what=9;
                        Bundle data = new Bundle();
                        data.putInt("Type",1);
                        msg.setData(data);
                        Singleton.getInstance().getGameLogicHandler().sendMessage(msg);
                        if(GameActivity.this.pre_countdown_timer!=null) GameActivity.this.pre_countdown_timer.cancel();
                        Singleton.getInstance().setGameActivityStatus(null);
                        StatsManager.getInstance().AbortCurrentGameStats();
                        Message reload_msg = new Message();
                        reload_msg.what = 4;
                        Bundle reload_data = new Bundle();
                        reload_data.putString("Path",game_info.file_name);
                        reload_msg.setData(reload_data);
                        Singleton.getInstance().getGameLogicHandler().sendMessage(reload_msg);
                        Singleton.getInstance().setGameActivityStatus(null);
                        GameActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        Integer weapon_index=0;
        String w_string = (String)parent.getItemAtPosition(pos);
        weapon_index = Integer.parseInt(w_string.substring(w_string.indexOf("(")+1,w_string.indexOf(")")));
        Message msg = new Message();
        msg.what = 13;
        Bundle msg_data = new Bundle();
        msg_data.putInt("WeaponType",weapon_index);
        msg.setData(msg_data);
        Singleton.getInstance().getGameLogicHandler().sendMessage(msg);
    }

    public void onNothingSelected(AdapterView<?> parent) {
        parent.setSelection(0);
    }


    private String getItemValueText(GameFileLoader.GameInformation game_info, Integer ID, Integer data){
        String value="";
        switch(ID){
            case 1:
            case 2:
                value = game_info.damage_mapping.get(data).toString();
                break;
            case 7:
            case 8:
            case 9:
                value = game_info.damage_mapping.get(data).toString();
                break;
            case 10:
                value = "30s";
                break;
            case 11:
                value = game_info.duration_mapping.get(data).toString()+"s";
                break;
        }
        return value;
    }

    private Integer getItemDuration(Integer ID, Integer data, GameFileLoader.GameInformation game_info){
        switch (ID){
            case 7:
                return 15;
            case 10:
                return 30;
            case 11:
                return game_info.duration_mapping.get(data);
        }
        return 2;
    }

    private void fill_end_stats_list(){
        ListView lv = (ListView) findViewById(R.id.game_stats_list);
        endstatslist = new ArrayList<>();
        for(String key : game_info.player_stats.keySet()){
            endstatslist.add(new EndStatsItem(key, game_info.player_stats.get(key)));
        }
        endstatsadapter = new EndStatsAdapter(this, endstatslist);
        lv.setAdapter(endstatsadapter);
    }
}
