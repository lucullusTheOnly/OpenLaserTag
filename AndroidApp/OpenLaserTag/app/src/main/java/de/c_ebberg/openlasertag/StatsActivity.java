package de.c_ebberg.openlasertag;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class StatsActivity extends BaseActivity implements StatsFragment.OnStatsFragmentListener {
    private final static String LOG_TAG="StatsActivity";
    String current_stat="";
    private Toolbar toolbar=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (Singleton.getInstance().getStatsActivityStatus() == null) {
            Singleton.getInstance().setStatsActivityStatus(Singleton.getInstance().createNewStatsActivityStatus());
            Singleton.getInstance().getStatsActivityStatus().user_name = getIntent().getStringExtra("User");

            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            StatsFragment fragment = StatsFragment.newInstance(getIntent().getStringExtra("User"));
            fragmentTransaction.addToBackStack("StatsFragment");
            fragmentTransaction.add(R.id.content_stats, fragment, "StatsFragment");
            fragmentTransaction.commit();
        } else {
            if (Singleton.getInstance().getStatsActivityStatus().state) {
                if(getSupportFragmentManager().findFragmentByTag("StatsDetailFragment_"+Singleton.getInstance().getStatsActivityStatus().current_stats_name)==null) {
                    android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
                    android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    StatsDetailFragment fragment = new StatsDetailFragment();
                    fragmentTransaction.addToBackStack("StatsDetailFragment_"+Singleton.getInstance().getStatsActivityStatus().current_stats_name);
                    fragmentTransaction.add(R.id.content_stats, fragment, "StatsDetailFragment_"+Singleton.getInstance().getStatsActivityStatus().current_stats_name);
                    fragmentTransaction.commit();
                }
            } else {
                if(getSupportFragmentManager().findFragmentByTag("StatsFragment")==null) {
                    android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
                    android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    StatsFragment fragment = StatsFragment.newInstance(Singleton.getInstance().getStatsActivityStatus().user_name);
                    fragmentTransaction.addToBackStack("StatsFragment");
                    fragmentTransaction.add(R.id.content_stats, fragment, "StatsFragment");
                    fragmentTransaction.commit();
                }
            }
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        toolbar.setTitle(getResources().getString(R.string.title_activity_stats) + " " + Singleton.getInstance().getStatsActivityStatus().user_name);
    }

    @Override
    public void onBackPressed(){
        if(Singleton.getInstance().getStatsActivityStatus().state){
            Singleton.getInstance().getStatsActivityStatus().state = false;
            getSupportFragmentManager().popBackStack();
        } else{
            getSupportFragmentManager().popBackStack();
            Singleton.getInstance().setStatsActivityStatus(null);
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            Log.i(LOG_TAG,"home selected");
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public void onStatSelected(String  name){
        if(!name.equals(Singleton.getInstance().getStatsActivityStatus().current_stats_name)
                && getSupportFragmentManager().findFragmentByTag("StatDetailFragment_"+Singleton.getInstance().getStatsActivityStatus().current_stats_name)!=null){
            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(fragmentManager.findFragmentByTag("StatDetailFragment_"+Singleton.getInstance().getStatsActivityStatus().current_stats_name));
            fragmentTransaction.commit();
        }

        Singleton.getInstance().getStatsActivityStatus().state=true;
        Singleton.getInstance().getStatsActivityStatus().current_stats_name = name;
        if(getSupportFragmentManager().findFragmentByTag("StatDetailFragment_"+Singleton.getInstance().getStatsActivityStatus().current_stats_name)!=null){
            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.content_stats, fragmentManager.findFragmentByTag("StatDetailFragment_"+Singleton.getInstance().getStatsActivityStatus().current_stats_name));
            fragmentTransaction.commit();
        } else {
            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            StatsDetailFragment fragment = StatsDetailFragment.newInstance(Singleton.getInstance().getStatsActivityStatus().current_stats_name);
            fragmentTransaction.replace(R.id.content_stats, fragment, "StatsDetailFragment_"+Singleton.getInstance().getStatsActivityStatus().current_stats_name);
            fragmentTransaction.addToBackStack("StatsDetailFragment_"+Singleton.getInstance().getStatsActivityStatus().current_stats_name);
            fragmentTransaction.commit();
        }
    }
}
