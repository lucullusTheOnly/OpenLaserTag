package de.c_ebberg.openlasertag;

import android.app.VoiceInteractor;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TaggerConfigActivity extends BaseActivity {
    private final String LOG_TAG = "TaggerConfigActivity";
    ViewPagerAdapter adapter;

    private Toolbar toolbar;
    private ViewPager viewPager;

    private int accent_color= Color.RED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tagger_config);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        viewPager = (ViewPager) findViewById(R.id.tagger_config_viewpager);
        setupViewPager(viewPager);

        Resources.Theme themes = getTheme();
        TypedValue storedValueInTheme = new TypedValue();
        if (themes.resolveAttribute(R.attr.colorAccent, storedValueInTheme, true)) {
            accent_color = storedValueInTheme.data;
        }
    }

    private void setupViewPager(final ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new ReceiverConfigFragment(), "Receiver Config");
        adapter.addFragment(new BTConfigFragment(),"Bluetooth Config");
        adapter.addFragment(new IRConfigFragment(),"IR Config");
        viewPager.setAdapter(adapter);

        ((Button)findViewById(R.id.tagger_config_receiver_tabbutton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(0);
                ((ImageView)findViewById(R.id.tagger_config_receiver_tabmarker)).setBackgroundColor(accent_color);
                ((ImageView)findViewById(R.id.tagger_config_bluetooth_tabmarker)).setBackgroundColor(0x00000000);
                ((ImageView)findViewById(R.id.tagger_config_ir_tabmarker)).setBackgroundColor(0x00000000);
                Singleton.getInstance().getTaggerConfigActivityStatus().active_tab=0;
            }
        });
        ((Button)findViewById(R.id.tagger_config_bluetooth_tabbutton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(1);
                ((ImageView)findViewById(R.id.tagger_config_bluetooth_tabmarker)).setBackgroundColor(accent_color);
                ((ImageView)findViewById(R.id.tagger_config_ir_tabmarker)).setBackgroundColor(0x00000000);
                ((ImageView)findViewById(R.id.tagger_config_receiver_tabmarker)).setBackgroundColor(0x00000000);
                Singleton.getInstance().getTaggerConfigActivityStatus().active_tab=1;
            }
        });
        ((Button)findViewById(R.id.tagger_config_ir_tabbutton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(2);
                ((ImageView)findViewById(R.id.tagger_config_ir_tabmarker)).setBackgroundColor(accent_color);
                ((ImageView)findViewById(R.id.tagger_config_receiver_tabmarker)).setBackgroundColor(0x00000000);
                ((ImageView)findViewById(R.id.tagger_config_bluetooth_tabmarker)).setBackgroundColor(0x00000000);
                Singleton.getInstance().getTaggerConfigActivityStatus().active_tab=2;
            }
        });

        if(Singleton.getInstance().getTaggerConfigActivityStatus()==null) {
            Singleton.getInstance().setTaggerConfigActivityStatus(Singleton.getInstance().createNewTaggerConfigActivityStatus(0));
        } else {
            viewPager.setCurrentItem(Singleton.getInstance().getTaggerConfigActivityStatus().active_tab);
            switch (Singleton.getInstance().getTaggerConfigActivityStatus().active_tab){
                case 0:
                    ((ImageView)findViewById(R.id.tagger_config_receiver_tabmarker)).setBackgroundColor(accent_color);
                    ((ImageView)findViewById(R.id.tagger_config_bluetooth_tabmarker)).setBackgroundColor(0x00000000);
                    ((ImageView)findViewById(R.id.tagger_config_ir_tabmarker)).setBackgroundColor(0x00000000);
                    break;
                case 1:
                    ((ImageView)findViewById(R.id.tagger_config_bluetooth_tabmarker)).setBackgroundColor(accent_color);
                    ((ImageView)findViewById(R.id.tagger_config_ir_tabmarker)).setBackgroundColor(0x00000000);
                    ((ImageView)findViewById(R.id.tagger_config_receiver_tabmarker)).setBackgroundColor(0x00000000);
                    break;
                case 2:
                    ((ImageView)findViewById(R.id.tagger_config_ir_tabmarker)).setBackgroundColor(accent_color);
                    ((ImageView)findViewById(R.id.tagger_config_receiver_tabmarker)).setBackgroundColor(0x00000000);
                    ((ImageView)findViewById(R.id.tagger_config_bluetooth_tabmarker)).setBackgroundColor(0x00000000);
                    break;
            }
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
