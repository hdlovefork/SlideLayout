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
import android.widget.Toast;

import com.dean.customviewgroup.data.Cheeses;
import com.dean.customviewgroup.ui.GooView;
import com.dean.customviewgroup.ui.SweepLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ItemStatusChangeListener mOnStatusChangeListener = new ItemStatusChangeListener();
    private ArrayList<SweepLayout> mItemLayouts = new ArrayList<>();
    private OnItemClick mOnItemClick;
    private boolean[] mGooViewVisibles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView lvMain = (ListView) findViewById(R.id.lv_main);
        lvMain.setAdapter(new MainAdapter(this, R.layout.item_list, Cheeses.NAMES));
        mOnItemClick = new OnItemClick();
        mGooViewVisibles = new boolean[Cheeses.NAMES.length];
        Arrays.fill(mGooViewVisibles, true);
    }

    class MainAdapter extends ArrayAdapter<String> {

        private int mResource;

        public MainAdapter(Context context, int resource, String[] objects) {
            super(context, resource, objects);
            mResource = resource;
        }


        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(mResource, parent, false);
                viewHolder = ViewHolder.createInstance(convertView);
                viewHolder.slItem.setOnStatusChangeListener(mOnStatusChangeListener);
                viewHolder.llContent.setOnClickListener(mOnItemClick);
                viewHolder.tvCall.setOnClickListener(mOnItemClick);
                viewHolder.tvDel.setOnClickListener(mOnItemClick);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.llContent.setTag("you selected is :" + getItem(position));
            viewHolder.tvCall.setTag(getItem(position) + "call");
            viewHolder.tvDel.setTag(getItem(position) + "del");
            viewHolder.tvName.setText(getItem(position));
            if (mGooViewVisibles[position]) {
                //让GooView可见
                Random rand = new Random(System.currentTimeMillis());
                viewHolder.gvGoo.setText(rand.nextInt(10) + 1 + "");
                viewHolder.gvGoo.show();
            } else {
                //让GooView隐藏
                viewHolder.gvGoo.hide();
            }
            viewHolder.gvGoo.setOnStatusChangeListener(new GooView.SimpleOnStatusChangeListener() {
                @Override
                public void onRemove(GooView gooView) {
                    mGooViewVisibles[position] = false;
                }
            });
            return convertView;
        }
    }

    private class OnItemClick implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tv_call:
                case R.id.tv_del:
                case R.id.ll_content:
                    String str = (String) v.getTag();
                    Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }


    static class ViewHolder {
        SweepLayout slItem;
        TextView tvName;
        ViewGroup llContent;
        View tvCall;
        View tvDel;
        GooView gvGoo;

        public static ViewHolder createInstance(View view) {
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.tvName = (TextView) view.findViewById(R.id.tv_name);
            viewHolder.tvCall = view.findViewById(R.id.tv_call);
            viewHolder.tvDel = view.findViewById(R.id.tv_del);
            viewHolder.slItem = (SweepLayout) view.findViewById(R.id.sl_container);
            viewHolder.llContent = (ViewGroup) view.findViewById(R.id.ll_content);
            viewHolder.gvGoo = (GooView) view.findViewById(R.id.gv_goo);
            viewHolder.gvGoo.setRemoveAnimation(R.drawable.anim_bubble_pop);
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

        }

        @Override
        public void onDragging(SweepLayout view) {

        }

        @Override
        public void onPrepareOpen(SweepLayout view) {
            for (SweepLayout layout : mItemLayouts) {
                layout.close();
            }
            mItemLayouts.add(view);
        }

        @Override
        public void onPrepareClose(SweepLayout view) {

        }
    }
}
