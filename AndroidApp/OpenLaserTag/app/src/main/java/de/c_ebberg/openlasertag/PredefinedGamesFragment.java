package de.c_ebberg.openlasertag;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Created by christian on 20.05.17.
 */

public class PredefinedGamesFragment extends Fragment {
    String tag="";
    String LOG_TAG = "PredefinedGamesFragment";
    View ground_view;
    FilePathAdapter predef_adapter;
    ArrayList<FilePath> predef_games = new ArrayList<FilePath>();

    static PredefinedGamesFragment newInstance(String tag) {
        PredefinedGamesFragment f = new PredefinedGamesFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("Tag",tag);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tag = getArguments() != null ? getArguments().getString("Tag") : "";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout to use as dialog or embedded fragment
        ground_view = inflater.inflate(R.layout.file_list_layout, container, false);
        if(getActivity()!=null){
            init(getActivity());
        }
        return ground_view;
    }

    PredefinedGamesFragmentListener event_listener;
    public interface PredefinedGamesFragmentListener {
        void onFileChosen(String path, String file_name);
        void onInfoButtonClicked(String game_name,String description);
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            event_listener = (PredefinedGamesFragmentListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement PredefinedGamesFragmentListener");
        }
    }

    private void init(final Context context) {
        predef_games.clear();
        try {
            for(Integer i=1;;i++) {
                Class res = R.raw.class;
                Field field = res.getField("predef_game_"+i.toString());
                int rawId = field.getInt(null);
                InputStream ifstream = context.getResources().openRawResource(rawId);
                byte[] b = new byte[ifstream.available()];
                ifstream.read(b);
                String input = new String(b);
                if(input.contains("<GameName value=\"") && input.contains("<Description>")){
                    FilePath predef_game = new FilePath("raw/predef_game_"+i.toString(),
                            input.substring(input.indexOf("<GameName value=")+17,
                                    input.indexOf("\"/>",input.indexOf("<GameName value=\""))),
                            false,
                            input.substring(input.indexOf("<GameName value=")+17,
                                    input.indexOf("\"/>",input.indexOf("<GameName value=\""))),
                            input.substring(input.indexOf("<Description>")+13,
                                    input.indexOf("</Description>")));
                    predef_games.add(predef_game);
                }
                ifstream.close();
            }
        }
        catch (Exception e) {// catch exception, when no more resources are found
        }
        predef_adapter = new FilePathAdapter(context,predef_games,true);
        ListView lv = (ListView) ground_view.findViewById(R.id.file_list_view);
        lv.setAdapter(predef_adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (predef_adapter.getItem(arg2) == null) return;
                String selected_path="";
                for(FilePath i : predef_games){
                    if(i.file_name.equals((predef_adapter.getItem(arg2)).file_name)){
                        selected_path = i.path;
                    }
                }
                if(selected_path.equals("")) return;
                event_listener.onFileChosen(selected_path, (predef_adapter.getItem(arg2)).file_name);
            }
        });
    }


    private class FilePath{
        String path;
        String file_name;
        String full_path;
        Boolean is_directory;
        String game_name="";
        String game_description="";

        public FilePath(String _path, String _file_name, Boolean _is_directory, String _game_name, String _game_description){
            path=_path;
            file_name=_file_name;
            is_directory=_is_directory;
            full_path=path+ File.separator+file_name;
            game_name = _game_name;
            game_description = _game_description;
        }
    }

    private class FilePathAdapter extends ArrayAdapter<FilePath> {
        private final Context context;
        private final ArrayList<FilePath> modelsArrayList;
        private boolean predef=false;

        public FilePathAdapter(Context context, ArrayList<FilePath> modelsArrayList, boolean _predef) {
            super(context, R.layout.list_item_file_list, modelsArrayList);

            this.predef = _predef;
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

                v = inflater.inflate(R.layout.list_item_file_list, parent, false);
                mViewHolder.textview = (TextView) v.findViewById(R.id.item_textview);
                mViewHolder.iconview = (ImageView) v.findViewById(R.id.item_icon);
                mViewHolder.info_button = (ImageButton) v.findViewById(R.id.info_button);

                mViewHolder.info_button.setOnClickListener(new ImageButton.OnClickListener(){
                    public void onClick(View button){
                        int pos=0;
                        for(int i=0;i<modelsArrayList.size();i++){
                            if(modelsArrayList.get(i).file_name.equals(mViewHolder.textview.getText())){
                                pos=i;
                                break;
                            }
                        }
                        event_listener.onInfoButtonClicked(modelsArrayList.get(pos).game_name,
                                modelsArrayList.get(pos).game_description);
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
            if(modelsArrayList.get(position).is_directory){
                mViewHolder.iconview.setImageResource(R.drawable.ic_folder_icon);
                mViewHolder.info_button.setVisibility(View.INVISIBLE);
            } else {
                mViewHolder.iconview.setImageResource(R.drawable.ic_file_icon_xml);
                mViewHolder.info_button.setVisibility(View.VISIBLE);
            }
            mViewHolder.textview.setText(modelsArrayList.get(position).file_name);

            // 5. return rowView
            return v;
        }
    }

    static class ViewHolder{
        private TextView textview;
        private ImageView iconview;
        private ImageButton info_button;
    }
}
