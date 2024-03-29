package com.example.androidcontroller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;

import com.example.androidcontroller.databinding.ActivityMainBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    private HomeFragment homeFragment = new HomeFragment();
    private BluetoothFragment bluetoothFragment = new BluetoothFragment();

    private final int[] ICONS = new int[]{
            R.drawable.ic_baseline_home_24,
            R.drawable.ic_baseline_bluetooth_24
    };

    //FOR BOTTOM NAVIGATION BAR
    //https://www.youtube.com/watch?v=Bb8SgfI4Cm4
    ActivityMainBinding binding;
    private final String[] TAB_TITLE = new String[]{
            "Home",
            "Bluetooth"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        TabLayout tabLayout = findViewById(R.id.tabs);

        ViewPager2 viewPager2 = findViewById(R.id.view_pager);
        //help to preload and keep the other fragment
        viewPager2.setOffscreenPageLimit(3);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);

        viewPager2.setAdapter(adapter);
        viewPager2.setUserInputEnabled(false);

//      tabLayout.addTab(tabLayout.newTab().setText("Home").setIcon(ICONS[0]));
//      tabLayout.addTab(tabLayout.newTab().setText("Bluetooth").setIcon(ICONS[1]));


        //commented to change tab to icon
        //TAB_TITLE = adapter.getTabTitles();
        //tabLayout.setSelectedTabIndicator(R.color.black);

        //commented to change tab to icon
        new TabLayoutMediator(tabLayout, viewPager2, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(TAB_TITLE[position]);
                tab.setIcon(ICONS[position]);

            }
        }).attach();

//        binding = ActivityMainBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());
//
//        //Set listener for item selected
//        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
//            switch (item.getItemId()){
//                case R.id.navigation_home:
//                    replaceFragment(homeFragment);
//                    break;
//                case R.id.navigation_bluetoth:
//                    replaceFragment(bluetoothFragment);
//                    break;
//            }
//            return true;
//        });

        //Default to home fragment
        //replaceFragment(homeFragment);
    }

//    private void replaceFragment(Fragment fragment){
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//        fragmentTransaction.replace(R.id.frame_layout,fragment);
//        fragmentTransaction.commit();
//    }
}