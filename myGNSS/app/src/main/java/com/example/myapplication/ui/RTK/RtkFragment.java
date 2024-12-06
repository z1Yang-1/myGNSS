package com.example.myapplication.ui.RTK;

import static com.example.myapplication.MainActivity.if_auto_record;
import static com.example.myapplication.MainActivity.infile;
import static com.example.myapplication.MainActivity.outputFilePath;
import static com.example.myapplication.ui.Sol.SolViewModel.open;
import static com.example.myapplication.ui.status.HomeFragment.PERMISSION_REQUEST_CODE;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.GnssMeasurementsEvent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentGalleryBinding;
import com.example.myapplication.ui.Ntrip.rtcm_t;

import java.io.IOException;

public class RtkFragment extends Fragment {
    private RtkViewModel rtkViewModel;
    private FragmentGalleryBinding binding;
    private EditText strName;
    private EditText strNameValue;

    private final BroadcastReceiver rtcmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!rtkViewModel.first_rec){
                rtkViewModel.first_rec=true;
            }
            Log.d("rtk","receive rtcm");
            RtkViewModel.rtcm = (rtcm_t) intent.getSerializableExtra("rtcmData");
            RtkViewModel.obssd = RtkViewModel.rtcm.getObs().data.clone();
            RtkViewModel.stapos = RtkViewModel.rtcm.getSta().getPos();
        }
    };

    public static RtkFragment newInstance() {
        return new RtkFragment();
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rtkViewModel = new ViewModelProvider(this, new RTKViewModelFactory(requireContext())).get(RtkViewModel.class);
        View root1 = inflater.inflate(R.layout.fragment_rtk, container, false);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(rtcmReceiver,
                new IntentFilter("com.example.myapplication.RTCM_DATA_RECEIVED"));
        final TextView textView = root1.findViewById(R.id.rtksatcount);
        final TextView show_rtcmnum = root1.findViewById(R.id.rtk_rtcmmesgcount);
        final TextView show_latitude = root1.findViewById(R.id.rtk_latitudeValue);
        final TextView show_longitude = root1.findViewById(R.id.rtk_longitudeValue);
        final TextView show_elevation = root1.findViewById(R.id.rtk_elevationValue);
        final TextView show_v_accuracy = root1.findViewById(R.id.rtk_v_accuracy_Value);
        final TextView show_tips = root1.findViewById(R.id.rtktext);
        final TextView show_sol_state = root1.findViewById(R.id.rtk_solutionStatusValue);
        final TextView show_sol_ratio = root1.findViewById(R.id.rtk_solution_ratio_value);
        final TextView show_h_accuracy = root1.findViewById(R.id.rtk_h_accuracy_Value);
        final TextView show_u_accuracy = root1.findViewById(R.id.rtk_u_accuracy_Value);
        strName =requireActivity().findViewById(R.id.str_name);
        strNameValue = requireActivity().findViewById(R.id.str_name_value);
        strName.setText("1");
        strNameValue.setText("1");
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        Button record_Button = requireActivity().findViewById(R.id.btn_record);
        if(if_auto_record){
            record_Button.setEnabled(false);
        }
        record_Button.setOnClickListener(v -> {
            // 获取 EditText 中的数字
            String input = strNameValue.getText().toString();
            if (!input.isEmpty()) {
                int number = Integer.parseInt(input); // 将字符串转换为整数
                number += 1; // 加1
                strNameValue.setText(String.valueOf(number)); // 更新 EditText 的值
            }
            int num = Integer.parseInt(strNameValue.getText().toString())-1;
            rtkViewModel.str_name_value = strName.getText()+"-"+num;
            // 设置按钮为不可点击
            record_Button.setEnabled(false);
            if_auto_record = true;
            // 再延迟 1 秒
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 设置布尔变量 a 为 false
                    if_auto_record = false;
                    record_Button.setEnabled(true);
                    // 恢复按钮状态
                    record_Button.setEnabled(true);
                    Toast.makeText(requireContext(),"record successfully", Toast.LENGTH_SHORT).show();
//                                record_Button.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light)); // 恢复原来的颜色
                }
            }, 1000); // 1 秒
//                if_ave_result=true;
//                // 设置按钮为灰色并不可点击
//                record_Button.setEnabled(false);
//                // 开始倒计时
//                showCountdownDialog(); // 5秒倒计时
////                record_Button.setBackgroundColor(Color.GRAY);
//                // 使用 Handler 延迟执行
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        // 设置布尔变量 a 为 true
//                        if_auto_record = true;
//                        if_ave_result=false;
//                        // 再延迟 1 秒
//                        new Handler().postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                // 设置布尔变量 a 为 false
//                                if_auto_record = false;
//                                // 恢复按钮状态
//                                record_Button.setEnabled(true);
//                                Toast.makeText(getApplicationContext(),"record successfully", Toast.LENGTH_SHORT).show();
////                                record_Button.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light)); // 恢复原来的颜色
//                            }
//                        }, 1000); // 1 秒
//                    }
//                }, 5000); // 5 秒
        });
        rtkViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        rtkViewModel.getrtcmnum().observe(getViewLifecycleOwner(), show_rtcmnum::setText);
        rtkViewModel.getlatvalue().observe(getViewLifecycleOwner(),show_latitude::setText);
        rtkViewModel.getlonvalue().observe(getViewLifecycleOwner(),show_longitude::setText);
        rtkViewModel.getelevation().observe(getViewLifecycleOwner(),show_elevation::setText);
        rtkViewModel.getdgpstips().observe(getViewLifecycleOwner(),show_tips::setText);
        rtkViewModel.getsolstate().observe(getViewLifecycleOwner(), show_sol_state::setText);
        rtkViewModel.getUpdateVAccuracy().observe(getViewLifecycleOwner(), show_v_accuracy::setText);
        rtkViewModel.getUpdateHAccuracy().observe(getViewLifecycleOwner(), show_h_accuracy::setText);
        rtkViewModel.getUpdateUAccuracy().observe(getViewLifecycleOwner(), show_u_accuracy::setText);
        rtkViewModel.getUpdateStrName().observe(getViewLifecycleOwner(),
                s -> rtkViewModel.str_name = String.valueOf(strName.getText()));
        rtkViewModel.getUpdateStrNameValue().observe(getViewLifecycleOwner(),
                s -> rtkViewModel.str_name_value = String.valueOf(strNameValue.getText()));
        rtkViewModel.getUpdateStatus().observe(getViewLifecycleOwner(),status ->
                Toast.makeText(getActivity(), status, Toast.LENGTH_SHORT).show());
        // 更新 TextView 的颜色
        // 设置背景颜色
        rtkViewModel.getTextColor().observe(getViewLifecycleOwner(), show_sol_state::setBackgroundColor);
        rtkViewModel.getUpdateRatio().observe(getViewLifecycleOwner(), show_sol_ratio::setText);
        // 启动定时任务
        rtkViewModel.startScheduledTask(requireContext(), infile, outputFilePath);
        // 检查是否具有位置权限
        if (ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // 如果没有位置权限，则请求权限
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            // 如果已经有位置权限，则注册 GNSS 测量回调监听器
            RegisterMeasurements();
        }
        if(open){
            RtkViewModel.write_log_head_baseandobs();
        }
        return root1;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        if(RtkViewModel.rtcm==null){
//            requireActivity().runOnUiThread(() -> {
//                // 在UI线程中显示Toast消息
//                Toast.makeText(getActivity(), "Please connect to the CORS network first", Toast.LENGTH_SHORT).show();
//            });
//        }
    }

    private final GnssMeasurementsEvent.Callback gnssMeasurementEventListener =
            new GnssMeasurementsEvent.Callback() {
                @Override
                public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
                    super.onGnssMeasurementsReceived(eventArgs);
                    Log.d("SPP", "measurement called");
                    rtkViewModel.processGnssMeasurements(eventArgs);
                }
                @Override
                public void onStatusChanged(int status) {
                    super.onStatusChanged(status);
                }
            };
    //注册监听器
    public void RegisterMeasurements() {
        @SuppressLint("MissingPermission")
        boolean is_register_success =
                rtkViewModel.mLocationManager.registerGnssMeasurementsCallback(gnssMeasurementEventListener);
        if (is_register_success) {
            requireActivity().runOnUiThread(() -> {
                // 在UI线程中显示Toast消息
//                Toast.makeText(getActivity(), "已注册GNSS测量回调监听器", Toast.LENGTH_SHORT).show();
            });
        } else {
            // 如果注册回调监听器失败，则进行错误提示等处理
            requireActivity().runOnUiThread(() -> {
                // 在UI线程中显示Toast消息
                Toast.makeText(getActivity(), "Failed to register GNSS measurement callback listener", Toast.LENGTH_SHORT).show();
            });
        }
    }
    public void unRegisterMeasurements(){
        rtkViewModel.mLocationManager.unregisterGnssMeasurementsCallback(gnssMeasurementEventListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterMeasurements();
        RtkViewModel.tips.setLength(0);
        if (rtkViewModel.bufferedWriter != null) {
            try {
                // 确保所有数据已经写入
                rtkViewModel.bufferedWriter.flush();
                // 关闭 BufferedWriter
                rtkViewModel.bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                rtkViewModel.bufferedWriter = null;
            }
        }
        binding = null;
    }
}