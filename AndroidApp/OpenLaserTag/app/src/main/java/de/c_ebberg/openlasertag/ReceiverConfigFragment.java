package de.c_ebberg.openlasertag;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by christian on 30.04.17.
 */

public class ReceiverConfigFragment extends Fragment implements HumanView.HumanViewConfigChangedListener {
    private final String LOG_TAG = "ReceiverConfigFragment";
    private View ground_view=null;
    private ArrayList<Receiver_ID> available_receiver_IDs= new ArrayList<>();
    private Boolean front=true;

    private class Receiver_ID{
        Integer ID;
        Boolean used;

        public Receiver_ID(Integer _ID, Boolean _used){
            ID = _ID;
            used = _used;
        }
    }

    public ReceiverConfigFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE) {
            ground_view = inflater.inflate(R.layout.receiver_config_layout_landscape, container,false);
        } else {
            ground_view = inflater.inflate(R.layout.receiver_config_layout, container,false);
        }
        ((HumanView)ground_view.findViewById(R.id.receiver_config_human_view)).setOnConfigChangedListener(this);
        // Inflate the layout for this fragment
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        if(!sharedPref.contains("ReceiverIDs")){
            Toast.makeText(this.getContext(),
                    getResources().getString(R.string.NoReceiverListFromTaggerAvailableWarning),
                    Toast.LENGTH_SHORT).show();
        }
        String receiver_ids = sharedPref.getString("ReceiverIDs","0,1,2,3,4,5")+",";
        while (receiver_ids.contains(",")){
            try{
                Integer id = Integer.parseInt(receiver_ids.substring(0,receiver_ids.indexOf(",")));
                boolean flag = false;
                for(Receiver_ID rec_id : available_receiver_IDs){
                    if(rec_id.ID.equals(id)){flag = true; break;}
                }
                if(flag) {receiver_ids = receiver_ids.substring(receiver_ids.indexOf(",")+1);continue;}
                available_receiver_IDs.add(new Receiver_ID(
                        id,
                        false));
                receiver_ids = receiver_ids.substring(receiver_ids.indexOf(",")+1);
            } catch(NumberFormatException e){
                break;
            }
        }

        String conf = sharedPref.getString("ReceiverConfigFront","") +","+ sharedPref.getString("ReceiverConfigBack","");
        for(Receiver_ID id : available_receiver_IDs){
            if(conf.contains(id.ID.toString())){
                id.used = true;
            }
        }

        ((HumanView)ground_view.findViewById(R.id.receiver_config_human_view))
                .setReceivers(sharedPref.getString("ReceiverConfigFront",""));

        ((Button)ground_view.findViewById(R.id.receiver_config_add_button))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Integer new_id = 0;
                        for(new_id=0;new_id<available_receiver_IDs.size() && available_receiver_IDs.get(new_id).used;new_id++);
                        if(new_id== available_receiver_IDs.size()){
                            Toast.makeText(ReceiverConfigFragment.this.getContext(),
                                    getResources().getString(R.string.NoUnusedReceiverLeft),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        if(((HumanView)ground_view.findViewById(R.id.receiver_config_human_view))
                                .addReceiver(available_receiver_IDs.get(new_id).ID)) {
                            available_receiver_IDs.get(new_id).used = true;
                        } else {
                            Toast.makeText(ReceiverConfigFragment.this.getContext(),
                                    getResources().getString(R.string.NoUnusedPositionLeft),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        ((Button)ground_view.findViewById(R.id.receiver_config_remove_button))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Integer result = ((HumanView)ground_view.findViewById(R.id.receiver_config_human_view))
                                .removeSelectedReceiver();
                        if(result!=-1){
                            for(Receiver_ID id : available_receiver_IDs){
                                if(id.ID.equals(result)){
                                    id.used = false;
                                    break;
                                }
                            }
                        }
                    }
                });

        ((ImageButton)ground_view.findViewById(R.id.receiver_config_change_front_back_button))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        front=!front;
                        if(front){
                            Log.i(LOG_TAG,"Front:"+sharedPref.getString("ReceiverConfigFront",""));
                            ((TextView)ground_view.findViewById(R.id.receiver_config_front_back_text))
                                    .setText(getResources().getString(R.string.Front));
                            ((HumanView)ground_view.findViewById(R.id.receiver_config_human_view))
                                    .setReceivers(sharedPref.getString("ReceiverConfigFront",""));
                        } else {
                            Log.i(LOG_TAG,"Back:"+sharedPref.getString("ReceiverConfigBack",""));
                            ((TextView)ground_view.findViewById(R.id.receiver_config_front_back_text))
                                    .setText(getResources().getString(R.string.Back));
                            ((HumanView)ground_view.findViewById(R.id.receiver_config_human_view))
                                    .setReceivers(sharedPref.getString("ReceiverConfigBack",""));
                        }
                    }
                });

        return ground_view;
    }

    public void onConfigChanged(String new_config){
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        if(front) {
            sharedPref.edit().putString("ReceiverConfigFront", new_config).apply();
        } else {
            sharedPref.edit().putString("ReceiverConfigBack", new_config).apply();
        }
        /*String ids = "";
        for(Receiver_ID id : available_receiver_IDs){
            ids += id.ID +",";
        }
        if(ids.length()>0) ids = ids.substring(0,ids.length()-1);
        sharedPref.edit().putString("ReceiverIDs",ids).apply();*/// I don't know, why I wanted to update the available receiver ids. The string value saved in the shared prefs can only change, when we get new info about the receivers from the tagger system
    }
}
