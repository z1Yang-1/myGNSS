package com.example.myapplication.ui.DGPS;

import static com.example.myapplication.ui.Sol.SolViewModel.open;
import static com.example.myapplication.ui.status.HomeFragment.PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.myapplication.databinding.FragmentGalleryBinding;
import com.example.myapplication.ui.Ntrip.OnRtcmDataReceivedListener;
import com.example.myapplication.ui.Ntrip.rtcm_t;
import com.example.myapplication.ui.Spp.SppViewModel;
import com.example.myapplication.ui.Spp.SppViewModelFactory;

import java.io.IOException;

public class DgpsFragment extends Fragment {

    private FragmentGalleryBinding binding;
    DgpsViewModel dgpsViewModel;

    private final BroadcastReceiver rtcmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("dgps","receive rtcm");
            if(!dgpsViewModel.first_rec){
                dgpsViewModel.first_rec=true;
            }
            DgpsViewModel.rtcm = (rtcm_t) intent.getSerializableExtra("rtcmData");
            DgpsViewModel.obssd = DgpsViewModel.rtcm.getObs().data.clone();
            DgpsViewModel.stapos = DgpsViewModel.rtcm.getSta().getPos();
        }
    };
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dgpsViewModel =
                new ViewModelProvider(this, new DgpsViewModelFactory(requireContext())).get(DgpsViewModel.class);

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(rtcmReceiver,
                new IntentFilter("com.example.myapplication.RTCM_DATA_RECEIVED"));
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.dgpssatcount;
        final TextView show_rtcmnum = binding.rtcmmesgcount;
        final TextView show_latitude = binding.latitudeValue;
        final TextView show_longitude = binding.longitudeValue;
        final TextView show_elevation = binding.elevationValue;
        final TextView show_tips = binding.dgpstext;
        final TextView show_e = binding.eAccuracyValue;
        final TextView show_n = binding.nAccuracyValue;
        final TextView show_u = binding.uAccuracyValue;
        dgpsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        dgpsViewModel.getrtcmnum().observe(getViewLifecycleOwner(), show_rtcmnum::setText);
        dgpsViewModel.getlatvalue().observe(getViewLifecycleOwner(),show_latitude::setText);
        dgpsViewModel.getlonvalue().observe(getViewLifecycleOwner(),show_longitude::setText);
        dgpsViewModel.getelevation().observe(getViewLifecycleOwner(),show_elevation::setText);
        dgpsViewModel.getdgpstips().observe(getViewLifecycleOwner(),show_tips::setText);
        dgpsViewModel.getUpdateEAccuracy().observe(getViewLifecycleOwner(),show_e::setText);
        dgpsViewModel.getUpdateNAccuracy().observe(getViewLifecycleOwner(),show_n::setText);
        dgpsViewModel.getUpdateUAccuracy().observe(getViewLifecycleOwner(),show_u::setText);

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
            DgpsViewModel.write_log_head_baseandobs();
        }
        return root;
    }

    private final GnssMeasurementsEvent.Callback gnssMeasurementEventListener =
            new GnssMeasurementsEvent.Callback() {
                @Override
                public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
                    super.onGnssMeasurementsReceived(eventArgs);
                    Log.d("SPP", "measurement called");
                    dgpsViewModel.processGnssMeasurements(eventArgs);
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
                dgpsViewModel.mLocationManager.registerGnssMeasurementsCallback(gnssMeasurementEventListener);
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
        dgpsViewModel.mLocationManager.unregisterGnssMeasurementsCallback(gnssMeasurementEventListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unRegisterMeasurements();
        DgpsViewModel.tips.setLength(0);
        if (dgpsViewModel.bufferedWriter != null) {
            try {
                // 确保所有数据已经写入
                dgpsViewModel.bufferedWriter.flush();
                // 关闭 BufferedWriter
                dgpsViewModel.bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                dgpsViewModel.bufferedWriter = null;
            }
        }
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dgpsViewModel.closeWriter();
    }
}