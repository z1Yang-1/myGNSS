package com.example.myapplication.ui.Ntrip.Adapter;

import static androidx.core.content.ContentProviderCompat.requireContext;
import static com.baidu.vi.VIContext.getContext;
import static com.example.myapplication.MainActivity.menuItem;
import static com.example.myapplication.ui.Ntrip.NtripViewModel.receivertcmdata;
import static com.example.myapplication.ui.Ntrip.NtripViewModel.stoprtcmReceiver;
import static com.google.android.material.internal.ContextUtils.getActivity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Switch;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.myapplication.R;
import com.example.myapplication.ui.MyService;
import com.example.myapplication.ui.Ntrip.NtripClient;
import com.example.myapplication.ui.Ntrip.NtripViewModel;
import com.example.myapplication.ui.Ntrip.NtripViewModelFactory;
import com.example.myapplication.ui.Ntrip.OnRtcmDataReceivedListener;
import com.example.myapplication.ui.Ntrip.rtcm_t;

import java.io.Serializable;
import java.util.List;

public class CustomSwicthAdapter extends BaseAdapter{
    private final Context context;
    private final String[] items;
    private NtripViewModel ntripViewModel; // 添加 ViewModelStoreOwner
    Intent serviceIntent;

    public CustomSwicthAdapter(Context context, String[] items, NtripViewModel viewModelStoreOwner) {
        this.context = context;
        this.items = items;
        this.ntripViewModel = viewModelStoreOwner;
    }

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public Object getItem(int position) {
        return items[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_switch, parent, false);
        }


        TextView textView = convertView.findViewById(R.id.textView);
        Switch switchButton = convertView.findViewById(R.id.switchButton);
        ntripViewModel.getSwitchState().observeForever(state -> {
            switchButton.setChecked(state);
        });

        textView.setText(items[position]);
        switchButton.setVisibility(position == 0 ? View.VISIBLE : View.GONE); // 只在"opt1"显示开关按钮

        switchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // SwitchButton 被开启
                Log.d("SwitchButton", "Switch is ON");
                List<NtripClient> mData = ntripViewModel.mData; // 获取 mData
                double[] gga_message = ntripViewModel.getGgaMessage(); // 获取 gga_message

                serviceIntent = new Intent(context, MyService.class);
                serviceIntent.putExtra("NtripDataList", (Serializable) mData); // 传递 mData
                serviceIntent.putExtra("GgaMessage", gga_message); // 传递 gga_message
                context.startService(serviceIntent);
                // 绑定监听器
                MyService myService = new MyService();
                myService.setOnRtcmDataReceivedListener(rtcmData -> {
                    // 处理接收到的 RTCM 数据
                    Log.d("RTCM Data", "Received RTCM data: " + rtcmData);
                    // Notify ViewModel
                    ntripViewModel.updateRtcmData(rtcmData);
                });
            }else {
                // SwitchButton 被关闭
                Log.d("SwitchButton", "Switch is OFF");
                // 停止 Service
                context.stopService(serviceIntent);
//                stoprtcmReceiver();
//                menuItem.setEnabled(false);
                // 执行关闭的逻辑
            }
        });
        return convertView;
    }
}

