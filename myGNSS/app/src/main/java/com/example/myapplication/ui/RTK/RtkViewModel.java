package com.example.myapplication.ui.RTK;

import static com.example.myapplication.MainActivity.auto_record_mode;
import static com.example.myapplication.MainActivity.checked;
import static com.example.myapplication.MainActivity.if_auto_record;
import static com.example.myapplication.MainActivity.newFolder2;
import static com.example.myapplication.MainActivity.outputFilePath;
import static com.example.myapplication.MainActivity.sateph;
import static com.example.myapplication.positiondatabase.insertData;
import static com.example.myapplication.ui.DGPS.DgpsViewModel.out_loghead_base;
import static com.example.myapplication.ui.FtpConnection.downloadFile;
import static com.example.myapplication.ui.Sol.SolViewModel.open;
import static com.example.myapplication.ui.Sol.SolViewModel.prcopt;
import static com.example.myapplication.ui.measure_util.calculateSelectedValue;
import static com.example.myapplication.ui.measure_util.isSpp;
import static com.example.myapplication.ui.measure_util.out_sol;
import static com.example.myapplication.ui.measure_util.out_sol_record;
import static com.example.myapplication.ui.measure_util.readobs;
import static com.example.myapplication.ui.measure_util.resetobs;
import static com.example.myapplication.ui.measure_util.solopt_default;
import static com.example.myapplication.ui.measure_util.sortobs;
import static com.example.myapplication.ui.status.HomeViewModel.out_loghead;
import static com.rtklib.bean.nav_t.MAXRCV;
import static com.rtklib.bean.nav_t.getMaxSat;
import static com.rtklib.pntpos.pntPos.PMODE_SINGLE;
import static com.rtklib.pntpos.pntPos.SOLQ_NONE;
import static com.rtklib.pntpos.pntPos.pnt_pos;
import static com.rtklib.pntpos.rescode.R2D;
import static com.rtklib.pntpos.rescode.ecef2pos;
import static com.rtklib.postpos.outHead.outrpos;
import static com.rtklib.postpos.outSol.SOLF_ENU;
import static com.rtklib.postpos.outSol.SOLF_LLH;
import static com.rtklib.postpos.outSol.SOLF_XYZ;
import static com.rtklib.postpos.outSol.covenu;
import static com.rtklib.postpos.outSol.deg2dms;
import static com.rtklib.postpos.outSol.opt2sep;
import static com.rtklib.postpos.outSol.soltocov;
import static com.rtklib.postpos.procPos.MAXERRMSG;
import static com.rtklib.postpos.procPos.pppnx;
import static com.rtklib.readobsnav.readObsNav.read_obsnav;
import static com.rtklib.readobsnav.readrnxh.SYS_CMP;
import static com.rtklib.readobsnav.readrnxh.SYS_GAL;
import static com.rtklib.readobsnav.readrnxh.SYS_GLO;
import static com.rtklib.readobsnav.readrnxh.SYS_GPS;
import static com.rtklib.readobsnav.readrnxh.SYS_IRN;
import static com.rtklib.readobsnav.readrnxh.SYS_QZS;
import static com.rtklib.readobsnav.readrnxh.SYS_SBS;
import static com.rtklib.readobsnav.satSys.sat_sys;
import static com.rtklib.rtkpos.rtkPos.D2R;
import static com.rtklib.rtkpos.rtkPos.PMODE_FIXED;
import static com.rtklib.rtkpos.rtkPos.rel_pos;
import static com.rtklib.rtkpos.udState.NR;
import static com.rtklib.rtkpos.udState.NX;
import static com.rtklib.rtkpos.udState.PMODE_DGPS;
import static com.rtklib.rtkpos.udState.PMODE_MOVEB;
import static com.rtklib.util.math.zeros;
import static com.rtklib.util.util1.time2epoch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.MainActivity;
import com.example.myapplication.positiondatabase;
import com.example.myapplication.ui.FtpConnection;
import com.example.myapplication.ui.MyService;
import com.example.myapplication.ui.Ntrip.rtcm_t;
import com.rtklib.bean.ambc_t;
import com.rtklib.bean.gtime_t;
import com.rtklib.bean.nav_t;
import com.rtklib.bean.obs_t;
import com.rtklib.bean.obsd_t;
import com.rtklib.bean.pcv_t;
import com.rtklib.bean.prcopt_t;
import com.rtklib.bean.rtk_t;
import com.rtklib.bean.sol_t;
import com.rtklib.bean.solopt_t;
import com.rtklib.bean.ssat_t;
import com.rtklib.bean.sta_t;
import com.rtklib.rtkpos.udState;

import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RtkViewModel extends ViewModel {
    // TODO: Implement the ViewModel
    String str_name_value = "";
    String str_name = "";
    private final MutableLiveData<String> mText;
    private final MutableLiveData<String> rtcm_num;
    private final MutableLiveData<String> latvalue;
    private final MutableLiveData<String> lonvalue;
    private final MutableLiveData<String> height;
    private final MutableLiveData<String> rtktips;
    private final MutableLiveData<String> solstate;
    private final MutableLiveData<String> updateStatus;
    private final MutableLiveData<String> updateRatio;
    private final MutableLiveData<Integer> textColor;
    private final MutableLiveData<String> updateVAccuracy;
    private final MutableLiveData<String> updateHAccuracy;
    private final MutableLiveData<String> updateUAccuracy;
    private final MutableLiveData<String> updateStrName;
    private final MutableLiveData<String> updateStrNameValue;
    private final DecimalFormat df = new DecimalFormat("#.###"); // 保留3位小数
    private final DecimalFormat df1 = new DecimalFormat("#.#####");
    LocationManager mLocationManager;
    private static obsd_t[] obs = new obsd_t[getMaxSat()];
    boolean first_rec = false;
    static StringBuilder tips = new StringBuilder();
    double[] current_lat = new double[3];
    double[] current_lon = new double[3];
    double elevation = 0;
    gtime_t ts = new gtime_t(0, 0);
    gtime_t te = new gtime_t(0, 0);
    double ti = 0.0;
    int[] index = {0};
    static BufferedWriter obsWriter;
    static BufferedWriter baseWriter;
    static nav_t navs=new nav_t();          /* navigation data */
    nav_t tempnavs = new nav_t();
    static obs_t obss = new obs_t();
    public static sta_t[] stas = new sta_t[MAXRCV];
    pcv_t[] pcv = new pcv_t[3];
    int nr=0;//基准站数据个数
    static obsd_t[] obssd = new obsd_t[getMaxSat()];
    int n=0;//流动站数据个数
    int nn=0;
//    int rtcm_gps_obs_n = 0;
    static rtcm_t rtcm;
    static double[] stapos = new double[3];
    private rtk_t rtk = new rtk_t();
    private int result;
    double[] pos = new double[3];
    double[] dms1 = new double[3];
    double[] dms2 = new double[3];
    SQLiteDatabase db;
    BufferedWriter bufferedWriter;
    BufferedWriter bufferedWriterSpp;
    StringBuilder msg;
    String msg1 = "";
    private FTPClient client;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private static final String COMMENTH = "%";
    private ArrayList<String> ave_result = new ArrayList<>();
    private sol_t spp_sol = new sol_t();

    public RtkViewModel(Context context){
        // 在 Activity 或 Fragment 中启动服务之前设置监听器
        MyService myService = new MyService();
        myService.setOnRtcmDataReceivedListener(rtcmData -> {
            // 处理接收到的 RTCM 数据
            rtcm.getObs().data = rtcmData.getObs().data.clone();
        });
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        positiondatabase dbHelper = new positiondatabase(context);
        db = dbHelper.getWritableDatabase();
        // 获取当前的UTC时间
        LocalDateTime utcTime = LocalDateTime.now(ZoneOffset.UTC);
        // 创建一个DateTimeFormatter来定义输出格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_M_d_H_mm_ss");
        // 使用formatter来格式化时间并输出
        String formattedTime = utcTime.format(formatter);
        mText = new MutableLiveData<>();
        rtcm_num = new MutableLiveData<>();
        latvalue = new MutableLiveData<>();
        lonvalue = new MutableLiveData<>();
        height = new MutableLiveData<>();
        rtktips = new MutableLiveData<>();
        solstate = new MutableLiveData<>();
        updateStatus = new MutableLiveData<>();
        updateRatio = new MutableLiveData<>();
        textColor = new MutableLiveData<>();
        updateVAccuracy = new MutableLiveData<>();
        updateHAccuracy = new MutableLiveData<>();
        updateUAccuracy = new MutableLiveData<>();
        updateStrName = new MutableLiveData<>();
        updateStrNameValue = new MutableLiveData<>();
        updateVAccuracy.setValue("0"+"m");
        updateHAccuracy.setValue("0"+"m");
        mText.setValue("0");
        rtcm_num.setValue("0");
        updateRatio.setValue("0");
        latvalue.setValue(current_lat[0]+"°"+current_lat[1]+"′"+current_lat[2]+"″");
        lonvalue.setValue(current_lon[0]+"°"+current_lon[1]+"′"+current_lon[2]+"″");
        height.setValue(elevation +"m");
        if(first_rec){
            tips.append("Reference station not activated");
            rtktips.setValue(tips.toString());
        }else {
            tips.append("Start");
            //读取星历文件
            readdata(outputFilePath, sateph);
            for (int i = 0; i < getMaxSat(); i++) {
                obs[i] = new obsd_t();
            }
            for (int i = 0; i < getMaxSat(); i++) {
                obssd[i] = new obsd_t();
            }
            //初始化pcv
            for (int i = 0; i < getMaxSat(); i++) {
                navs.pcvs[i] = new pcv_t();
            }
            for (int i = 0; i < 3; i++) {
                pcv[i] = new pcv_t();
            }
            tips.append("\n").append("GNSS System:");
//        prcopt.setElmin(15 * D2R);
            int navsys = calculateSelectedValue(checked);
            if (navsys == 0) {
                prcopt.navsys = SYS_CMP;
                tips.append("BDS");
            } else {
                prcopt.navsys = navsys;
            }
            //提示当前卫星系统
            String[] s7 = {"GPS", "BDS", "GLONASS", "Galileo"};
            for (int ii = 0; ii < checked.length; ii++) {
                if (checked[ii])
                    tips.append(String.format(" %s", s7[ii]));
            }
            if (prcopt.getIonoopt() == 0) {
                tips.append("\n").append("Ionosphere Model:").append("off");
            } else if (prcopt.getIonoopt() == 1) {
                tips.append("\n").append("Ionosphere Model:").append("Klobuchar");
            }
            if (prcopt.getTropopt() == 0) {
                tips.append("\n").append("Troposphere Model:").append("off");
            } else if (prcopt.getTropopt() == 1) {
                tips.append("\n").append("Troposphere Model:").append("Saastamoinen");
            }
            tips.append("\n").append("Cut-off Angle:").append(prcopt.getElmin() / D2R).append("°");
            tips.append("\n").append("Frenquencies:L1");
            tips.append("\n").append("Integer Ambiguity Res:");
            if (prcopt.getModear() == 0) {
                tips.append("OFF");
            } else if (prcopt.getModear() == 1) {
                tips.append("Continuous");
            } else if (prcopt.getModear() == 2) {
                tips.append("Instantaneous");
            } else if (prcopt.getModear() == 3) {
                tips.append("Fix and Hold");
            }
            solstate.postValue("0");
            if (open) {
                tips.append("\n").append("Data recording has been enabled, positioning will not be performed.");
                tips.append("\n").append("Recording...");
            }
//            updateRatio.setValue("0");
            prcopt.setSateph(0);
            prcopt.mode = 2;
            prcopt.refpos = 1;
            prcopt.setNf(1);
            rtkinit(rtk, prcopt);
            tips.append("\n").append("Min ratio to fix ambiguity:");
            tips.append(rtk.opt.getThresar()[0]);
            rtktips.setValue(tips.toString());

            File newFolder = new File(context.getExternalFilesDir(null), "MyGnss");
            if (!newFolder.exists()) {
                newFolder.mkdirs();
            }
            try {
                File out_folder = new File(newFolder, "RTK" + formattedTime + ".pos");
                File out_folder_spp = new File(newFolder, "SPP" + formattedTime + ".pos");
                FileWriter fileWriter = new FileWriter(out_folder);
                FileWriter fileWriterSpp = new FileWriter(out_folder_spp);
                bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriterSpp = new BufferedWriter(fileWriterSpp);
                if(if_auto_record){
                    out_head(bufferedWriter, prcopt, solopt_default);
                    out_head(bufferedWriterSpp, prcopt, solopt_default);
                }else{
                    out_head_record(bufferedWriter, prcopt, solopt_default);
                    out_head_record(bufferedWriterSpp, prcopt, solopt_default);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
//            changeTextColor(Color.rgb(255, 0, 0));
        }
    }
    void startScheduledTask(Context context, String[] infile, String[] outputFilePath) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try {
                        if (outputFilePath == null) {
                            updateStatus.postValue("Output path is null");
                            Log.e("Decompress", "Output path is null");
                            return; // 或者抛出异常
                        }
                        FTPClient client = FtpConnection.getFTPClient();
                        assert client != null;
                        if (client.isConnected()) {
                            downloadFile(context, client, MainActivity.remoteDirPath, infile, 1);
                            client.logout();
                            client.disconnect();
                            FtpConnection.decompressGzipFile(infile, outputFilePath);
                        }
                        // 执行完成后更新LiveData
                        updateStatus.postValue("Successfully updated ephemeris");
                        readdata(outputFilePath,sateph);
                    } catch (IOException e) {
                        Log.e("TAG", "An error occurred", e); // 使用 Log 记录异常
                        // 执行完成后更新LiveData
                        updateStatus.postValue("Failed to update ephemeris");
                    }
                }).start();
                // 重新安排下次执行
                handler.postDelayed(this, 30 * 60 * 1000); // 30分钟
            }
        };
        // 开始定时器
        handler.post(runnable);
    }
    public static void out_head_record(BufferedWriter bufferedWriter, prcopt_t opt, solopt_t sopt) throws IOException {
        int i;
        int[] sys = {SYS_GPS, SYS_GLO, SYS_GAL, SYS_QZS, SYS_CMP, SYS_IRN, SYS_SBS, 0};
        String[] s1 = {"Single", "DGPS", "Kinematic", "Static", "Moving-Base", "Fixed",
                "PPP Kinematic", "PPP Static", "PPP Fixed", "", "", ""};
        String[] s2 = {"L1", "L1+2", "L1+2+3", "L1+2+3+4", "L1+2+3+4+5", "L1+2+3+4+5+6", "", "", ""};
        String[] s3 = {"Forward", "Backward", "Combined", "", "", ""};
        String[] s4 = {"OFF", "Broadcast", "SBAS", "Iono-Free LC", "Estimate TEC", "IONEX TEC",
                "QZSS Broadcast", "", "", "", ""};
        String[] s5 = {"OFF", "Saastamoinen", "SBAS", "Estimate ZTD", "Estimate ZTD+Grad", "", "", ""};
        String[] s6 = {"Broadcast", "Precise", "Broadcast+SBAS", "Broadcast+SSR APC",
                "Broadcast+SSR CoM", "", "", ""};
//        String[] s7 = {"GPS", "GLONASS", "Galileo", "QZSS", "BDS", "NavIC", "SBAS", "", "", ""};
        String[] s7 = {"GPS", "BDS", "GLONASS", "Galileo"};
        String out = "% program   : RTKLIB(Java) ver.1.0";

        String[] s11 = {"WGS84", "Tokyo"};
        String[] s22 = {"ellipsoidal", "geodetic"};
        String[] s33 = {"GPST", "UTC ", "JST "};
        char[] sep = opt2sep(sopt);
        String leg1 = "Q=1:fix,2:float,3:sbas,4:dgps,5:single,6:ppp";
        String leg2 = "ns=# of satellites";

        StringBuilder sb2 = new StringBuilder();

        if (sopt.outhead != 0) {
            sb2.append(String.format("%s (", COMMENTH));
            if (sopt.posf == SOLF_XYZ)
                sb2.append("x/y/z-ecef=WGS84");
            else if (sopt.posf == SOLF_ENU)
                sb2.append("e/n/u-baseline=WGS84");
            else
                sb2.append(String.format("lat/lon/height=%s/%s", s11[sopt.datum], s22[sopt.height]));
            sb2.append(String.format(",%s,%s)\r\n", leg1, leg2));
        }
        sb2.append(String.format("%s  %s       ", COMMENTH, s33[sopt.times]))
                .append(sep[0]);
        if (sopt.posf == SOLF_LLH) { /* lat/lon/hgt */
            if (sopt.degf != 0) {
                sb2.append(String.format("%s%16s%s%16s%s%10s%s%3s%s%3s%s%8s%s%8s%s%8s%s%8s%s%8s%s%8s%s%6s%s%6s"
                                , "name","latitude(d'\")", sep[0], "longitude(d'\")", sep[0], "height(m)",
                                sep[0], "Q", sep[0], "ns", sep[0], "sdn(m)", sep[0], "sde(m)", sep[0], "sdu(m)",
                                sep[0], "sdne(m)", sep[0], "sdeu(m)", sep[0], "sdue(m)", sep[0], "age(s)",
                                sep[0], "ratio"))
                        .append("\n");
            } else {
                sb2.append(String.format("%s%14s%s%14s%s%10s%s%3s%s%3s%s%8s%s%8s%s%8s%s%8s%s%8s%s%8s%s%6s%s%6s",
                                "name", "latitude(deg)", sep[0], "longitude(deg)", sep[0], "height(m)", sep[0],
                                "Q", sep[0], "ns", sep[0], "sdn(m)", sep[0], "sde(m)", sep[0], "sdu(m)", sep[0],
                                "sdne(m)", sep[0], "sdeu(m)", sep[0], "sdun(m)", sep[0], "age(s)", sep[0],
                                "ratio"))
                        .append("\n");
            }
            if (sopt.outvel != 0) {
                sb2.append(String.format("%s%10s%s%10s%s%10s%s%9s%s%8s%s%8s%s%8s%s%8s%s%8s",
                                sep[0], "vn(m/s)", sep[0], "ve(m/s)", sep[0], "vu(m/s)", sep[0], "sdvn", sep[0],
                                "sdve", sep[0], "sdvu", sep[0], "sdvne", sep[0], "sdveu", sep[0], "sdvun"))
                        .append("\n");
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s pos mode  : %s\r\n", COMMENTH, s1[opt.mode]));
        if (PMODE_DGPS <= opt.mode && opt.mode <= PMODE_FIXED) {
            sb.append(String.format("%s freqs     : %s\r\n", COMMENTH, s2[opt.getNf() - 1]));
        }
        if (opt.mode > PMODE_SINGLE) {
            sb.append(String.format("%s solution  : %s\r\n", COMMENTH, s3[opt.getSoltype()]));
        }
        sb.append(String.format("%s elev mask : %.2f deg\r\n", COMMENTH, opt.getElmin() * R2D));
        if (opt.mode > PMODE_SINGLE) {
            sb.append(String.format("%s dynamics  : %s\r\n", COMMENTH, opt.getDynamics() == 1 ? "on" : "off"));
            sb.append(String.format("%s tidecorr  : %s\r\n", COMMENTH, opt.getTidecorr() == 1 ? "on" : "off"));
        }
        if (opt.mode <= PMODE_FIXED) {
            sb.append(String.format("%s ionos opt : %s\r\n", COMMENTH, s4[opt.ionoopt]));
        }
        sb.append(String.format("%s tropo opt : %s\r\n", COMMENTH, s5[opt.tropopt]));
        sb.append(String.format("%s ephemeris : %s\r\n", COMMENTH, s6[opt.getSateph()]));
        sb.append(String.format("%s navi sys  :", COMMENTH));
//        if (PMODE_DGPS<=prcopt.mode&&prcopt.mode<=PMODE_FIXED&&prcopt.mode!=PMODE_MOVEB) {
////            fprintf(fp,"%s ref pos   :",COMMENTH);
//            sb.append(String.format("%s ref pos   :", COMMENTH));
//            outrpos(bufferedWriter,stapos,sopt);
////            fprintf(fp,"\n");
//        }
//        for (i = 0; sys[i] == 1; i++) {
//            if ((opt.navsys & sys[i]) == 1)
//                sb.append(String.format(" %s", s7[i]));
//        }
//        for(int ii=0;ii<7;ii++){
//            if(opt.navsys == sys[ii]){
//                sb.append(String.format(" %s", s7[ii]));
//            }
//        }
        for(int ii=0;ii<checked.length;ii++){
            if(checked[ii])
                sb.append(String.format(" %s", s7[ii]));
        }
        sb.append("\r\n");
        try {
            bufferedWriter.write(out);
            bufferedWriter.newLine();//换行写入
            if(PMODE_DGPS<=prcopt.mode&&prcopt.mode<=PMODE_FIXED&&prcopt.mode!=PMODE_MOVEB){
                sb.append(String.format("%s ref pos   :", COMMENTH));
                bufferedWriter.write(sb.toString());
                outrpos(bufferedWriter,stapos,sopt);
                bufferedWriter.newLine();//换行写入
            }else{
                bufferedWriter.write(sb.toString());
                bufferedWriter.newLine();//换行写入
            }
            bufferedWriter.write(sb2.toString());
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void out_head(BufferedWriter bufferedWriter, prcopt_t opt, solopt_t sopt) throws IOException {
        int i;
        int[] sys = {SYS_GPS, SYS_GLO, SYS_GAL, SYS_QZS, SYS_CMP, SYS_IRN, SYS_SBS, 0};
        String[] s1 = {"Single", "DGPS", "Kinematic", "Static", "Moving-Base", "Fixed",
                "PPP Kinematic", "PPP Static", "PPP Fixed", "", "", ""};
        String[] s2 = {"L1", "L1+2", "L1+2+3", "L1+2+3+4", "L1+2+3+4+5", "L1+2+3+4+5+6", "", "", ""};
        String[] s3 = {"Forward", "Backward", "Combined", "", "", ""};
        String[] s4 = {"OFF", "Broadcast", "SBAS", "Iono-Free LC", "Estimate TEC", "IONEX TEC",
                "QZSS Broadcast", "", "", "", ""};
        String[] s5 = {"OFF", "Saastamoinen", "SBAS", "Estimate ZTD", "Estimate ZTD+Grad", "", "", ""};
        String[] s6 = {"Broadcast", "Precise", "Broadcast+SBAS", "Broadcast+SSR APC",
                "Broadcast+SSR CoM", "", "", ""};
//        String[] s7 = {"GPS", "GLONASS", "Galileo", "QZSS", "BDS", "NavIC", "SBAS", "", "", ""};
        String[] s7 = {"GPS", "BDS", "GLONASS", "Galileo"};
        String out = "% program   : RTKLIB(Java) ver.1.0";

        String[] s11 = {"WGS84", "Tokyo"};
        String[] s22 = {"ellipsoidal", "geodetic"};
        String[] s33 = {"GPST", "UTC ", "JST "};
        char[] sep = opt2sep(sopt);
        String leg1 = "Q=1:fix,2:float,3:sbas,4:dgps,5:single,6:ppp";
        String leg2 = "ns=# of satellites";

        StringBuilder sb2 = new StringBuilder();

        if (sopt.outhead != 0) {
            sb2.append(String.format("%s (", COMMENTH));
            if (sopt.posf == SOLF_XYZ)
                sb2.append("x/y/z-ecef=WGS84");
            else if (sopt.posf == SOLF_ENU)
                sb2.append("e/n/u-baseline=WGS84");
            else
                sb2.append(String.format("lat/lon/height=%s/%s", s11[sopt.datum], s22[sopt.height]));
            sb2.append(String.format(",%s,%s)\r\n", leg1, leg2));
        }
        sb2.append(String.format("%s  %s       ", COMMENTH, s33[sopt.times]))
                .append(sep[0]);
        if (sopt.posf == SOLF_LLH) { /* lat/lon/hgt */
            if (sopt.degf != 0) {
                sb2.append(String.format("%16s%s%16s%s%10s%s%3s%s%3s%s%8s%s%8s%s%8s%s%8s%s%8s%s%8s%s%6s%s%6s"
                                , "latitude(d'\")", sep[0], "longitude(d'\")", sep[0], "height(m)",
                                sep[0], "Q", sep[0], "ns", sep[0], "sdn(m)", sep[0], "sde(m)", sep[0], "sdu(m)",
                                sep[0], "sdne(m)", sep[0], "sdeu(m)", sep[0], "sdue(m)", sep[0], "age(s)",
                                sep[0], "ratio"))
                        .append("\n");
            } else {
                sb2.append(String.format("%14s%s%14s%s%10s%s%3s%s%3s%s%8s%s%8s%s%8s%s%8s%s%8s%s%8s%s%6s%s%6s",
                                "latitude(deg)", sep[0], "longitude(deg)", sep[0], "height(m)", sep[0],
                                "Q", sep[0], "ns", sep[0], "sdn(m)", sep[0], "sde(m)", sep[0], "sdu(m)", sep[0],
                                "sdne(m)", sep[0], "sdeu(m)", sep[0], "sdun(m)", sep[0], "age(s)", sep[0],
                                "ratio"))
                        .append("\n");
            }
            if (sopt.outvel != 0) {
                sb2.append(String.format("%s%10s%s%10s%s%10s%s%9s%s%8s%s%8s%s%8s%s%8s%s%8s",
                                sep[0], "vn(m/s)", sep[0], "ve(m/s)", sep[0], "vu(m/s)", sep[0], "sdvn", sep[0],
                                "sdve", sep[0], "sdvu", sep[0], "sdvne", sep[0], "sdveu", sep[0], "sdvun"))
                        .append("\n");
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s pos mode  : %s\r\n", COMMENTH, s1[opt.mode]));
        if (PMODE_DGPS <= opt.mode && opt.mode <= PMODE_FIXED) {
            sb.append(String.format("%s freqs     : %s\r\n", COMMENTH, s2[opt.getNf() - 1]));
        }
        if (opt.mode > PMODE_SINGLE) {
            sb.append(String.format("%s solution  : %s\r\n", COMMENTH, s3[opt.getSoltype()]));
        }
        sb.append(String.format("%s elev mask : %.2f deg\r\n", COMMENTH, opt.getElmin() * R2D));
        if (opt.mode > PMODE_SINGLE) {
            sb.append(String.format("%s dynamics  : %s\r\n", COMMENTH, opt.getDynamics() == 1 ? "on" : "off"));
            sb.append(String.format("%s tidecorr  : %s\r\n", COMMENTH, opt.getTidecorr() == 1 ? "on" : "off"));
        }
        if (opt.mode <= PMODE_FIXED) {
            sb.append(String.format("%s ionos opt : %s\r\n", COMMENTH, s4[opt.ionoopt]));
        }
        sb.append(String.format("%s tropo opt : %s\r\n", COMMENTH, s5[opt.tropopt]));
        sb.append(String.format("%s ephemeris : %s\r\n", COMMENTH, s6[opt.getSateph()]));
        sb.append(String.format("%s navi sys  :", COMMENTH));
//        if (PMODE_DGPS<=prcopt.mode&&prcopt.mode<=PMODE_FIXED&&prcopt.mode!=PMODE_MOVEB) {
////            fprintf(fp,"%s ref pos   :",COMMENTH);
//            sb.append(String.format("%s ref pos   :", COMMENTH));
//            outrpos(bufferedWriter,stapos,sopt);
////            fprintf(fp,"\n");
//        }
//        for (i = 0; sys[i] == 1; i++) {
//            if ((opt.navsys & sys[i]) == 1)
//                sb.append(String.format(" %s", s7[i]));
//        }
//        for(int ii=0;ii<7;ii++){
//            if(opt.navsys == sys[ii]){
//                sb.append(String.format(" %s", s7[ii]));
//            }
//        }
        for(int ii=0;ii<checked.length;ii++){
            if(checked[ii])
                sb.append(String.format(" %s", s7[ii]));
        }
        sb.append("\r\n");
        try {
            bufferedWriter.write(out);
            bufferedWriter.newLine();//换行写入
            if(PMODE_DGPS<=prcopt.mode&&prcopt.mode<=PMODE_FIXED&&prcopt.mode!=PMODE_MOVEB){
                sb.append(String.format("%s ref pos   :", COMMENTH));
                bufferedWriter.write(sb.toString());
                outrpos(bufferedWriter,stapos,sopt);
                bufferedWriter.newLine();//换行写入
            }else{
                bufferedWriter.write(sb.toString());
                bufferedWriter.newLine();//换行写入
            }
            bufferedWriter.write(sb2.toString());
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static void rtkinit(rtk_t rtk, prcopt_t opt)
    {
        int i;
        rtk.sol=new sol_t();
        for (i=0;i<6;i++) rtk.rb[i]=0.0;
        rtk.nx=opt.mode<= udState.PMODE_FIXED?NX(opt):pppnx(opt);
        rtk.na=opt.mode<= udState.PMODE_FIXED?NR(opt):pppnx(opt);
        rtk.tt=0.0;
        rtk.x=zeros(rtk.nx,1);
        rtk.P=zeros(rtk.nx,rtk.nx);
        rtk.xa=zeros(rtk.na,1);
        rtk.Pa=zeros(rtk.na,rtk.na);
        rtk.nfix=rtk.neb=0;
        for (i=0;i<getMaxSat();i++) {
            rtk.ambc[i]=new ambc_t();
            rtk.ssat[i]=new ssat_t();
            // 初始化二维数组的每个元素
            for (int k = 0; k < rtk.ssat[i].pt.length; k++) {
                for (int j = 0; j < rtk.ssat[i].pt[k].length; j++) {
                    rtk.ssat[i].pt[k][j] = new gtime_t(0, 0.0); // 使用默认值初始化
                }
            }
        }
        for (i=0;i<MAXERRMSG;i++) rtk.errbuf[i]=0;
        rtk.opt=opt;
    }
    private void readdata(String[] infile, int sateph) {
        File brdc = new File(infile[0]);
        File prec = new File(infile[1]);
        File clk = new File(infile[2]);
        //读取广播星历文件
        if (brdc.exists()) {
            try {
                if (read_obsnav(ts, te, ti, infile, index, 1, prcopt, obss, tempnavs, stas) == 0) {
                    navs = tempnavs;
                    if(navs.n!=0){
                        tips.append("\n").append("Eph file read successfully.");
                        rtktips.postValue(String.valueOf(tips));
                        Log.d("SPP", "Read successfully.");
                    }else{
                        tips.append("\n").append("Eph file read failed.");
                        rtktips.postValue(String.valueOf(tips));
                    }
                } else {
                    tips.append("\n").append("Eph file read failed.");
                }
            } catch (IOException | CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        } else {
            tips.append("\n").append("No eph file available, please update the eph.");
        }
        //读取精密星历文件和钟差文件
//        if (sateph == 1) {
//            if (prec.exists() && clk.exists()) {
//                try {
//                    readpreceph(infile, infile.length, prcopt, navs, sbss);
//                    if (navs.peph != null) {
//                        tips.append("\n").append("精密星历文件读取成功");
//                        tips.append("\n").append("钟差文件读取成功");
//                    } else {
//                        tips.append("\n").append("精密星历文件读取失败");
//                    }
//                } catch (IOException | CloneNotSupportedException e) {
//                    throw new RuntimeException(e);
//                }
//            } else {
//                tips.append("\n").append("精密星历文件或钟差文件不存在，请更新");
//            }
//        }
    }

    public void processGnssMeasurements(GnssMeasurementsEvent eventArgs) {
        msg = new StringBuilder("position failed");
//        System.out.println(str_name_value);
        // 创建SimpleDateFormat对象，指定日期时间格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        // 获取当前时间
        Date currentTime = new Date();
        // 将当前时间转换为字符串表示
        String currentTimeString = sdf.format(currentTime);
        // 获取GNSS测量数据列表
        List<GnssMeasurement> measurements = new ArrayList<>(eventArgs.getMeasurements());
        resetobs(obs);
        // 遍历测量数据列表
        for (GnssMeasurement measurement : measurements) {
            readobs(measurement, eventArgs.getClock(), obs);
        }
        //计算基准站数据个数
        nr = 0;
        for (obsd_t obsdT : obssd) {
            if (obsdT.sat[0] != 0) {
                nr++;
                // 检查时间是否异常
                if (obssd[0].time.time != obsdT.time.time) {
                    tips.append("\n")
                            .append(currentTimeString)
                            .append(": Reference station: The ")
                            .append(nr)
                            .append("th one has an anomaly: Satellite: ")
                            .append(obsdT.sat[0])
                            .append(" error occurred");
                    obsdT.sat[0] = 0;
                }
            }
        }
        rtktips.postValue(tips.toString());
        //UI更新RTCM中的卫星数量
        rtcm_num.postValue(String.valueOf(nr));
        if (open) {
            log_obs_base(obs, obssd, nr);
        }else {
        int valid_sat = isSpp(obs, navs);
        mText.postValue(String.valueOf(valid_sat));
        if (valid_sat >= 4 && obssd[0].sat[0] != 0) {
            nn = 0;
//            rtcm_gps_obs_n = 0;
            try {
                //计算流动站数据个数
                n = (int) Arrays.stream(obs).filter(o -> o.sat[0] != 0).count(); // 使用流式计算
                obsd_t[] temp = sortobs(obs);
                //判断流动站的数据来自同一个历元
                for (int i = 1; i < n; i++) {
                    if (temp[0].time.time == temp[i].time.time) {
                        nn++;
                    } else {
                        tips.append("\n").append(currentTimeString)
                                .append(":").append("Rover:")
                                .append("The").append(i + 1)
                                .append("th one has an anomaly:")
                                .append("Satellite:").append(temp[i].sat[0])
                                .append("error occurred").append("\n")
                                .append("The time difference is:")
                                .append(temp[0].time.time - temp[i].time.time);
                    }
                }
                if (nn == n-1 && n >= 4) {
                    // 使用 ArrayList 来动态存储有效的 obsd_t 对象
                    List<obsd_t> tempobssdList = new ArrayList<>();
                    for (obsd_t obsdT : obssd) {
                        if (obsdT.sat[0] != 0) {
                            obsd_t clonedObs = (obsd_t) obsdT.clone();
                            clonedObs.rcv = 2; // 设置接收器值
                            tempobssdList.add(clonedObs); // 添加到列表中
                        }
                    }
                    // 将 ArrayList 转换为数组
                    obsd_t[] tempobssd = tempobssdList.toArray(new obsd_t[0]);
                    for (int iu = 0; iu < rtcm.getObs().data.length; iu++) {
                        rtcm.getObs().data[iu].rcv = 2;
                    }
                    // 将两个站的数据赋值到 dgps_obs 中，用于计算
                    obsd_t[] dgps_obs = new obsd_t[n + tempobssd.length];
                    System.arraycopy(temp, 0, dgps_obs, 0, n); // 复制流动站数据
                    System.arraycopy(tempobssd, 0, dgps_obs, n, tempobssd.length); // 复制基准站数据
                    prcopt.setRb(stapos);
                    rtk.opt = prcopt;
                    rtk.rb = stapos;
                    int state = pnt_pos(temp, n, navs, rtk.opt, rtk.sol, null, rtk.ssat, msg1);
                    if (state != 0) {
                        for (int i = 0; i < 6; i++) {
                            spp_sol.rr[i] = rtk.sol.rr[i];
                            spp_sol.qr[i] = rtk.sol.qr[i];
                        }
                        spp_sol.stat = rtk.sol.stat;
                        spp_sol.age = rtk.sol.age;
                        spp_sol.type = rtk.sol.type;
                        spp_sol.ratio = rtk.sol.ratio;
                        spp_sol.time = rtk.sol.time;
                        rtk.sol.stat = SOLQ_NONE;
                        //计算差分龄期，大于某个阈值不予计算
                        double diff = local_timediff(dgps_obs[0].time, dgps_obs[nr].time);
                        rtk.sol.age = (float) diff;
                        prcopt.setMaxtdiff(1);
                        if (Math.abs(diff) >= 2.1) {
                            tips.append("\n").append(currentTimeString).append(":").append("age of differential error:").append(diff);
                            rtktips.postValue(tips.toString());
                            updateRatio.postValue("0");
                            updateVAccuracy.postValue(0+"m");
                            updateHAccuracy.postValue(0+"m");
                            updateUAccuracy.postValue(0+"m");
                            solstate.postValue("failed");
                            changeTextColor(Color.rgb(255,0,0));
                            resetobs(dgps_obs);
                        } else {
                            //这里将配置结构体opt内基准站的坐标赋值给解算结构体rtk内基准站的坐标
                            result = rel_pos(rtk, dgps_obs, n, nr, navs);
                            if (result >= 1) {
                                ecef2pos(rtk.sol.rr, pos);
                                deg2dms(pos[0] * R2D, dms1, 5);
                                deg2dms(pos[1] * R2D, dms2, 5);

                                String strdms11 = Arrays.stream(dms1)
                                        .mapToObj(String::valueOf) // 将 int 转换为 String
                                        .collect(Collectors.joining(", ")); // 使用逗号连接
                                String strdms22 = Arrays.stream(dms2)
                                        .mapToObj(String::valueOf) // 将 int 转换为 String
                                        .collect(Collectors.joining(", ")); // 使用逗号连接
                                current_lat = dms1;
                                current_lon = dms2;

                                String formattedNumber_lat = df.format(current_lat[2]);
                                String formattedNumber_lon = df.format(current_lon[2]);
                                latvalue.postValue(current_lat[0] + "°" + current_lat[1] + "′" + formattedNumber_lat + "″");
                                lonvalue.postValue(current_lon[0] + "°" + current_lon[1] + "′" + formattedNumber_lon + "″");
                                String height1 = df.format(pos[2]);
                                height.postValue(height1+"m");
                                float[] temp_qr = new float[rtk.sol.qr.length];
                                // 遍历原始数组，计算每个元素的开根号
                                for (int i = 0; i < rtk.sol.qr.length; i++) {
                                    temp_qr[i] = (float) Math.sqrt(Math.abs(rtk.sol.qr[i]));
                                }
                                String strqr = Arrays.toString(temp_qr);
                                // 调用插入函数
                                insertData(db, currentTimeString, strdms11, strdms22, height1, strqr, String.valueOf(2));
                                StringBuilder sb = new StringBuilder();
                                StringBuilder sb1 = new StringBuilder();
                                solopt_default.timef = 0;
                                // 遍历原始数组，计算每个元素的开根号
                                double[] P = new double[9];
                                double[] Q = new double[9];
                                soltocov(rtk.sol,P);
                                covenu(pos,P,Q);
                                updateVAccuracy.postValue(df1.format(Math.sqrt(Math.abs(Q[0])))+"m");
                                updateHAccuracy.postValue(df1.format(Math.sqrt(Math.abs(Q[4])))+"m");
                                updateUAccuracy.postValue(df1.format(Math.sqrt(Math.abs(Q[8])))+"m");
                                if(if_auto_record){
                                    if(auto_record_mode){
                                        out_sol(rtk.sol, solopt_default, sb);
                                        out_sol(spp_sol, solopt_default, sb1);
                                    }else{
                                        out_sol_record(str_name_value, rtk.sol, solopt_default, sb);
                                        out_sol_record(str_name_value, spp_sol, solopt_default, sb1);
                                    }
                                    try {
                                        bufferedWriter.write(sb.toString());
                                        bufferedWriterSpp.write(sb1.toString());
                                        bufferedWriter.flush();
                                        bufferedWriterSpp.flush();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                if (rtk.sol.stat == 2) {
                                    solstate.postValue("float");
                                    updateRatio.postValue(String.valueOf(rtk.sol.ratio));
                                    changeTextColor(Color.rgb(255, 255, 0));
                                } else if (rtk.sol.stat == 1) {
                                    solstate.postValue("fix");
                                    updateRatio.postValue(String.valueOf(rtk.sol.ratio));
                                    changeTextColor(Color.rgb(0, 255, 0));
                                }
                                tips.append("\n").append(currentTimeString).append(":").append("Differential positioning：").append("Positioning successful");
                                rtktips.postValue(tips.toString());
                            } else {
                                updateVAccuracy.postValue(0+"m");
                                updateHAccuracy.postValue(0+"m");
                                updateUAccuracy.postValue(0+"m");
                                updateRatio.postValue("0");
//                                resetobs(obs);
                                resetobs(dgps_obs);
                                tips.append("\n").append(currentTimeString).append(":").append("Differential positioning failed:").append(msg.toString());
                                solstate.postValue("rtk failed");
                                rtktips.postValue(tips.toString());
                                changeTextColor(Color.rgb(255, 192, 203));
                            }
                        }
                    } else {
                        updateVAccuracy.postValue(0+"m");
                        updateHAccuracy.postValue(0+"m");
                        updateUAccuracy.postValue(0+"m");
                        updateRatio.postValue("0");
                        solstate.postValue("spp failed");
                        tips.append("\n").append(currentTimeString).append(":").append("Single-point solution failed:").append(msg.toString());
                        rtktips.postValue(tips.toString());
                        changeTextColor(Color.rgb(255,0,0));
                    }
                } else {
                    rtktips.postValue(tips.toString());
                    updateVAccuracy.postValue(0+"m");
                    updateHAccuracy.postValue(0+"m");
                    updateUAccuracy.postValue(0+"m");
                    solstate.postValue("failed");
//                    resetobs(obs);
                    changeTextColor(Color.rgb(255,0,0));
                }
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }else {
            updateVAccuracy.postValue(0+"m");
            updateHAccuracy.postValue(0+"m");
            updateUAccuracy.postValue(0+"m");
            updateRatio.postValue("0");
            changeTextColor(Color.rgb(255,0,0));
        }
    }
    }
    static void write_log_head_baseandobs() {
        // 获取当前的UTC时间
        LocalDateTime utcTime = LocalDateTime.now(java.time.ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_M_d_H_mm_ss");
        String formattedTime = utcTime.format(formatter);
        try {
            File out_folder_obs = new File(newFolder2, "rtk_gnss_log_obs" + formattedTime + ".txt");
            File out_folder_base = new File(newFolder2, "rtk_gnss_log_base" + formattedTime + ".txt");
            FileWriter obsWriter1 = new FileWriter(out_folder_obs);
            FileWriter baseWriter1 = new FileWriter(out_folder_base);
            obsWriter = new BufferedWriter(obsWriter1);
            baseWriter = new BufferedWriter(baseWriter1);
            out_loghead(obsWriter);
            out_loghead_base(baseWriter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    static void log_obs_base(obsd_t[] obs, obsd_t[] obsd, int nr){
        gtime_t firstObsTime = null; // 初始化为 null，表示尚未设置
        int num_sat = 0;
        for(int i=0;i<getMaxSat();i++){
            if(obs[i].sat[0]!=0){
                num_sat++;
                if (firstObsTime == null) {
                    firstObsTime = obs[i].time; // 更新为第一次有效观测的时间
                }
            }
        }
        if(firstObsTime!=null) {
            StringBuilder sb = new StringBuilder();
            double[] ep = new double[6];
            time2epoch(firstObsTime, ep);
            @SuppressLint("DefaultLocale")
            String obstime = String.format("> %4d %2d %2d %2d %2d%11.7f  0 %2d", (int) ep[0], (int) ep[1], (int) ep[2], (int) ep[3], (int) ep[4], ep[5], num_sat);
            sb.append(obstime).append("\n");
            for (int i = 0; i < getMaxSat(); i++) {
                if (obs[i].sat[0] != 0) {
                    int[] prn = new int[1];
                    sat_sys(obs[i].sat[0], prn);
                    if (obs[i].sat[0] <= 32) {
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("G%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
                                prn[0], obs[i].P[0], " ", " ", obs[i].L[0], " ", " ", obs[i].D[0], " ", " ", (double) obs[i].SNR[0], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    }else if(obs[i].sat[0]>=106&&obs[i].sat[0]<=168){
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("C%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
                                prn[0], obs[i].P[0], " ", " ", obs[i].L[0], " ", " ", obs[i].D[0], " ", " ", (double) obs[i].SNR[0], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    }else if(obs[i].sat[0]>=33&&obs[i].sat[0]<=59){
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("R%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
                                prn[0], obs[i].P[0], " ", " ", obs[i].L[0], " ", " ", obs[i].D[0], " ", " ", (double) obs[i].SNR[0], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    } else if(obs[i].sat[0]>=60&&obs[i].sat[0]<=95){
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("E%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
                                prn[0], obs[i].P[0], " ", " ", obs[i].L[0], " ", " ", obs[i].D[0], " ", " ", (double) obs[i].SNR[0], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    }else if(obs[i].sat[0]>=96&&obs[i].sat[0]<=105){
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("J%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
                                prn[0], obs[i].P[0], " ", " ", obs[i].L[0], " ", " ", obs[i].D[0], " ", " ", (double) obs[i].SNR[0], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    }
                }
            }
            resetobs(obs);
            try {
                obsWriter.write(sb.toString());
                obsWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if(nr>0) {
            StringBuilder sb = new StringBuilder();
            double[] ep = new double[6];
            time2epoch(obsd[0].time, ep);
            @SuppressLint("DefaultLocale")
            String obstime = String.format("> %4d %2d %2d %2d %2d%11.7f  0 %2d", (int) ep[0], (int) ep[1], (int) ep[2], (int) ep[3], (int) ep[4], ep[5], nr);
            sb.append(obstime).append("\n");
            for (int i = 0; i < getMaxSat(); i++) {
                if (obsd[i].sat[0] != 0) {
                    int[] prn = new int[1];
                    sat_sys(obsd[i].sat[0], prn);
                    if (obsd[i].sat[0] <= 32) {
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("G%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
                                prn[0], obsd[i].P[0], " ", " ", obsd[i].L[0], " ", " ", obsd[i].D[0], " ", " ", (double) obsd[i].SNR[0], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    }else if(obsd[i].sat[0]>=106&&obsd[i].sat[0]<=168){
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("C%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
                                prn[0], obsd[i].P[0], " ", " ", obsd[i].L[0], " ", " ", obsd[i].D[0], " ", " ", (double) obsd[i].SNR[0], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    }else if(obsd[i].sat[0]>=33&&obsd[i].sat[0]<=59){
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("R%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
                                prn[0], obsd[i].P[0], " ", " ", obsd[i].L[0], " ", " ", obsd[i].D[0], " ", " ", (double) obsd[i].SNR[0], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    } else if(obsd[i].sat[0]>=60&&obsd[i].sat[0]<=95){
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("E%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
                                prn[0], obsd[i].P[0], " ", " ", obsd[i].L[0], " ", " ", obsd[i].D[0], " ", " ", (double) obsd[i].SNR[0], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    }else if(obsd[i].sat[0]>=96&&obsd[i].sat[0]<=105){
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("J%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
                                prn[0], obsd[i].P[0], " ", " ", obsd[i].L[0], " ", " ", obsd[i].D[0], " ", " ", (double) obsd[i].SNR[0], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    }
                }
            }
            resetobs(obsd);
            try {
                baseWriter.write(sb.toString());
                baseWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static double local_timediff(gtime_t t1, gtime_t t2) {
        double diffInSeconds = t1.getTime() - t2.getTime();
        double diffInFractionalSeconds = t1.getSec() - t2.getSec();
        return diffInSeconds + diffInFractionalSeconds;
    }
    public LiveData<String> getText() {
        return mText;
    }
    public LiveData<String> getrtcmnum() {
        return rtcm_num;
    }
    public MutableLiveData<String> getlatvalue() {
        return latvalue;
    }
    public MutableLiveData<String> getlonvalue() {
        return lonvalue;
    }
    public MutableLiveData<String> getelevation() {
        return height;
    }
    public MutableLiveData<String> getdgpstips() {
        return rtktips;
    }
    public MutableLiveData<String> getsolstate(){
        return solstate;
    }
    public MutableLiveData<String> getUpdateStatus(){
        return updateStatus;
    }
    public LiveData<Integer> getTextColor() {
        return textColor;
    }
    public MutableLiveData<String> getUpdateRatio(){
        return updateRatio;
    }
    public MutableLiveData<String> getUpdateVAccuracy(){
        return updateVAccuracy;
    }
    public MutableLiveData<String> getUpdateHAccuracy(){
        return updateHAccuracy;
    }
    public MutableLiveData<String> getUpdateUAccuracy(){
        return updateUAccuracy;
    }
    public MutableLiveData<String> getUpdateStrName(){
        return updateStrName;
    }
    public MutableLiveData<String> getUpdateStrNameValue(){
        return updateStrNameValue;
    }
    public void changeTextColor(int color) {
        textColor.postValue(color);
    }



}