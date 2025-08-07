package com.example.myapplication.ui.Ntrip.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.ui.Ntrip.NtripClient;

import java.util.LinkedList;

public class ShowSubitemAdapter extends BaseAdapter {
    private final Context context;
    private LinkedList<NtripClient> mData;;
    private final String[] item;

    public ShowSubitemAdapter(Context context, LinkedList<NtripClient> mData, String[] item) {
        this.context = context;
        this.mData = mData;
        this.item = item;
    }

    @Override
    public int getCount() {
        return 5;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint({"UseSwitchCompatOrMaterialCode", "ResourceType"})
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.xml.list_subitem, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.textViewTitle);
        TextView textView2 = convertView.findViewById(R.id.textViewDescription);

        if(position == 0){
            textView.setText(item[position]);
            textView2.setText(mData.get(0).getHost());
        }else if (position == 1) {
            textView.setText(item[position]);
            textView2.setText(String.valueOf(mData.get(0).getPort()));
        }else if(position == 2){
            textView.setText(item[position]);
            textView2.setText(mData.get(0).getMountPoint());
        }else if(position == 3){
            textView.setText(item[position]);
            textView2.setText(mData.get(0).getNtrip_User());
        }else if(position == 4){
            textView.setText(item[position]);
            textView2.setText(mData.get(0).getNtrip_password());
        }
        return convertView;
    }
    public void updateData(LinkedList<NtripClient> newData) {
        this.mData = newData;
        notifyDataSetChanged();  // 通知数据已更改，需要刷新视图
    }
}
