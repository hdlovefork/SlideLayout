package com.dean.customviewgroup;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dean.customviewgroup.data.Cheeses;
import com.dean.customviewgroup.ui.SweepLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ItemStatusChangeListener mOnStatusChangeListener = new ItemStatusChangeListener();
    private ArrayList<SweepLayout> mItemLayouts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView lvMain = (ListView) findViewById(R.id.lv_main);
        lvMain.setAdapter(new MainAdapter(this, R.layout.item_list, Cheeses.NAMES));
    }

    class MainAdapter extends ArrayAdapter<String> {

        private int mResource;

        public MainAdapter(Context context, int resource, String[] objects) {
            super(context, resource, objects);
            mResource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(mResource, parent, false);
                viewHolder = ViewHolder.createInstance(convertView);
                viewHolder.slItem.setOnStatusChangeListener(mOnStatusChangeListener);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.tvName.setText(getItem(position));
            return convertView;
        }
    }



    static class ViewHolder {
        SweepLayout slItem;
        TextView tvName;

        public static ViewHolder createInstance(View view) {
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.tvName = (TextView) view.findViewById(R.id.tv_name);
            viewHolder.slItem = (SweepLayout) view.findViewById(R.id.sl_container);
            return viewHolder;
        }
    }

    private class ItemStatusChangeListener implements SweepLayout.OnStatusChangeListener {
        @Override
        public void onClose(SweepLayout view) {
            mItemLayouts.remove(view);
        }

        @Override
        public void onOpen(SweepLayout view) {
            mItemLayouts.add(view);
        }

        @Override
        public void onDragging(SweepLayout view) {

        }

        @Override
        public void onPrepareOpen(SweepLayout view) {
            for (SweepLayout layout:mItemLayouts){
                layout.close();
            }
        }

        @Override
        public void onPrepareClose(SweepLayout view) {

        }
    }
}
