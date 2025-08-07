package com.example.myapplication.ui.Spp;


import static com.example.myapplication.MainActivity.outputFilePath;
import static com.example.myapplication.MainActivity.remoteDirPath;
import static com.example.myapplication.MainActivity.setOutputFilePath;
import static com.example.myapplication.ui.status.HomeFragment.PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.GnssMeasurementsEvent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.databinding.FragmentSppBinding;

import java.io.File;
import java.io.IOException;

public class SppFragment extends Fragment {

    private @NonNull FragmentSppBinding binding;
    private SppViewModel sppViewModel;

    private final GnssMeasurementsEvent.Callback gnssMeasurementEventListener =
            new GnssMeasurementsEvent.Callback() {
                @Override
                public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
                    super.onGnssMeasurementsReceived(eventArgs);
                    Log.d("SPP", "measurement called");
                    sppViewModel.processGnssMeasurements(eventArgs);
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
                sppViewModel.mLocationManager.registerGnssMeasurementsCallback(gnssMeasurementEventListener);
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
        sppViewModel.mLocationManager.unregisterGnssMeasurementsCallback(gnssMeasurementEventListener);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        sppViewModel = new ViewModelProvider(this, new SppViewModelFactory(requireContext())).get(SppViewModel.class);

        binding = FragmentSppBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.spptext;
        final TextView sat_conut = binding.sppsatcount;
        final TextView show_latitude = binding.latitudeValue;
        final TextView show_longitude = binding.longitudeValue;
        final TextView show_elevation = binding.elevationValue;
        final TextView show_v_accuracy = binding.vAccuracyValue;
        final TextView show_n_accuracy = binding.nAccuracyValue;
        final TextView show_u_accuracy = binding.uAccuracyValue;

        sppViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        sppViewModel.getsatcountText().observe(getViewLifecycleOwner(), sat_conut::setText);
        sppViewModel.getlatvalue().observe(getViewLifecycleOwner(),show_latitude::setText);
        sppViewModel.getlonvalue().observe(getViewLifecycleOwner(),show_longitude::setText);
        sppViewModel.getelevation().observe(getViewLifecycleOwner(),show_elevation::setText);
        sppViewModel.getUpdateVAccuracy().observe(getViewLifecycleOwner(),show_v_accuracy::setText);
        sppViewModel.getUpdateNAccuracy().observe(getViewLifecycleOwner(),show_n_accuracy::setText);
        sppViewModel.getUpdateUAccuracy().observe(getViewLifecycleOwner(),show_u_accuracy::setText);
        sppViewModel.getUpdateStatus().observe(requireActivity(), status -> {
            // 更新 UI
            Toast.makeText(requireContext(), status, Toast.LENGTH_SHORT).show();
        });

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
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        File externalStorageDir = requireActivity().getExternalFilesDir(null);
        String outpath;
        if(externalStorageDir!=null){
            outpath = externalStorageDir.getAbsolutePath();
        } else {
            outpath = "";
        }
        setOutputFilePath(outpath, outputFilePath);
        // 启动定时任务
        sppViewModel.startScheduledTask(requireContext(), remoteDirPath, outputFilePath);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("spp","destroy");
        binding = null;
        SppViewModel.tips.setLength(0);
        if (sppViewModel.bufferedWriter != null) {
            try {
                // 确保所有数据已经写入
                sppViewModel.bufferedWriter.flush();
                // 关闭 BufferedWriter
                sppViewModel.bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                sppViewModel.bufferedWriter = null;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterMeasurements();
    }
}
