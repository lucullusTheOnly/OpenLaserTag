package de.c_ebberg.openlasertag;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.IOException;

import static java.lang.Thread.sleep;

public class OpenLaserTagMain extends BaseActivity implements BluetoothDialogFragment.OnBluetoothFragmentListener {

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    private SerialThread serial_thread;
    private GameLogicThread game_logic_thread;

    private final String LOG_TAG = "OpenLaserTagMain";


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    // manages messages for current Thread (main)
    // received from our Thread
    public Handler mainHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch(msg.what){
                case 0:

                    break;
                case 1: { // Message from SerialThread
                    TextView tv = (TextView) findViewById(R.id.bt_connect_textView);
                    Button con_but = (Button) findViewById(R.id.connect_Button);
                    int msg_type = msg.getData().getInt("Type");
                    switch (msg_type) {
                        case 0: // Pair Tagger first
                            tv.setText(getResources().getString(R.string.PairTaggerFirst));
                            con_but.setText(getResources().getString(R.string.Connect));
                            break;
                        case 1: // Connection established
                            tv.setText(getResources().getString(R.string.ConnectedtoTagger));
                            con_but.setText(getResources().getString(R.string.Disconnect));
                            break;
                        case 2: // Connection failed
                            tv.setText(getResources().getString(R.string.NotConnected));
                            con_but.setText(getResources().getString(R.string.Connect));
                            break;
                        case 3: // Disconnected
                            tv.setText(getResources().getString(R.string.NotConnected));
                            con_but.setText(getResources().getString(R.string.Connect));
                            break;
                    }
                    break;
                }
                case 2: { // Message from GameLogicThread
                    //TextView tv = (TextView) findViewById(R.id.sample_text);
                    int msg_type = msg.getData().getInt("Type");
                    switch(msg_type){
                        case 1: // save receiver ids to preferences
                            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(OpenLaserTagMain.this);
                            sharedPref.edit().putString("ReceiverIDs",msg.getData().getString("ReceiverIDs")).apply();
                            Log.i(LOG_TAG,"Found Receiver Modules: "+msg.getData().getString("ReceiverIDs"));
                            break;
                    }
                    break;
                }
            }
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_laser_tag_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Check for app folder in public documents dir for files introduced by the user
        File pub_dir = Environment.getExternalStorageDirectory();
        File app_folder = new File(pub_dir.getAbsolutePath()+File.separator+"OpenLaserTag");
        Singleton.getInstance().setUserFilesPath(pub_dir.getAbsolutePath()+File.separator+"OpenLaserTag");
        if(!app_folder.exists()){
            try {
                app_folder.mkdir();
                new File(app_folder.getAbsolutePath() + File.separator + "Sounds").mkdir();
                new File(app_folder.getAbsolutePath(), "testfile.txt").createNewFile();
                new File(app_folder.getAbsolutePath() + File.separator + "Sounds", ".nomedia").createNewFile();
                new File(app_folder.getAbsolutePath() + File.separator + "UserGames").mkdir();
                new File(app_folder.getAbsolutePath() + File.separator + "UserGames", "testfile.txt").createNewFile();
                new File(app_folder.getAbsolutePath() + File.separator + "UserIcons").mkdir();
                new File(app_folder.getAbsolutePath() + File.separator + "UserIcons", "textfile.txt").createNewFile();
                String[] paths = new String[4];
                paths[0]=app_folder.getAbsolutePath() + File.separator + "Sounds"+ File.separator+ ".nomedia";
                paths[1]=app_folder.getAbsolutePath() + File.separator + "UserGames"+ File.separator + "testfile.txt";
                paths[2]=app_folder.getAbsolutePath() + File.separator + "UserIcons"+ File.separator + "testfile.txt";
                paths[3]=app_folder.getAbsolutePath() + File.separator + "testfile.txt";
                MediaScannerConnection.scanFile(this, paths, null, null);
            } catch(IOException e){
                Log.e(LOG_TAG,"Exception during creating of App folders - "+e);
            }
        } else{ // check if test files exist, and remove them
            if(new File(app_folder.getAbsolutePath(), "testfile.txt").exists()){
                new File(app_folder.getAbsolutePath() + File.separator + "UserGames","testfile.txt").delete();
                new File(app_folder.getAbsolutePath(),"testfile.txt").delete();
                new File(app_folder.getAbsolutePath() + File.separator + "UserIcons", "textfile.txt").delete();
                String[] paths = new String[3];
                paths[0]=app_folder.getAbsolutePath() + File.separator + "UserGames"+ File.separator + "testfile.txt";
                paths[1]=app_folder.getAbsolutePath() + File.separator + "testfile.txt";
                paths[2]=app_folder.getAbsolutePath() + File.separator + "UserIcons" + File.separator + "textfile.txt";
                MediaScannerConnection.scanFile(this, paths, null, null);
            }
        }

        if(Singleton.getInstance().getGameLogicThread()==null){
            // Define and start GameLogicThread
            game_logic_thread=new GameLogicThread((Context)this,mainHandler);
            serial_thread = new SerialThread(game_logic_thread.getHandler(),mainHandler);
            game_logic_thread.setSerialHandler(serial_thread.getHandler());
            game_logic_thread.start();
            serial_thread.start();

            Singleton.getInstance().setGameLogicHandler(game_logic_thread.getHandler());
            Singleton.getInstance().setSerialHandler(serial_thread.getHandler());
            Singleton.getInstance().setGameLogicThread(game_logic_thread);
            Singleton.getInstance().setSerialThread(serial_thread);
        } else {
            game_logic_thread = Singleton.getInstance().getGameLogicThread();
            serial_thread = Singleton.getInstance().getSerialThread();
            if(serial_thread.getStatus()){
                Message con_msg = new Message();
                con_msg.what=1;
                Bundle msg_data=new Bundle();
                msg_data.putInt("Type",1);// Connection established
                con_msg.setData(msg_data);
                mainHandler.sendMessage(con_msg);
            }
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this); //defined above
        if(!sharedPref.contains("RememberBTDevice")){
            sharedPref.edit().putBoolean("RememberBTDevice",false).apply();
        }
        if(!sharedPref.contains("CurrentUser")){
            LayoutInflater inflater = this.getLayoutInflater();
            final View dialogtextview = inflater.inflate(R.layout.new_user_dialog_edit_text,null);
            new AlertDialog.Builder(this)
                    .setMessage(getResources().getString(R.string.EnterNewUsername))
                    .setView(dialogtextview)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            sharedPref.edit().putString("CurrentUser",((EditText)dialogtextview.findViewById(R.id.new_user_dialog_edit_text)).getText().toString()).commit();
                            sharedPref.edit().putString("User0",((EditText)dialogtextview.findViewById(R.id.new_user_dialog_edit_text)).getText().toString()).apply();
                            sharedPref.edit().putString("User0Alias",((EditText)dialogtextview.findViewById(R.id.new_user_dialog_edit_text)).getText().toString()).apply();
                            sharedPref.edit().putInt("UserNum",1).apply();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        Singleton.getInstance().setCurrentUser(sharedPref.getString("CurrentUser","PlayerOne"));
        for(Integer i=0;sharedPref.contains("User"+i.toString());i++){
            if(sharedPref.getString("User"+i.toString(),"").equals(Singleton.getInstance().getCurrentUser())){
                sharedPref.edit().putString("CurrentAlias",sharedPref.getString("User"+i.toString()+"Alias","")).apply();
                Singleton.getInstance().setCurrentAlias(sharedPref.getString("User"+i.toString()+"Alias",""));
                break;
            }
        }
        ((TextView)findViewById(R.id.current_user_name_text)).setText(Singleton.getInstance().getCurrentUser());

        CheckBox rem_bt_box = (CheckBox) findViewById(R.id.bt_remember_checkbox);
        rem_bt_box.setChecked(sharedPref.getBoolean("RememberBTDevice",false));
        rem_bt_box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean state = ((CheckBox) findViewById(R.id.bt_remember_checkbox)).isChecked();
                sharedPref.edit().putBoolean("RememberBTDevice",state).apply();
                if(!state && sharedPref.contains("BTDeviceName")){
                    sharedPref.edit().remove("BTDeviceName").apply();
                }
                if(!state && sharedPref.contains("BTDeviceAddr")){
                    sharedPref.edit().remove("BTDeviceAddr").apply();
                }
            }
        });

        Button connect_button = (Button) findViewById(R.id.connect_Button);
        connect_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View V) {
                TextView tv = (TextView) findViewById(R.id.bt_connect_textView);
                if(tv.getText().equals(getResources().getString(R.string.ConnectedtoTagger))){
                    Message msg = new Message();
                    msg.what=1;
                    msg.obj = mainHandler;
                    serial_thread.getHandler().sendMessage(msg);
                    return;
                }
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter == null) {
                    tv.setText(getResources().getString(R.string.BluetoothNotSupported));
                    return;
                }
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableAdapter, 0);
                    return;
                }
                if(((CheckBox)findViewById(R.id.bt_remember_checkbox)).isChecked()
                        && sharedPref.contains("BTDeviceName")
                        && sharedPref.contains("BTDeviceAddr")){
                    Message serial_msg=new Message();
                    serial_msg.what=1; // toggle Bluetooth state
                    serial_msg.obj = mainHandler;
                    Bundle msg_data=new Bundle();
                    msg_data.putString("DeviceName",sharedPref.getString("BTDeviceName","HC-06"));
                    msg_data.putString("DeviceAddr",sharedPref.getString("BTDeviceAddr",""));
                    serial_msg.setData(msg_data);
                    serial_thread.getHandler().sendMessage(serial_msg);
                } else  {
                    showChooseBTDeviceDialog();
                }

            }
        });

        ((Button) findViewById(R.id.load_game_button)).setOnClickListener(new View.OnClickListener(){
            public void onClick(View V){
                Intent intent = new Intent(OpenLaserTagMain.this, LoadGameActivity.class);
                startActivity(intent);
            }
        });
        ((Button) findViewById(R.id.settings_button)).setOnClickListener(new View.OnClickListener(){
            public void onClick(View V){
                Intent intent = new Intent(OpenLaserTagMain.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
        ((ImageButton) findViewById(R.id.current_user_change_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OpenLaserTagMain.this, ManageUsersActivity.class);
                startActivity(intent);
            }
        });
        ((ImageButton) findViewById(R.id.current_user_stats_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OpenLaserTagMain.this,StatsActivity.class);
                intent.putExtra("User",sharedPref.getString("CurrentUser",""));
                startActivity(intent);
            }
        });
        ((Button) findViewById(R.id.tagger_config_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OpenLaserTagMain.this, TaggerConfigActivity.class);
                startActivity(intent);
            }
        });

        //StatsManager.getInstance().write_test_data_to_user_file("Chrisl",this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((TextView)findViewById(R.id.current_user_name_text)).setText(Singleton.getInstance().getCurrentUser());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_open_laser_tag_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id){
            case R.id.action_settings:{
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;}
            case R.id.UserManager_menu:{
                Intent intent = new Intent(this, ManageUsersActivity.class);
                startActivity(intent);
                return true;}
        }

        return super.onOptionsItemSelected(item);
    }


    public void showChooseBTDeviceDialog() {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        BluetoothDialogFragment newFragment = BluetoothDialogFragment.newInstance();

        // The device is smaller, so show the fragment fullscreen
        android.support.v4.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
        // For a little polish, specify a transition animation
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        // To make it fullscreen, use the 'content' root view as the container
        // for the fragment, which is always the root view for the activity
        transaction.add(android.R.id.content, newFragment,"BluetoothDialogFragment")
                .addToBackStack(null).commit();
    }

    public void onDeviceChosen(String name, String addr){
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Message serial_msg=new Message();
        serial_msg.what=1; // toggle Bluetooth state
        serial_msg.obj = mainHandler;
        Bundle msg_data=new Bundle();
        msg_data.putString("DeviceName",name);
        msg_data.putString("DeviceAddr",addr);
        serial_msg.setData(msg_data);
        serial_thread.getHandler().sendMessage(serial_msg);

        if(((CheckBox)findViewById(R.id.bt_remember_checkbox)).isChecked()){
            sharedPref.edit().putString("BTDeviceName",name).apply();
            sharedPref.edit().putString("BTDeviceAddr",addr).apply();
        }
    }
}
