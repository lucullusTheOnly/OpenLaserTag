package de.c_ebberg.openlasertag;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Created by christian on 18.02.17.
 */

public class SerialThread extends Thread {
    private Handler game_logic_Handler;
    private Handler mainHandler;
    private String LOG_TAG="SerialThread";

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private boolean bluetooth_state; // false if not connected, true if connected
    private BluetoothDevice bt_device;
    private String bt_device_name;
    private UUID serial_port_UUID=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket bt_socket;
    private OutputStream bt_output_stream;
    private InputStream bt_input_stream;

    private Boolean first_config_query=false;
    // local Handler manages messages for MyThread
    // received from mainThread
    private Handler myThreadHandler = new Handler(){
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case 0://abort Thread

                    break;
                case 1: // toggle bt state
                    if (bluetooth_state){
                        disconnect((Handler)msg.obj);
                    }else {
                        connect_to_bt_device(msg.getData().getString("DeviceName"), msg.getData().getString("DeviceAddr"),(Handler)msg.obj);
                        String ping_msg = ".\n";
                        try {
                            bt_output_stream.write(ping_msg.getBytes());
                        } catch(IOException e) {
                            Log.e(LOG_TAG,"IOException during BT Serial write attempt -> closing socket");
                            bluetooth_state=false;
                            try {
                                bt_socket.close();
                            } catch(IOException ei){
                                Log.e(LOG_TAG,"IOException during BTwrite IOException");
                            }
                            Message failed_msg = new Message();
                            failed_msg.what=1;
                            Bundle msg_data = new Bundle();
                            msg_data.putInt("Type",2); // Connection failed
                            failed_msg.setData(msg_data);
                            Singleton.getInstance().setSerialConnected(false);
                            ((Handler)msg.obj).sendMessage(failed_msg);
                        }
                    }
                    break;
                case 2: // send bytes over serial
                    try {
                        bt_output_stream.write(msg.getData().getByteArray("WriteData"));
                    } catch(IOException e) {
                        Log.e(LOG_TAG,"IOException during BT Serial write attempt -> closing socket");
                        bluetooth_state=false;
                        try {
                            bt_socket.close();
                        } catch(IOException ei){
                            Log.e(LOG_TAG,"IOException during BTwrite IOException");
                        }
                        Message failed_msg = new Message();
                        failed_msg.what=1;
                        Bundle msg_data = new Bundle();
                        msg_data.putInt("Type",2); // Connection failed
                        failed_msg.setData(msg_data);
                        Singleton.getInstance().setSerialConnected(false);
                        mainHandler.sendMessage(failed_msg);
                    }
            }
        }
    };

    //constructor
    public SerialThread(Handler gl_Handler, Handler mainh){
        // initialize instance vars
        this.game_logic_Handler = gl_Handler;
        this.mainHandler=mainh;
    }

    @Override
    public void run() {
        super.run();
        try {
            // Thread loop
            byte[] buf=new byte[1024];
            int buf_pos=0;
            while (true)
            {
                //byte buf[];
                if(!bluetooth_state || bt_input_stream.available()<1) continue;
                try {
                    // Read from the InputStream
                    byte[] buffer = new byte[1024];
                    int bread = bt_input_stream.read(buffer);
                    //buf = new byte[bread];
                    System.arraycopy(buffer, 0, buf, buf_pos, bread);
                    buf_pos+=bread;
                    //serial_receive_string+= new String(buf);
                    boolean found_newline=false;
                    int n_pos=1025;
                    for(int i=0;i<buf_pos;i++){
                        if(buf[i]=='\n') {
                            found_newline=true;
                            n_pos=i;
                            break;
                        }
                    }
                    //if(serial_receive_string.contains("\n")) {// indexOf('\n')
                    if(found_newline) {
                        // Send the obtained IR command to the GameLogic Thread
                        byte[] temp_command_buffer = new byte[1024];
                        System.arraycopy(buf,0,temp_command_buffer,0,n_pos); // Command alleine in neues Array und mit 0 abschließen
                        temp_command_buffer[n_pos+1]=0;
                        System.arraycopy(buf,n_pos+1,buf,0,buf_pos-n_pos-1);// rest nach vorne rücken lassen

                        if(temp_command_buffer[0]=='o' && temp_command_buffer[1]=='k' && !first_config_query){
                            first_config_query=true;
                            String config_query = "cr\n";
                            try {
                                bt_output_stream.write(config_query.getBytes());
                            } catch(IOException e) {
                                Log.e(LOG_TAG,"IOException during BT Serial write attempt -> closing socket");
                                bluetooth_state=false;
                                try {
                                    bt_socket.close();
                                } catch(IOException ei){
                                    Log.e(LOG_TAG,"IOException during BTwrite IOException");
                                }
                                Message failed_msg = new Message();
                                failed_msg.what=1;
                                Bundle msg_data = new Bundle();
                                msg_data.putInt("Type",2); // Connection failed
                                failed_msg.setData(msg_data);
                                Singleton.getInstance().setSerialConnected(false);
                                mainHandler.sendMessage(failed_msg);
                            }
                        }

                        Message msg_to_game_logic = new Message();
                        Bundle msg_data = new Bundle();
                        msg_to_game_logic.what=5;
                        msg_data.putByteArray("ByteData",temp_command_buffer);
                        msg_to_game_logic.setData(msg_data);
                        game_logic_Handler.sendMessage(msg_to_game_logic);

                        buf_pos=buf_pos-n_pos-1;
                    }
                } catch (IOException e) {
                    Log.d(LOG_TAG, "BT disconnected", e);
                    this.interrupt();
                    return;
                }
            }
        }
        catch (Exception e) {
            // Logging exception
            Log.e(LOG_TAG,"Main loop exception - " + e);
            this.interrupt();
        }
    }

    private boolean connect_to_bt_device(String DeviceName, String DeviceAddr,Handler target_handler){
        bt_device_name=DeviceName;
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        if (bondedDevices.isEmpty()) {
            Message main_msg = new Message();
            main_msg.what=1;
            Bundle msg_data=new Bundle();
            msg_data.putInt("Type",0); // Pair Tagger before connecting
            main_msg.setData(msg_data);
            target_handler.sendMessage(main_msg);
            return false;
        } else {

            for (BluetoothDevice iterator : bondedDevices) {

                if (iterator.getAddress().equals(DeviceAddr)) //Replace with iterator.getName() if comparing Device names.

                {
                    try {
                        bt_device = iterator; //device is an object of type BluetoothDevice
                        try {
                            Boolean uuid_flag=false;
                            for(Integer i=0;i<bt_device.getUuids().length;i++){
                                if(bt_device.getUuids()[i].getUuid().equals(serial_port_UUID)){
                                    uuid_flag=true;
                                    break;
                                }
                            }
                            if(!uuid_flag){
                                Log.e(LOG_TAG,"Bluetooth device "+bt_device.getName()+" has no serial port UUID! It cannot be used with this app");
                                Message fail_msg = new Message();
                                fail_msg.what=1;
                                Bundle msg_data=new Bundle();
                                msg_data.putInt("Type",2); // Connection failed
                                fail_msg.setData(msg_data);
                                target_handler.sendMessage(fail_msg);
                                Singleton.getInstance().setSerialConnected(false);
                                bluetooth_state=false;
                                return false;
                            }
                            bt_socket = bt_device.createRfcommSocketToServiceRecord(serial_port_UUID);
                            bt_socket.connect();
                            bt_output_stream = bt_socket.getOutputStream();
                            bt_input_stream = bt_socket.getInputStream();
                        } catch (IOException e) {
                            Log.e(LOG_TAG,"IOException during connecting of socket and opening of streams - "+e);
                            Message fail_msg = new Message();
                            fail_msg.what=1;
                            Bundle msg_data=new Bundle();
                            msg_data.putInt("Type",2); // Connection failed
                            fail_msg.setData(msg_data);
                            target_handler.sendMessage(fail_msg);
                            Singleton.getInstance().setSerialConnected(false);
                            bluetooth_state=false;
                            return false;
                        }
                        Message con_msg = new Message();
                        con_msg.what=1;
                        Bundle msg_data=new Bundle();
                        msg_data.putInt("Type",1);// Connection established
                        con_msg.setData(msg_data);
                        target_handler.sendMessage(con_msg);
                        Singleton.getInstance().setSerialConnected(true);
                        bluetooth_state = true;
                        return true;
                    } catch(Exception e){
                        Log.e(LOG_TAG,"Error establishing bluetooth connection - " + e);
                        Message fail_msg = new Message();
                        fail_msg.what=1;
                        Bundle msg_data=new Bundle();
                        msg_data.putInt("Type",2); // Connection failed
                        fail_msg.setData(msg_data);
                        target_handler.sendMessage(fail_msg);
                        Singleton.getInstance().setSerialConnected(false);
                        bluetooth_state = false;
                        return false;
                    }
                }
            }
            //If not found with address, search for DeviceName
            for (BluetoothDevice iterator : bondedDevices) {

                if (iterator.getName().equals(DeviceName))
                {
                    try {
                        bt_device = iterator; //device is an object of type BluetoothDevice
                        try {
                            Boolean uuid_flag=false;
                            for(Integer i=0;i<bt_device.getUuids().length;i++){
                                if(bt_device.getUuids()[i].getUuid().equals(serial_port_UUID)){
                                    uuid_flag=true;
                                    break;
                                }
                            }
                            if(!uuid_flag){
                                Log.e(LOG_TAG,"Bluetooth device "+bt_device.getName()+" has no serial port UUID! It cannot be used with this app");
                                Message fail_msg = new Message();
                                fail_msg.what=1;
                                Bundle msg_data=new Bundle();
                                msg_data.putInt("Type",2); // Connection failed
                                fail_msg.setData(msg_data);
                                target_handler.sendMessage(fail_msg);
                                Singleton.getInstance().setSerialConnected(false);
                                bluetooth_state=false;
                                return false;
                            }
                            bt_socket = bt_device.createRfcommSocketToServiceRecord(serial_port_UUID);
                            bt_socket.connect();
                            bt_output_stream = bt_socket.getOutputStream();
                            bt_input_stream = bt_socket.getInputStream();
                        } catch (IOException e) {
                            Log.e(LOG_TAG,"IOException during connecting of socket and opening of streams - "+e);
                            Message fail_msg = new Message();
                            fail_msg.what=1;
                            Bundle msg_data=new Bundle();
                            msg_data.putInt("Type",2); // Connection failed
                            fail_msg.setData(msg_data);
                            target_handler.sendMessage(fail_msg);
                            Singleton.getInstance().setSerialConnected(false);
                            bluetooth_state=false;
                            return false;
                        }
                        Message con_msg = new Message();
                        con_msg.what=1;
                        Bundle msg_data=new Bundle();
                        msg_data.putInt("Type",1);// Connection established
                        con_msg.setData(msg_data);
                        target_handler.sendMessage(con_msg);
                        bluetooth_state = true;
                        Singleton.getInstance().setSerialConnected(true);
                        return true;
                    } catch(Exception e){
                        Log.e(LOG_TAG,"Error establishing bluetooth connection - " + e);
                        Message fail_msg = new Message();
                        fail_msg.what=1;
                        Bundle msg_data=new Bundle();
                        msg_data.putInt("Type",2); // Connection failed
                        fail_msg.setData(msg_data);
                        target_handler.sendMessage(fail_msg);
                        Singleton.getInstance().setSerialConnected(false);
                        bluetooth_state = false;
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private void disconnect(Handler target_handler){
        try{
            bt_socket.close();
        } catch(IOException e){
            Log.e(LOG_TAG,"IOException during BT socket closing");
        }
        Message discon_msg = new Message();
        discon_msg.what=1;
        Bundle msg_data = new Bundle();
        msg_data.putInt("Type",3);// Disconnected
        discon_msg.setData(msg_data);
        Singleton.getInstance().setSerialConnected(false);
        bluetooth_state = false;
        target_handler.sendMessage(discon_msg);
    }

    // getter for local Handler
    public Handler getHandler() {
        return myThreadHandler;
    }

    public Boolean getStatus(){return bluetooth_state;}

}
/*public class SerialThread extends Thread {  // Attention: Dummy Class for testing
    private Handler game_logic_Handler;
    private Handler mainHandler;
    private String LOG_TAG="SerialThread";

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private boolean bluetooth_state; // false if not connected, true if connected
    private BluetoothDevice bt_device;
    private String bt_device_name;
    private UUID serial_port_UUID=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket bt_socket;
    private OutputStream bt_output_stream;
    private InputStream bt_input_stream;
    // local Handler manages messages for MyThread
    // received from mainThread
    private Handler myThreadHandler = new Handler(){
        public void handleMessage(Message msg) {
            switch(msg.what){
                case 0://abort Thread

                    break;
                case 1: // toggle bt state
                    if(bluetooth_state)
                        disconnect((Handler)msg.obj);
                    else
                        connect_to_bt_device(msg.getData().getString("DeviceName"),(Handler)msg.obj);
                    break;
                case 2: // send bytes over serial

                    break;
                case 20: // send dummy IR command to GameLogicThread
                    Message IR_msg = new Message();
                    IR_msg.what=5;
                    Bundle msg_data = new Bundle();
                    byte[] dummy_IR = new byte[3];
                    dummy_IR[0]='r';
                    dummy_IR[1]=0b00111111;
                    dummy_IR[2]=0b00010001;
                    msg_data.putByteArray("ByteData",dummy_IR);
                    IR_msg.setData(msg_data);
                    game_logic_Handler.sendMessage(IR_msg);
            }
        }
    };

    //constructor
    public SerialThread(Handler gl_Handler, Handler mainh){
        // initialize instance vars
        this.game_logic_Handler = gl_Handler;
        this.mainHandler=mainh;
    }

    @Override
    public void run() {
        super.run();

    }

    private boolean connect_to_bt_device(String DeviceName, Handler target_handler){
        bt_device_name=DeviceName;
        Message con_msg = new Message();
        con_msg.what=1;
        Bundle msg_data=new Bundle();
        msg_data.putInt("Type",1);// Connection established
        con_msg.setData(msg_data);
        target_handler.sendMessage(con_msg);
        Singleton.getInstance().setSerialConnected(true);
        bluetooth_state = true;
        return true;
    }

    private void disconnect(Handler target_handler){
        bluetooth_state=false;
        Message discon_msg = new Message();
        discon_msg.what=1;
        Bundle msg_data = new Bundle();
        msg_data.putInt("Type",3);// Disconnected
        discon_msg.setData(msg_data);
        Singleton.getInstance().setSerialConnected(false);
        target_handler.sendMessage(discon_msg);
    }

    // getter for local Handler
    public Handler getHandler() {
        return myThreadHandler;
    }

    public Boolean getStatus(){return bluetooth_state;}
}
*/