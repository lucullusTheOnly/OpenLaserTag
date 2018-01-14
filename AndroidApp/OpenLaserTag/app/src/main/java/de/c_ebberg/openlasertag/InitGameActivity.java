package de.c_ebberg.openlasertag;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;

public class InitGameActivity extends BaseActivity {
    private final String LOG_TAG= "InitGameActivity";
    GameFileLoader.GameInformation gi=null;
    ArrayAdapter<String> team_adapter;
    ArrayList<String> team_list;
    ArrayAdapter<String> player_id_adapter;
    ArrayList<String> player_id_list;
    private Toolbar toolbar=null;

    public Handler mainHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 15: // received requested game information from game logic
                    gi = (GameFileLoader.GameInformation) msg.obj;
                    if(gi==null) break;
                    toolbar.setTitle(gi.game_name);
                    ((TextView)findViewById(R.id.description_text)).setText(gi.description);
                    ((TextView)findViewById(R.id.duration_text)).setText(
                        String.format("%d:%02d:%02d",((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration/3600,                             (((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration % 3600)/60,
                            ((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration % 60));

                    team_list = new ArrayList<>();
                    for(Integer i=0; gi.definitions.containsKey("Team_"+i.toString());i++){
                        team_list.add("("+i.toString()+") "+((GameFileLoader.Team)gi.definitions.get("Team_"+i.toString()).value).name);
                    }
                    team_adapter = new ArrayAdapter<String>(InitGameActivity.this,android.R.layout.simple_list_item_1,team_list);
                    ((Spinner)findViewById(R.id.join_team_spinner)).setAdapter(team_adapter);
                    ((Spinner)findViewById(R.id.join_team_spinner)).setSelection((Integer)gi.game_variables.get("TeamID").value);

                    final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(InitGameActivity.this);
                    Log.i(LOG_TAG,"InitGameStats...");
                    try {
                        StatsManager.getInstance().InitGameStats(sharedPref.getString("CurrentUser", ""), ((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration, InitGameActivity.this);
                        Log.i(LOG_TAG,"InitDefaultGameStats");
                        StatsManager.getInstance().InitDefaultGameStats();
                        Log.i(LOG_TAG,"Init user defined stats");
                        for(String key : gi.player_stats.keySet()){
                            StatsManager.getInstance().InitStat(key);
                        }
                    }catch(IOException e){
                        Log.e(LOG_TAG,"IOException during Initiation of Game Stats!");
                    }
                    break;
            }
        }
    };

    @Override
    protected void onResume(){
        super.onResume();
        if(gi!=null && StatsManager.getInstance().IsCurrentGameStatSet()){
            ((TextView)findViewById(R.id.duration_text)).setText(
                    String.format("%d:%02d:%02d",((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration/3600,                             (((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration % 3600)/60,
                            ((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration % 60));

            team_list = new ArrayList<>();
            for(Integer i=0; gi.definitions.containsKey("Team_"+i.toString());i++){
                team_list.add("("+i.toString()+") "+((GameFileLoader.Team)gi.definitions.get("Team_"+i.toString()).value).name);
            }
            team_adapter = new ArrayAdapter<String>(InitGameActivity.this,android.R.layout.simple_list_item_1,team_list);
            ((Spinner)findViewById(R.id.join_team_spinner)).setAdapter(team_adapter);
            ((Spinner)findViewById(R.id.join_team_spinner)).setSelection((Integer)gi.game_variables.get("TeamID").value);
        }
        if(!StatsManager.getInstance().IsCurrentGameStatSet()){
            Message g_msg = new Message();
            g_msg.what=3;
            g_msg.obj = mainHandler;
            Singleton.getInstance().getGameLogicHandler().sendMessage(g_msg);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init_game);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Message g_msg = new Message();
        g_msg.what=3;
        g_msg.obj = mainHandler;
        Singleton.getInstance().getGameLogicHandler().sendMessage(g_msg);

        player_id_list = new ArrayList<>();
        for(Integer i=0;i<32;i++){
            player_id_list.add(i.toString());
        }
        player_id_adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,player_id_list);
        ((Spinner)findViewById(R.id.player_id_spinner)).setAdapter(player_id_adapter);

        ((EditText)findViewById(R.id.player_name_edit_text)).setText(Singleton.getInstance().getCurrentAlias());

        ((Button) findViewById(R.id.start_game_button)).setOnClickListener(new View.OnClickListener(){
            public void onClick(View V){
                gi.game_variables.get("PlayerID").value = player_id_adapter.getPosition(((Spinner)findViewById(R.id.player_id_spinner)).getSelectedItem().toString());
                gi.game_variables.get("TeamID").value = team_adapter.getPosition(((Spinner)findViewById(R.id.join_team_spinner)).getSelectedItem().toString());
                gi.player_name = ((EditText)findViewById(R.id.player_name_edit_text)).getText().toString();
                Intent intent = new Intent(InitGameActivity.this, GameActivity.class);
                startActivity(intent);
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Spinner sp = (Spinner)findViewById(R.id.player_id_spinner);
        sp.setFocusable(true);
        sp.setFocusableInTouchMode(true);
        sp.requestFocus();

        ((ImageButton)findViewById(R.id.init_game_settings_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InitGameActivity.this, GameConfigActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed(){
        if(gi!=null){
            StatsManager.getInstance().AbortCurrentGameStats();
        }
        super.onBackPressed();
    }
}
