package de.c_ebberg.openlasertag;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GameConfigActivity extends BaseActivity implements SelectXMLFileFragment.SelectXMLFileFragmentListener {
    private final String LOG_TAG = "GameConfigActivity";
    private ArrayList<ListViewItem> game_config_list = new ArrayList<>();
    private ListViewAdapter game_config_adapter;
    private GameFileLoader.GameInformation gi;
    private ArrayList<ColorTextView> colortextlist;
    private ColorTextAdapter colortextadapter;
    private Toolbar toolbar=null;

    private class ColorTextView{
        String text;
        String text_lang;
        Integer color;

        public ColorTextView(String t, String t_lang, Integer col){text = t; color= col; text_lang = t_lang;}
    }

    private class ColorTextAdapter extends ArrayAdapter<ColorTextView>{
        private final Context context;
        private final ArrayList<ColorTextView> modelsArrayList;

        public ColorTextAdapter(Context context, ArrayList<ColorTextView> modelsArrayList) {
            super(context, R.layout.list_item_textview_with_imageview,R.id.item_textview_textview, modelsArrayList);

            this.context = context;
            this.modelsArrayList = modelsArrayList;
        }

        @Override
        public View getDropDownView(int position, View convertView,ViewGroup parent){
            return getView(position,convertView,parent);
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final TeamSpinnerViewHolder mViewHolder;
            View v = convertView;

            if(v==null) {
                mViewHolder = new TeamSpinnerViewHolder();
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                v = inflater.inflate(R.layout.list_item_textview_with_imageview, parent, false);
                mViewHolder.textview = (TextView) v.findViewById(R.id.item_textview_textview);
                mViewHolder.imageview = (ImageView) v.findViewById(R.id.item_textview_imageview);

                v.setTag(mViewHolder);
            } else {
                mViewHolder = (TeamSpinnerViewHolder) v.getTag();
            }

            // 4. Set the text for textView
            mViewHolder.textview.setText(modelsArrayList.get(position).text_lang);
            mViewHolder.imageview.setBackgroundColor(modelsArrayList.get(position).color);

            // 5. return rowView
            return v;
        }
    }

    static class TeamSpinnerViewHolder{
        private TextView textview;
        private ImageView imageview;
    }

    public Handler mainHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 15: // received requested game information from game logic
                    gi = (GameFileLoader.GameInformation) msg.obj;
                    if(gi==null) break;
                    toolbar.setTitle(getResources().getString(R.string.title_activity_game_config) + gi.game_name);
                    game_config_list.clear();
                    game_config_adapter.notifyDataSetChanged();
                    addCaption("General:");
                    addSimpleValue(getResources().getString(R.string.GameDuration),
                            String.format("%d:%02d:%02d",((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration/3600,                             (((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration % 3600)/60,
                                    ((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration % 60),
                            "seconds");
                    addCaption(getResources().getString(R.string.WeaponsDoublePoint));
                    for(Integer i=0;i<GameFileLoader.max_weapon_type;i++){
                        if(gi.definitions.containsKey("Weapon"+i.toString())){
                            addWeapon(((GameFileLoader.WeaponInfo)gi.definitions.get("Weapon"+i.toString()).value).name,i,
                                    ((GameFileLoader.WeaponInfo)gi.definitions.get("Weapon"+i.toString()).value).damage_sign,
                                    ((GameFileLoader.WeaponInfo)gi.definitions.get("Weapon"+i.toString()).value).shot_frequency,
                                    ((GameFileLoader.WeaponInfo)gi.definitions.get("Weapon"+i.toString()).value).range,
                                    ((GameFileLoader.WeaponInfo)gi.definitions.get("Weapon"+i.toString()).value).allowed);
                        }
                    }
                    boolean flag = false;
                    for(Integer i=0;i<GameFileLoader.max_weapon_type;i++){
                        if(!gi.definitions.containsKey("Weapon"+i.toString())){
                            flag = true;
                            break;
                        }
                    }
                    if(flag){
                        addAction(getResources().getString(R.string.AddWeapon),Actions.ADD_WEAPON);
                    }

                    addCaption(getResources().getString(R.string.TeamsDoublePoint));
                    for(Integer i=0;i<GameFileLoader.max_team_number;i++){
                        if(gi.definitions.containsKey("Team_"+i.toString())){
                            String color_name=((GameFileLoader.Team)gi.definitions.get("Team_"+i.toString()).value).color;
                            String[] color_res = getResources().getStringArray(R.array.team_color_names);
                            String[] color_res_lang = getResources().getStringArray(R.array.team_color_names_language);
                            for(Integer j=0;j<color_res.length;j++){
                                if(color_res[j].equals(color_name)){
                                    color_name = color_res_lang[j];
                                    break;
                                }
                            }
                            addTeam(((GameFileLoader.Team)gi.definitions.get("Team_"+i.toString()).value).name,
                                    color_name,
                                    ((GameFileLoader.Team)gi.definitions.get("Team_"+i.toString()).value).index);
                        }
                    }
                    flag = false;
                    for(Integer i=0;i<GameFileLoader.max_team_number;i++){
                        if(!gi.definitions.containsKey("Team_"+i.toString())){
                            flag = true;
                            break;
                        }
                    }
                    if(flag){
                        addAction(getResources().getString(R.string.AddNewTeam),Actions.ADD_TEAM);
                    }

                    for(String key : gi.definitions.keySet()){ //Check, if there are other Timers than GameTimer
                        if(key.contains("Timer_") && !key.equals("Timer_GameTimer")){
                            addCaption(getResources().getString(R.string.TimerDoublePoint));
                            break;
                        }
                    }
                    for(Map.Entry entry : gi.definitions.entrySet()){
                        if(entry.getKey().toString().contains("Timer_") && !entry.getKey().toString().contains("GameTimer")){
                            addTimer(((GameFileLoader.Timer)entry.getValue()).name,
                                    ((GameFileLoader.Timer)entry.getValue()).duration,
                                    ((GameFileLoader.Timer)entry.getValue()).ticks);
                        }
                    }

                    addCaption(getResources().getString(R.string.VariablesDoublePoint));
                    ArrayList<Map.Entry> entry_list = new ArrayList<>();
                    Iterator it = gi.game_variables.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry)it.next();
                        entry_list.add(pair);
                    }
                    entry_list = sortEntryListLexically(entry_list,true);
                    for(Map.Entry entry : entry_list){
                        addVariable((String)entry.getKey(),
                                ((GameFileLoader.GameVariable)entry.getValue()).value.toString(),
                                ((GameFileLoader.GameVariable)entry.getValue()).type);
                    }
                    break;
            }
        }
    };

    private class ListViewItem{
        ItemType type;
        String name_text;
        String value_text;
        String caption_text;

        String simple_value_type;

        Boolean weapon_allowed;
        Integer weapon_index;
        Double weapon_frequency;
        Integer weapon_range;
        Integer weapon_damage;

        Integer timer_duration;
        Integer timer_ticks;

        String variable_type;

        Integer team_index;
        Integer team_color;

        Actions action_type;
    }

    private class ListViewAdapter extends ArrayAdapter<ListViewItem> {
        private final Context context;
        private final ArrayList<ListViewItem> modelsArrayList;

        public ListViewAdapter(Context context, ArrayList<ListViewItem> modelsArrayList) {
            super(context, R.layout.list_item_file_list, modelsArrayList);

            this.context = context;
            this.modelsArrayList = modelsArrayList;
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final ViewHolder mViewHolder;
            View v = convertView;

            if(v==null || !((ViewHolder)v.getTag()).type.equals(modelsArrayList.get(position).type)) {
                mViewHolder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                mViewHolder.type = modelsArrayList.get(position).type;
                switch(modelsArrayList.get(position).type){
                    case CAPTION:
                        v = inflater.inflate(R.layout.game_config_caption_layout, parent, false);
                        mViewHolder.caption_text = (TextView) v.findViewById(R.id.game_config_item_caption_text);
                        break;
                    case WEAPON:
                        v = inflater.inflate(R.layout.game_config_weapon_layout, parent, false);
                        mViewHolder.name_text = (TextView) v.findViewById(R.id.game_config_item_weapon_name);
                        mViewHolder.weapon_allowed = (TextView) v.findViewById(R.id.game_config_item_weapon_allowed);
                        mViewHolder.weapon_damage = (TextView) v.findViewById(R.id.game_config_item_weapon_damagesign);
                        mViewHolder.weapon_frequency = (TextView) v.findViewById(R.id.game_config_item_weapon_frequency);
                        mViewHolder.weapon_range = (TextView) v.findViewById(R.id.game_config_item_weapon_range);
                        break;
                    case TIMER:
                        v = inflater.inflate(R.layout.game_config_timer_layout, parent, false);
                        mViewHolder.name_text = (TextView) v.findViewById(R.id.game_config_item_timer_name);
                        mViewHolder.timer_duration = (TextView) v.findViewById(R.id.game_config_item_timer_duration);
                        mViewHolder.timer_ticks = (TextView) v.findViewById(R.id.game_config_item_timer_ticks);
                        break;
                    case VARIABLE:
                        v = inflater.inflate(R.layout.game_config_variable_layout, parent, false);
                        mViewHolder.name_text = (TextView) v.findViewById(R.id.game_config_item_variable_name);
                        mViewHolder.value_text =(TextView) v.findViewById(R.id.game_config_item_variable_value);
                        mViewHolder.variable_type = (TextView) v.findViewById(R.id.game_config_item_variable_type);
                        break;
                    case TEAM:
                        v = inflater.inflate(R.layout.game_config_team_layout, parent, false);
                        mViewHolder.name_text = (TextView) v.findViewById(R.id.game_config_item_team_name);
                        mViewHolder.value_text = (TextView) v.findViewById(R.id.game_config_item_team_color);
                        mViewHolder.team_color = (ImageView) v.findViewById(R.id.game_config_item_team_color_view);
                        break;
                    case ACTION:
                        v = inflater.inflate(R.layout.game_config_action_layout, parent, false);
                        mViewHolder.name_text = (TextView) v.findViewById(R.id.game_config_item_action_name);
                        break;
                    case SIMPLEVALUE:
                    default:
                        v = inflater.inflate(R.layout.game_config_simple_value_layout, parent, false);
                        mViewHolder.name_text = (TextView) v.findViewById(R.id.game_config_item_simple_value_name_text);
                        mViewHolder.value_text = (TextView) v.findViewById(R.id.game_config_item_simple_value_text);
                        break;
                }

                v.setTag(mViewHolder);
            } else {
                mViewHolder = (ViewHolder) v.getTag();
            }

            // 4. Set the text for textView
            switch (modelsArrayList.get(position).type){
                case CAPTION:
                    mViewHolder.caption_text.setText(modelsArrayList.get(position).caption_text);
                    break;
                case WEAPON:
                    mViewHolder.name_text.setText(modelsArrayList.get(position).name_text+" ("+modelsArrayList.get(position).weapon_index.toString()+")");
                    if(modelsArrayList.get(position).weapon_allowed){
                        mViewHolder.weapon_allowed.setText(getResources().getString(R.string.allowed));
                    } else {
                        mViewHolder.weapon_allowed.setText(getResources().getString(R.string.notallowed));
                    }
                    if(modelsArrayList.get(position).weapon_damage<0){
                        mViewHolder.weapon_damage.setText("-");
                    } else {
                        mViewHolder.weapon_damage.setText("+");
                    }
                    mViewHolder.weapon_frequency.setText(modelsArrayList.get(position).weapon_frequency.toString()+" Hz");
                    mViewHolder.weapon_range.setText(modelsArrayList.get(position).weapon_range.toString());
                    break;
                case TIMER:
                    mViewHolder.name_text.setText(modelsArrayList.get(position).name_text);
                    mViewHolder.timer_duration.setText(String.format("%d:%02d:%02d",modelsArrayList.get(position).timer_duration/3600,
                            (modelsArrayList.get(position).timer_duration % 3600)/60,
                            modelsArrayList.get(position).timer_duration % 60));
                    mViewHolder.timer_ticks.setText(String.format("%d:%02d:%02d",modelsArrayList.get(position).timer_ticks/3600,
                            (modelsArrayList.get(position).timer_ticks % 3600)/60,
                            modelsArrayList.get(position).timer_ticks % 60));
                    break;
                case VARIABLE:
                    mViewHolder.name_text.setText(modelsArrayList.get(position).name_text);
                    mViewHolder.value_text.setText(modelsArrayList.get(position).value_text);
                    mViewHolder.variable_type.setText(modelsArrayList.get(position).variable_type);
                    break;
                case TEAM:
                    mViewHolder.name_text.setText(modelsArrayList.get(position).name_text);
                    mViewHolder.value_text.setText(modelsArrayList.get(position).value_text);
                    mViewHolder.team_color.setBackgroundColor(modelsArrayList.get(position).team_color);
                    break;
                case ACTION:
                    mViewHolder.name_text.setText(modelsArrayList.get(position).name_text);
                    break;
                default:
                case SIMPLEVALUE:
                    mViewHolder.name_text.setText(modelsArrayList.get(position).name_text);
                    mViewHolder.value_text.setText(modelsArrayList.get(position).value_text);
                    break;
            }

            // 5. return rowView
            return v;
        }
    }

    static class ViewHolder{
        private ItemType type;
        private TextView caption_text;
        private TextView name_text;
        private TextView value_text;

        private TextView weapon_allowed;
        private TextView weapon_damage;
        private TextView weapon_frequency;
        private TextView weapon_range;

        private TextView timer_duration;
        private TextView timer_ticks;

        private TextView variable_type;

        private ImageView team_color;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_config);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        colortextlist = new ArrayList<>();
        ArrayList<String> colortexts = new ArrayList<>();
        colortexts.addAll(Arrays.asList(getResources().getStringArray(R.array.team_color_names)));
        ArrayList<String> colorsl = new ArrayList<>();
        colorsl.addAll(Arrays.asList(getResources().getStringArray(R.array.team_color_names_language)));
        ArrayList<Integer> color_values = new ArrayList<>();
        int[] colors = getResources().getIntArray(R.array.team_color_values);
        for(Integer i=0;i<colors.length;i++) color_values.add(colors[i]);
        for(Integer i=0;i<colortexts.size();i++){
            colortextlist.add(new ColorTextView(colortexts.get(i),colorsl.get(i),color_values.get(i)));
        }
        colortextadapter = new ColorTextAdapter(this,colortextlist);

        game_config_list = new ArrayList<>();
        game_config_adapter = new ListViewAdapter(this,game_config_list);
        final ListView lv = (ListView)findViewById(R.id.game_config_listview);
        lv.setAdapter(game_config_adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Integer pos = position;
                switch (game_config_list.get(position).type){
                    case CAPTION:{
                        break;}
                    case SIMPLEVALUE:{
                        if(game_config_list.get(position).simple_value_type.equals("seconds")){
                            LayoutInflater inflater = GameConfigActivity.this.getLayoutInflater();
                            final View dialogview = inflater.inflate(R.layout.game_config_time_dialog_layout,null);
                            NumberPicker hour_view = ((NumberPicker)dialogview.findViewById(R.id.game_config_time_dialog_hours));
                            NumberPicker min_view = ((NumberPicker)dialogview.findViewById(R.id.game_config_time_dialog_minutes));
                            NumberPicker sec_view = ((NumberPicker)dialogview.findViewById(R.id.game_config_time_dialog_seconds));

                            hour_view.setMinValue(0);
                            hour_view.setMaxValue(100);
                            min_view.setMinValue(0);
                            min_view.setMaxValue(59);
                            sec_view.setMinValue(0);
                            sec_view.setMaxValue(59);

                            hour_view.setValue(((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration/3600);
                            min_view.setValue((((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration%3600)/60);
                            sec_view.setValue(((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration%60);

                            new AlertDialog.Builder(GameConfigActivity.this)
                                    .setMessage(getResources().getString(R.string.ChooseTimeValue))
                                    .setView(dialogview)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            String name = game_config_list.get(pos).name_text;
                                            ((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration =
                                                    ((NumberPicker)dialogview.findViewById(R.id.game_config_time_dialog_seconds)).getValue()
                                                    +((NumberPicker)dialogview.findViewById(R.id.game_config_time_dialog_minutes)).getValue()*60
                                                    +((NumberPicker)dialogview.findViewById(R.id.game_config_time_dialog_hours)).getValue()*3600;
                                            game_config_list.get(pos).value_text = String.format("%d:%02d:%02d",
                                                    ((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration/3600,
                                                    (((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration % 3600)/60,
                                                    ((GameFileLoader.Timer)gi.definitions.get("Timer_GameTimer").value).duration % 60);
                                            game_config_adapter.notifyDataSetChanged();
                                        }
                                    })
                                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    })
                                    .show();
                        }
                        break;}
                    case TEAM:{
                        LayoutInflater inflater = GameConfigActivity.this.getLayoutInflater();
                        final View dialogview = inflater.inflate(R.layout.game_config_team_dialog_layout,null);
                        Spinner sp = ((Spinner)dialogview.findViewById(R.id.game_config_team_dialog_color_spinner));
                        sp.setAdapter(colortextadapter);
                        for(Integer i=0;i<colortextlist.size();i++){
                            if(colortextlist.get(i).text.equals(game_config_list.get(pos).value_text)){
                                sp.setSelection(i);
                                break;
                            }
                        }

                        String name = game_config_list.get(pos).name_text;
                        ((TextView)dialogview.findViewById(R.id.game_config_team_dialog_name)).setText(name.substring(0,name.indexOf(" (")));
                        new AlertDialog.Builder(GameConfigActivity.this)
                                .setMessage(getResources().getString(R.string.EditTeamInfo))
                                .setView(dialogview)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Integer index = game_config_list.get(pos).team_index;
                                        String name = ((TextView)dialogview.findViewById(R.id.game_config_team_dialog_name)).getText().toString();
                                        String color_old = ((TextView)((Spinner)dialogview.findViewById(R.id.game_config_team_dialog_color_spinner)).getSelectedView().findViewById(R.id.item_textview_textview)).getText().toString();
                                        String color=color_old;
                                        Integer color_value=0;

                                        for(ColorTextView c : colortextlist){
                                            if(color_old.equals(c.text_lang)){
                                                color = c.text;
                                                color_value = c.color;
                                                break;
                                            }
                                        }

                                        ((GameFileLoader.Team)gi.definitions.get("Team_"+index.toString()).value).name = name;
                                        ((GameFileLoader.Team)gi.definitions.get("Team_"+index.toString()).value).color = color;
                                        game_config_list.get(pos).name_text =name + " ("+index.toString()+")";
                                        game_config_list.get(pos).value_text = color_old;
                                        game_config_list.get(pos).team_color = color_value;
                                        game_config_adapter.notifyDataSetChanged();
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .show();
                        break;}
                    case WEAPON:{
                        LayoutInflater inflater = GameConfigActivity.this.getLayoutInflater();
                        final View dialogview = inflater.inflate(R.layout.game_config_weapon_dialog_layout,null);
                        ((TextView)dialogview.findViewById(R.id.game_config_weapon_dialog_name)).setText(game_config_list.get(pos).name_text);
                        ((EditText)dialogview.findViewById(R.id.game_config_weapon_dialog_frequency)).setText(game_config_list.get(pos).weapon_frequency.toString());
                        if(game_config_list.get(pos).weapon_damage.equals(-1)){
                            ((RadioGroup)dialogview.findViewById(R.id.game_config_weapon_dialog_damagesign)).check(R.id.game_config_weapon_dialog_damagesign_minus);
                        } else {
                            ((RadioGroup)dialogview.findViewById(R.id.game_config_weapon_dialog_damagesign)).check(R.id.game_config_weapon_dialog_damagesign_plus);
                        }
                        ((EditText)dialogview.findViewById(R.id.game_config_weapon_dialog_range)).setText(game_config_list.get(pos).weapon_range.toString());
                        ((Switch)dialogview.findViewById(R.id.game_config_weapon_dialog_allowed)).setChecked(game_config_list.get(pos).weapon_allowed);

                        new AlertDialog.Builder(GameConfigActivity.this)
                                .setMessage(getResources().getString(R.string.EditWeaponInfo))
                                .setView(dialogview)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Integer index = game_config_list.get(pos).weapon_index;

                                        ((GameFileLoader.WeaponInfo)gi.definitions.get("Weapon"+index.toString()).value).name = ((EditText)dialogview.findViewById(R.id.game_config_weapon_dialog_name)).getText().toString();
                                        game_config_list.get(pos).name_text = ((EditText)dialogview.findViewById(R.id.game_config_weapon_dialog_name)).getText().toString();
                                        ((GameFileLoader.WeaponInfo)gi.definitions.get("Weapon"+index.toString()).value).
                                                shot_frequency = Double.parseDouble(
                                                ((EditText)dialogview.findViewById(R.id.game_config_weapon_dialog_frequency)).getText().toString());
                                        game_config_list.get(pos).weapon_frequency = Double.parseDouble(
                                                ((EditText)dialogview.findViewById(R.id.game_config_weapon_dialog_frequency)).getText().toString());
                                        ((GameFileLoader.WeaponInfo)gi.definitions.get("Weapon"+index.toString()).value).range =
                                                Integer.parseInt(((EditText)dialogview.findViewById(R.id.game_config_weapon_dialog_range)).getText().toString());
                                        game_config_list.get(pos).weapon_range = Integer.parseInt(((EditText)dialogview.findViewById(R.id.game_config_weapon_dialog_range)).getText().toString());
                                        ((GameFileLoader.WeaponInfo)gi.definitions.get("Weapon"+index.toString()).value).allowed =
                                                ((Switch)dialogview.findViewById(R.id.game_config_weapon_dialog_allowed)).isChecked();
                                        game_config_list.get(pos).weapon_allowed = ((Switch)dialogview.findViewById(R.id.game_config_weapon_dialog_allowed)).isChecked();
                                        if(((RadioButton)dialogview.findViewById(R.id.game_config_weapon_dialog_damagesign_plus)).isChecked()){
                                            ((GameFileLoader.WeaponInfo)gi.definitions.get("Weapon"+index.toString()).value).damage_sign = 1;
                                            game_config_list.get(pos).weapon_damage = 1;
                                        } else{
                                            ((GameFileLoader.WeaponInfo)gi.definitions.get("Weapon"+index.toString()).value).damage_sign = -1;
                                            game_config_list.get(pos).weapon_damage = -1;
                                        }

                                        game_config_adapter.notifyDataSetChanged();
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .show();
                        break;}
                    case TIMER:{
                        LayoutInflater inflater = GameConfigActivity.this.getLayoutInflater();
                        final View dialogview = inflater.inflate(R.layout.game_config_weapon_dialog_layout,null);
                        NumberPicker duration_hour = (NumberPicker)dialogview.findViewById(R.id.game_config_timer_dialog_duration_hours);
                        NumberPicker duration_min = (NumberPicker)dialogview.findViewById(R.id.game_config_timer_dialog_duration_minutes);
                        NumberPicker duration_sec = (NumberPicker)dialogview.findViewById(R.id.game_config_timer_dialog_duration_seconds);
                        duration_hour.setMinValue(0);
                        duration_min.setMinValue(0);
                        duration_min.setMaxValue(59);
                        duration_sec.setMinValue(0);
                        duration_sec.setMaxValue(59);
                        duration_hour.setValue(((GameFileLoader.Timer)gi.definitions.get("Timer_"+game_config_list.get(pos).name_text).value).duration/3600);
                        duration_min.setValue((((GameFileLoader.Timer)gi.definitions.get("Timer_"+game_config_list.get(pos).name_text).value).duration%3600)/60);
                        duration_sec.setValue(((GameFileLoader.Timer)gi.definitions.get("Timer_"+game_config_list.get(pos).name_text).value).duration%60);

                        NumberPicker ticks_hour = (NumberPicker)dialogview.findViewById(R.id.game_config_timer_dialog_ticks_hours);
                        NumberPicker ticks_min = (NumberPicker)dialogview.findViewById(R.id.game_config_timer_dialog_ticks_minutes);
                        NumberPicker ticks_sec = (NumberPicker)dialogview.findViewById(R.id.game_config_timer_dialog_ticks_seconds);
                        ticks_hour.setMinValue(0);
                        ticks_min.setMinValue(0);
                        ticks_min.setMaxValue(59);
                        ticks_sec.setMinValue(0);
                        ticks_sec.setMaxValue(59);
                        ticks_hour.setValue(((GameFileLoader.Timer)gi.definitions.get("Timer_"+game_config_list.get(pos).name_text).value).ticks/3600);
                        ticks_min.setValue((((GameFileLoader.Timer)gi.definitions.get("Timer_"+game_config_list.get(pos).name_text).value).ticks%3600)/60);
                        ticks_sec.setValue(((GameFileLoader.Timer)gi.definitions.get("Timer_"+game_config_list.get(pos).name_text).value).ticks%60);

                        new AlertDialog.Builder(GameConfigActivity.this)
                                .setMessage(getResources().getString(R.string.EditTimerInfo))
                                .setView(dialogview)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        String name = game_config_list.get(pos).name_text;
                                        ((GameFileLoader.Timer)gi.definitions.get("Timer_"+name).value).duration =
                                                ((NumberPicker)dialogview.findViewById(R.id.game_config_timer_dialog_duration_hours)).getValue()*3600
                                                +((NumberPicker)dialogview.findViewById(R.id.game_config_timer_dialog_duration_minutes)).getValue()*60
                                                +((NumberPicker)dialogview.findViewById(R.id.game_config_timer_dialog_duration_seconds)).getValue();
                                        ((GameFileLoader.Timer)gi.definitions.get("Timer_"+name).value).ticks =
                                                ((NumberPicker)dialogview.findViewById(R.id.game_config_timer_dialog_ticks_hours)).getValue()*3600
                                                        +((NumberPicker)dialogview.findViewById(R.id.game_config_timer_dialog_ticks_minutes)).getValue()*60
                                                        +((NumberPicker)dialogview.findViewById(R.id.game_config_timer_dialog_ticks_seconds)).getValue();

                                        game_config_list.get(pos).timer_duration = ((GameFileLoader.Timer)gi.definitions.get("Timer_"+name).value).duration;
                                        game_config_list.get(pos).timer_ticks = ((GameFileLoader.Timer)gi.definitions.get("Timer_"+name).value).ticks;

                                        game_config_adapter.notifyDataSetChanged();
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .show();
                        break;}
                    case VARIABLE:{
                        LayoutInflater inflater = GameConfigActivity.this.getLayoutInflater();
                        final View dialogview = inflater.inflate(R.layout.game_config_variable_dialog_layout,null);
                        if(game_config_list.get(pos).variable_type.equals("int")){
                            ((EditText)dialogview.findViewById(R.id.game_config_variable_dialog_value)).setInputType(InputType.TYPE_CLASS_NUMBER);
                        } else if(game_config_list.get(pos).variable_type.equals("double")){
                            ((EditText)dialogview.findViewById(R.id.game_config_variable_dialog_value)).setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        } else if(game_config_list.get(pos).variable_type.equals("boolean")){
                            ((EditText)dialogview.findViewById(R.id.game_config_variable_dialog_value)).setInputType(InputType.TYPE_CLASS_TEXT);
                        } else {// string as default
                            ((EditText)dialogview.findViewById(R.id.game_config_variable_dialog_value)).setInputType(InputType.TYPE_CLASS_TEXT);
                        }
                        ((EditText)dialogview.findViewById(R.id.game_config_variable_dialog_value)).setText(game_config_list.get(pos).value_text);

                        new AlertDialog.Builder(GameConfigActivity.this)
                                .setMessage(getResources().getString(R.string.NewVariableValue))
                                .setView(dialogview)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        String name = game_config_list.get(pos).name_text;
                                        String type = game_config_list.get(pos).variable_type;
                                        switch(type) {
                                            case "int": {
                                                gi.game_variables.get(name).value = Integer.parseInt(((EditText) dialogview.findViewById(R.id.game_config_variable_dialog_value)).getText().toString());
                                                game_config_list.get(pos).value_text = gi.game_variables.get(name).value.toString();
                                                break;
                                            }
                                            case "double": {
                                                gi.game_variables.get(name).value = Double.parseDouble(((EditText) dialogview.findViewById(R.id.game_config_variable_dialog_value)).getText().toString());
                                                game_config_list.get(pos).value_text = gi.game_variables.get(name).value.toString();
                                                break;
                                            }
                                            case "boolean": {
                                                String value = ((EditText) dialogview.findViewById(R.id.game_config_variable_dialog_value)).getText().toString().toLowerCase();
                                                if(!(value.equals("true") || value.equals("false"))){
                                                    Toast.makeText(GameConfigActivity.this,
                                                            getResources().getString(R.string.NoValidValueMessage),
                                                            Toast.LENGTH_SHORT).show();
                                                    return;
                                                }
                                                gi.game_variables.get(name).value = Boolean.parseBoolean(((EditText) dialogview.findViewById(R.id.game_config_variable_dialog_value)).getText().toString());
                                                break;
                                            }
                                            case "string":
                                            default: {
                                                gi.game_variables.get(name).value = ((EditText) dialogview.findViewById(R.id.game_config_variable_dialog_value)).getText().toString();
                                                game_config_list.get(pos).value_text = gi.game_variables.get(name).value.toString();
                                                break;
                                            }
                                        }
                                        game_config_adapter.notifyDataSetChanged();
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .show();
                        break;}
                    case ACTION:{
                        switch (game_config_list.get(pos).action_type){
                            case ADD_TEAM:{
                                Integer new_index=-1;
                                for(Integer i=0;i<GameFileLoader.max_team_number;i++){
                                    if(!gi.definitions.containsKey("Team_"+i.toString())){
                                        new_index=i;
                                        break;
                                    }
                                }
                                if(new_index.equals(-1)) break;
                                final Integer index = new_index;

                                LayoutInflater inflater = GameConfigActivity.this.getLayoutInflater();
                                final View dialogview = inflater.inflate(R.layout.game_config_team_dialog_layout,null);
                                Spinner sp = ((Spinner)dialogview.findViewById(R.id.game_config_team_dialog_color_spinner));
                                sp.setAdapter(colortextadapter);
                                sp.setSelection(0);

                                ((TextView)dialogview.findViewById(R.id.game_config_team_dialog_name)).setText(getResources().getString(R.string.DefaultTeamName));
                                new AlertDialog.Builder(GameConfigActivity.this)
                                        .setMessage(getResources().getString(R.string.AddNewTeam))
                                        .setView(dialogview)
                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                String name = ((EditText)dialogview.findViewById(R.id.game_config_team_dialog_name)).getText().toString();
                                                String color_old = ((TextView)((Spinner)dialogview.findViewById(R.id.game_config_team_dialog_color_spinner)).getSelectedView().findViewById(R.id.item_textview_textview)).getText().toString();
                                                String color=color_old;
                                                Integer color_value=0;

                                                for(ColorTextView c : colortextlist){
                                                    if(color_old.equals(c.text_lang)){
                                                        color = c.text;
                                                        color_value = c.color;
                                                        break;
                                                    }
                                                }

                                                addTeam(pos,name,color_old,index);

                                                Message msg = new Message();
                                                msg.what = 15;
                                                Bundle data = new Bundle();
                                                data.putInt("Index",index);
                                                data.putString("Name",name);
                                                data.putString("Color",color);
                                                msg.setData(data);
                                                Singleton.getInstance().getGameLogicHandler().sendMessage(msg);

                                                boolean flag=false;
                                                for(Integer i=0;i<GameFileLoader.max_team_number;i++){
                                                    if(!gi.definitions.containsKey("Team_"+i.toString()) && !i.equals(index)){
                                                        flag=true;
                                                        break;
                                                    }
                                                }
                                                if(!flag) {
                                                    game_config_list.remove(game_config_list.get(pos+1));
                                                    game_config_adapter.notifyDataSetChanged();
                                                }
                                            }
                                        })
                                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        })
                                        .show();

                                break;}
                            case ADD_WEAPON:{
                                Integer new_index=-1;
                                for(Integer i=0;i<GameFileLoader.max_weapon_type;i++){
                                    if(!gi.definitions.containsKey("Weapon"+i.toString())){
                                        new_index=i;
                                        break;
                                    }
                                }
                                if(new_index.equals(-1)) break;
                                final Integer index = new_index;

                                LayoutInflater inflater = GameConfigActivity.this.getLayoutInflater();
                                final View dialogview = inflater.inflate(R.layout.game_config_weapon_dialog_layout,null);
                                ((TextView)dialogview.findViewById(R.id.game_config_weapon_dialog_name)).setText(getResources().getString(R.string.DefaultWeaponName));
                                ((EditText)dialogview.findViewById(R.id.game_config_weapon_dialog_frequency)).setText("2");
                                ((RadioGroup)dialogview.findViewById(R.id.game_config_weapon_dialog_damagesign)).check(R.id.game_config_weapon_dialog_damagesign_plus);
                                ((EditText)dialogview.findViewById(R.id.game_config_weapon_dialog_range)).setText("1");
                                ((Switch)dialogview.findViewById(R.id.game_config_weapon_dialog_allowed)).setChecked(true);
                                new AlertDialog.Builder(GameConfigActivity.this)
                                        .setMessage(getResources().getString(R.string.AddWeapon))
                                        .setView(dialogview)
                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                String name = ((EditText)dialogview.findViewById(R.id.game_config_weapon_dialog_name)).getText().toString();
                                                Double frequency = Double.parseDouble(((EditText)dialogview.findViewById(R.id.game_config_weapon_dialog_frequency)).getText().toString());
                                                Integer range = Integer.parseInt(((EditText) dialogview.findViewById(R.id.game_config_weapon_dialog_range)).getText().toString());
                                                Boolean allowed = ((Switch)dialogview.findViewById(R.id.game_config_weapon_dialog_allowed)).isChecked();
                                                Integer damagesign = 1;
                                                if(((RadioButton)dialogview.findViewById(R.id.game_config_weapon_dialog_damagesign_plus)).isChecked()){
                                                    damagesign = 1;
                                                } else {
                                                    damagesign = -1;
                                                }

                                                addWeapon(pos,name,index,damagesign,frequency,range,allowed);

                                                Message msg = new Message();
                                                msg.what = 14;
                                                Bundle data = new Bundle();
                                                data.putInt("Index",index);
                                                data.putString("Name",name);
                                                data.putDouble("Frequency",frequency);
                                                data.putInt("Range",range);
                                                data.putBoolean("Allowed",allowed);
                                                data.putInt("DamageSign",damagesign);
                                                msg.setData(data);
                                                Singleton.getInstance().getGameLogicHandler().sendMessage(msg);

                                                boolean flag =false;
                                                for(Integer i=0;i<GameFileLoader.max_weapon_type;i++){
                                                    if(!gi.definitions.containsKey("Weapon"+i.toString()) && !i.equals(index)){
                                                        flag = true;
                                                        break;
                                                    }
                                                }
                                                if(!flag){
                                                    game_config_list.remove(game_config_list.get(pos+1));
                                                    game_config_adapter.notifyDataSetChanged();
                                                }
                                            }
                                        })
                                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        })
                                        .show();
                                break;}
                        }
                        break;}
                }
            }
        });

        Message g_msg = new Message();
        g_msg.what=3;
        g_msg.obj = mainHandler;
        Singleton.getInstance().getGameLogicHandler().sendMessage(g_msg);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_game_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id){
            case R.id.menu_save_game_to_file:{
                LayoutInflater inflater = GameConfigActivity.this.getLayoutInflater();
                final View dialogview = inflater.inflate(R.layout.save_game_to_file_dialog_layout,null);
                ArrayList<String> ext = new ArrayList<String>();
                ext.add(".xml");
                ((SelectXMLFileFragment)getSupportFragmentManager().findFragmentById(R.id.save_game_to_file_select_path_fragment)).setExtensions(ext);

                final AlertDialog dialog = new AlertDialog.Builder(GameConfigActivity.this)
                        .setMessage(getResources().getString(R.string.SaveGameToFile))
                        .setView(dialogview)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                Fragment fragment = (getSupportFragmentManager().findFragmentById(R.id.save_game_to_file_select_path_fragment));
                                FragmentTransaction ft = GameConfigActivity.this.getSupportFragmentManager().beginTransaction();
                                ft.remove(fragment);
                                ft.commit();
                            }
                        }).show();
                ((SelectXMLFileFragment)getSupportFragmentManager().findFragmentById(R.id.save_game_to_file_select_path_fragment)).setSelectXMLFileFragmentListener(new SelectXMLFileFragment.SelectXMLFileFragmentListener() {
                    @Override
                    public void onFileChosen(String path, String file_name) {
                        ((EditText)dialogview.findViewById(R.id.save_game_to_file_name)).setText(file_name);
                    }

                    @Override
                    public void onInfoButtonClicked(String game_name, String description) {

                    }

                    @Override
                    public void onHideDescription() {

                    }

                    @Override
                    public void onFolderChanged(String path){

                    }
                });
                ((Button)dialogview.findViewById(R.id.save_game_to_file_cancel)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                ((Button)dialogview.findViewById(R.id.save_game_to_file_ok)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String path = ((SelectXMLFileFragment)getSupportFragmentManager()
                                .findFragmentById(R.id.save_game_to_file_select_path_fragment))
                                .getCurrentFolderPath()
                                + File.separator
                                + ((EditText)dialogview.findViewById(R.id.save_game_to_file_name)).getText().toString();
                        if(path.length()==0) return;
                        if(!path.endsWith(".xml")) path+=".xml";
                        Boolean overwrite = ((CheckBox)dialogview.findViewById(R.id.save_game_to_file_overwrite)).isChecked();
                        GameFileLoader loader = new GameFileLoader(GameConfigActivity.this);
                        try {
                            loader.write_game_info_to_file(gi, path, overwrite);
                        } catch(IOException e){
                            Log.e(LOG_TAG,"IOException during SaveGameToFile!");
                        }
                        dialog.dismiss();
                    }
                });
                return true;}
        }

        return super.onOptionsItemSelected(item);
    }

    private void addCaption(String text){
        ListViewItem item = new ListViewItem();
        item.type = ItemType.CAPTION;
        item.caption_text = text;

        game_config_list.add(item);
        game_config_adapter.notifyDataSetChanged();
    }

    private void addSimpleValue(String name, String value, String type){
        ListViewItem item = new ListViewItem();
        item.type = ItemType.SIMPLEVALUE;
        item.name_text = name;
        item.value_text = value;
        item.simple_value_type = type;

        game_config_list.add(item);
        game_config_adapter.notifyDataSetChanged();
    }

    private void addTeam(String name, String color, Integer index){
        ListViewItem item = new ListViewItem();
        item.type = ItemType.TEAM;
        item.name_text = name + " ("+index.toString()+")";
        item.value_text = color;
        item.team_index = index;

        for(ColorTextView c : colortextlist){
            if(c.text_lang.equals(color)){
                item.team_color = c.color;
                break;
            }
        }

        game_config_list.add(item);
        game_config_adapter.notifyDataSetChanged();
    }

    private void addTeam(Integer position, String name, String color, Integer index){
        ListViewItem item = new ListViewItem();
        item.type = ItemType.TEAM;
        item.name_text = name + " ("+index.toString()+")";
        item.value_text = color;
        item.team_index = index;

        for(ColorTextView c : colortextlist){
            if(c.text_lang.equals(color)){
                item.team_color = c.color;
                break;
            }
        }

        game_config_list.add(position,item);
        game_config_adapter.notifyDataSetChanged();
    }

    private void addWeapon(String name, Integer index, Integer damage_sign, Double frequency, Integer range, Boolean allowed){
        ListViewItem item = new ListViewItem();
        item.type = ItemType.WEAPON;
        item.name_text = name;
        item.weapon_index = index;
        item.weapon_damage = damage_sign;
        item.weapon_frequency = frequency;
        item.weapon_range = range;
        item.weapon_allowed = allowed;

        game_config_list.add(item);
        game_config_adapter.notifyDataSetChanged();
    }

    private void addWeapon(Integer position,String name, Integer index, Integer damage_sign, Double frequency, Integer range, Boolean allowed){
        ListViewItem item = new ListViewItem();
        item.type = ItemType.WEAPON;
        item.name_text = name;
        item.weapon_index = index;
        item.weapon_damage = damage_sign;
        item.weapon_frequency = frequency;
        item.weapon_range = range;
        item.weapon_allowed = allowed;

        game_config_list.add(position,item);
        game_config_adapter.notifyDataSetChanged();
    }

    private void addTimer(String name, Integer duration, Integer ticks){
        ListViewItem item = new ListViewItem();
        item.type = ItemType.TIMER;
        item.name_text = name;
        item.timer_duration = duration;
        item.timer_ticks = ticks;

        game_config_list.add(item);
        game_config_adapter.notifyDataSetChanged();
    }

    private void addVariable(String name, String value, String type){
        ListViewItem item = new ListViewItem();
        item.type = ItemType.VARIABLE;
        item.name_text = name;
        item.value_text = value;
        item.variable_type = type;

        game_config_list.add(item);
        game_config_adapter.notifyDataSetChanged();
    }

    private void addAction(String name, Actions type){
        ListViewItem item = new ListViewItem();
        item.type = ItemType.ACTION;
        item.name_text = name;
        item.action_type = type;

        game_config_list.add(item);
        game_config_adapter.notifyDataSetChanged();
    }

    private enum ItemType{CAPTION, SIMPLEVALUE, WEAPON, TIMER, VARIABLE, TEAM, ACTION}

    private enum Actions{ADD_WEAPON,ADD_TEAM}

    private ArrayList<Map.Entry> sortEntryListLexically(List<Map.Entry> list, Boolean use_key){
        ArrayList<Map.Entry> list_c = new ArrayList<>(list);
        ArrayList<Map.Entry> n_list = new ArrayList<>();
        if(list.size()==0) return n_list;

        while(list_c.size()>0) {
            Map.Entry lowest = list_c.get(0);
            for (Map.Entry entry : list_c) {
                if (use_key) {
                    if (entry.getKey().toString().compareToIgnoreCase(lowest.getKey().toString()) < 0) {
                        lowest = entry;
                    }
                } else {
                    if (entry.getValue().toString().compareToIgnoreCase(lowest.getValue().toString()) < 0) {
                        lowest = entry;
                    }
                }
            }
            n_list.add(lowest);
            list_c.remove(lowest);
        }

        return n_list;
    }


    public void onFileChosen(String path, String file_name){

    }

    public void onInfoButtonClicked(String game_name, String description){

    }

    public void onHideDescription(){

    }

    public void onFolderChanged(String path){

    }
}
