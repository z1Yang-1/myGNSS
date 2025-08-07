package com.example.myapplication.ui.DGPS;

import static com.example.myapplication.MainActivity.Year;
import static com.example.myapplication.MainActivity.checked;
import static com.example.myapplication.MainActivity.newFolder;
import static com.example.myapplication.MainActivity.newFolder2;
import static com.example.myapplication.MainActivity.outputFilePath;
import static com.example.myapplication.positiondatabase.insertData;
import static com.example.myapplication.ui.Sol.SolViewModel.open;
import static com.example.myapplication.ui.Sol.SolViewModel.prcopt;
import static com.example.myapplication.ui.Spp.SppViewModel.out_head;
import static com.example.myapplication.ui.measure_util.bds_b1_lamda;
import static com.example.myapplication.ui.measure_util.calculateSelectedValue;
import static com.example.myapplication.ui.measure_util.gps_l1_lamda;
import static com.example.myapplication.ui.measure_util.isSpp;
import static com.example.myapplication.ui.measure_util.out_sol;
import static com.example.myapplication.ui.measure_util.prcopt_default;
import static com.example.myapplication.ui.measure_util.readobs;
import static com.example.myapplication.ui.measure_util.resetobs;
import static com.example.myapplication.ui.measure_util.solopt_default;
import static com.example.myapplication.ui.measure_util.sortobs;
import static com.example.myapplication.ui.status.HomeViewModel.out_loghead;
import static com.rtklib.bean.nav_t.MAXRCV;
import static com.rtklib.bean.nav_t.getMaxSat;
import static com.rtklib.pntpos.pntPos.SOLQ_NONE;
import static com.rtklib.pntpos.pntPos.pnt_pos;
import static com.rtklib.pntpos.rescode.R2D;
import static com.rtklib.pntpos.rescode.ecef2pos;
import static com.rtklib.postpos.outSol.covenu;
import static com.rtklib.postpos.outSol.deg2dms;
import static com.rtklib.postpos.outSol.soltocov;
import static com.rtklib.postpos.procPos.MAXERRMSG;
import static com.rtklib.postpos.procPos.pppnx;
import static com.rtklib.readobsnav.readObsNav.read_obsnav;
import static com.rtklib.readobsnav.readrnxh.SYS_CMP;
import static com.rtklib.readobsnav.satSys.sat_sys;
import static com.rtklib.rtkpos.rtkPos.D2R;
import static com.rtklib.rtkpos.rtkPos.PMODE_FIXED;
import static com.rtklib.rtkpos.rtkPos.rel_pos;
import static com.rtklib.rtkpos.udState.NR;
import static com.rtklib.rtkpos.udState.NX;
import static com.rtklib.util.math.zeros;
import static com.rtklib.util.util1.time2epoch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.MainActivity;
import com.example.myapplication.positiondatabase;
import com.example.myapplication.ui.MyService;
import com.example.myapplication.ui.Ntrip.OnRtcmDataReceivedListener;
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
import com.rtklib.bean.ssat_t;
import com.rtklib.bean.sta_t;

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

public class DgpsViewModel extends ViewModel {
    private final MutableLiveData<String> mText;
    private final MutableLiveData<String> rtcm_num;
    private final MutableLiveData<String> latvalue;
    private final MutableLiveData<String> lonvalue;
    private final MutableLiveData<String> height;
    private final MutableLiveData<String> dgpstips;
    private final MutableLiveData<String> updateEAccuracy;
    private final MutableLiveData<String> updateNAccuracy;
    private final MutableLiveData<String> updateUAccuracy;

    private final DecimalFormat df = new DecimalFormat("#.###"); // 保留3位小数
    private final DecimalFormat df1 = new DecimalFormat("#.#####");//保留5位小数
    private StringBuilder msg;
    static rtcm_t rtcm;
    LocationManager mLocationManager;
    private static obsd_t[] obs = new obsd_t[getMaxSat()];
    static nav_t navs = new nav_t();          /* navigation data */
    static obs_t obss = new obs_t();
    gtime_t ts = new gtime_t(0, 0);
    gtime_t te = new gtime_t(0, 0);
    double ti = 0.0;
    int[] index = {0};
    //    prcopt_t prcopt = prcopt_default;
    boolean first_rec = false;
    public static sta_t[] stas = new sta_t[MAXRCV];
    static StringBuilder tips = new StringBuilder();
    static obsd_t[] obssd = new obsd_t[getMaxSat()];
    int n = 0;//流动站数据个数
    int nn = 0;
    int nr = 0;//基准站数据个数
//    int rtcm_gps_obs_n = 0;
    private rtk_t rtk = new rtk_t();
    static double[] stapos = new double[3];
    static boolean support_CarrierPhase = true;
    double[] pos = new double[3];
    double[] dms1 = new double[3];
    double[] dms2 = new double[3];
    private int result;
    BufferedWriter bufferedWriter;
    static BufferedWriter obsWriter;
    static BufferedWriter baseWriter;
    pcv_t[] pcv = new pcv_t[3];
    double[] current_lat = new double[3];
    double[] current_lon = new double[3];
    double elevation = 0;
    SQLiteDatabase db;
    String msg1 = "";


    public DgpsViewModel(Context context) {
        // 在 Activity 或 Fragment 中启动服务之前设置监听器
        MyService myService = new MyService();
        myService.setOnRtcmDataReceivedListener(new OnRtcmDataReceivedListener() {
            @Override
            public void onRtcmDataReceived(rtcm_t rtcmData) {
                // 处理接收到的 RTCM 数据
                rtcm.getObs().data = rtcmData.getObs().data.clone();
            }
        });
        positiondatabase dbHelper = new positiondatabase(context);
        db = dbHelper.getWritableDatabase();
        // 获取当前的UTC时间
        LocalDateTime utcTime = LocalDateTime.now(ZoneOffset.UTC);
        // 创建一个DateTimeFormatter来定义输出格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_M_d_H_mm_ss");
        // 使用formatter来格式化时间并输出
        String formattedTime = utcTime.format(formatter);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mText = new MutableLiveData<>();
        rtcm_num = new MutableLiveData<>();
        latvalue = new MutableLiveData<>();
        lonvalue = new MutableLiveData<>();
        height = new MutableLiveData<>();
        dgpstips = new MutableLiveData<>();
        updateEAccuracy = new MutableLiveData<>();
        updateNAccuracy = new MutableLiveData<>();
        updateUAccuracy = new MutableLiveData<>();
        mText.setValue("0");
        rtcm_num.setValue("0");
        latvalue.setValue(current_lat[0] + "°" + current_lat[1] + "′" + current_lat[2] + "″");
        lonvalue.setValue(current_lon[0] + "°" + current_lon[1] + "′" + current_lon[2] + "″");
        height.setValue(elevation + "m");
        if (first_rec) {
            tips.append("Reference station not activated");
            dgpstips.setValue(tips.toString());
        } else {
            tips.append("Start");
            //读取星历文件
            readdata(outputFilePath);
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
            tips.append("\n").append("Satellite System：");
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
                tips.append("\n").append("Ionosphere Model：").append("off");
            } else if (prcopt.getIonoopt() == 1) {
                tips.append("\n").append("Ionosphere Model：").append("Klobuchar");
            }
            if (prcopt.getTropopt() == 0) {
                tips.append("\n").append("Troposphere Model：").append("off");
            } else if (prcopt.getTropopt() == 1) {
                tips.append("\n").append("Troposphere Model：").append("Saastamoinen");
            }
            tips.append("\n").append("Cut-off Angle：").append(prcopt.getElmin() / D2R).append("°");
            tips.append("\n").append("Frequencies：L1");

            if (open) {
                tips.append("\n").append("Data recording has been enabled, positioning will not be performed.");
                tips.append("\n").append("Recording...");
            }
            dgpstips.setValue(tips.toString());
            prcopt.setSateph(0);
//        prcopt.ionoopt = 1;/* positioning mode: DGPS/DGNSS */
//        prcopt.tropopt = 1;//Saastamoinen model
            prcopt.mode = 1;
            prcopt.refpos = 1;
            prcopt.setNf(1);
            File newFolder = new File(context.getExternalFilesDir(null), "MyGnss");
            if (!newFolder.exists()) {
                newFolder.mkdirs();
            }
            try {
                File out_folder = new File(newFolder, "DGPS" + formattedTime + ".pos");
                FileWriter fileWriter = new FileWriter(out_folder);
                bufferedWriter = new BufferedWriter(fileWriter);
                out_head(bufferedWriter, prcopt, solopt_default);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            rtkinit(rtk, prcopt);
        }
    }

    static void write_log_head_baseandobs() {
        // 获取当前的UTC时间
        LocalDateTime utcTime = LocalDateTime.now(java.time.ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_M_d_H_mm_ss");
        String formattedTime = utcTime.format(formatter);
        try {
            File out_folder_obs = new File(newFolder2, "(user)dgps_gnss_log" + formattedTime + "." + (Year % 100) + "o");
            File out_folder_base = new File(newFolder2, "(base)dgps_gnss_log" + formattedTime + "." + (Year % 100) + "o");
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

    @SuppressLint("DefaultLocale")
    public static void out_loghead_base(BufferedWriter bufferedWriter) throws IOException {
        Date now = new Date();
        //RINEX Version Type
        bufferedWriter.write(String.format("     %3.2f           OBSERVATION DATA    M                   RINEX VERSION / TYPE", 3.03));
        bufferedWriter.newLine();
        bufferedWriter.write("G = GPS  R = GLONASS  E = GALILEO  J = QZSS  C = BDS        COMMENT             ");
        bufferedWriter.newLine();
        bufferedWriter.write("S = SBAS payload  M = Mixed                                 COMMENT             ");
        bufferedWriter.newLine();
        String PGM = String.format("%-20s", "GRitz Logger");
        String RUNBY = String.format("%-20s", Build.MODEL);
        String DATE = String.format("%-20s", now.getTime());
        bufferedWriter.write(PGM + RUNBY + DATE + "UTC PGM / RUN BY / DATE");
        bufferedWriter.newLine();
        bufferedWriter.write("                                                            MARKER NAME         ");
        bufferedWriter.newLine();
        bufferedWriter.write("                                                            MARKER NUMBER       ");
        bufferedWriter.newLine();
        bufferedWriter.write("                                                            OBSERVER / AGENCY   ");
        bufferedWriter.newLine();
        bufferedWriter.write("                                                            REC # / TYPE / VERS ");
        bufferedWriter.newLine();
        bufferedWriter.write("                                                            ANT # / TYPE        ");
        bufferedWriter.newLine();
        String X = String.format("%14.4f", stapos[0]);
        String Y = String.format("%14.4f", stapos[1]);
        String Z = String.format("%14.4f", stapos[2]);
        bufferedWriter.write(X + Y + Z + "                  " + "APPROX POSITION XYZ");
        bufferedWriter.newLine();
        bufferedWriter.write("        0.0000        0.0000        0.0000                  ANTENNA: DELTA H/E/N");
        bufferedWriter.newLine();
        bufferedWriter.write("     0                                                      RCV CLOCK OFFS APPL ");
        bufferedWriter.newLine();
        if (support_CarrierPhase) {
            bufferedWriter.write("G    8 C1C L1C D1C S1C C2P L2P D2P S2P                      SYS / # / OBS TYPES ");
            bufferedWriter.newLine();
        }
        if (support_CarrierPhase) {
            bufferedWriter.write("R    8 C1C L1C D1C S1C C2P L2P D2P S2P                      SYS / # / OBS TYPES ");
            bufferedWriter.newLine();
        }
        if (support_CarrierPhase) {
            bufferedWriter.write("J    6 C1C L1C S1C C5X L5X S5X                              SYS / # / OBS TYPES ");
            bufferedWriter.newLine();
        }
        if (support_CarrierPhase) {
            bufferedWriter.write("E    8 C1X L1X D1X S1X C5X L5X D5X S5X                      SYS / # / OBS TYPES ");
            bufferedWriter.newLine();
        }
        if (support_CarrierPhase) {
            bufferedWriter.write("C    4 C1I L1I D1I S1I                                      SYS / # / OBS TYPES ");
            bufferedWriter.newLine();
        }
        bufferedWriter.write("                                                            END OF HEADER");
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    private static void rtkinit(rtk_t rtk, prcopt_t opt) {
        int i;
        rtk.sol = new sol_t();
        for (i = 0; i < 6; i++) rtk.rb[i] = 0.0;
        rtk.nx = opt.mode <= PMODE_FIXED ? NX(opt) : pppnx(opt);
        rtk.na = opt.mode <= PMODE_FIXED ? NR(opt) : pppnx(opt);
        rtk.tt = 0.0;
        rtk.x = zeros(rtk.nx, 1);
        rtk.P = zeros(rtk.nx, rtk.nx);
        rtk.xa = zeros(rtk.na, 1);
        rtk.Pa = zeros(rtk.na, rtk.na);
        rtk.nfix = rtk.neb = 0;
        for (i = 0; i < getMaxSat(); i++) {
            rtk.ambc[i] = new ambc_t();
            rtk.ssat[i] = new ssat_t();
        }
        for (i = 0; i < MAXERRMSG; i++) rtk.errbuf[i] = 0;
        rtk.opt = opt;
    }

    private void readdata(String[] infile) {
        File brdc = new File(infile[0]);
        File prec = new File(infile[1]);
        File clk = new File(infile[2]);
        //读取广播星历文件
        if (brdc.exists()) {
            try {
                if (read_obsnav(ts, te, ti, infile, index, 1, prcopt, obss, navs, stas) == 0) {
                    tips.append("\n").append("Eph file read successfully.");
                    tips.append("\n").append("Current CN0 Threshold:").append(MainActivity.Cn0DbHz);
                    Log.d("RTD", "Read successfully.");
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
//        if(prec.exists()&&clk.exists()){
//            try {
//                readpreceph(infile,infile.length,prcopt,navs,sbss);
//                if(navs.peph!=null){
//                    tips.append("\n").append("精密星历文件读取成功");
//                    tips.append("\n").append("钟差文件读取成功");
//                    requireActivity().runOnUiThread(() -> mTextView.setText(tips));
//                }else {
//                    tips.append("\n").append("精密星历文件读取失败");
//                    requireActivity().runOnUiThread(() -> mTextView.setText(tips));
//                }
//            } catch (IOException | CloneNotSupportedException e) {
//                throw new RuntimeException(e);
//            }
//        }else {
//            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
//            builder.setTitle("需要更新精密星历文件")
//                    .setCancelable(false)
//                    .setPositiveButton("确定", (dialogInterface, i) -> {
//                        // 点击“是”时执行以下代码
//                        ProgressDialog progressDialog = new ProgressDialog(requireActivity());
//                        progressDialog.setMessage("正在更新精密星历...");
//                        progressDialog.setCancelable(false); // 设置为不可取消
//                        progressDialog.show();
//                        // 在子线程中执行FTP操作
//                        new Thread(() -> {
//                            try {
//                                client = FtpConnection.getFTPClient();
//                                // .gz文件的路径
//                                assert client != null;
//                                if (client.isConnected()){
//                                    downloadFile(requireActivity(), client, remoteDirPath, infile, option);
//                                    client.logout();
//                                    client.disconnect();
//                                    FtpConnection.decompressGzipFile(infile, outputFilePath);
//                                }
//                                // 可以在这里处理FTPClient的其他操作
//                                // 执行完成后关闭加载圈
//                                cn.runOnUiThread(() -> {
//                                    Toast.makeText(requireActivity(), "更新成功", Toast.LENGTH_SHORT).show();
//                                    progressDialog.dismiss();
//                                });
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                                // 如果发生异常也关闭加载圈
//                                cn.runOnUiThread(() -> {
//                                    Toast.makeText(requireActivity(), "更新失败", Toast.LENGTH_SHORT).show();
//                                    progressDialog.dismiss();
//                                });
//                            }
//                        }).start();
//                    });
//            builder.show();
//        }
    }

    static void log_obs_base(obsd_t[] obs, obsd_t[] obsd, int nr) {
        gtime_t firstObsTime = null; // 初始化为 null，表示尚未设置
        int num_sat = 0;
        for (int i = 0; i < getMaxSat(); i++) {
            if (obs[i].sat[0] != 0) {
                num_sat++;
                if (firstObsTime == null) {
                    firstObsTime = obs[i].time; // 更新为第一次有效观测的时间
                }
            }
        }
        if (firstObsTime != null) {
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
                    } else if (obs[i].sat[0] >= 106 && obs[i].sat[0] <= 168) {
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("C%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
                                prn[0], obs[i].P[0], " ", " ", obs[i].L[0], " ", " ", obs[i].D[0], " ", " ", (double) obs[i].SNR[0], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    } else if (obs[i].sat[0] >= 33 && obs[i].sat[0] <= 59) {
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("R%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
                                prn[0], obs[i].P[0], " ", " ", obs[i].L[0], " ", " ", obs[i].D[0], " ", " ", (double) obs[i].SNR[0], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    } else if (obs[i].sat[0] >= 60 && obs[i].sat[0] <= 95) {
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("E%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
                                prn[0], obs[i].P[0], " ", " ", obs[i].L[0], " ", " ", obs[i].D[0], " ", " ", (double) obs[i].SNR[0], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    } else if (obs[i].sat[0] >= 96 && obs[i].sat[0] <= 105) {
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

        if (nr > 0) {
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
                        String obsmeasurements = String.format("G%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%3d%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%3d%s%s",
                                prn[0], obsd[i].P[0], " ", " ", obsd[i].L[0], " ", " ", obsd[i].D[0], " ", " ", obsd[i].SNR[0], " ", " ",
                                obsd[i].P[1], " ", " ", obsd[i].L[1], " ", " ", obsd[i].D[1], " ", " ", obsd[i].SNR[1], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    } else if (obsd[i].sat[0] >= 106 && obsd[i].sat[0] <= 168) {
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("C%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%3d%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%3d%s%s",
                                prn[0], obsd[i].P[0], " ", " ", obsd[i].L[0], " ", " ", obsd[i].D[0], " ", " ", obsd[i].SNR[0], " ", " ",
                                obsd[i].P[1], " ", " ", obsd[i].L[1], " ", " ", obsd[i].D[1], " ", " ", obsd[i].SNR[1], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    } else if (obsd[i].sat[0] >= 33 && obsd[i].sat[0] <= 59) {
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("R%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%3d%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%3d%s%s",
                                prn[0], obsd[i].P[0], " ", " ", obsd[i].L[0], " ", " ", obsd[i].D[0], " ", " ", obsd[i].SNR[0], " ", " ",
                                obsd[i].P[1], " ", " ", obsd[i].L[1], " ", " ", obsd[i].D[1], " ", " ", obsd[i].SNR[1], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    } else if (obsd[i].sat[0] >= 60 && obsd[i].sat[0] <= 95) {
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("E%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%3d%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%3d%s%s",
                                prn[0], obsd[i].P[0], " ", " ", obsd[i].L[0], " ", " ", obsd[i].D[0], " ", " ", obsd[i].SNR[0], " ", " ",
                                obsd[i].P[1], " ", " ", obsd[i].L[1], " ", " ", obsd[i].D[1], " ", " ", obsd[i].SNR[1], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    } else if (obsd[i].sat[0] >= 96 && obsd[i].sat[0] <= 105) {
                        @SuppressLint("DefaultLocale")
                        String obsmeasurements = String.format("J%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%3df%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%3d%s%s",
                                prn[0], obsd[i].P[0], " ", " ", obsd[i].L[0], " ", " ", obsd[i].D[0], " ", " ", obsd[i].SNR[0], " ", " ",
                                obsd[i].P[1], " ", " ", obsd[i].L[1], " ", " ", obsd[i].D[1], " ", " ", obsd[i].SNR[1], " ", " ");
                        sb.append(obsmeasurements).append("\n");
                    }
                }
            }
//            for (int i = 0; i < getMaxSat(); i++) {
//                if (obsd[i].sat[0] != 0) {
//                    int[] prn = new int[1];
//                    sat_sys(obsd[i].sat[0], prn);
//                    if (obsd[i].sat[0] <= 32) {
//                        @SuppressLint("DefaultLocale")
//                        String obsmeasurements = String.format("G%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
//                                prn[0], obsd[i].P[0], " ", " ", obsd[i].L[0], " ", " ", obsd[i].D[0], " ", " ", (double) obsd[i].SNR[0], " ", " ",
//                                obsd[i].P[1], " ", " ", obsd[i].L[1], " ", " ", obsd[i].D[1], " ", " ", (double) obsd[i].SNR[1], " ", " ");
//                        sb.append(obsmeasurements).append("\n");
//                    }else if(obsd[i].sat[0]>=106&&obsd[i].sat[0]<=168){
//                        @SuppressLint("DefaultLocale")
//                        String obsmeasurements = String.format("C%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
//                                prn[0], obsd[i].P[0], " ", " ", obsd[i].L[0], " ", " ", obsd[i].D[0], " ", " ", (double) obsd[i].SNR[0], " ", " ",
//                                obsd[i].P[1], " ", " ", obsd[i].L[1], " ", " ", obsd[i].D[1], " ", " ", (double) obsd[i].SNR[1], " ", " ");
//                        sb.append(obsmeasurements).append("\n");
//                    }else if(obsd[i].sat[0]>=33&&obsd[i].sat[0]<=59){
//                        @SuppressLint("DefaultLocale")
//                        String obsmeasurements = String.format("R%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
//                                prn[0], obsd[i].P[0], " ", " ", obsd[i].L[0], " ", " ", obsd[i].D[0], " ", " ", (double) obsd[i].SNR[0], " ", " ",
//                                obsd[i].P[1], " ", " ", obsd[i].L[1], " ", " ", obsd[i].D[1], " ", " ", (double) obsd[i].SNR[1], " ", " ");
//                        sb.append(obsmeasurements).append("\n");
//                    } else if(obsd[i].sat[0]>=60&&obsd[i].sat[0]<=95){
//                        @SuppressLint("DefaultLocale")
//                        String obsmeasurements = String.format("E%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
//                                prn[0], obsd[i].P[0], " ", " ", obsd[i].L[0], " ", " ", obsd[i].D[0], " ", " ", (double) obsd[i].SNR[0], " ", " ",
//                                obsd[i].P[1], " ", " ", obsd[i].L[1], " ", " ", obsd[i].D[1], " ", " ", (double) obsd[i].SNR[1], " ", " ");
//                        sb.append(obsmeasurements).append("\n");
//                    }else if(obsd[i].sat[0]>=96&&obsd[i].sat[0]<=105){
//                        @SuppressLint("DefaultLocale")
//                        String obsmeasurements = String.format("J%02d%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s%14.3f%s%s",
//                                prn[0], obsd[i].P[0], " ", " ", obsd[i].L[0], " ", " ", obsd[i].D[0], " ", " ", (double) obsd[i].SNR[0], " ", " ",
//                                obsd[i].P[1], " ", " ", obsd[i].L[1], " ", " ", obsd[i].D[1], " ", " ", (double) obsd[i].SNR[1], " ", " ");
//                        sb.append(obsmeasurements).append("\n");
//                    }
//                }
//            }
            resetobs(obsd);
            try {
                baseWriter.write(sb.toString());
                baseWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void processGnssMeasurements(GnssMeasurementsEvent eventArgs) {
        msg = new StringBuilder("");
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
        nr = (int) Arrays.stream(obssd).filter(obsdT -> (obsdT.sat[0] != 0)).count();
        //UI更新RTCM中的卫星数量
        rtcm_num.postValue(String.valueOf(nr));
        for (int i = 0; i < nr; i++) {
            if (obssd[0].time.time != obssd[i].time.time) {
                tips.append("\n").append(currentTimeString).append(":").append("Reference station:").append("The").append(i + 1).append("th one has an anomaly：").append("Satellite:").append(obssd[i].sat[0]).append("error occurred");
                dgpstips.postValue(tips.toString());
                obssd[i].sat[0] = 0;
            }
        }
        if (open) {
            log_obs_base(obs, obssd, nr);
        } else if (isSpp(obs, navs) >= 4 && nr != 0) {
            mText.postValue(String.valueOf(isSpp(obs, navs)));
            nn = 0;
//            rtcm_gps_obs_n = 0;
            try {
                //计算流动站数据个数
                n = (int) Arrays.stream(obs).filter(obsdT -> (obsdT.sat[0] != 0)).count();
                obsd_t[] temp = sortobs(obs);
                //判断流动站的数据来自同一个历元
                for (int i = 0; i < n; i++) {
                    if (temp[0].time.time == temp[i].time.time) {
                        nn++;
                    } else {
                        double diff = temp[0].time.time - temp[i].time.time;
                        tips.append("\n").append(currentTimeString).append(":").append("Rover:").append("The").append(i + 1).append("th one has an anomaly:").append("Satellite:").append(temp[i].sat[0]).append("error occurred").append("\n").append("The time difference is:").append(diff);
                        dgpstips.postValue(tips.toString());
                        return;
                    }
                }
                if (nn == n || n >= 4) {
                    obsd_t[] tempobssd = new obsd_t[nr];
                    for (int i = 0; i < nr; i++) {
                        tempobssd[i] = new obsd_t();
                    }
                    int ii = 0;
                    for (obsd_t obsdT : obssd) {
                        if (obsdT.sat[0] != 0) {
                            tempobssd[ii] = (obsd_t) obsdT.clone();
                            tempobssd[ii].rcv = 2;
                            ii++;
                        }
                    }
                    for (int iu = 0; iu < rtcm.getObs().data.length; iu++) {
                        rtcm.getObs().data[iu].rcv = 2;
                    }
                    //将两个站的数据赋值道dgps_obs当中，用于计算
                    obsd_t[] dgps_obs = new obsd_t[n + nr];
                    for (int i = 0; i < n + nr; i++) {
                        dgps_obs[i] = new obsd_t();
                    }
                    for (int i = 0; i < n + nr; i++) {
                        if (i < n)
                            dgps_obs[i] = (obsd_t) temp[i].clone();
                        else
                            dgps_obs[i] = (obsd_t) tempobssd[i - n].clone();
                    }
                    prcopt.setRb(stapos);
                    rtk.opt = prcopt;
                    rtk.rb = stapos;
                    int state = pnt_pos(temp, n, navs, rtk.opt, rtk.sol, null, rtk.ssat, msg1);
                    if (state != 0) {
                        rtk.sol.stat = SOLQ_NONE;
                        //tips.append("\n").append(currentTimeString).append(":").append("Single-point positioning successful, proceeding to differential positioning...");
                        dgpstips.postValue(tips.toString());
                        //计算差分龄期，大于某个阈值不予计算
                        double diff = local_timediff(dgps_obs[0].time, dgps_obs[nr].time);
                        rtk.sol.age = (float) diff;
                        prcopt.setMaxtdiff(1);
                        if (Math.abs(diff) >= 2.1) {
                            tips.append("\n").append(currentTimeString).append(":").append("age of differential error:").append(diff);
                            dgpstips.postValue(tips.toString());
                            resetobs(dgps_obs);
                        } else {
                            //这里将配置结构体opt内基准站的坐标赋值给解算结构体rtk内基准站的坐标
                            result = rel_pos(rtk, dgps_obs, n, nr, navs);
                            if (result >= 1) {
                                ecef2pos(rtk.sol.rr, pos);
                                deg2dms(pos[0] * R2D, dms1, 5);
                                deg2dms(pos[1] * R2D, dms2, 5);

                                current_lat = dms1;
                                current_lon = dms2;

                                String strdms11 = dmsArrayToString(current_lat);
                                String strdms22 = dmsArrayToString(current_lon);


                                String formattedNumber_lat = df.format(current_lat[2]);
                                String formattedNumber_lon = df.format(current_lon[2]);

                                latvalue.postValue(current_lat[0] + "°" + current_lat[1] + "′" + formattedNumber_lat + "″");
                                lonvalue.postValue(current_lon[0] + "°" + current_lon[1] + "′" + formattedNumber_lon + "″");
                                String height1 = df.format(pos[2]);
                                height.postValue(height1);
                                float[] temp_qr = new float[rtk.sol.qr.length];
                                // 遍历原始数组，计算每个元素的开根号
                                for (int i = 0; i < rtk.sol.qr.length; i++) {
                                    temp_qr[i] = (float) Math.sqrt(Math.abs(rtk.sol.qr[i]));
                                }
                                String strqr = Arrays.toString(temp_qr);
                                // 调用插入函数
                                insertData(db, currentTimeString, strdms11, strdms22, height1, strqr, String.valueOf(2));
                                StringBuilder sb = new StringBuilder();
                                solopt_default.timef = 0;
                                double[] P = new double[9];
                                double[] Q = new double[9];
                                soltocov(rtk.sol,P);
                                covenu(pos,P,Q);
                                updateEAccuracy.postValue(df1.format(Math.sqrt(Math.abs(Q[0])))+"m");
                                updateNAccuracy.postValue(df1.format(Math.sqrt(Math.abs(Q[4])))+"m");
                                updateUAccuracy.postValue(df1.format(Math.sqrt(Math.abs(Q[8])))+"m");
                                out_sol(rtk.sol, solopt_default, sb);
                                try {
                                    bufferedWriter.write(sb.toString());
                                    bufferedWriter.flush();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                tips.append("\n").append(currentTimeString).append(":").append("Differential positioning：").append("Positioning successful.");
                                dgpstips.postValue(tips.toString());
                            } else {
                                updateEAccuracy.postValue(0+"m");
                                updateNAccuracy.postValue(0+"m");
                                updateUAccuracy.postValue(0+"m");
                                resetobs(dgps_obs);
                                tips.append("\n").append(currentTimeString).append(":")
                                        .append("Differential positioning failed:")
                                        .append(msg.toString());
                                dgpstips.postValue(tips.toString());
                            }
                        }
                    } else {
                        updateEAccuracy.postValue(0+"m");
                        updateNAccuracy.postValue(0+"m");
                        updateUAccuracy.postValue(0+"m");
                        tips.append("\n").append(currentTimeString)
                                .append(":").append("Single-point solution failed:")
                                .append(msg.toString());
                        dgpstips.postValue(tips.toString());
                    }
                } else {
                    updateEAccuracy.postValue(0+"m");
                    updateNAccuracy.postValue(0+"m");
                    updateUAccuracy.postValue(0+"m");
                    tips.append("\n").append(currentTimeString)
                            .append(":").append("Rover data error.");
                    dgpstips.postValue(tips.toString());
                }
            } catch (CloneNotSupportedException | NullPointerException e) {
                throw new RuntimeException(e);
            }
        } else {
            mText.postValue(String.valueOf(isSpp(obs, navs)));
        }
    }

    // 将 DMS 数组转换为字符串
    private String dmsArrayToString(double[] dms) {
        return Arrays.stream(dms)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    public static double local_timediff(gtime_t t1, gtime_t t2) {
        double diffInSeconds = t1.getTime() - t2.getTime();
        double diffInFractionalSeconds = t1.getSec() - t2.getSec();
        return diffInSeconds + diffInFractionalSeconds;
    }

    void closeWriter() {
        if (obsWriter != null) {
            try {
                obsWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            obsWriter = null;
        }
        if (baseWriter != null) {
            try {
                baseWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            baseWriter = null;
        }
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
        return dgpstips;
    }
    public MutableLiveData<String> getUpdateEAccuracy() {
        return updateEAccuracy;
    }
    public MutableLiveData<String> getUpdateNAccuracy() {
        return updateNAccuracy;
    }
    public MutableLiveData<String> getUpdateUAccuracy() {
        return updateUAccuracy;
    }

}