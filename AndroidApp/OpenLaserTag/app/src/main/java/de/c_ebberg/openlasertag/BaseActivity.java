package de.c_ebberg.openlasertag;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by christian on 25.05.17.
 */

public class BaseActivity extends AppCompatActivity {
    private static final String LOG_TAG="BaseActivity";
    private String current_theme="Light";
    @Override
    protected void onCreate(Bundle savedInstanceState){
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Integer index = 0;
        String[] index_texts = getResources().getStringArray(R.array.preference_theme_entries);
        int[] index_values = getResources().getIntArray(R.array.preference_theme_values);
        String index_setting = sharedPref.getString("theme_preference","Light");
        for(Integer i=0;i<index_texts.length;i++){
            if(index_texts[i].equals(index_setting)){
                index = index_values[i];
                break;
            }
        }
        current_theme = index_texts[index];

        switch(index){
            case 0:
                this.setTheme(R.style.LightNoActionBar);
                break;
            case 1:
                this.setTheme(R.style.DarkNoActionBar);
                break;
            case 2:
                this.setTheme(R.style.BeeNoActionBar);
                break;
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume(){
        super.onResume();
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if(!sharedPref.getString("theme_preference","Light").equals(current_theme)){
            this.recreate();
        }
    }

}
