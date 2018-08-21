package com.example.rage.bluetoothcontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    //ui widgets
    private SeekBar slider;
    private Button set;
    private TextView progressSeek;
    //bluetooth adapter and stuff
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //seekbar variable for value
    private char value;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (checkBluetooth()!='a') {//bt problem
            showDialogBluetoothStatus(checkBluetooth());
        }
        else {
            showDialogPairedDevices();
            setWidgets();
        }
    }

    //function to initialise widgets at runtime
    private void setWidgets(){
        progressSeek = findViewById(R.id.textViewValue);
        slider = findViewById(R.id.seekBarValue);
        slider.setMax(255);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //updating value
                value = (char)progress;
                updateText(progress+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        set = findViewById(R.id.buttonSet);
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //writing the char to arduino
                write(value+"");
            }
        });
    }

    //updates text on textview
    private void updateText(String s){
        progressSeek.setText(s);
    }

    //this function gets the bluetooth adapter and responds to it current state
    //returns false if bluetooth is not usable
    private char checkBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {//device is not having a bt adapter
            Toast.makeText(getApplicationContext(), "Bluetooth Adapter Missing.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Adapter Missing");
            return 'n';
        }
        else if (!bluetoothAdapter.isEnabled()){
            Toast.makeText(getApplicationContext(), "Please turn on bluetooth and try again.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Adapter Off");
            return 'o';
        }
        return 'a';
    }

    //this method shows a dialog related to bluetooth status
    private void showDialogBluetoothStatus(char status){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Bluetooth");
        if (status == 'o')
            alert.setMessage("Bluetooth Device Off. Please turn it on and open app again.");
        else if (status == 'n')
            alert.setMessage("Bluetooth Device Not available.");
        alert.setCancelable(false);
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        alert.show();
    }

    //this function reads the currently paired devices and displays a list of them to choose.
    private void showDialogPairedDevices(){
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        final ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
        String deviceNames[] = new String[pairedDevices.size()];
        int i = 0;
        for (BluetoothDevice btdev : pairedDevices){//getting all devices
            deviceNames[i] = btdev.getName();
            deviceList.add(btdev);
            i++;
        }
        final BluetoothDevice[] selectedDevice = new BluetoothDevice[1];//this will store the user selected bt device
        AlertDialog.Builder alertdialog = new AlertDialog.Builder(this);
        alertdialog.setTitle("Bluetooth");
        alertdialog.setCancelable(false);
        if (deviceList.size() > 0){//saved devices available
            alertdialog.setSingleChoiceItems(deviceNames, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    selectedDevice[0] = deviceList.get(which);
                    Log.d(TAG, "Device Selected : "+selectedDevice[0].getName());
                }
            });
            alertdialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //start connection here
                    startConnection(selectedDevice[0]);
                }
            });
        }
        else {
            alertdialog.setMessage("No saved Devices available");
            alertdialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //No devices available so exit
                    finish();
                }
            });
        }
        alertdialog.show();
    }

    //this method starts the connection by calling the inner class
    private void startConnection(BluetoothDevice device){
        Connect connect = new Connect(device);
        Log.d(TAG, "Starting Connection");
        connect.run();
        if (connect.successful) {
            Log.e(TAG, "Connection Successful");
            Toast.makeText(getApplicationContext(), "Connection Successful", Toast.LENGTH_LONG).show();
        }
        else {
            Log.e(TAG, "Connection Unsuccessful");
            Toast.makeText(getApplicationContext(), "Connection Unsuccessful", Toast.LENGTH_LONG).show();
        }
    }

    //this method writes a string to device
    public void write(String s) {
        try {
            bluetoothSocket.getOutputStream().write(s.getBytes());
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Cannot write.", Toast.LENGTH_LONG).show();
            Log.e(TAG, e.toString());
        }
    }


    private class Connect extends Thread{

        private boolean successful;


        public Connect(BluetoothDevice device) {
            BluetoothSocket tempSocket = null;
            //try to connect now
            try{
                BluetoothDevice tempDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());
                tempSocket = tempDevice.createInsecureRfcommSocketToServiceRecord(uuid);
                bluetoothAdapter.cancelDiscovery();

            } catch (IOException e) {
                Log.e(TAG, "Connection failed");
            }
            bluetoothSocket = tempSocket;
            OutputStream t = null;
            try{
                t = bluetoothSocket.getOutputStream();
            }
            catch (Exception e){
                Log.e(TAG, "OP "+e.toString());
            }
            successful = true;
        }

        public void run(){
            //cancel discovery of devices first
            bluetoothAdapter.cancelDiscovery();
            try{
                bluetoothSocket.connect();//start connection
            }
            catch (Exception e){
                Log.e(TAG, "ERROR : "+e.toString());
                Log.e(TAG, "Trying to close socket!");
                try{
                    bluetoothSocket.close();
                }
                catch (Exception a){
                    Log.e(TAG, "NOPE :" + a.toString());
                }
            }
        }

        public void cancel(){
            try{
                bluetoothSocket.close();
            }
            catch (Exception a){
                Log.e(TAG, "INOPE :" + a.toString());
            }
        }

    }
}
//Address of HC-05 module
//00:18:E4:34:EB:8F
