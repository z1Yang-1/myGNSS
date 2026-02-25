package com.example.myapplication;

import static com.example.myapplication.ui.FtpConnection.downloadFile;
import static com.rtklib.bean.nav_t.getMaxSat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.myapplication.ui.FtpConnection;
import com.example.myapplication.ui.MyService;
import com.example.myapplication.ui.Ntrip.OnRtcmDataReceivedListener;
import com.example.myapplication.ui.Ntrip.rtcm_t;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.rtklib.bean.sol_t;

import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements OnRtcmDataReceivedListener {
    private final String[] strCn0DbHz = {"0","25","30","35","40","45"};
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    public static int Cn0DbHz = 25;
    private FTPClient client;
    public static int sateph=0;
    private static final ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
    public static int Year = utcDateTime.getYear();
    private static int dayofyear = utcDateTime.getDayOfYear();
    public static int dayOfYear = utcDateTime.getDayOfYear();
    private static int utc_hour = utcDateTime.getHour()-1;
    public static String[] infile = new String[3];
    public static String[] outputFilePath = new String[3];
    public static final String[] remoteDirPath = {"/pub/gps/data/hourly/"+Year+"/"+dayofyear+"/", "/pub/gnss/products/mgex", "/pub/gps/data/daily/"+Year+"/"+"brdc","/pub/gps/data/hourly/"+Year+"/"+dayofyear}; // 远程FTP服务器上的文件夹路径
    static NavController navController;
    private positiondatabase mydb;
    NavigationView navigationView;
    public static File newFolder;
    public static MenuItem menuItem;
    public static File newFolder2;
    public static boolean[] checked = {false, false, false, false};
    private static final String PREFS_NAME = "gnss_prefs";
    private static final String PREF_GPS = "pref_gps";
    private static final String PREF_BDS = "pref_bds";
    private static final String PREF_GLONASS = "pref_glonass";
    private static final String PREF_GALILEO = "pref_galileo";
    public static boolean if_auto_record = false;
    public static boolean auto_record_mode = false;
    public static boolean if_setting = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndPromptForGps();
        checkServiceStatus();
        File newFolder1 = new File(this.getExternalFilesDir(null), "MyGnss");//用于存储实时解算的结果文件
        if (!newFolder1.exists()) {
            newFolder1.mkdirs();
        }
        newFolder2 = new File(this.getExternalFilesDir(null), "Gnss_log");
        if (!newFolder2.exists()) {
            newFolder2.mkdirs();
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;
        navigationView = binding.navView;

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow, R.id.nav_Ntrip, R.id.nav_map, R.id.nav_solution, R.id.nav_rtk)
                .setOpenableLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        // 禁用某个项目
        Menu menu = navigationView.getMenu();
        menuItem = menu.findItem(R.id.nav_gallery); // 找到要禁用的项目
        menuItem.setEnabled(true);
        // 设置 NavigationView 的导航项选择监听器
        navigationView.setNavigationItemSelectedListener(item -> {
            // 处理导航项选择
            if (item.getItemId() == R.id.nav_gallery) {
                if(isMyServiceRunning()&&if_setting){
                    navController.navigate(R.id.nav_dgps); // 导航到 DGPS Fragment
                }else{
                    if(!isMyServiceRunning()) {
                        // 显示弹窗“未连接CORS”
                        new AlertDialog.Builder(this)
                                .setTitle("CORS STATUS")
                                .setMessage("CORS is not enabled")
                                .setPositiveButton("YES", (dialog, which) -> {
                                    navController.navigate(R.id.nav_Ntrip);
                                }) // 点击确定按钮后关闭弹窗 // 点击确定按钮后关闭弹窗
                                .show();
                    }else{
                        new AlertDialog.Builder(this)
                                .setTitle("UNSET")
                                .setMessage("Please set up first")
                                .setPositiveButton("YES", (dialog, which) -> {
                                    navController.navigate(R.id.nav_solution);
                                }) // 点击确定按钮后关闭弹窗
                                .show();
                    }
                }
            } else if (item.getItemId() == R.id.nav_home) {
                navController.navigate(R.id.nav_home); // 导航到 Home Fragment
            }else if (item.getItemId() == R.id.nav_Ntrip) {
                navController.navigate(R.id.nav_Ntrip); // 导航到 Home Fragment
            }
            else if (item.getItemId() == R.id.nav_rtk) {
                if(isMyServiceRunning()&&if_setting){
                    navController.navigate(R.id.nav_rtk_activity);
                }else{
                    if(!isMyServiceRunning()) {
                        // 显示弹窗“未连接CORS”
                        new AlertDialog.Builder(this)
                                .setTitle("CORS STATUS")
                                .setMessage("CORS is not enabled")
                                .setPositiveButton("YES", (dialog, which) -> {
                                    navController.navigate(R.id.nav_Ntrip);
                                }) // 点击确定按钮后关闭弹窗 // 点击确定按钮后关闭弹窗
                                .show();
                    }else{
                        new AlertDialog.Builder(this)
                                .setTitle("UNSET")
                                .setMessage("Please set up first")
                                .setPositiveButton("YES", (dialog, which) -> {
                                    navController.navigate(R.id.nav_solution);
                                }) // 点击确定按钮后关闭弹窗
                                .show();
                    }
                }
            }
            else if (item.getItemId() == R.id.nav_spp) {
                if(if_setting) {
                    navController.navigate(R.id.nav_spp); // 导航到 Home Fragment
                }else{
                    new AlertDialog.Builder(this)
                            .setTitle("UNSET")
                            .setMessage("Please set up first")
                            .setPositiveButton("YES", (dialog, which) -> {
                                navController.navigate(R.id.nav_solution);
                            }) // 点击确定按钮后关闭弹窗
                            .show();
                }
            }
            else if (item.getItemId() == R.id.nav_solution) {
                navController.navigate(R.id.nav_solution);
            }
            // 关闭导航抽屉
            drawer.closeDrawers();
            return true; // 返回 true 表示事件已处理
        });

        File externalStorageDir = this.getExternalFilesDir(null);
        String outpath;
        if(externalStorageDir!=null){
            outpath = externalStorageDir.getAbsolutePath();
        } else {
            outpath = "";
        }
        setOutputFilePath(outpath, outputFilePath);

        if(utc_hour<10&&utc_hour>=0){
            remoteDirPath[0] = "/pub/gps/data/hourly/"+Year+"/"+dayofyear+"/"+"0"+utc_hour;
        }else if(utc_hour<0){
            utc_hour+=24;
            dayofyear-=1;
            remoteDirPath[0] = "/pub/gps/data/hourly/"+Year+"/"+dayofyear+"/"+utc_hour;
        }

        if(dayofyear>100){
            remoteDirPath[3] = "/pub/gps/data/hourly/"+Year+"/"+dayofyear;
        }else{
            remoteDirPath[3] = "/pub/gps/data/hourly/"+Year+"/"+"0"+dayofyear;
        }

        binding.appBarMain.fab.setOnClickListener(view -> {
            // 创建对话框视图
            @SuppressLint("ResourceType") View dialogView = getLayoutInflater().inflate(R.xml.dialog_radio_buttons, null);
            RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroup);
            // 创建对话框
            @SuppressLint("QueryPermissionsNeeded") AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Select an option")
                    .setView(dialogView)
                    .setPositiveButton("Confirm", (dialogInterface, i) -> {
                        int selectedId = radioGroup.getCheckedRadioButtonId();
                        if (selectedId == R.id.radioButton1) {
                            // 创建一个指向该文件夹的 URI
//                            Uri uri = FileProvider.getUriForFile(this, "com.example.myapplication.fileprovider", newFolder1);
                            Uri uri1 = Uri.parse(String.valueOf(newFolder1));
                            // 检查文件夹是否存在
                            if (newFolder1.exists()) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(uri1, "*/*");
                                // 添加FLAG_GRANT_READ_URI_PERMISSION以确保文件管理器有权限访问该Uri
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                // 检索文件管理器
                                PackageManager packageManager = getPackageManager();
                                List<ResolveInfo> fileManagers = packageManager.queryIntentActivities(intent, 0);

                                if (!fileManagers.isEmpty()) {
                                    // 如果找到文件管理器，显示选择对话框
                                    Intent chooser = Intent.createChooser(intent, "Select File Manager");
                                    startActivity(chooser);
                                } else {
                                    // 如果没有找到文件管理器，提示用户
                                    Toast.makeText(this, "File manager not found, please install a file manager application.", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "The folder does not exist", Toast.LENGTH_SHORT).show();
                            }
                        } else if (selectedId == R.id.radioButton2) {
                            // 创建一个指向该文件夹的 URI
                            Uri selectedUri = Uri.parse(String.valueOf(newFolder2));
                            // 检查文件夹是否存在
                            if (newFolder2.exists()) {
                                // 使用 ACTION_VIEW Intent 打开文件夹
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(selectedUri, "*/*");
                                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                // 检索文件管理器
                                PackageManager packageManager = getPackageManager();
                                List<ResolveInfo> fileManagers = packageManager.queryIntentActivities(intent, 0);

                                if (!fileManagers.isEmpty()) {
                                    // 如果找到文件管理器，显示选择对话框
                                    Intent chooser = Intent.createChooser(intent, "Select File Manager");
                                    startActivity(chooser);
                                } else {
                                    // 如果没有找到文件管理器，提示用户
                                    Toast.makeText(this, "File manager not found, please install a file manager application.", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "The folder does not exist", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.show();
        });
    }

    public static void setOutputFilePath(String outpath, String[] outputFilePath){
        if(dayOfYear<100) {
            outputFilePath[0] = outpath+"/myFolder/brdm0"+dayOfYear+"0.24n";
            outputFilePath[1] = outpath+"/myFolder/WUM0MGXULT_"+Year+"0"+dayOfYear+"0.sp3";
        } else
        {
            outputFilePath[0] = outpath+"/myFolder/brdm"+dayOfYear+"0.24n";
            outputFilePath[1] = outpath+"/myFolder/WUM0MGXULT_"+Year+dayOfYear+"0.sp3";
        }
        outputFilePath[2] = outpath+"/myFolder/WUM0MGXULT_"+Year+"0"+dayOfYear+"0.clk";
    }



    public static boolean isGpsEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    public static void openGpsSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }
    private void checkAndPromptForGps() {
        boolean isGpsEnabled = isGpsEnabled(this);
        if (!isGpsEnabled) {
            // 创建对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enable Location Service");
            builder.setMessage("The app requires the location service to function properly. Would you like to go to the settings now to enable it?");
            // 添加确定按钮
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 点击确定后，跳转至定位服务设置
                    openGpsSettings(MainActivity.this);
                }
            });
            // 添加取消按钮
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 用户点击取消后，对话框关闭，不执行任何操作
                    // 用户点击取消后，关闭所有活动并退出应用
                    MainActivity.this.finishAffinity();
//                    dialog.dismiss();
                }
            });
            // 显示对话框
            builder.show();
        }
    }

    public static void test(){
        navController.navigate(R.id.nav_home);
    }

    private void udEph(int option){
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Is it necessary to update the ephemeris?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", (dialog, id) -> {
                        // 点击“是”时执行以下代码
                        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.setMessage("Updating the ephemeris...");
                        progressDialog.setCancelable(false); // 设置为不可取消
                        progressDialog.show();
                        // 在子线程中执行FTP操作
                        new Thread(() -> {
                            try {
                                client = FtpConnection.getFTPClient();
                                assert client != null;
                                if (client.isConnected()){
                                    downloadFile(MainActivity.this, client, remoteDirPath, infile, option);
                                    client.logout();
                                    client.disconnect();
                                    FtpConnection.decompressGzipFile(infile, outputFilePath);
                                }
                                // 执行完成后关闭加载圈
                                runOnUiThread(() -> {
//                                    Toast.makeText(MainActivity.this, "Update successful", Toast.LENGTH_SHORT).show();
                                    progressDialog.dismiss();
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                                String errorMsg = "Update failed: ";
                                if (e instanceof java.net.UnknownHostException) {
                                    errorMsg += "Cannot connect to server";
                                } else if (e instanceof java.net.SocketTimeoutException) {
                                    errorMsg += "Connection timeout";
                                } else {
                                    errorMsg += e.getMessage();
                                }

                                final String finalMsg = errorMsg;
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this, finalMsg, Toast.LENGTH_LONG).show();
                                    progressDialog.dismiss();
                                });
                            }
                        }).start();
                    })
                    .setNegativeButton("No", (dialog, id) -> {
                        // 点击“否”时不执行任何操作
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // 递归清理文件夹
    private void cleanFilesRecursive(File directory, Calendar now) {
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isDirectory()) {
                cleanFilesRecursive(file, now);
            } else {
                Calendar fileModifiedTime = Calendar.getInstance();
                fileModifiedTime.setTimeInMillis(file.lastModified());

                if (fileModifiedTime.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        fileModifiedTime.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                        fileModifiedTime.get(Calendar.DAY_OF_MONTH) < now.get(Calendar.DAY_OF_MONTH)) {
                    if (file.delete()) {
                        Log.d("FileCleaner", "Deleted file: " + file.getAbsolutePath());
                    } else {
                        Log.d("FileCleaner", "Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }
    private boolean[] loadCheckboxState() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new boolean[]{
                preferences.getBoolean(PREF_GPS, false),
                preferences.getBoolean(PREF_BDS, false),
                preferences.getBoolean(PREF_GLONASS, false),
                preferences.getBoolean(PREF_GALILEO, false)
        };
    }
    private void saveCheckboxState(boolean[] checkedItems) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_GPS, checkedItems[0]);
        editor.putBoolean(PREF_BDS, checkedItems[1]);
        editor.putBoolean(PREF_GLONASS, checkedItems[2]);
        editor.putBoolean(PREF_GALILEO, checkedItems[3]);
        editor.apply();
    }
    private void showCheckboxDialog() {
        // 复选框选项
        String[] items = {"GPS", "BDS", "GLONASS", "Galileo"};
        boolean[] checkedItems = loadCheckboxState(); // 加载复选框的初始状态
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select GNSS System")
                .setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        // 处理复选框选中状态变化
                        checkedItems[which] = isChecked;
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 保存复选框状态
                        saveCheckboxState(checkedItems);
                        checked = checkedItems;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private int cleanFiles() {
        // 获取SD卡的根目录
        File externalStorageDirectory = this.getExternalFilesDir(null);
        // 设置要清理的文件夹路径（这里假设是"MyFolder"）
        File folderToClean = new File(externalStorageDirectory, "myFolder");
        // 检查文件夹是否存在
        if (folderToClean.exists() && folderToClean.isDirectory()) {
            // 获取当前时间的Calendar实例
            Calendar now = Calendar.getInstance();
            // 遍历文件夹下的所有文件和子目录
            for (File file : Objects.requireNonNull(folderToClean.listFiles())) {
                // 如果文件是目录，则递归清理
                if (file.isDirectory()) {
                    cleanFilesRecursive(file, now);
                } else {
                    // 获取文件的最后修改时间
                    Calendar fileModifiedTime = Calendar.getInstance();
                    fileModifiedTime.setTimeInMillis(file.lastModified());
                    // 如果文件的最后修改时间是在当前时间的前一天，则删除
                    if (fileModifiedTime.get(Calendar.YEAR) < now.get(Calendar.YEAR)){
                        if (file.delete()) {
                            Log.d("FileCleaner", "Deleted file: " + file.getAbsolutePath());
                        } else {
                            Log.d("FileCleaner", "Failed to delete file: " + file.getAbsolutePath());
                            return 0;
                        }
                    }else if(fileModifiedTime.get(Calendar.MONTH) < now.get(Calendar.MONTH)){
                        if (file.delete()) {
                            Log.d("FileCleaner", "Deleted file: " + file.getAbsolutePath());
                        } else {
                            Log.d("FileCleaner", "Failed to delete file: " + file.getAbsolutePath());
                            return 0;
                        }
                    }else if(fileModifiedTime.get(Calendar.DAY_OF_MONTH) < now.get(Calendar.DAY_OF_MONTH)){
                        if (file.delete()) {
                            Log.d("FileCleaner", "Deleted file: " + file.getAbsolutePath());
                        } else {
                            Log.d("FileCleaner", "Failed to delete file: " + file.getAbsolutePath());
                            return 0;
                        }
                    }
                }
            }
        }
        return 1;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_about){
            udEph(1);
        }else if (item.getItemId() == R.id.action_clean){
            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Should the ephemeris files be cleared?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(cleanFiles()==1){
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Cleanup successful", Toast.LENGTH_SHORT).show();
                            });
                        }else {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Cleanup failed", Toast.LENGTH_SHORT).show();
                            });
                        };
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            });
        }
        else if(item.getItemId() == R.id.action_exit){
            finish();
        }
//        else if(item.getItemId() == R.id.action_setting){
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle("Select C/N0 for positioning(current:"+Cn0DbHz+")")
//                    .setSingleChoiceItems(strCn0DbHz, 1, (dialog, which) -> {
//                    })
//                    .setPositiveButton("Confirm", (dialogInterface, i) -> {
//                        int selectedPosition = ((AlertDialog) dialogInterface).getListView().getCheckedItemPosition();
//                        if(selectedPosition == 0) Cn0DbHz=0;
//                        if(selectedPosition == 1) Cn0DbHz=25;
//                        if(selectedPosition == 2) Cn0DbHz=30;
//                        if(selectedPosition == 3) Cn0DbHz=35;
//                        if(selectedPosition == 4) Cn0DbHz=40;
//                        if(selectedPosition == 5) Cn0DbHz=45;
//                    })
//                    .setNegativeButton("Cancel", (dialogInterface, i) -> {
//                    });
//            builder.show();
//        }
//        else if(item.getItemId() == R.id.action_gnss){
//            // 显示带有复选框的对话框
//            showCheckboxDialog();
//        }
        else if(item.getItemId() == R.id.action_exit){
            finish(); // 关闭当前 Activity
            return true;
        }else if(item.getItemId() == R.id.action_mixed_eph){
            udEph(5);
        }else if(item.getItemId() == R.id.action_check){
            Intent intent = new Intent(MainActivity.this, showposdata.class);
            startActivity(intent);
        }else if(item.getItemId() == R.id.action_clean_pos){
            mydb = new positiondatabase(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Confirm deletion of all database data?")
                    .setPositiveButton("Confirm", (dialogInterface, i) -> {
                        mydb.clearDatabase();
                        Toast.makeText(MainActivity.this,"Cleanup successful",Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", (dialogInterface, i) -> {

                    });
            builder.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRtcmDataReceived(rtcm_t rtcmData) {
//        dgpsfragment.onRtcmDataReceived(rtcmData);
    }
    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MyService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    private void checkServiceStatus() {
        boolean isServiceRunning = isMyServiceRunning();
        if (isServiceRunning) {
            // 服务正在运行
            showNotification("服务正在运行");
        } else {
            // 服务未运行
            showNotification("服务未运行");
        }
    }
    private void showNotification(String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = "my_service_channel";

        // 创建通知渠道（Android 8.0 及以上需要）
        NotificationChannel channel = new NotificationChannel(channelId, "Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("服务状态")
                .setContentText(message)
                .setSmallIcon(R.drawable.position_icon) // 替换为你的图标
                .build();
        notificationManager.notify(1, notification);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}