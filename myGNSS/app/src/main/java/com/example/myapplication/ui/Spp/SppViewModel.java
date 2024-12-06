package com.example.myapplication.ui.Spp;

import static com.example.myapplication.MainActivity.auto_record_mode;
import static com.example.myapplication.MainActivity.checked;
import static com.example.myapplication.MainActivity.if_auto_record;
import static com.example.myapplication.MainActivity.outputFilePath;
import static com.example.myapplication.MainActivity.sateph;
import static com.example.myapplication.positiondatabase.insertData;
import static com.example.myapplication.ui.FtpConnection.downloadFile;
import static com.example.myapplication.ui.Sol.SolViewModel.prcopt;
import static com.example.myapplication.ui.measure_util.calculateSelectedValue;
import static com.example.myapplication.ui.measure_util.out_sol;
import static com.example.myapplication.ui.measure_util.resetobs;
import static com.example.myapplication.ui.measure_util.solopt_default;
import static com.rtklib.bean.nav_t.MAXRCV;
import static com.rtklib.bean.nav_t.getMaxSat;
import static com.rtklib.pntpos.pntPos.PMODE_SINGLE;
import static com.rtklib.pntpos.pntPos.pnt_pos;
import static com.rtklib.pntpos.rescode.R2D;
import static com.rtklib.pntpos.rescode.ecef2pos;
import static com.rtklib.postpos.outSol.SOLF_ENU;
import static com.rtklib.postpos.outSol.SOLF_LLH;
import static com.rtklib.postpos.outSol.SOLF_XYZ;
import static com.rtklib.postpos.outSol.covenu;
import static com.rtklib.postpos.outSol.deg2dms;
import static com.rtklib.postpos.outSol.opt2sep;
import static com.rtklib.postpos.outSol.soltocov;
import static com.rtklib.readobsnav.readObsNav.read_obsnav;
import static com.rtklib.readobsnav.readrnxh.SYS_CMP;
import static com.rtklib.readobsnav.readrnxh.SYS_GAL;
import static com.rtklib.readobsnav.readrnxh.SYS_GLO;
import static com.rtklib.readobsnav.readrnxh.SYS_GPS;
import static com.rtklib.readobsnav.readrnxh.SYS_IRN;
import static com.rtklib.readobsnav.readrnxh.SYS_QZS;
import static com.rtklib.readobsnav.readrnxh.SYS_SBS;
import static com.rtklib.rtkpos.rtkPos.D2R;
import static com.rtklib.rtkpos.rtkPos.PMODE_FIXED;
import static com.rtklib.rtkpos.udState.PMODE_DGPS;

import static java.lang.Math.abs;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
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
import com.example.myapplication.ui.measure_util;
import com.rtklib.bean.gtime_t;
import com.rtklib.bean.nav_t;
import com.rtklib.bean.obs_t;
import com.rtklib.bean.obsd_t;
import com.rtklib.bean.pcv_t;
import com.rtklib.bean.prcopt_t;
import com.rtklib.bean.sbs_t;
import com.rtklib.bean.sol_t;
import com.rtklib.bean.solopt_t;
import com.rtklib.bean.sta_t;

import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SppViewModel extends ViewModel {
    private final MutableLiveData<String> mText;
    private final MutableLiveData<String> satcountText;
    private final MutableLiveData<String> latvalue;
    private final MutableLiveData<String> lonvalue;
    private final MutableLiveData<String> height;
    private final MutableLiveData<String> updateStatus;
    private final MutableLiveData<String> updateVAccuracy;
    private final MutableLiveData<String> updateNAccuracy;
    private final MutableLiveData<String> updateUAccuracy;
    private final DecimalFormat df = new DecimalFormat("#.###"); // 保留3位小数
    private final DecimalFormat df1 = new DecimalFormat("#.#####");
    private final Handler handler = new Handler(Looper.getMainLooper());
    LocationManager mLocationManager;
    BufferedWriter bufferedWriter;
    obsd_t[] obs = new obsd_t[getMaxSat()];
    nav_t navs = new nav_t();
    nav_t tempnavs = new nav_t();
    sol_t sol = new sol_t();
    StringBuilder msg;
    int n = 0;
    int nn = 0;
    double[] pos = new double[3];//用于存储纬度、经度、高程
    double[] dms1 = new double[3];//用于存储纬度（° ′ ″）
    double[] dms2 = new double[3];//用于存储经度（° ′ ″）
    gtime_t ts = new gtime_t(0, 0);
    gtime_t te = new gtime_t(0, 0);
    double ti = 0.0;
    int[] index = {0};
    static obs_t obss = new obs_t();
    public static sta_t[] stas = new sta_t[MAXRCV];
    private static sbs_t sbss = new sbs_t();
    static StringBuilder tips = new StringBuilder();

    private static final String COMMENTH = "%";
    double[] current_lat = new double[3];
    double[] current_lon = new double[3];
    double elevation = 0;
    SQLiteDatabase db;
    String msg1 = "";
    private ArrayList<String> ave_result = new ArrayList<>();

    public SppViewModel(Context context) {
        positiondatabase dbHelper = new positiondatabase(context);
        db = dbHelper.getWritableDatabase();
        // 获取当前的UTC时间
        LocalDateTime utcTime = LocalDateTime.now(java.time.ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_M_d_H_mm_ss");
        String formattedTime = utcTime.format(formatter);
        mText = new MutableLiveData<>();
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        for (int i = 0; i < 2; i++) {
            prcopt.pcvr[i] = new pcv_t();
        }
        for (int i = 0; i < getMaxSat(); i++) {
            navs.pcvs[i] = new pcv_t();
        }
        for (int i = 0; i < getMaxSat(); i++) {
            obs[i] = new obsd_t();
        }
        tips.append("Start......");
        readdata(outputFilePath, sateph);
        tips.append("\n").append("GNSS System：");
//        prcopt.setElmin(15.0 * D2R);
        int navsys = calculateSelectedValue(checked);
        if(navsys == 0){
            prcopt.navsys = SYS_CMP;
            tips.append("BDS");
        }else{
            prcopt.navsys = navsys;
        }
        String[] sys = {"GPS", "BDS", "GLONASS", "Galileo"};
        for(int ii=0; ii<checked.length; ii++){
            if(checked[ii])
                tips.append(String.format(" %s", sys[ii]));
        }
        if(prcopt.getIonoopt()==0){
            tips.append("\n").append("Ionosphere Model:").append("off");
        }else if(prcopt.getIonoopt()==1){
            tips.append("\n").append("Ionosphere Model:").append("Klobuchar");
        }
        if(prcopt.getTropopt()==0){
            tips.append("\n").append("Troposphere Model:").append("off");
        }else if(prcopt.getTropopt()==1){
            tips.append("\n").append("Troposphere Model:").append("Saastamoinen");
        }
        tips.append("\n").append("Cut-off angle:").append(prcopt.getElmin()/D2R).append("°");
        tips.append("\n").append("Frequencies：L1");
//        if(open){
//            tips.append("\n").append("伪距外推：").append("on");
//        }else{
//            tips.append("\n").append("伪距外推：").append("off");
//        }
        satcountText = new MutableLiveData<>();
        latvalue = new MutableLiveData<>();
        lonvalue = new MutableLiveData<>();
        height = new MutableLiveData<>();
        updateStatus = new MutableLiveData<>();
        updateVAccuracy = new MutableLiveData<>();
        updateNAccuracy = new MutableLiveData<>();
        updateUAccuracy = new MutableLiveData<>();
        mText.setValue(tips.toString());
        latvalue.setValue(current_lat[0]+"°"+current_lat[1]+"′"+current_lat[2]+"″");
        lonvalue.setValue(current_lon[0]+"°"+current_lon[1]+"′"+current_lon[2]+"″");
        height.setValue(elevation +"m");
        updateVAccuracy.setValue("0"+"m");
        updateNAccuracy.setValue("0"+"m");
        updateUAccuracy.setValue("0"+"m");
        solopt_default.times = 1;

        File newFolder = new File(context.getExternalFilesDir(null), "MyGnss");
        if (!newFolder.exists()) {
            newFolder.mkdirs();
        }

        try {
            File out_folder = new File(newFolder, "spp" + formattedTime + ".txt");
            FileWriter fileWriter = new FileWriter(out_folder);
            bufferedWriter = new BufferedWriter(fileWriter);
            out_head(bufferedWriter, prcopt, solopt_default);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
                        e.printStackTrace();
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
                    mText.postValue(String.valueOf(tips));
                    Log.d("SPP", "Read successfully.");
                    }else{
                        tips.append("\n").append("Eph file read failed.");
                        mText.postValue(String.valueOf(tips));
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

    public void processGnssMeasurements(GnssMeasurementsEvent eventArgs){
        msg = new StringBuilder("");
        // 创建SimpleDateFormat对象，指定日期时间格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        // 获取当前时间
        Date currentTime = new Date();
        // 将当前时间转换为字符串表示
        String currentTimeString = sdf.format(currentTime);
        // 获取GNSS测量数据列表
        List<GnssMeasurement> measurements = new ArrayList<>(eventArgs.getMeasurements());
        // 遍历测量数据列表
        for (GnssMeasurement measurement : measurements) {
            measure_util.readobs(measurement, eventArgs.getClock(), obs);
        }
        int count = measure_util.isSpp(obs, navs);
        if (count >= 4) {
            satcountText.postValue(String.valueOf(count));
            nn = 0;//判断观测数据是否来自同一个历元
            try {
                n = (int) Arrays.stream(obs)
                        .filter(o -> o.sat[0] != 0)
                        .count();
                obsd_t[] sortobss = measure_util.sortobs(obs);
                for (int i = 1; i < n; i++) { // 从 1 开始，因为 0 已经被比较
                    if (sortobss[0].time.time != sortobss[i].time.time) {
                        tips.append("\n")
                                .append(currentTimeString)
                                .append(": The ")
                                .append(i)
                                .append("th one has an anomaly: Satellite: ")
                                .append(sortobss[i].sat[0])
                                .append(" error occurred");
                        mText.postValue(tips.toString());
                        break; // 找到异常后立即退出循环
                    }else{
                        nn++;
                    }
                }
                if (nn == n-1) {
                    //进行单点定位
                    int state = pnt_pos(sortobss, n, navs, prcopt, sol, null, null, msg1);
                    if (state == 0) {
                        updateVAccuracy.postValue(0+"m");
                        updateNAccuracy.postValue(0+"m");
                        updateUAccuracy.postValue(0+"m");
                        resetobs(obs);
                        tips.append("\n").append(currentTimeString).append("：").append("solution failed：").append(msg.toString());
                        mText.postValue(tips.toString());
                    } else {
                        ecef2pos(sol.rr, pos);
                        deg2dms(pos[0] * R2D, dms1, 5);
                        deg2dms(pos[1] * R2D, dms2, 5);
                        dms1[0] = (int)dms1[0];
                        dms1[1] = (int)dms1[1];
                        dms2[0] = (int)dms2[0];
                        dms2[1] = (int)dms2[1];
                        current_lat = dms1;
                        current_lon = dms2;

                        String formattedNumber_lat = df.format(current_lat[2]);
                        String formattedNumber_lon = df.format(current_lon[2]);
                        latvalue.postValue(current_lat[0]+"°"+current_lat[1]+"′"+formattedNumber_lat+"″");
                        lonvalue.postValue(current_lon[0]+"°"+current_lon[1]+"′"+formattedNumber_lon+"″");
                        String strdms1 = Arrays.stream(dms1)
                                .mapToObj(String::valueOf) // 将 int 转换为 String
                                .collect(Collectors.joining(", ")); // 使用逗号连接
                        String strdms2 = Arrays.stream(dms2)
                                .mapToObj(String::valueOf) // 将 int 转换为 String
                                .collect(Collectors.joining(", ")); // 使用逗号连接
                        String height1 = df.format(pos[2]);
                        height.postValue(height1+"m");
                        float[] temp_qr = new float[sol.qr.length];
                        // 遍历原始数组，计算每个元素的开根号
                        double[] P = new double[9];
                        double[] Q = new double[9];
                        soltocov(sol,P);
                        covenu(pos,P,Q);
//                        updateVAccuracy.postValue(df1.format(Math.sqrt(Math.sqrt((Q[0] * Q[0] + Q[4] * Q[4]) / 2))) +"m");
                        updateVAccuracy.postValue(df1.format(Math.sqrt(Math.abs(Q[0])))+"m");
                        updateNAccuracy.postValue(df1.format(Math.sqrt(Math.abs(Q[4])))+"m");
                        updateUAccuracy.postValue(df1.format(Math.sqrt(Math.abs(Q[8])))+"m");
                        for (int i = 0; i < sol.qr.length; i++) {
                            temp_qr[i] = (float) Math.sqrt(abs(sol.qr[i]));
                        }
                        String strqr = Arrays.toString(temp_qr);
                        StringBuilder sb = new StringBuilder();
                        solopt_default.timef=0;

                        if(auto_record_mode){
                            out_sol(sol, solopt_default, sb);
                            try {
                                bufferedWriter.write(sb.toString());
                                bufferedWriter.flush();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }else {
                            if (if_auto_record) {
                                out_sol(sol, solopt_default, sb);
                                try {
                                    bufferedWriter.write(sb.toString());
                                    bufferedWriter.flush();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            if_auto_record = false;
                        }
                        // 调用插入函数
                        insertData(db, currentTimeString, strdms1, strdms2, height1, strqr, String.valueOf(state));
                        tips.append("\n").append(currentTimeString).append(":").append("Positioning successful");
                        n = 0;
                        mText.postValue(tips.toString());
                    }
                } else {
                    resetobs(obs);
                }
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }else{
            satcountText.postValue(String.valueOf(measure_util.isSpp(obs, navs)));
        }
    }
    public static void out_head(BufferedWriter bufferedWriter, prcopt_t opt, solopt_t sopt) {
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
            bufferedWriter.write(sb.toString());
            bufferedWriter.newLine();//换行写入
            bufferedWriter.write(sb2.toString());
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public LiveData<String> getText() {
        return mText;
    }
    public LiveData<String> getsatcountText() {
        return satcountText;
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
    public LiveData<String> getUpdateStatus() {
        return updateStatus;
    }
    public LiveData<String> getUpdateVAccuracy() {
        return updateVAccuracy;
    }
    public LiveData<String> getUpdateNAccuracy() {
        return updateNAccuracy;
    }
    public LiveData<String> getUpdateUAccuracy() {
        return updateUAccuracy;
    }

}