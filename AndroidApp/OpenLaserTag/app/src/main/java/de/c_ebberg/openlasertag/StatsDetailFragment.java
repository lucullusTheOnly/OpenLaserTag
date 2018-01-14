package de.c_ebberg.openlasertag;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.HashMap;


/**
 * A simple {@link Fragment} subclass.
 * to handle interaction events.
 */
public class StatsDetailFragment extends Fragment implements StatsDiagramView.OnStatsDiagramViewListener {
    private View ground_view;
    private static final String LOG_TAG="StatsDetailFragment";

    static StatsDetailFragment newInstance(String stat_name) {
        StatsDetailFragment f = new StatsDetailFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("StatName",stat_name);
        f.setArguments(args);

        return f;
    }

    public StatsDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null && Singleton.getInstance().getStatsActivityStatus()!=null) {
            Singleton.getInstance().getStatsActivityStatus().current_stats_name = getArguments().getString("StatName");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ground_view = inflater.inflate(R.layout.fragment_stats_detail, container, false);

        StatsDiagramView dv = ((StatsDiagramView)ground_view.findViewById(R.id.fragment_stats_detail_diagram));
        dv.setCurrentStat(StatsManager.getInstance().getAllStatOccurences(Singleton.getInstance().getStatsActivityStatus().current_stats_name));
        dv.setOnStatsDiagramViewListener(this);
        ((TextView)ground_view.findViewById(R.id.fragment_stats_detail_overall_total)).setText(dv.axisManager.DoubleToString(dv.getOverallTotal(),2));
        ((TextView)ground_view.findViewById(R.id.fragment_stats_detail_overall_average)).setText(dv.axisManager.DoubleToString(dv.getOverallAverage(),2));
        ((TextView)ground_view.findViewById(R.id.fragment_stats_detail_inview_total)).setText(dv.axisManager.DoubleToString(dv.getViewTotal(),2));
        ((TextView)ground_view.findViewById(R.id.fragment_stats_detail_inview_average)).setText(dv.axisManager.DoubleToString(dv.getViewAverage(),2));

        ((TextView)ground_view.findViewById(R.id.fragment_stats_detail_statname)).setText(Singleton.getInstance().getStatsActivityStatus().current_stats_name);

        SeekBar sb = ((SeekBar)ground_view.findViewById(R.id.fragment_stats_detail_binning_seekbar));
        sb.setMax(dv.getMinDuration()-1);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress==0) {seekBar.setProgress(1);progress=1;}
                ((TextView)ground_view.findViewById(R.id.fragment_stats_detail_binning_value)).setText(Integer.valueOf(progress).toString());
                ((StatsDiagramView)ground_view.findViewById(R.id.fragment_stats_detail_diagram)).setBinningLength(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sb = ((SeekBar)ground_view.findViewById(R.id.fragment_stats_detail_zoom_seekbar));
        sb.setMax(1000);
        sb.setProgress(100);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress<100) {seekBar.setProgress(100);progress=100;}
                ((TextView)ground_view.findViewById(R.id.fragment_stats_detail_zoom_label)).setText(Integer.valueOf(progress).toString());
                StatsDiagramView d = ((StatsDiagramView)ground_view.findViewById(R.id.fragment_stats_detail_diagram));
                d.setZoom(Integer.valueOf(progress).floatValue());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        return ground_view;
    }



    public void onDataViewPositionChange(Double view_total, Double view_average){
        StatsDiagramView d = ((StatsDiagramView)ground_view.findViewById(R.id.fragment_stats_detail_diagram));
        ((TextView)ground_view.findViewById(R.id.fragment_stats_detail_inview_total)).setText(d.axisManager.DoubleToString(d.getViewTotal(),2));
        ((TextView)ground_view.findViewById(R.id.fragment_stats_detail_inview_average)).setText(d.axisManager.DoubleToString(d.getViewAverage(),2));
    }
}
