package com.example.androidcontroller;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

public class BluetoothFragment extends Fragment {
    private BluetoothAdapter bluetoothAdapter;
    private boolean bluetoothOn;

    private Button btnToggleBluetooth;
    private Button btnSearchBluetooth;

    public BluetoothFragment() {
        bluetoothOn = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView =  inflater.inflate(R.layout.fragment_bluetooth, container, false);

        btnToggleBluetooth = rootView.findViewById(R.id.bluetooth_btn_toggle);
        btnToggleBluetooth.setOnClickListener(v -> {
            toggleBluetooth();
        });
        btnSearchBluetooth = rootView.findViewById(R.id.bluetooth_btn_search);
        initializeBluetooth();
        // Inflate the layout for this fragment
        return rootView;
    }

    private void initializeBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            Toast.makeText(getActivity(), "Bluetooth not supported",Toast.LENGTH_LONG).show();
            return;
        }

        btnToggleBluetooth.setEnabled(true);
        if(bluetoothAdapter.isEnabled()) {
            bluetoothOn = true;
        }
        updateBluetoothControlButtons();
    }

    private void toggleBluetooth(){
        //Toggle the status
        bluetoothOn = !bluetoothOn;
        if(bluetoothOn){
            bluetoothAdapter.enable();
        }else{
            bluetoothAdapter.disable();
        }
        updateBluetoothControlButtons();
    }

    private void searchBluetooth(){

    }

    private void updateBluetoothControlButtons(){
        if(bluetoothOn){
            btnToggleBluetooth.setText("Bluetooth: ON");
            btnSearchBluetooth.setEnabled(true);
        }else{
            btnToggleBluetooth.setText("Bluetooth: OFF");
        }
    }
}