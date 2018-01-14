package de.c_ebberg.openlasertag;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StatsFragment.OnStatsFragmentListener} interface
 * to handle interaction events.
 */
public class StatsFragment extends Fragment {
    private static final String LOG_TAG = "StatsFragment";
    private View ground_view;
    private OnStatsFragmentListener mListener;
    private ArrayList<Stat> stats_list;
    private Stat_Adapter stats_adapter;
    private String user="";

    /****************************************
     * Class Definitions
     ***************************************/
    private class Stat {
        String name;
        Integer games;
        Double total;
        Double average;

        Stat(String _name, Integer _games, Double _total, Double _average){
            name = _name;
            games = _games;
            total = _total;
            average = _average;
        }
    }

    private class Stat_Adapter extends ArrayAdapter<Stat> {
        private final Context context;
        private final ArrayList<Stat> modelsArrayList;

        public Stat_Adapter(Context context, ArrayList<Stat> modelsArrayList) {
            super(context, R.layout.fragment_stats_list_item, modelsArrayList);

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

                v = inflater.inflate(R.layout.fragment_stats_list_item, parent, false);
                mViewHolder.name = (TextView) v.findViewById(R.id.stats_list_item_name);
                mViewHolder.games = (TextView) v.findViewById(R.id.stats_list_item_games);
                mViewHolder.total = (TextView) v.findViewById(R.id.stats_list_item_total);
                mViewHolder.average = (TextView) v.findViewById(R.id.stats_list_item_average);

                v.setTag(mViewHolder);
            } else {
                mViewHolder = (ViewHolder) v.getTag();
            }
            mViewHolder.name.setText(modelsArrayList.get(position).name);
            mViewHolder.games.setText(modelsArrayList.get(position).games.toString());
            mViewHolder.total.setText(modelsArrayList.get(position).total.toString());
            mViewHolder.average.setText(modelsArrayList.get(position).average.toString());

            return v;
        }
    }

    static class ViewHolder{
        private TextView name;
        private TextView games;
        private TextView total;
        private TextView average;
    }

    /****************************************
     * Method Definitions
     ***************************************/
    public StatsFragment() {
        // Required empty public constructor
    }

    static StatsFragment newInstance(String user_name) {
        StatsFragment f = new StatsFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("User",user_name);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = getArguments().getString("User","");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ground_view = inflater.inflate(R.layout.fragment_stats, container, false);
        stats_list = new ArrayList<>();
        try {
            StatsManager.getInstance().LoadUserStatsCollection(user, this.getContext());
            HashMap<String, StatsManager.Stat> stat_map = StatsManager.getInstance().getOverallStatsMapWithoutPoints();
            for(String key : stat_map.keySet()){
                stats_list.add(new Stat(key,stat_map.get(key).getTotalGames(),stat_map.get(key).getTotal(),stat_map.get(key).getAverage()));
            }
        } catch(IOException e){
            Log.e(LOG_TAG, "IOException while loading Stats! Quitting");
        } catch(RuntimeException e){
            Log.e(LOG_TAG, e+"! Quitting");
        }
        stats_adapter = new Stat_Adapter(this.getContext(),stats_list);
        ListView lv = (ListView)ground_view.findViewById(R.id.fragment_stats_list_view);
        lv.setAdapter(stats_adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mListener.onStatSelected(stats_list.get(position).name);
            }
        });
        return ground_view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnStatsFragmentListener) {
            mListener = (OnStatsFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnStatsListListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnStatsFragmentListener {
        void onStatSelected(String name);
    }

    public String getUser(){return user;}
}
