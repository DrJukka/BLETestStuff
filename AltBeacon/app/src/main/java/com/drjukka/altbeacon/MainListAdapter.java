package com.drjukka.altbeacon;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by juksilve on 20.11.2015.
 */
public class MainListAdapter extends ArrayAdapter<AltBeacon> {

    private final Activity context;
    private final ArrayList<AltBeacon> beacon;

    public MainListAdapter(Activity context, ArrayList<AltBeacon> beacon) {
        super(context, R.layout.listbox_layout, beacon);
        this.context = context;
        this.beacon  = beacon;
    }

    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.listbox_layout, null,true);

        if(beacon.size() > position) {
            TextView secondLine = (TextView) rowView.findViewById(R.id.secondLine);
            secondLine.setText(beacon.get(position).getId1());

            TextView firstLine = (TextView) rowView.findViewById(R.id.firstLine);
            firstLine.setText(beacon.get(position).getManufacturer() + " :: " + beacon.get(position).getId2() + " : " + beacon.get(position).getId3());

            TextView thirdLine = (TextView) rowView.findViewById(R.id.thirdLine);
            thirdLine.setText("Distance : " + beacon.get(position).getDistance() + " meters");
        }
        return rowView;
    };
}