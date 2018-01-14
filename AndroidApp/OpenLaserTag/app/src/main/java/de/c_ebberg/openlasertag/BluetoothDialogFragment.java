package de.c_ebberg.openlasertag;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BluetoothDialogFragment.OnBluetoothFragmentListener} interface
 * to handle interaction events.
 * Use the {@link BluetoothDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BluetoothDialogFragment extends android.support.v4.app.DialogFragment {
    private OnBluetoothFragmentListener mListener;
    private View ground_view;
    BT_Device_Adapter device_adapter;
    ArrayList<BT_Device> device_list;
    Context context;

    private class BT_Device {
        String name;
        String addr;
        BluetoothDevice bt_device;

        public BT_Device(String _name, String _addr, BluetoothDevice dev) {
            name = _name;
            addr = _addr;
            bt_device = dev;
        }
    }

    private class BT_Device_Adapter extends ArrayAdapter<BT_Device>{
        private final Context context;
        private final ArrayList<BT_Device> modelsArrayList;

        public BT_Device_Adapter(Context context, ArrayList<BT_Device> modelsArrayList) {
            super(context, R.layout.list_item_file_list, modelsArrayList);

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

                v = inflater.inflate(R.layout.bt_list_layout, parent, false);
                mViewHolder.name_text = (TextView) v.findViewById(R.id.bt_device_name_text);
                mViewHolder.address_text = (TextView) v.findViewById(R.id.bt_device_address_text);

                v.setTag(mViewHolder);
            } else {
                mViewHolder = (ViewHolder) v.getTag();
            }
            // 3. Get icon,title & counter views from the rowView
            //ImageView imgView = (ImageView) rowView.findViewById(R.id.item_icon);
            //TextView titleView = (TextView) rowView.findViewById(R.id.item_textview);

            // 4. Set the text for textView
            mViewHolder.name_text.setText(modelsArrayList.get(position).name);
            mViewHolder.address_text.setText(modelsArrayList.get(position).addr);

            // 5. return rowView
            return v;
        }
    }

    static class ViewHolder{
        private TextView name_text;
        private TextView address_text;
    }

    public BluetoothDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     *
     * @return A new instance of fragment BluetoothDialogFragment.
     */
    public static BluetoothDialogFragment newInstance() {
        BluetoothDialogFragment fragment = new BluetoothDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ground_view = inflater.inflate(R.layout.fragment_bluetooth_dialog, container, false);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        ArrayList<BluetoothDevice> bondedDevices = new ArrayList<BluetoothDevice>(bluetoothAdapter.getBondedDevices());
        device_list = new ArrayList<>();
        for(BluetoothDevice dev : bondedDevices){
            device_list.add(new BT_Device(dev.getName(),dev.getAddress(),dev));
        }
        if(getActivity()!=null){
            context = getActivity();
        }
        device_adapter = new BT_Device_Adapter(context,device_list);
        ListView lv = ((ListView)ground_view.findViewById(R.id.bt_device_listview));
        lv.setAdapter(device_adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mListener.onDeviceChosen(device_adapter.getItem(arg2).name, (device_adapter.getItem(arg2)).addr);
                BluetoothDialogFragment.this.dismiss();
            }
        });
        return ground_view;
    }

    /** The system calls this only when creating the layout in a dialog. */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnBluetoothFragmentListener) {
            mListener = (OnBluetoothFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnBluetoothFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnBluetoothFragmentListener {
        void onDeviceChosen(String name, String address);
    }
}
