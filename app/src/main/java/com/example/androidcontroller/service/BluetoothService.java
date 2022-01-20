package com.example.androidcontroller.service;

public class BluetoothService {
    private static BluetoothService INSTANCE;

    private boolean bluetoothSupported;

    private BluetoothService(){}

    public static BluetoothService getInstance(){
        if(INSTANCE == null){
            INSTANCE = new BluetoothService();
        }
        return  INSTANCE;
    }


}
