package de.c_ebberg.openlasertag;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

public class ManageUsersActivity extends BaseActivity {
    ArrayList<User> user_list;
    User_Adapter user_adapter;
    final String LOG_TAG = "ManageUsersActivity";
    Integer mark_color;
    Integer divider_color;
    Integer background_color;

    private class User {
        String name;
        String alias;
        Integer played_games;
        Boolean current_user;

        public User(String _name, String _alias, Integer _played_games, Boolean current) {
            name = _name;
            alias = _alias;
            played_games = _played_games;
            current_user = current;
        }
    }

    private class User_Adapter extends ArrayAdapter<User> {
        private final Context context;
        private final ArrayList<User> modelsArrayList;

        public User_Adapter(Context context, ArrayList<User> modelsArrayList) {
            super(context, R.layout.list_item_manage_user_list, modelsArrayList);

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

                v = inflater.inflate(R.layout.list_item_manage_user_list, parent, false);
                mViewHolder.layout = (RelativeLayout) v.findViewById(R.id.manage_user_item_layout);
                mViewHolder.name_text = (TextView) v.findViewById(R.id.manage_user_name_text);
                mViewHolder.alias_text = (TextView) v.findViewById(R.id.manage_user_alias_text);
                mViewHolder.played_games_text = (TextView) v.findViewById(R.id.manage_user_played_games_text);
                mViewHolder.rahmen_bottom = (ImageView) v.findViewById(R.id.manage_user_rahmen_bottom);
                mViewHolder.rahmen_left = (ImageView) v.findViewById(R.id.manage_user_rahmen_left);
                mViewHolder.rahmen_right = (ImageView) v.findViewById(R.id.manage_user_rahmen_right);
                mViewHolder.rahmen_top = (ImageView) v.findViewById(R.id.manage_user_rahmen_top);
                mViewHolder.edit_button = (ImageButton) v.findViewById(R.id.manage_user_edit_button);
                mViewHolder.stats_button = (ImageButton) v.findViewById(R.id.manage_user_show_stats_button);
                mViewHolder.stats_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos=0;
                        for(int i=0;i<modelsArrayList.size();i++){
                            if(modelsArrayList.get(i).name.equals(mViewHolder.name_text.getText())){
                                pos=i;
                                break;
                            }
                        }
                        Intent intent = new Intent(ManageUsersActivity.this,StatsActivity.class);
                        intent.putExtra("User",modelsArrayList.get(pos).name);
                        startActivity(intent);
                    }
                });
                mViewHolder.edit_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LayoutInflater inflater = ManageUsersActivity.this.getLayoutInflater();
                        final View dialogview = inflater.inflate(R.layout.add_user_dialog_layout,null);
                        int pos=0;
                        for(int i=0;i<modelsArrayList.size();i++){
                            if(modelsArrayList.get(i).name.equals(mViewHolder.name_text.getText())){
                                pos=i;
                                break;
                            }
                        }
                        ((EditText)dialogview.findViewById(R.id.add_new_user_name_edit_text)).setText(modelsArrayList.get(pos).name);
                        ((EditText)dialogview.findViewById(R.id.add_new_user_alias_edit_text)).setText(modelsArrayList.get(pos).alias);
                        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ManageUsersActivity.this);
                        final Integer _pos=pos;
                        new AlertDialog.Builder(ManageUsersActivity.this)
                                .setMessage(getResources().getString(R.string.EnterNewUserDetails))
                                .setView(dialogview)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        sharedPref.edit().putString("User"+_pos.toString(),
                                                ((EditText)dialogview.findViewById(R.id.add_new_user_name_edit_text)).getText().toString()).apply();
                                        sharedPref.edit().putString("User"+_pos.toString()+"Alias",
                                                ((EditText)dialogview.findViewById(R.id.add_new_user_alias_edit_text)).getText().toString()).apply();
                                        user_list.get(_pos).name =((EditText)dialogview.findViewById(R.id.add_new_user_name_edit_text))
                                                .getText().toString();
                                        user_list.get(_pos).alias = ((EditText)dialogview.findViewById(R.id.add_new_user_alias_edit_text))
                                                .getText().toString();
                                        if(user_list.get(_pos).current_user){
                                            sharedPref.edit().putString("CurrentUser",
                                                    ((EditText)dialogview.findViewById(R.id.add_new_user_name_edit_text))
                                                            .getText().toString()).apply();
                                            sharedPref.edit().putString("CurrentAlias",
                                                    ((EditText)dialogview.findViewById(R.id.add_new_user_alias_edit_text))
                                                            .getText().toString()).apply();
                                            Singleton.getInstance().setCurrentUser(sharedPref.getString("CurrentUser",""));
                                            Singleton.getInstance().setCurrentAlias(sharedPref.getString("CurrentAlias",""));
                                        }
                                        user_adapter.notifyDataSetChanged();
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
                                    public void onClick(DialogInterface dialog, int which){
                                        dialog.dismiss();
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                });

                v.setTag(mViewHolder);
            } else {
                mViewHolder = (ViewHolder) v.getTag();
            }
            // 3. Get icon,title & counter views from the rowView
            //ImageView imgView = (ImageView) rowView.findViewById(R.id.item_icon);
            //TextView titleView = (TextView) rowView.findViewById(R.id.item_textview);

            // 4. Set the text for textView
            mViewHolder.name_text.setText(modelsArrayList.get(position).name);
            mViewHolder.alias_text.setText(modelsArrayList.get(position).alias);
            mViewHolder.played_games_text.setText(modelsArrayList.get(position).played_games.toString());
            if(modelsArrayList.get(position).current_user){
                mViewHolder.rahmen_top.setBackgroundColor(mark_color);
                mViewHolder.rahmen_right.setBackgroundColor(mark_color);
                mViewHolder.rahmen_bottom.setBackgroundColor(mark_color);
                mViewHolder.rahmen_left.setBackgroundColor(mark_color);
                mViewHolder.layout.setBackgroundColor(mark_color);
            } else {
                mViewHolder.rahmen_top.setBackgroundColor(divider_color);
                mViewHolder.rahmen_right.setBackgroundColor(divider_color);
                mViewHolder.rahmen_bottom.setBackgroundColor(divider_color);
                mViewHolder.rahmen_left.setBackgroundColor(divider_color);
                mViewHolder.layout.setBackgroundColor(background_color);
            }

            // 5. return rowView
            return v;
        }
    }

    static class ViewHolder{
        private TextView name_text;
        private TextView alias_text;
        private TextView played_games_text;
        private ImageView rahmen_left;
        private ImageView rahmen_top;
        private ImageView rahmen_bottom;
        private ImageView rahmen_right;
        private RelativeLayout layout;
        private ImageButton edit_button;
        private ImageButton stats_button;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        user_list = new ArrayList<>();
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        for(Integer i=0; sharedPref.contains("User"+i.toString());i++){
            Boolean current=false;
            if(sharedPref.getString("CurrentUser","").equals(sharedPref.getString("User"+i.toString(),""))){
                current = true;
            }
            Integer played_games = 0;
            try{
                played_games = StatsManager.getInstance().getStatsCollectionLength(sharedPref.getString("User"+i.toString(),""),ManageUsersActivity.this);
            } catch(IOException e){
                Log.e(LOG_TAG,"IOException during getStatsCollectionLength for ManageUsersActivity");
            }
            user_list.add(new User(sharedPref.getString("User"+i.toString(),""),
                    sharedPref.getString("User"+i.toString()+"Alias",""),
                    played_games,
                    current));
        }

        Resources.Theme themes = getTheme();
        TypedValue storedValueInTheme = new TypedValue();
        if (themes.resolveAttribute(R.attr.colorTextBackgroundAccent, storedValueInTheme, true)) {
            mark_color = storedValueInTheme.data;
        }
        storedValueInTheme = new TypedValue();
        if (themes.resolveAttribute(R.attr.colorItemDivider, storedValueInTheme, true)) {
            divider_color = storedValueInTheme.data;
        }
        storedValueInTheme = new TypedValue();
        if (themes.resolveAttribute(R.attr.colorBackground1, storedValueInTheme, true)) {
            background_color = storedValueInTheme.data;
        }

        user_adapter = new User_Adapter(this,user_list);
        ListView lv = (ListView) findViewById(R.id.manage_user_listview);
        lv.setAdapter(user_adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                for(User user : user_list){
                    user.current_user = false;
                }
                user_adapter.getItem(arg2).current_user = true;
                sharedPref.edit().putString("CurrentUser",user_adapter.getItem(arg2).name).apply();
                sharedPref.edit().putString("CurrentAlias",user_adapter.getItem(arg2).alias).apply();
                Singleton.getInstance().setCurrentUser(user_adapter.getItem(arg2).name);
                Singleton.getInstance().setCurrentAlias(user_adapter.getItem(arg2).alias);
                user_adapter.notifyDataSetChanged();
            }
        });

        Button remove_button = (Button) findViewById(R.id.manage_users_remove_button);
        remove_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sharedPref.getInt("UserNum",1)==1){
                    Toast.makeText(ManageUsersActivity.this, getResources().getString(R.string.CannotRemoveLastUser), Toast.LENGTH_SHORT).show();
                    return;
                }
                Integer user_pos=0;
                Integer user_num = sharedPref.getInt("UserNum",2);
                Integer new_user_pos=0;
                for(Integer i=0; sharedPref.contains("User"+i.toString());i++){
                    if(sharedPref.getString("User"+i.toString(),"").equals(sharedPref.getString("CurrentUser",""))){
                        user_pos=i;
                        if(user_pos<user_num-1){
                            sharedPref.edit().putString("CurrentUser",sharedPref.getString("User"+Integer.valueOf(user_pos+1).toString(),"")).apply();
                            Singleton.getInstance().setCurrentUser(sharedPref.getString("User"+Integer.valueOf(user_pos+1).toString(),""));
                            new_user_pos = user_pos+1;
                            for(Integer j=user_pos;j<user_num-1;j++){
                                sharedPref.edit().putString("User"+j.toString(),sharedPref.getString("User"+Integer.valueOf(j+1).toString(),"")).apply();
                                sharedPref.edit().putString("User"+j.toString()+"Alias",sharedPref.getString("User"+Integer.valueOf(j+1).toString()+"Alias","")).apply();
                            }
                            sharedPref.edit().putInt("UserNum",user_num-1).apply();
                            sharedPref.edit().remove("User"+Integer.valueOf(user_num-1).toString()).apply();
                            sharedPref.edit().remove("User"+Integer.valueOf(user_num-1).toString()+"Alias").apply();
                        } else{ // user is last in list
                            sharedPref.edit().putString("CurrentUser",sharedPref.getString("User"+ Integer.valueOf(user_pos-1).toString(),"")).apply();
                            new_user_pos = user_pos-1;
                            Singleton.getInstance().setCurrentUser(sharedPref.getString("User"+Integer.valueOf(user_pos-1).toString(),""));
                            sharedPref.edit().remove("User"+user_pos.toString()).apply();
                            sharedPref.edit().remove("User"+user_pos.toString()+"Alias").apply();
                            sharedPref.edit().putInt("UserNum",user_num-1).apply();
                        }
                        for(Integer j=0;j<user_list.size();j++){
                            user_list.get(j).current_user = false;
                        }
                        user_list.get(new_user_pos).current_user = true;
                        user_list.remove(user_pos.intValue());
                        user_adapter.notifyDataSetChanged();
                        break;
                    }
                }
            }
        });

        Button add_button = (Button) findViewById(R.id.manage_users_add_button);
        add_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = ManageUsersActivity.this.getLayoutInflater();
                final View dialogview = inflater.inflate(R.layout.add_user_dialog_layout,null);
                new AlertDialog.Builder(ManageUsersActivity.this)
                        .setMessage(getResources().getString(R.string.EnterNewUserDetails))
                        .setView(dialogview)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Integer user_num = sharedPref.getInt("UserNum",2);
                                sharedPref.edit().putString("User"+user_num.toString(),((EditText)dialogview.findViewById(R.id.add_new_user_name_edit_text)).getText().toString()).apply();
                                sharedPref.edit().putString("User"+user_num.toString()+"Alias",((EditText)dialogview.findViewById(R.id.add_new_user_alias_edit_text)).getText().toString()).apply();
                                sharedPref.edit().putInt("UserNum",user_num+1).apply();
                                user_list.add(new User(((EditText)dialogview.findViewById(R.id.add_new_user_name_edit_text)).getText().toString(),
                                        ((EditText)dialogview.findViewById(R.id.add_new_user_alias_edit_text)).getText().toString(),0,false));
                                user_adapter.notifyDataSetChanged();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which){
                                dialog.dismiss();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
