package de.c_ebberg.openlasertag;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

/**
 * Created by christian on 16.04.17.
 */

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final String LOG_TAG="Settings_Fragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals("start_game_countdown")) {
            Preference connectionPref = findPreference(key);
            connectionPref.setSummary(sharedPreferences.getString(key, ""));
        }
        switch(key){
            case "start_game_countdown":{
                Preference connectionPref = findPreference(key);
                connectionPref.setSummary(sharedPreferences.getString(key, ""));
                break;}
            case "theme_preference":{
                Preference connectionPref = findPreference(key);
                connectionPref.setSummary(sharedPreferences.getString(key, ""));
                event_listener.onThemeChanged(sharedPreferences.getString(key,""));
                break;}
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        Preference connectionPref = findPreference("start_game_countdown");
        // Set summary to be the user-description for the selected value
        connectionPref.setSummary(getPreferenceScreen().getSharedPreferences().getString("start_game_countdown", ""));

        connectionPref = findPreference("theme_preference");
        // Set summary to be the user-description for the selected value
        connectionPref.setSummary(getPreferenceScreen().getSharedPreferences().getString("theme_preference", ""));

        event_listener = (OnSettingsFragmentListener) getActivity();
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    OnSettingsFragmentListener event_listener=null;
    public interface OnSettingsFragmentListener {
        void onThemeChanged(String theme);
    }

}
