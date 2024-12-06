package com.example.myapplication.ui;

import static com.example.myapplication.ui.Ntrip.NtripFragment.initrtcm;
import static com.example.myapplication.ui.Ntrip.NtripFragment.inputrtcm3;
import static com.example.myapplication.ui.Ntrip.NtripViewModel.gettime;
import static com.rtklib.bean.nav_t.getMaxSat;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.myapplication.R;
import com.example.myapplication.ui.Ntrip.NtripClient;
import com.example.myapplication.ui.Ntrip.OnRtcmDataReceivedListener;
import com.example.myapplication.ui.Ntrip.rtcm_t;
import com.rtklib.bean.gtime_t;
import com.rtklib.bean.obs_t;
import com.rtklib.bean.obsd_t;
import com.rtklib.bean.sta_t;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.List;

public class MyService extends Service implements OnRtcmDataReceivedListener {

    private static final String CHANNEL_ID = "my_channel_id";
    private static final int NOTIFICATION_ID = 1;
    private Thread getrtcmThread;
    private boolean isRunning = true; // 控制线程运行
    private List<NtripClient> mData; // 存储来自 ViewModel 的数据
    private double[] gga_message;
    static rtcm_t rtcm = new rtcm_t();
    static gtime_t[] obstime = new gtime_t[getMaxSat()];
    static obsd_t[] obssd = new obsd_t[getMaxSat()];
    static double[] stapos = new double[3];
    static int no = 0;
    static OnRtcmDataReceivedListener dataReceivedListener;
    private Handler handler;
    private Runnable runnable;
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "My Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("This is my notification channel");

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        handler = new Handler(Looper.getMainLooper());

        runnable = new Runnable() {
            @Override
            public void run() {
                startRtcmThread(); // 启动线程
                handler.postDelayed(this, 4 * 60 * 1000); // 每四分钟重新执行
            }
        };
        handler.post(runnable); // 启动第一次执行
    }
    public MyService() {
        initrtcm();
        rtcm.setSta(new sta_t());
        for(int i=0;i<getMaxSat();i++){
            obssd[i] = new obsd_t();
            obstime[i] = new gtime_t(0,0);
        }
    }
    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 从 Intent 中获取传递过来的数据
        mData = (List<NtripClient>) intent.getSerializableExtra("NtripDataList");
        gga_message = intent.getDoubleArrayExtra("GgaMessage");
        // 确保 mData 不为空
        if (mData != null && !mData.isEmpty()) {
            startRtcmThread(); // 启动线程
        } else {
            stopSelf(); // 数据为空，停止服务
        }
        return START_STICKY;
    }
    private void startRtcmThread() {
        getrtcmThread = new Thread(() -> {
            try {
                Socket socket = new Socket(mData.get(0).getHost(), mData.get(0).getPort());
                try (
                    BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    OutputStream os = socket.getOutputStream()) {
                    String auth = mData.get(0).getNtrip_User() + ":" + mData.get(0).getNtrip_password();
                    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                    String request = "GET /" + mData.get(0).getMountPoint() + " HTTP/1.0\r\n";
                    request += "User-Agent: NTRIP GNSSInternetRadio/1.4.10\r\n";
                    request += "Authorization: Basic " + encodedAuth + "\r\n";
                    request += "Accept: */*\r\n";
                    request += "Connection: close\r\n";
                    request += "\r\n";
                    os.write(request.getBytes());
                    os.flush();

                    String line;
                    while ((line = reader.readLine()) != null && isRunning) {
                        if (line.contains("200 OK")) {
                            System.out.println("Connected to mount point and ready to receive RTCM data.");
                            Intent intent = new Intent("com.example.myapplication.SHOW_TOAST");
                            intent.putExtra("toastMessage", "Successfully connected to the CORS");
                            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                            String ggaData = "$GPGGA," + gettime() + "," + gga_message[0] + ",N," + gga_message[1] +
                                    ",E,2,10,5.00,10.0000,M,-2.860,M,08,0000*7A\r\n";
                            os.write(ggaData.getBytes());
                            os.flush();

                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while (((bytesRead = bis.read(buffer)) != -1) && isRunning) {
                                // 数据处理逻辑
                                for (int i = 0; i < bytesRead; i++) {
                                    int result = inputrtcm3(rtcm, buffer[i], obssd, obstime, stapos);
                                    if (result > 0) {
//                                        Log.d("成功解码消息，类型: ", String.valueOf(result));
                                        if(stapos[0]!=0){
                                            rtcm.getSta().setPos(stapos);
                                        }
                                        if (result == 1) {
                                            no=0;
                                            for (int k = 0; k < obstime.length; k++) {
                                                if (obssd[k].sat[0] != 0) no++;
                                            }
                                            for (int k = 0; k < obstime.length; k++) {
                                                if (obstime[k].time != 0) {
                                                    obssd[k].time.setTime(obstime[k].time);
                                                    obssd[k].time.setSec(obstime[k].sec);
                                                }
                                            }
                                            obs_t tempobs = new obs_t();
                                            tempobs.setN(no);
                                            tempobs.data = obssd.clone();
                                            rtcm.setObs(tempobs);
                                            //传输数据
//                                            dataReceivedListener.onRtcmDataReceived(rtcm);
                                           Intent broadcastIntent = new Intent("com.example.myapplication.RTCM_DATA_RECEIVED");
                                           broadcastIntent.putExtra("rtcmData", rtcm);
                                           LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                                        }
                                    }
                                }
                            }
                        } else if (line.startsWith("ERROR")) {
                            System.err.println("Error connecting to mount point: " + line);
                            isRunning = false;
                            Intent intent = new Intent("com.example.myapplication.SHOW_TOAST");
                            intent.putExtra("toastMessage", "Incorrect password, failed to connect to the CORS");
                            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

                            Intent broadcastIntent = new Intent("com.example.myapplication.SWITCH_STATE");
                            broadcastIntent.putExtra("isswitchsate", false);
                            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        getrtcmThread.start();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 在服务销毁时停止线程
        Log.d("service","destory");
//        isRunning = false;
//        if (getrtcmThread != null && getrtcmThread.isAlive()) {
//            getrtcmThread.interrupt();
//        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onRtcmDataReceived(rtcm_t rtcmData) {
        dataReceivedListener = (OnRtcmDataReceivedListener) rtcmData;
    }
    public void setOnRtcmDataReceivedListener(OnRtcmDataReceivedListener listener) {
        dataReceivedListener = listener;
    }
}