/*
 * Copyright 2015 Winson Chiu
 */

package com.winsonchiu.reader.settings;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.winsonchiu.reader.AppSettings;
import com.winsonchiu.reader.R;

public class FragmentHeaders extends Fragment {

    public static final String TAG = FragmentHeaders.class.getCanonicalName();
    private RecyclerView recyclerHeaders;
    private AdapterHeaders adapterHeaders;
    private Activity activity;
    private SharedPreferences preferences;

    public static FragmentHeaders newInstance() {
        return new FragmentHeaders();
    }

    public FragmentHeaders() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_headers, container, false);

        preferences = PreferenceManager.getDefaultSharedPreferences(activity);

        adapterHeaders = new AdapterHeaders(activity, new AdapterHeaders.EventListener() {
            @Override
            public void onClickHeader(int position) {

                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction().addToBackStack(null).hide(FragmentHeaders.this);

                switch (position) {
                    case 0:
                        fragmentTransaction
                                .add(R.id.frame_fragment, new FragmentDisplay());
                        break;
                    case 1:
                        fragmentTransaction
                                .add(R.id.frame_fragment, new FragmentBehavior());
                        break;
                    case 2:
                        fragmentTransaction
                                .add(R.id.frame_fragment, new FragmentMail());
                        break;
                    case 3:
                        fragmentTransaction
                                .add(R.id.frame_fragment, new FragmentAbout());
                        break;
                }

                fragmentTransaction.addToBackStack(null).commit();
            }
        });

        recyclerHeaders = (RecyclerView) view.findViewById(R.id.recycler_headers);
        recyclerHeaders.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        recyclerHeaders.setHasFixedSize(true);
        recyclerHeaders.setAdapter(adapterHeaders);
        recyclerHeaders.setItemAnimator(null);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity = null;
    }

}
