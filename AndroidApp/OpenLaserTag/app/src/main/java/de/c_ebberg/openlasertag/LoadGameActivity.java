package de.c_ebberg.openlasertag;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LoadGameActivity extends BaseActivity implements SelectXMLFileFragment.SelectXMLFileFragmentListener,PredefinedGamesFragment.PredefinedGamesFragmentListener {
    private static final String LOG_TAG="LoadGameActivity";
    private ViewPager viewPager;
    ViewPagerAdapter adapter;

    int accent_color = Color.RED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_game);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Resources.Theme themes = getTheme();
        TypedValue storedValueInTheme = new TypedValue();
        if (themes.resolveAttribute(R.attr.colorAccent, storedValueInTheme, true)) {
            accent_color = storedValueInTheme.data;
        }

        viewPager = (ViewPager) findViewById(R.id.load_game_viewpager);
        setupViewPager(viewPager);

        ((ImageButton)findViewById(R.id.description_close_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.description_layout).setVisibility(View.GONE);
            }
        });
    }

    private void setupViewPager(final ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        /*PredefinedGamesFragment predef_fragment;
        SelectXMLFileFragment file_fragment;

        predef_fragment = new PredefinedGamesFragment();
        adapter.addFragment(predef_fragment, "Predefined Games");
        ArrayList<String> exts = new ArrayList<String>();
        exts.add(".xml");
        file_fragment = new SelectXMLFileFragment();
        file_fragment.setExtensions(exts);
        adapter.addFragment(file_fragment, "Select XML File");*/
        viewPager.setAdapter(adapter);

        ((Button)findViewById(R.id.load_game_select_predefined_tabbutton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(0);
                ((ImageView)findViewById(R.id.load_game_select_predefined_tabmarker)).setBackgroundColor(accent_color);
                ((ImageView)findViewById(R.id.load_game_select_file_tabmarker)).setBackgroundColor(0x00000000);
                findViewById(R.id.description_layout).setVisibility(View.GONE);
                Singleton.getInstance().getLoadGameActivityStatus().active_tab=0;
            }
        });
        ((Button)findViewById(R.id.load_game_select_file_tabbutton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(1);
                ((ImageView)findViewById(R.id.load_game_select_file_tabmarker)).setBackgroundColor(accent_color);
                ((ImageView)findViewById(R.id.load_game_select_predefined_tabmarker)).setBackgroundColor(0x00000000);
                findViewById(R.id.description_layout).setVisibility(View.GONE);
                Singleton.getInstance().getLoadGameActivityStatus().active_tab=1;
            }
        });

        if(Singleton.getInstance().getLoadGameActivityStatus()==null) {
            Singleton.getInstance().setLoadGameActivityStatus(
                    Singleton.getInstance().createNewLoadGameActivityStatus(0,
                            Environment.getExternalStorageDirectory().getPath()+File.separator+"OpenLaserTag"+File.separator+"UserGames"));
        } else {
            viewPager.setCurrentItem(Singleton.getInstance().getLoadGameActivityStatus().active_tab);
            switch (Singleton.getInstance().getLoadGameActivityStatus().active_tab){
                case 0:
                    ((ImageView)findViewById(R.id.load_game_select_predefined_tabmarker)).setBackgroundColor(accent_color);
                    ((ImageView)findViewById(R.id.load_game_select_file_tabmarker)).setBackgroundColor(0x00000000);
                    break;
                case 1:
                    ((ImageView)findViewById(R.id.load_game_select_file_tabmarker)).setBackgroundColor(accent_color);
                    ((ImageView)findViewById(R.id.load_game_select_predefined_tabmarker)).setBackgroundColor(0x00000000);
                    break;
            }
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();
        private SelectXMLFileFragment xml_fragment = null;
        private PredefinedGamesFragment predef_fragment = null;

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position){
                case 0:
                    predef_fragment = PredefinedGamesFragment.newInstance("PredefGames");
                    return predef_fragment;
                case 1:
                    ArrayList<String> ext = new ArrayList<>();
                    ext.add(".xml");
                    xml_fragment = SelectXMLFileFragment.newInstance("SelectXMLFile",ext);
                    xml_fragment.setCurrentFolderPath(Singleton.getInstance().getLoadGameActivityStatus().current_folder);
                    return xml_fragment;
                default:
                    return null;
            }
            //return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return 2;
            //return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position){
                case 0:
                    return "Predefined Games";
                case 1:
                    return "Select XML File";
                default:
                    return "";
            }
            //return mFragmentTitleList.get(position);
        }
    }

    public void onFileChosen(String path, String file_name){
        Message load_file_msg = new Message();
        load_file_msg.what=4; // Load Game file
        String newpath="";
        if(path.startsWith("raw/")){
            newpath=path;
        } else {
            newpath = path + File.separator + file_name;
        }
        Bundle msg_data= new Bundle();
        msg_data.putString("Path",newpath);
        load_file_msg.setData(msg_data);
        Singleton.getInstance().getGameLogicHandler().sendMessage(load_file_msg);

        Intent intent = new Intent(this, InitGameActivity.class);
        startActivity(intent);
    }

    public void onInfoButtonClicked(String game_name, String description){
        RelativeLayout description_layout = (RelativeLayout) findViewById(R.id.description_layout);
        description_layout.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.description_text)).setText(description);
        ((TextView) findViewById(R.id.description_label)).setText(
                getResources().getString(R.string.DescriptionDoublePoint)
                        +"\n"+game_name);
    }

    public void onHideDescription(){
        findViewById(R.id.description_layout).setVisibility(View.GONE);
    }

    public void onFolderChanged(String path){
        if(Singleton.getInstance().getLoadGameActivityStatus()!=null){
            Singleton.getInstance().getLoadGameActivityStatus().current_folder = path;
        }
    }

}
