package com.example.androidcontroller;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {
    public static String TAG = "HomeFragment";
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    View rootview;

    PopupWindow arena_popup;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootview = inflater.inflate(R.layout.fragment_home, container, false);

        Button arena_options_btn = rootview.findViewById(R.id.arenaSetupBtn);
        arena_options_btn.setOnClickListener(v -> {
            arenaSetOptions();
        });

        ImageButton controlBtnUp = rootview.findViewById(R.id.upArrowBtn);
        ImageButton controlBtnDown = rootview.findViewById(R.id.downArrowBtn);
        ImageButton controlBtnLeft = rootview.findViewById(R.id.leftArrowBtn);
        ImageButton controlBtnRight = rootview.findViewById(R.id.rightArrowBtn);

        //CONTROL BUTTON: Forward
        controlBtnUp.setOnClickListener(v -> {
            try{
                Intent upDirectionIntent = new Intent("sendBTMessage");
                upDirectionIntent.putExtra("msg","f");
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(upDirectionIntent);
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occured while making message intent");
            }
        });

        //CONTROL BUTTON: Reverse
        controlBtnDown.setOnClickListener(v -> {
            try{
                Intent downDirectionIntent = new Intent("sendBTMessage");
                downDirectionIntent.putExtra("msg","r");
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(downDirectionIntent);
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occured while making message intent");
            }
        });

        //CONTROL BUTTON: Left
        controlBtnLeft.setOnClickListener(v -> {
            try{
                Intent leftDirectionIntent = new Intent("sendBTMessage");
                leftDirectionIntent.putExtra("msg","tl");
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(leftDirectionIntent);
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occured while making message intent");
            }
        });

        //CONTROL BUTTON: Right
        controlBtnRight.setOnClickListener(v -> {
            try{
                Intent rightDirectionIntent = new Intent("sendBTMessage");
                rightDirectionIntent.putExtra("msg","tr");
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(rightDirectionIntent);
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occured while making message intent");
            }
        });


        // Inflate the layout for this fragment
        return rootview;
    }

    private void arenaSetOptions(){
//        View arenaPopUpView = getLayoutInflater().inflate(R.layout.arena_setup, null);
//        arena_popup = new PopupWindow(arenaPopUpView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
//        arena_popup.setBackgroundDrawable(new BitmapDrawable());
//        arena_popup.showAtLocation(rootview, Gravity.CENTER, 0, 0);

        View popupView = getLayoutInflater().inflate(R.layout.arena_setup, null);
        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(rootview, Gravity.CENTER, 0, 0);
    }
}