package com.example.androidcontroller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

import com.example.androidcontroller.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private HomeFragment homeFragment = new HomeFragment();
    private BluetoothFragment bluetoothFragment = new BluetoothFragment();

    //FOR BOTTOM NAVIGATION BAR
    //https://www.youtube.com/watch?v=Bb8SgfI4Cm4
    ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Set listener for item selected
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()){
                case R.id.navigation_home:
                    replaceFragment(homeFragment);
                    break;
                case R.id.navigation_bluetoth:
                    replaceFragment(bluetoothFragment);
                    break;
            }
            return true;
        });

        //Default to home fragment
        replaceFragment(homeFragment);
    }

    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout,fragment);
        fragmentTransaction.commit();
    }
}