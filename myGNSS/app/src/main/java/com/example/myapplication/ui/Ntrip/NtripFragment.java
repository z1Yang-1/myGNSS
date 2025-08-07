package com.example.myapplication.ui.Ntrip;

import static com.example.myapplication.MainActivity.menuItem;
import static com.example.myapplication.positiondatabase.getLatestData;
import static com.example.myapplication.ui.Ntrip.NtripViewModel.gettime;
import static com.example.myapplication.ui.Ntrip.NtripViewModel.receivertcmdata;
import static com.example.myapplication.ui.Sol.SolViewModel.open;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.positiondatabase;
import com.example.myapplication.ui.DGPS.DgpsFragment;
import com.example.myapplication.ui.DGPS.DgpsViewModel;
import com.example.myapplication.ui.Ntrip.Adapter.CustomSwicthAdapter;
import com.example.myapplication.ui.Ntrip.Adapter.ShowSubitemAdapter;
import com.example.myapplication.ui.Spp.SppViewModel;
import com.example.myapplication.ui.Spp.SppViewModelFactory;
import com.example.myapplication.ui.ToastBroadcastReceiver;
import com.rtklib.bean.gtime_t;
import com.rtklib.bean.obs_t;
import com.rtklib.bean.obsd_t;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class NtripFragment extends Fragment implements OnRtcmDataReceivedListener{
    private NtripViewModel ntripViewModel; // 声明ViewModel
    private DgpsFragment dgpsfragment = new DgpsFragment();
    private ToastBroadcastReceiver mReceiver;

    public NtripFragment() {

    }
    static {
        System.loadLibrary("mygnss");
    }
    public static native int inputrtcm3(rtcm_t rtcm, byte data, obsd_t[] obss, gtime_t[] time, double[] stapos);
    public static native void initrtcm();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mReceiver = new ToastBroadcastReceiver();
        ntripViewModel = new ViewModelProvider(this, new NtripViewModelFactory(requireContext())).get(NtripViewModel.class);
        ntripViewModel.setDataReceivedListener((OnRtcmDataReceivedListener) this); // 设置监听器
    }
    private BroadcastReceiver switchstateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean switch_state = intent.getBooleanExtra("isswitchsate",false);
            ntripViewModel.setSwitchState(switch_state);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_ntrip, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(switchstateReceiver,
                new IntentFilter("com.example.myapplication.SWITCH_STATE"));
        CustomSwicthAdapter adapter = new CustomSwicthAdapter(getContext(), ntripViewModel.data, ntripViewModel);
        ListView listView = requireView().findViewById(R.id.fer);
        listView.setAdapter(adapter);
        ntripViewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            // 显示Toast消息
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });
        ntripViewModel.getSwitchState().observe(getViewLifecycleOwner(), isChecked -> {
        });

        initrtcm();
        // 设置ListView的点击事件
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            if(position == 1){
                showNtripsetting();
            }else if(position == 2){
                showradiodialog();
            }
        });
    }
    //这里是设置GPGGA的选项，选项一是通过单点解，选项二是通过输入经纬度
    private void showradiodialog(){
        // 创建一个AlertDialog.Builder实例
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        // 设置对话框的标题
        builder.setTitle("Select an Option");
        LayoutInflater inflater = LayoutInflater.from(getContext());
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.radio_layout, null);
        // 设置对话框的视图，这里使用你之前创建的dialog_layout.xml布局文件
        builder.setView(view);
        // 设置确定按钮
        builder.setPositiveButton("OK", (dialog, which) -> {
            // 获取选中的RadioButton
            @SuppressLint({"MissingInflatedId", "LocalSuppress"}) RadioButton radioButtonOpt1 = view.findViewById(R.id.radioButtonOpt1);
            @SuppressLint({"MissingInflatedId", "LocalSuppress"}) RadioButton radioButtonOpt2 = view.findViewById(R.id.radioButtonOpt2);

            if (radioButtonOpt1.isChecked()) {
                positiondatabase dbHelper = new positiondatabase(requireContext());
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                String result = getLatestData(db);
                if(Objects.equals(result, "No data found")){
                    Toast.makeText(requireContext(),"There is no positioning data, please obtain positioning data first.",Toast.LENGTH_SHORT).show();
                }else{
                // 使用逗号分隔字符串
                String[] parts = result.split(",");
                // 提取度、分和秒
                String lat_degrees = parts[1]; // 度
                double num = Double.parseDouble(lat_degrees);
                int intValue = (int) num;
                lat_degrees = String.valueOf(intValue);

                String lat_minutes = parts[2]; // 分
                num = Double.parseDouble(lat_minutes);
                intValue = (int) num;
                lat_minutes = String.valueOf(intValue);

                String lat_seconds = parts[3]; // 秒
                double min_lat_seconds = Double.parseDouble(lat_seconds);
                lat_seconds = String.valueOf(min_lat_seconds/60.0);

                String lon_degrees = parts[4]; // 度
                num = Double.parseDouble(lon_degrees);
                intValue = (int) num;
                lon_degrees = String.valueOf(intValue);

                String lon_minutes = parts[5]; // 分
                num = Double.parseDouble(lon_minutes);
                intValue = (int) num;
                lon_minutes = String.valueOf(intValue);

                String lon_seconds = parts[6]; // 秒
                double min_lon_seconds = Double.parseDouble(lon_seconds);
                lon_seconds = String.valueOf(min_lon_seconds/60.0);

                String temp = lat_degrees+lat_minutes;
                double temp1= Double.parseDouble(temp);
                double temp2 = Double.parseDouble(lat_seconds);
                String temp6 = lon_degrees+lon_minutes;
                double temp3= Double.parseDouble(temp6);
                double temp4 = Double.parseDouble(lon_seconds);
                String gga_lat = String.valueOf(temp1+temp2);
                String gga_lon = String.valueOf(temp3+temp4);
                NtripViewModel.gga_message[0] = Double.parseDouble(gga_lat);
                NtripViewModel.gga_message[1] = Double.parseDouble(gga_lon);
                }
            } else if (radioButtonOpt2.isChecked()) {
                showggaseting();
            }
        });

        // 设置取消按钮
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 用户点击取消按钮时，关闭对话框
                dialog.cancel();
            }
        });

        // 创建并显示对话框
        builder.create().show();
    }
    private void showggaseting(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("GGA Settings");
        // 设置自定义布局
        View view = getLayoutInflater().inflate(R.layout.gga_message, null);
        builder.setView(view);
        // 获取EditText的引用，以便后续操作
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) EditText inputOne = view.findViewById(R.id.inputOne);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) EditText inputTwo = view.findViewById(R.id.inputTwo);
        String latText = ntripViewModel.sharedPreferences.getString("Lat_text", "");
        String lotText = ntripViewModel.sharedPreferences.getString("Lot_text", "");
        if(latText.isEmpty() && lotText.isEmpty()){
            inputOne.setText("none");
            inputTwo.setText("none");
        }else{
            inputOne.setText(latText);
            inputTwo.setText(lotText);
        }
        if(NtripViewModel.gga_message[0]!=0) {
            inputOne.setText(String.valueOf(NtripViewModel.gga_message[0]));//纬度
        }
        if(NtripViewModel.gga_message[0]!=0) {
            inputTwo.setText(String.valueOf(NtripViewModel.gga_message[1]));//经度
        }
        // 设置确定按钮和其点击事件
        builder.setPositiveButton("OK", (dialog, which) -> {
            @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor =
                    ntripViewModel.sharedPreferences.edit();
            editor.putString("Lat_text", inputOne.getText().toString());
            editor.putString("Lot_text", inputTwo.getText().toString());
            editor.apply();
            // 使用Integer.parseInt()将字符串转换为int
            try {
                double latitude = Double.parseDouble(String.valueOf(inputOne.getText()));
                double longitude = Double.parseDouble(String.valueOf(inputTwo.getText()));
                NtripViewModel.gga_message[0] = latitude;
                NtripViewModel.gga_message[1] = longitude;

            } catch (NumberFormatException e) {
                // 如果输入的不是有效的整数，会捕获到NumberFormatException异常
                // 这里可以处理异常，例如提示用户输入无效
                Toast.makeText(getContext(), "Please enter a number", Toast.LENGTH_SHORT).show();
            }
        });
        // 设置取消按钮
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    private void showNtripsetting(){
        String[] items = {"IP address", "Port", "Mount point", "Account", "Password"};

        ntripViewModel.adapter = new ShowSubitemAdapter(getContext(), (LinkedList<NtripClient>) ntripViewModel.mData, items);
        //ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, items);
        // 创建AlertDialog.Builder实例
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Ntrip Client Settings");

        // 加载自定义布局
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_listview, null);
        builder.setView(dialogView);

        // 获取ListView并设置适配器
        ListView listView = dialogView.findViewById(R.id.listView);
        listView.setAdapter(ntripViewModel.adapter);

        // 设置ListView的点击事件
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) { // 假设“IP 地址”是数组中的第一个元素
                showIpInputDialog();
            } else if(position == 1){
                showportDialog();
            }else if(position == 2){
                new Thread(() -> {
                    // 执行网络请求
                    ntripViewModel.getSourceTable(NtripViewModel.mData.get(0).getHost(),ntripViewModel.mData.get(0).getPort(),ntripViewModel.Mountpoint);
                }).start();
                showDialogWithSpinner();
            }else if(position == 3){
                showUserDialog();
            }else if(position == 4){
                showPasswordDialog();
            }
        });
        builder.setPositiveButton("OK", (dialog, which) -> {
        });
// 创建并显示对话框
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void showDialogWithSpinner() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_spinner, null);
        builder.setView(dialogView);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        Spinner spinner = dialogView.findViewById(R.id.spinner);

        // 使用ArrayList来创建ArrayAdapter
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, ntripViewModel.Mountpoint);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter1);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 获取选中的项
                NtripViewModel.mData.get(0).setMountPoint(parent.getItemAtPosition(position).toString());
                ntripViewModel.adapter.updateData((LinkedList<NtripClient>) NtripViewModel.mData);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 通常无需处理
            }
        });

        builder.setTitle("Choose an option")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void showIpInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter IP Address");
        // 设置一个EditText视图来收集用户输入的IP地址
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        // 获取当前的IP地址并设置为EditText的初始文本
        String currentIp = NtripViewModel.mData.get(0).getHost();
        input.setText(currentIp);
        input.setSelection(currentIp.length()); // 将光标移动到文本末尾
        builder.setView(input);
        String savedText = ntripViewModel.sharedPreferences.getString("User_ip", "120.253.226.97");
        if(savedText.isEmpty()){
            input.setText("none");
        }else{
            input.setText(savedText);
        }
        // 设置确定按钮和其点击事件
        builder.setPositiveButton("OK", (dialog, which) -> {
            @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor =
                    ntripViewModel.sharedPreferences.edit();
            editor.putString("User_ip", input.getText().toString());
            editor.apply();
            String ipAddress = input.getText().toString();
            NtripViewModel.mData.get(0).setHost(ipAddress);
            ntripViewModel.adapter.updateData((LinkedList<NtripClient>) NtripViewModel.mData);
        });
        // 设置取消按钮
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    private void showportDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter Port Address");
        // 设置一个EditText视图来收集用户输入的IP地址
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        // 获取当前的IP地址并设置为EditText的初始文本
        int currentPort = ntripViewModel.mData.get(0).getPort();
        input.setText(String.valueOf(currentPort));
        input.setSelection(String.valueOf(currentPort).length()); // 将光标移动到文本末尾
        builder.setView(input);
        String savedText = ntripViewModel.sharedPreferences.getString("User_port", "8001");
        if(savedText.isEmpty()){
            input.setText("none");
        }else{
            input.setText(savedText);
        }
        // 设置确定按钮和其点击事件
        builder.setPositiveButton("OK", (dialog, which) -> {
            String port = input.getText().toString();
            @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor =
                    ntripViewModel.sharedPreferences.edit();
            editor.putString("User_port", input.getText().toString());
            editor.apply();
            // 使用Integer.parseInt()将字符串转换为int
            try {
                int number = Integer.parseInt(port);
                // 现在你有了一个int类型的变量number，可以用于进一步的计算等操作
                ntripViewModel.mData.get(0).setPort(number);
                ntripViewModel.adapter.updateData((LinkedList<NtripClient>) ntripViewModel.mData);
            } catch (NumberFormatException e) {
                // 如果输入的不是有效的整数，会捕获到NumberFormatException异常
                // 这里可以处理异常，例如提示用户输入无效
                Toast.makeText(getContext(), "Please enter a number", Toast.LENGTH_SHORT).show();
            }

        });
        // 设置取消按钮
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    private void showUserDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter User");

        // 设置一个EditText视图来收集用户输入的IP地址
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        builder.setView(input);
        String savedText = ntripViewModel.sharedPreferences.getString("User_text", "");
        if(savedText.isEmpty()){
            input.setText("none");
        }else{
            input.setText(savedText);
        }
        // 设置确定按钮和其点击事件
        builder.setPositiveButton("OK", (dialog, which) -> {
            @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor =
                    ntripViewModel.sharedPreferences.edit();
            editor.putString("User_text", input.getText().toString());
            editor.apply();
            String userAddress = input.getText().toString();
            ntripViewModel.mData.get(0).setNtrip_User(userAddress);
            ntripViewModel.adapter.updateData((LinkedList<NtripClient>) ntripViewModel.mData);
            if(!input.getText().toString().isEmpty()){
                ntripViewModel.User = input.getText().toString();
            }

        });
        // 设置取消按钮
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    private void showPasswordDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter Password");
        // 设置一个EditText视图来收集用户输入的IP地址
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        // 获取当前的IP地址并设置为EditText的初始文本
        builder.setView(input);
        // 从SharedPreferences中读取上一次的输入值
        String savedText = ntripViewModel.sharedPreferences.getString("Password_text", "");
        if(savedText.isEmpty()){
            input.setText("none");
        }else{
            input.setText(savedText);
        }
        // 设置确定按钮和其点击事件
        builder.setPositiveButton("OK", (dialog, which) -> {
            @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor =
                    ntripViewModel.sharedPreferences.edit();
            editor.putString("Password_text", input.getText().toString());
            editor.apply();
            String password = input.getText().toString();
            ntripViewModel.mData.get(0).setNtrip_password(password);
            ntripViewModel.adapter.updateData((LinkedList<NtripClient>) ntripViewModel.mData);
            if(!input.getText().toString().isEmpty()){
                ntripViewModel.Password = input.getText().toString();
            }
        });
        // 设置取消按钮
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        Log.d("Ntrip", "ondestroy");
//        ntripViewModel.stoprtcmReceiver();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onStart() {
        super.onStart();
        // 注册广播接收器
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mReceiver,
                new IntentFilter("com.example.myapplication.SHOW_TOAST"));
    }

    @Override
    public void onRtcmDataReceived(rtcm_t rtcmData) {
//        dgpsfragment.onRtcmDataReceived(rtcmData);
    }
}