package de.c_ebberg.openlasertag;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by christian on 30.04.17.
 */

public class IRConfigFragment extends Fragment {
    public View ground_view=null;

    public IRConfigFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if(getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE) {
            ground_view = inflater.inflate(R.layout.ir_config_layout_landscape, container,false);
        } else {
            ground_view = inflater.inflate(R.layout.ir_config_layout, container,false);
        }
        return ground_view;
    }
}
