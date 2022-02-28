package com.example.androidcontroller;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;


public class ViewPagerAdapter extends FragmentStateAdapter {

    // TODO: to be deleted after building the Arena
    //private static final String[] TAB_TITLES = new String[]{ "Arena"};

    // TODO: original tab
    private static final String[] TAB_TITLES = new String[]{ "Home", "Bluetooth"};


    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    //private final Context mContext;

    @Override
    public Fragment createFragment(int position) {
        //return null;
        Fragment fragment = null;

        switch (position)
        {
            case 0:
                fragment = HomeFragment.newInstance("", "");
                //return new ArenaFragment();
                break;
            case 1:
                fragment = BluetoothFragment.newInstance("", "");
                //return new BluetoothFragment();
                break;

        }
        return fragment;
    }

    public static String[] getTabTitles() {
        return TAB_TITLES;
    }

    @Override
    public int getItemCount() {
        return TAB_TITLES.length;
    }
}
