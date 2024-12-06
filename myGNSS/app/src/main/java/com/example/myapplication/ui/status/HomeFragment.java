package com.example.myapplication.ui.status;

import static com.example.myapplication.ui.status.HomeViewModel.write_log_head;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.GnssMeasurementsEvent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.BitmapUtils;
import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentHomeBinding;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView mTextView;
    private FragmentHomeBinding binding;
    HomeViewModel homeViewModel;
    private GnssSkyView gnssSkyView;

    boolean firstpaint;
    public static final int PERMISSION_REQUEST_CODE = 123;
    LocationManager mLocationManager;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        firstpaint = true;
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        // 初始化 LocationManager 对象
        mLocationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        // 检查是否具有位置权限
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // 如果没有位置权限，则请求权限
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            // 如果已经有位置权限，则注册 GNSS 测量回调监听器
            RegisterMeasurements();
            RegisterGnssStatus();
        }
        final TextView showbds = root.findViewById(R.id.bdssat_count);
        final TextView showgps = root.findViewById(R.id.gpssat_count);
        final TextView showglonass = root.findViewById(R.id.glonasssat_count);
        final TextView showgalileo = root.findViewById(R.id.galileosat_count);
        // 找到GnssSkyView视图
        gnssSkyView = root.findViewById(R.id.gnssSkyView);

//        final TextView textView = binding.textHome;
        homeViewModel.getbds().observe(getViewLifecycleOwner(), showbds::setText);
        homeViewModel.getgps().observe(getViewLifecycleOwner(), showgps::setText);
        homeViewModel.getglonass().observe(getViewLifecycleOwner(), showglonass::setText);
        homeViewModel.getgalileo().observe(getViewLifecycleOwner(), showgalileo::setText);

        ImageView imageView1 = root.findViewById(R.id.china);
        ImageView imageView2 = root.findViewById(R.id.america);
        ImageView imageView3 = root.findViewById(R.id.glonass);
        ImageView imageView4 = root.findViewById(R.id.europe);

        Button startButton = root.findViewById(R.id.btn_start_log);
        Button stopButton = root.findViewById(R.id.btn_stop_log);
        startButton.setOnClickListener(v -> {
            Log.d("btn","on");
            Toast.makeText(requireContext(),"recording...",Toast.LENGTH_SHORT).show();
            startButton.setEnabled(false); // 设置为不可点击
            startButton.setAlpha(0.5f); // 设置为灰色
            stopButton.setEnabled(true); // 设置为可点击
            stopButton.setAlpha(1.0f); // 恢复颜色
            write_log_head();
            homeViewModel.start_log = true;
        });
        stopButton.setOnClickListener(v -> {
            Log.d("btn","off");
            stopButton.setEnabled(false); // 设置为不可点击
            stopButton.setAlpha(0.5f); // 设置为灰色
            startButton.setEnabled(true); // 设置为可点击
            startButton.setAlpha(1.0f); // 恢复颜色
            homeViewModel.start_log = false;
        });

        Bitmap china = BitmapUtils.loadAndScaleBitmap(requireContext(), R.drawable.china, 150, 150); // 将位图缩放到 100x100 像素
        Bitmap america = BitmapUtils.loadAndScaleBitmap(requireContext(), R.drawable.america, 150, 150); // 将位图缩放到 100x100 像素
        Bitmap russia = BitmapUtils.loadAndScaleBitmap(requireContext(), R.drawable.russia, 150, 150); // 将位图缩放到 100x100 像素
        Bitmap europe = BitmapUtils.loadAndScaleBitmap(requireContext(), R.drawable.europe, 150, 150); // 将位图缩放到 100x100 像素
        imageView1.setImageBitmap(china); // 将位图设置到 ImageView
        imageView2.setImageBitmap(america);
        imageView3.setImageBitmap(russia);
        imageView4.setImageBitmap(europe);
        return root;
    }
    private final GnssMeasurementsEvent.Callback gnssMeasurementEventListener =
            new GnssMeasurementsEvent.Callback() {
                @Override
                public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
                    super.onGnssMeasurementsReceived(eventArgs);
                    Log.d("status", "measurement called");
                    homeViewModel.processGnssMeasurements(eventArgs);
                    if(homeViewModel.start_log){
                        homeViewModel.log_measurememts(eventArgs);
                    }
                }
                @Override
                public void onStatusChanged(int status) {
                    super.onStatusChanged(status);
                }
            };
    final android.location.GnssStatus.Callback gnssStatusCallback =
            new android.location.GnssStatus.Callback() {
                @Override
                public void onStarted() {
                    super.onStarted();
                    Log.d("GnssStatus", "onStarted() called");
                    // GNSS状态监听已经开始
                }

                @Override
                public void onStopped() {
                    super.onStopped();
                    Log.d("GnssStatus", "onStopped() called");
                    // GNSS状态监听已经停止
                }

                @Override
                public void onFirstFix(int ttffMillis) {
                    super.onFirstFix(ttffMillis);
                    Log.d("GnssStatus", "onFirstFix() called with ttffMillis: " + ttffMillis);
                    // 首次定位成功
                }

                @Override
                public void onSatelliteStatusChanged(@NonNull android.location.GnssStatus status) {
                    super.onSatelliteStatusChanged(status);
                    Log.d("GnssStatus", "onSatelliteStatusChanged() called");
                    if(firstpaint) {
                        List<SatelliteInfo> satelliteInfoList = new ArrayList<>();
                        for (int i = 0; i < status.getSatelliteCount(); i++) {
                            float elevation = status.getElevationDegrees(i);
                            float azimuth = status.getAzimuthDegrees(i);
                            int prn = status.getSvid(i);
                            int constellationType = status.getConstellationType(i);
                            satelliteInfoList.add(new SatelliteInfo(elevation, azimuth, prn, constellationType));
                        }
                        // 更新 GnssSkyView
                        gnssSkyView.setSatelliteInfo(satelliteInfoList);
                        firstpaint = false;
                    }
                }
            };

    private void RegisterGnssStatus() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        boolean is_register_success =
                mLocationManager.registerGnssStatusCallback(gnssStatusCallback);
        if (is_register_success) {
            // 如果成功注册回调监听器，则更新UI界面显示相关信息
//            Toast.makeText(requireContext(), "已注册GNSSSTATUS测量回调监听器", Toast.LENGTH_SHORT).show();
        } else {
            // 如果注册回调监听器失败，则进行错误提示等处理
            Toast.makeText(requireContext(), "Failed to register GNSS measurement callback listener", Toast.LENGTH_SHORT).show();
        }
    }
    //注册监听器
    public void RegisterMeasurements(){
        @SuppressLint("MissingPermission")
        boolean is_register_success=
                mLocationManager.registerGnssMeasurementsCallback(gnssMeasurementEventListener);
        if (is_register_success) {
            // 如果成功注册回调监听器，则更新UI界面显示相关信息
//            Toast.makeText(requireContext(), "已注册GNSS测量回调监听器", Toast.LENGTH_SHORT).show();
        } else {
            // 如果注册回调监听器失败，则进行错误提示等处理
            Toast.makeText(requireContext(), "Failed to register GNSS measurement callback listener", Toast.LENGTH_SHORT).show();
        }
    }
    //注销监听器
    public void unRegisterMeasurements(){
        mLocationManager.unregisterGnssMeasurementsCallback(gnssMeasurementEventListener);
    }
    public void unRegisterGNSSstatus(){
        mLocationManager.unregisterGnssStatusCallback(gnssStatusCallback);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        unRegisterMeasurements();
        unRegisterGNSSstatus();
    }
}