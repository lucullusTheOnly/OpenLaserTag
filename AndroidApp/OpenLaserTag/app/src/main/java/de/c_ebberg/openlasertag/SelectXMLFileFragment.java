package de.c_ebberg.openlasertag;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by christian on 20.05.17.
 */

public class SelectXMLFileFragment extends Fragment {
    String LOG_TAG = "SelectXMLFileFragment";
    String tag="";
    ArrayList<String> extensions=new ArrayList<String>();
    View ground_view;
    FilePathAdapter adapter;
    String current_path = Environment.getExternalStorageDirectory().getPath()+File.separator+"OpenLaserTag"+File.separator+"UserGames";;
    File current_file;
    ArrayList<FilePath> current_items;

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
            full_path=path+File.separator+file_name;
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

    static SelectXMLFileFragment newInstance(String tag, ArrayList<String> ext) {
        SelectXMLFileFragment f = new SelectXMLFileFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("Tag",tag);
        args.putStringArrayList("extensions",ext);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            tag = getArguments().getString("Tag");
            extensions = getArguments().getStringArrayList("extensions");
        }
    }


    /** The system calls this to get the DialogFragment's layout, regardless
     of whether it's being displayed as a dialog or an embedded fragment. */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout to use as dialog or embedded fragment
        ground_view = inflater.inflate(R.layout.file_chooser_view_layout, container, false);
        if(getActivity()!=null){
            init(getActivity());
        }
        return ground_view;
    }

    SelectXMLFileFragmentListener event_listener;
    public interface SelectXMLFileFragmentListener {
        void onFileChosen(String path, String file_name);
        void onInfoButtonClicked(String game_name,String description);
        void onHideDescription();
        void onFolderChanged(String path);
    }

    public void setSelectXMLFileFragmentListener(SelectXMLFileFragmentListener listener){event_listener=listener;}

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            event_listener = (SelectXMLFileFragmentListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement SelectXMLFileFragmentListener");
        }
    }

    private void init(final Context context) {
        //if (getArguments() != null) extensions = getArguments().getStringArrayList("extensions");
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ground_view = inflater.inflate(R.layout.file_list_layout,null);
        // fill listview
        current_path = Singleton.getInstance().getLoadGameActivityStatus()!=null ? Singleton.getInstance().getLoadGameActivityStatus().current_folder : Environment.getExternalStorageDirectory().getPath()+File.separator+"OpenLaserTag"+File.separator+"UserGames";
        current_file = new File(current_path);
        if (extensions.contains("*") || extensions.size()==0) {
            current_items = new ArrayList<FilePath>();
            File tempfile;
            for (String file : current_file.list()) {
                tempfile = new File(current_path + File.separator + file);
                String input="";
                String game_name="";
                String game_description="";
                if(!tempfile.isDirectory()) {
                    try {
                        InputStream ifstream = new FileInputStream(tempfile);
                        byte[] b = new byte[ifstream.available()];
                        ifstream.read(b);
                        input = new String(b);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "open file exception for file " + current_path + File.separator + file);
                    }
                    if (input.contains("<GameName value=\"") && input.contains("<Description>")) {
                        game_name = input.substring(input.indexOf("<GameName value=\"") + 17,
                                input.indexOf("\"/>", input.indexOf("<GameName value=\"")));
                        game_description = input.substring(input.indexOf("<Description>") + 13,
                                input.indexOf("</Description>"));
                    }
                }
                if(game_name.equals("")) game_name="<invalid>";
                if(game_description.equals("")) game_description="<invalid>";
                current_items.add(new FilePath(current_path, file, tempfile.isDirectory(), game_name, game_description));
            }
        } else {
            current_items = new ArrayList<FilePath>();
            for (String name : current_file.list()) {
                File f = new File(current_path + File.separator + name);
                for (String ext : extensions) {
                    if (name.endsWith(ext) || f.isDirectory()) {
                        String input="";
                        String game_name="";
                        String game_description="";
                        if(!f.isDirectory()) {
                            if(ext.contains("-")) continue;
                            try {
                                InputStream ifstream = new FileInputStream(f);
                                byte[] b = new byte[ifstream.available()];
                                ifstream.read(b);
                                input = new String(b);
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "open file exception for file " + current_path + File.separator + name);
                            }
                            if (input.contains("<GameName value=\"") && input.contains("<Description>")) {
                                game_name = input.substring(input.indexOf("<GameName value=\"") + 17,
                                        input.indexOf("\"/>", input.indexOf("<GameName value=\"")));
                                game_description = input.substring(input.indexOf("<Description>") + 13,
                                        input.indexOf("</Description>"));
                            }
                        }
                        current_items.add(new FilePath(current_path, name, f.isDirectory(), game_name, game_description));
                        break;
                    }
                }
            }
        }
        Collections.sort(current_items, new Comparator<FilePath>() {
            @Override
            public int compare(FilePath o1, FilePath o2) {
                File tempfile1 = new File(current_path + File.separator + o1.file_name);
                File tempfile2 = new File(current_path + File.separator + o2.file_name);
                if (tempfile1.isDirectory() && !tempfile2.isDirectory()) {
                    return -1;
                } else if (!tempfile1.isDirectory() && tempfile2.isDirectory()) {
                    return 1;
                }
                return o1.file_name.compareToIgnoreCase(o2.file_name);
            }
        });
        if (!current_path.equals("/")) current_items.add(0, new FilePath(current_path, "..", true, "",""));
        //adapter = new ArrayAdapter<String>(context, R.layout.list_item_file_list, current_items);

        adapter = new FilePathAdapter(context, current_items, false);

        ListView lv = (ListView) ground_view.findViewById(R.id.file_list_view);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (adapter.getItem(arg2) == null) return;
                if ((adapter.getItem(arg2)).file_name.equals("..")) { // parent directory
                    current_path = current_file.getParent();
                    current_file = new File(current_file.getParent());
                } else {
                    current_file = new File(current_path + File.separator + (adapter.getItem(arg2)).file_name);
                    event_listener.onHideDescription();
                    if (!current_file.isDirectory()) {
                        Log.i(LOG_TAG,"XML file chosen");
                        event_listener.onFileChosen(current_path, (adapter.getItem(arg2)).file_name);
                        current_file = new File(current_path);
                        return;
                    }
                    current_path = current_path + File.separator + adapter.getItem(arg2).file_name;
                }
                refreshFileList();
            }
        });
    }

    private void refreshFileList(){
        if(ground_view==null) return;
        event_listener.onFolderChanged(current_file.getAbsolutePath());
        current_items.clear();
        if(extensions.contains("*")) {
            File tempfile;
            for(String file : current_file.list()){
                tempfile = new File(current_path+File.separator+file);
                String input="";
                String game_name="";
                String game_description="";
                if(!tempfile.isDirectory()) {
                    try {
                        InputStream ifstream = new FileInputStream(tempfile);
                        byte[] b = new byte[ifstream.available()];
                        ifstream.read(b);
                        input = new String(b);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "open file exception for file " + current_path + File.separator + file);
                    }
                    if (input.contains("<GameName value=\"") && input.contains("<Description>")) {
                        game_name = input.substring(input.indexOf("<GameName value=\"") + 17,
                                input.indexOf("\"/>", input.indexOf("<GameName value=\"")));
                        game_description = input.substring(input.indexOf("<Description>") + 13,
                                input.indexOf("</Description>"));
                    }
                }
                current_items.add(new FilePath(current_path,file,tempfile.isDirectory(),game_name,game_description));
            }
        } else {
            for(String name : current_file.list()){
                File f = new File(current_path+File.separator+name);
                for(String ext : extensions){
                    if(name.endsWith(ext) || f.isDirectory()){
                        String input="";
                        String game_name="";
                        String game_description="";
                        if(!f.isDirectory()) {
                            if(ext.contains("-")) continue;
                            try {
                                InputStream ifstream = new FileInputStream(f);
                                byte[] b = new byte[ifstream.available()];
                                ifstream.read(b);
                                input = new String(b);
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "open file exception for file " + current_path + File.separator + name);
                            }
                            if (input.contains("<GameName value=\"") && input.contains("<Description>")) {
                                game_name = input.substring(input.indexOf("<GameName value=\"") + 17,
                                        input.indexOf("\"/>", input.indexOf("<GameName value=\"")));
                                game_description = input.substring(input.indexOf("<Description>") + 13,
                                        input.indexOf("</Description>"));
                            }
                        }
                        if(game_name.equals("")) game_name="<invalid>";
                        if(game_description.equals("")) game_description="<invalid>";
                        current_items.add(new FilePath(current_path,name,f.isDirectory(), game_name, game_description));
                        break;
                    }
                }
            }
        }
        Collections.sort(current_items, new Comparator<FilePath>() {
            @Override
            public int compare(FilePath o1, FilePath o2) {
                File tempfile1 = new File(current_path+File.separator+o1.file_name);
                File tempfile2 = new File(current_path+File.separator+o2.file_name);
                if(tempfile1.isDirectory() && !tempfile2.isDirectory()){
                    return -1;
                } else if(!tempfile1.isDirectory() && tempfile2.isDirectory()){
                    return 1;
                }
                return o1.file_name.compareToIgnoreCase(o2.file_name);
            }
        });
        if(!current_path.equals("/")) current_items.add(0,new FilePath(current_path,"..",true,"",""));
        adapter.notifyDataSetChanged();
    }

    public void setExtensions(ArrayList<String> ext){
        extensions = ext;
        refreshFileList();
    }

    public ArrayList<String> getExtensions(){
        return extensions;
    }

    public String getCurrentFolderPath(){
        return current_path;
    }

    public void setCurrentFolderPath(String _path){
        current_path = _path;
        current_file = new File(current_path);
        refreshFileList();
    }
}
