package com.example.myapplication.ui;

import static android.location.GnssMeasurement.ADR_STATE_VALID;
import static android.location.GnssStatus.CONSTELLATION_GALILEO;
import static android.location.GnssStatus.CONSTELLATION_GLONASS;
import static android.location.GnssStatus.CONSTELLATION_GPS;

import static com.example.myapplication.MainActivity.Cn0DbHz;
import static com.rtklib.bean.nav_t.getMaxSat;
import static com.rtklib.bean.prcopt_t.MAXANT;
import static com.rtklib.pntpos.pntPos.PMODE_SINGLE;
import static com.rtklib.pntpos.rescode.R2D;
import static com.rtklib.pntpos.rescode.ecef2enu;
import static com.rtklib.pntpos.rescode.ecef2pos;
import static com.rtklib.postpos.outSol.SOLF_LLH;
import static com.rtklib.postpos.outSol.covenu;
import static com.rtklib.postpos.outSol.deg2dms;
import static com.rtklib.postpos.outSol.geoidh;
import static com.rtklib.postpos.outSol.opt2sep;
import static com.rtklib.postpos.outSol.soltocov;
import static com.rtklib.postpos.outSol.soltocov_vel;
import static com.rtklib.postpos.outSol.sqvar;
import static com.rtklib.readobsnav.readrnxh.SYS_CMP;
import static com.rtklib.readobsnav.readrnxh.SYS_GAL;
import static com.rtklib.readobsnav.readrnxh.SYS_GLO;
import static com.rtklib.readobsnav.readrnxh.SYS_GPS;
import static com.rtklib.rtkpos.rtkPos.D2R;
import static com.rtklib.util.util1.gpst2utc;
import static com.rtklib.util.util1.sprintf;
import static com.rtklib.util.util1.time2gpst;
import static com.rtklib.util.util1.time2str;

import static java.lang.Math.floor;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;

import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssStatus;
import android.util.Log;

import com.rtklib.bean.gtime_t;
import com.rtklib.bean.nav_t;
import com.rtklib.bean.obsd_t;
import com.rtklib.bean.pcv_t;
import com.rtklib.bean.prcopt_t;
import com.rtklib.bean.snrmask_t;
import com.rtklib.bean.sol_t;
import com.rtklib.bean.solopt_t;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.function.IntFunction;

public class measure_util {
    private static final double c = 299792458;//光速 单位：m/s;
    public static double gps_l1_lamda = c/1.575420032e9;
    public static double gps_l5_lamda = c/1.17645005e9;
    public static double bds_b1_lamda = c/1.561097984E9;
    static pcv_t[] pcv = new pcv_t[2];
    public static boolean[] first_epoch = new boolean[getMaxSat()];
    public static long[] first_fullbiasnanos = new long[getMaxSat()];
    public static double[] first_biasnanos = new double[getMaxSat()];
    public static double[] first_p = new double[getMaxSat()];
    public static boolean[] first_l_epoch = new boolean[getMaxSat()];
    public static double[] first_l = new double[getMaxSat()];
    public static boolean[] first = new boolean[getMaxSat()];


    public static final prcopt_t prcopt_default = new prcopt_t(
            PMODE_SINGLE, 0, 1, SYS_GPS,   // 模式，解算类型，卫星系统数，导航系统
            15.0 * D2R, new snrmask_t(),   // 最小高度角，信噪比掩码
            0, 1, 1, 1,                  // 卫星星历，AR模式，GLO AR模式，BDS AR模式
            5, 0, 10, 1,                 // 最大输出数，最小锁定数，最小固定数，ARM中的最大迭代次数
            0, 0, 0, 0,                  // 是否估计IONOSPHERE，是否估计对流层延迟，动力学模型，潮汐效应校正
            1, 0, 0, 0, 0,               // GNSS解算迭代次数，码偏差平滑，交叉天线功率比，S-BAS校正，S-BAS卫星选择
            0, 1,                        // 测站坐标，参考站坐标
            new double[]{100.0, 100.0},  // 误差比例因子
            new double[]{100.0, 0.003, 0.003, 0.0, 1.0},  // 误差
            new double[]{30.0, 0.03, 0.3},  // 测量值标准差
            new double[]{1E-4, 1E-3, 1E-4, 1E-1, 1E-2, 0.0},  // 观测值权重
            5E-12,                      // 卫星钟差稳定性
            new double[]{3.0, 0.9999, 0.25, 0.1, 0.05},  // 需要满足的条件
            0.0, 0.0, 0.05,             // AR模式下的高度角，AR模式下的历元保持时间，滑动窗口阈值
            30.0, 30.0, 30.0,           // 最大时间差，最大无电离层组合数，最大GDOP
            new double[]{0, 0}, new double[]{0, 0, 0}, new double[]{0, 0, 0},  // 基线长度，测站位置，参考站位置
            new char[2][MAXANT],       // 天线类型
            new double[2][3], pcv, new byte[getMaxSat()] // 天线相位中心偏差，天线PCV，排除卫星
    );
    public static final solopt_t solopt_default = new solopt_t(/* defaults solution output options */
            SOLF_LLH, 1, 1, 3,    /* posf,times,timef,timeu */
            1, 1, 0, 0, 0, 0, 0,              /* degf,outhead,outopt,outvel,datum,height,geoid */
            0, 0, 0,                      /* solstatic,sstat,trace */
            new double[]{0.0, 0.0},                  /* nmeaintv */
            " ".toCharArray(), "".toCharArray()                      /* separator/program name */
    );
    private measure_util(){
    }
    public static void resetobs(obsd_t[] obs) {
        for (obsd_t ob : obs) {
            ob.sat[0] = 0;
        }
    }
    public static void out_sol_record(String name, sol_t sol, solopt_t opt, StringBuilder sb) {
        gtime_t time;
        double gpst;
        char[] s = new char[64];
        int timeu;
        int[] week = new int[1];
        double[] pos = new double[3];
        double[] vel = new double[3];
        double[] dms1 = new double[3];
        double[] dms2 = new double[3];
        double[] P = new double[9];
        double[] Q = new double[9];
        char[] sep = opt2sep(opt);
        ecef2pos(sol.rr, pos);
        soltocov(sol, P);
        covenu(pos, P, Q);

        time = sol.time;
        timeu = opt.timeu < 0 ? 0 : (min(opt.timeu, 20));
//        time=gpst2utc(time);
        if (opt.timef != 0) time2str(time, s, timeu);
        else {
            gpst = time2gpst(time, week);
            if (86400 * 7 - gpst < 0.5 / pow(10.0, timeu)) {
                week[0]++;
                gpst = 0.0;
            }
            sprintf(s, "%4d%.16s%10.3f", week[0], sep[0], gpst);
        }

        if (opt.height == 1) { // geodetic height
            //获取指定地理位置的大地高（geoid height）。它根据输入的经纬度坐标（以弧度表示），通过引用地球模型来计算大地高。
            pos[2] -= geoidh(pos);
        }
        if (opt.degf != 0) {
            String temps = String.valueOf(s).trim();
            deg2dms(pos[0] * R2D, dms1, 5);
            deg2dms(pos[1] * R2D, dms2, 5);
            sb.append(temps).append(sep)
                    .append(String.format(Locale.US, "%4s", name)).append(sep)
                    .append(String.format(Locale.US, "%4.0f", dms1[0])).append(sep)
                    .append(String.format(Locale.US, "%02.0f", dms1[1])).append(sep)
                    .append(String.format(Locale.US, "%08.5f", dms1[2])).append(sep)
                    .append(String.format(Locale.US, "%4.0f", dms2[0])).append(sep)
                    .append(String.format(Locale.US, "%02.0f", dms2[1])).append(sep)
                    .append(String.format(Locale.US, "%08.5f", dms2[2]));
        } else {
            sb.append(sep).append(String.format(Locale.US, "%14.9f", pos[0] * R2D)).append(sep)
                    .append(String.format(Locale.US, "%14.9f", pos[1] * R2D));
        }
        sb.append(sep).append(String.format(Locale.US, "%10.4f", pos[2])).append(sep)
                .append(sol.stat).append(sep).append(sol.ns).append(sep)
                .append(String.format(Locale.US, "%8.4f", sqrt(Q[4]))).append(sep)
                .append(String.format(Locale.US, "%8.4f", sqrt(Q[0]))).append(sep)
                .append(String.format(Locale.US, "%8.4f", sqrt(Q[8]))).append(sep)
                .append(String.format(Locale.US, "%8.4f", sqvar(Q[1]))).append(sep)
                .append(String.format(Locale.US, "%8.4f", sqvar(Q[2]))).append(sep)
                .append(String.format(Locale.US, "%8.4f", sqvar(Q[5]))).append(sep)
                .append(String.format(Locale.US, "%6.2f", sol.age)).append(sep)
                .append(String.format(Locale.US, "%6.1f", sol.ratio));
        if (opt.outvel != 0) { // output velocity
            soltocov_vel(sol, P);
            ecef2enu(pos, Arrays.copyOfRange(sol.rr, 3, 6), vel);
            covenu(pos, P, Q);
            sb.append(sep).append(String.format(Locale.US, "%10.5f", vel[1])).append(sep)
                    .append(String.format(Locale.US, "%10.5f", vel[0])).append(sep)
                    .append(String.format(Locale.US, "%10.5f", vel[2])).append(sep)
                    .append(String.format(Locale.US, "%9.5f", sqrt(Q[4]))).append(sep)
                    .append(String.format(Locale.US, "%8.5f", sqrt(Q[0]))).append(sep)
                    .append(String.format(Locale.US, "%8.5f", sqrt(Q[8]))).append(sep)
                    .append(String.format(Locale.US, "%8.5f", sqvar(Q[1]))).append(sep)
                    .append(String.format(Locale.US, "%8.5f", sqvar(Q[2]))).append(sep)
                    .append(String.format(Locale.US, "%8.5f", sqvar(Q[5])));
        }
        sb.append("\r\n");
    }
    public static void out_sol(sol_t sol, solopt_t opt, StringBuilder sb) {
        gtime_t time;
        double gpst;
        char[] s = new char[64];
        int timeu;
        int[] week = new int[1];
        double[] pos = new double[3];
        double[] vel = new double[3];
        double[] dms1 = new double[3];
        double[] dms2 = new double[3];
        double[] P = new double[9];
        double[] Q = new double[9];
        char[] sep = opt2sep(opt);
        ecef2pos(sol.rr, pos);
        soltocov(sol, P);
        covenu(pos, P, Q);

        time = sol.time;
        timeu = opt.timeu < 0 ? 0 : (min(opt.timeu, 20));
//        time=gpst2utc(time);
        if (opt.timef != 0) time2str(time, s, timeu);
        else {
            gpst = time2gpst(time, week);
            if (86400 * 7 - gpst < 0.5 / pow(10.0, timeu)) {
                week[0]++;
                gpst = 0.0;
            }
            sprintf(s, "%4d%.16s%10.3f", week[0], sep[0], gpst);
        }

        if (opt.height == 1) { // geodetic height
            //获取指定地理位置的大地高（geoid height）。它根据输入的经纬度坐标（以弧度表示），通过引用地球模型来计算大地高。
            pos[2] -= geoidh(pos);
        }
        if (opt.degf != 0) {
            String temps = String.valueOf(s).trim();
            deg2dms(pos[0] * R2D, dms1, 5);
            deg2dms(pos[1] * R2D, dms2, 5);
            sb.append(temps).append(sep)
                    .append(String.format(Locale.US, "%4.0f", dms1[0])).append(sep)
                    .append(String.format(Locale.US, "%02.0f", dms1[1])).append(sep)
                    .append(String.format(Locale.US, "%08.5f", dms1[2])).append(sep)
                    .append(String.format(Locale.US, "%4.0f", dms2[0])).append(sep)
                    .append(String.format(Locale.US, "%02.0f", dms2[1])).append(sep)
                    .append(String.format(Locale.US, "%08.5f", dms2[2]));
        } else {
            sb.append(sep).append(String.format(Locale.US, "%14.9f", pos[0] * R2D)).append(sep)
                    .append(String.format(Locale.US, "%14.9f", pos[1] * R2D));
        }
        sb.append(sep).append(String.format(Locale.US, "%10.4f", pos[2])).append(sep)
                .append(sol.stat).append(sep).append(sol.ns).append(sep)
                .append(String.format(Locale.US, "%8.4f", sqrt(Q[4]))).append(sep)
                .append(String.format(Locale.US, "%8.4f", sqrt(Q[0]))).append(sep)
                .append(String.format(Locale.US, "%8.4f", sqrt(Q[8]))).append(sep)
                .append(String.format(Locale.US, "%8.4f", sqvar(Q[1]))).append(sep)
                .append(String.format(Locale.US, "%8.4f", sqvar(Q[2]))).append(sep)
                .append(String.format(Locale.US, "%8.4f", sqvar(Q[5]))).append(sep)
                .append(String.format(Locale.US, "%6.2f", sol.age)).append(sep)
                .append(String.format(Locale.US, "%6.1f", sol.ratio));
        if (opt.outvel != 0) { // output velocity
            soltocov_vel(sol, P);
            ecef2enu(pos, Arrays.copyOfRange(sol.rr, 3, 6), vel);
            covenu(pos, P, Q);
            sb.append(sep).append(String.format(Locale.US, "%10.5f", vel[1])).append(sep)
                    .append(String.format(Locale.US, "%10.5f", vel[0])).append(sep)
                    .append(String.format(Locale.US, "%10.5f", vel[2])).append(sep)
                    .append(String.format(Locale.US, "%9.5f", sqrt(Q[4]))).append(sep)
                    .append(String.format(Locale.US, "%8.5f", sqrt(Q[0]))).append(sep)
                    .append(String.format(Locale.US, "%8.5f", sqrt(Q[8]))).append(sep)
                    .append(String.format(Locale.US, "%8.5f", sqvar(Q[1]))).append(sep)
                    .append(String.format(Locale.US, "%8.5f", sqvar(Q[2]))).append(sep)
                    .append(String.format(Locale.US, "%8.5f", sqvar(Q[5])));
        }
        sb.append("\r\n");
    }
    public static obsd_t[] sortobs(obsd_t[] obs) throws CloneNotSupportedException {
        // 使用流过滤和收集有效的 obsd_t 对象
        return Arrays.stream(obs)
                .filter(o -> o.sat[0] != 0) // 过滤掉 sat[0] 为 0 的对象
                .toArray(obsd_t[]::new);
    }
    //
    public static int isSpp(obsd_t[] obs, nav_t navs) {
        int k = 0;
        for (obsd_t ob : obs) {
            for (int b = 0; b < navs.n; b++) {
                if (ob != null && navs.eph[b] != null) {
                    int obsSat = ob.sat[0];
                    int ephSat = navs.eph[b].getSat();
                    if (obsSat != 0 && ephSat != 0) {
                        if (obsSat == ephSat) {
                            k++;
                            break;
                        }
                    }
                }
            }
        }

        if (navs.geph != null) {
            for (obsd_t ob : obs) {
                for (int b = 0; b < navs.ng; b++) {
                    if (ob != null && navs.geph[b] != null) {
                        int obsSat = ob.sat[0];
                        int gephSat = navs.geph[b].getSat();
                        if (obsSat != 0 && gephSat != 0) {
                            if (obsSat == gephSat) {
                                k++;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return k;
    }
    public static int calculateSelectedValue(boolean[] checked) {
        int value = 0;
        if (checked[0]) value |= SYS_GPS; // GPS
        if (checked[1]) value |= SYS_CMP; // BDS
        if (checked[2]) value |= SYS_GLO; // GLONASS
        if (checked[3]) value |= SYS_GAL; // Galileo
        return value;
    }
    //计算GPS时
    public static double getgpst(GnssClock gnssClock){
        double GPST;
        GPST = gnssClock.getTimeNanos()-(gnssClock.getFullBiasNanos()+gnssClock.getBiasNanos());
        return GPST;
    }
    //计算tRx_gnss
    public static double getTRxGnssNanos(GnssClock gnssClock, GnssMeasurement measure)
    {
        double TRxGnssNanos;
        double bias=0;
        if (gnssClock.hasBiasNanos())
            bias+=gnssClock.getBiasNanos();
        if (gnssClock.hasFullBiasNanos())
            bias+=gnssClock.getFullBiasNanos();
        TRxGnssNanos =gnssClock.getTimeNanos()+measure.getTimeOffsetNanos()-bias;
        return TRxGnssNanos;
    }
    /**
     * 计算伪距（Pseudorange），即从GNSS接收机到卫星的距离估计。
     * 该方法考虑了不同卫星系统的时钟偏差和闰秒。
     *
     * @param measurement 表示接收机测量到的GNSS信号数据，包括接收到的时间和卫星类型等信息。
     * @param gnssClock 表示GNSS接收机的时钟信息，包括时钟偏差和闰秒等。
     * @return 返回计算得到的伪距值，单位为米。如果计算结果不合理（小于1,000,000米或大于100,000,000米），则返回-1。
     */
    public static double getPseudorange(GnssMeasurement measurement,GnssClock gnssClock)
    {
        double Pseudorange;
        double tTx=measurement.getReceivedSvTimeNanos();
        double TRxGnss = getTRxGnssNanos(gnssClock,measurement);
        double numberNanoSecondWeek = 604800 * 1e9;
        double weekNumberNanos = floor(-gnssClock.getFullBiasNanos()/ numberNanoSecondWeek)* numberNanoSecondWeek;
        double numberNanoSecondDay = 86400 * 1e9;
        double dayNumberNanos = floor(-gnssClock.getFullBiasNanos()/ numberNanoSecondDay)* numberNanoSecondDay;
        int SvType=measurement.getConstellationType();//获取当前卫星类型
        double tRx;
        switch(SvType){
            case CONSTELLATION_GPS:
            case CONSTELLATION_GALILEO:
//                tRx = TRxGnss - weekNumberNanos;
                tRx = TRxGnss % numberNanoSecondWeek;
                break;
            case GnssStatus.CONSTELLATION_BEIDOU:
                tRx = (TRxGnss % numberNanoSecondWeek) - 14*1e9;
                break;
            case CONSTELLATION_GLONASS:
                if(gnssClock.getLeapSecond()!=0)
                    tRx = (TRxGnss % numberNanoSecondDay) + 10800*1e9 - gnssClock.getLeapSecond()*1e9;
                else tRx = (TRxGnss % numberNanoSecondDay) + 10800*1e9 - 18*1e9;
                break;
            default:
                return -1;//未定义的卫星类型
        }
        double prSeconds = (tRx - tTx)*1e-9;
        Pseudorange = (prSeconds)*c;
        if (Pseudorange<1e8 && Pseudorange>1e6)return Pseudorange;//单位：米
        else return -1;
    }
    /**
     * 读取GNSS测量数据并更新观测数据数组。
     * 该方法根据不同的卫星系统和载波频率，计算并记录伪距、载波相位、信噪比等信息。
     *
     * @param measurement 表示接收机测量到的GNSS信号数据，包括接收到的时间、卫星类型、载波频率等信息。
     * @param gnssClock 表示GNSS接收机的时钟信息，包括时钟偏差和闰秒等。
     * @param obs 表示观测数据的数组，将根据测量数据更新相应的观测信息。
     */
    public static void readobs(GnssMeasurement measurement, GnssClock gnssClock, obsd_t[] obs) {
        // 获取卫星类型
        int constellationType = measurement.getConstellationType();
        double carrier = measurement.getCarrierFrequencyHz();
        //获取卫星ID
        int Svid = measurement.getSvid();
        //如果是GPS
        if (constellationType == 1) {
            //如果是L1
            if (carrier == 1.575420032e9 && getPseudorange(measurement, gnssClock) != -1
                    && measurement.getCn0DbHz() > Cn0DbHz) {
                obs[Svid - 1].D[0] = (float) (-measurement.getPseudorangeRateMetersPerSecond() / gps_l1_lamda);
                obs[Svid - 1].sat[0] = Svid;
                obs[Svid - 1].P[0] = getPseudorange(measurement, gnssClock);//记录伪距
                if((measurement.getAccumulatedDeltaRangeState() & ADR_STATE_VALID)== ADR_STATE_VALID)
                {//记录载波
                    //改正后
                    if(!first_epoch[Svid - 1]&&obs[Svid - 1].P[0]!=-1)
                    {
                        first_p[Svid-1] = floor(obs[Svid - 1].P[0]);
                        first_fullbiasnanos[Svid - 1] = gnssClock.getFullBiasNanos();
                        first_biasnanos[Svid - 1] = gnssClock.getBiasNanos();
                        first_epoch[Svid - 1]=true;
                    }else
                    {
                        if(!first_l_epoch[Svid-1]){
                            first_l[Svid-1] = measurement.getAccumulatedDeltaRangeMeters()/gps_l1_lamda
                                    -(c/gps_l1_lamda)*(gnssClock.getFullBiasNanos()-first_fullbiasnanos[Svid - 1])*1e-9
                                    -(c/gps_l1_lamda)*(gnssClock.getBiasNanos()-first_biasnanos[Svid - 1])*1e-9;
                            first_l_epoch[Svid-1] = true;
//                        obs[Svid - 1].L[0] = measurement.getAccumulatedDeltaRangeMeters()/gps_l1_lamda
//                                -(c/gps_l1_lamda)*(gnssClock.getFullBiasNanos()-first_fullbiasnanos[Svid - 1])*1e-9
//                                -(c/gps_l1_lamda)*(gnssClock.getBiasNanos()-first_biasnanos[Svid - 1])*1e-9;
                        }else{
                            if(!first[Svid-1]){
                            obs[Svid - 1].L[0] = first_p[Svid-1]/gps_l1_lamda+measurement.getAccumulatedDeltaRangeMeters()/gps_l1_lamda
                                    -(c/gps_l1_lamda)*(gnssClock.getFullBiasNanos()-first_fullbiasnanos[Svid - 1])*1e-9
                                    -(c/gps_l1_lamda)*(gnssClock.getBiasNanos()-first_biasnanos[Svid - 1])*1e-9 - first_l[Svid-1];
                                first[Svid-1] = true;
                            }else{
                                obs[Svid - 1].L[0] = obs[Svid - 1].L[0] + measurement.getAccumulatedDeltaRangeMeters()/gps_l1_lamda
                                        -(c/gps_l1_lamda)*(gnssClock.getFullBiasNanos()-first_fullbiasnanos[Svid - 1])*1e-9
                                        -(c/gps_l1_lamda)*(gnssClock.getBiasNanos()-first_biasnanos[Svid - 1])*1e-9 - first_l[Svid-1];
                            }
                            first_l[Svid-1] = measurement.getAccumulatedDeltaRangeMeters()/gps_l1_lamda
                                    -(c/gps_l1_lamda)*(gnssClock.getFullBiasNanos()-first_fullbiasnanos[Svid - 1])*1e-9
                                    -(c/gps_l1_lamda)*(gnssClock.getBiasNanos()-first_biasnanos[Svid - 1])*1e-9;
                        }
                    }
                    //改正前
//                    obs[Svid - 1].L[0] = measurement.getAccumulatedDeltaRangeMeters()/gps_l1_lamda;
                }else{
                    obs[Svid - 1].L[0] = 0;
                }
                obs[Svid - 1].SNR[0] = (int) measurement.getCn0DbHz();
                gtime_t[] ttRx = new gtime_t[1];
                ttRx[0] = new gtime_t(0, 0);
                ttRx[0].time = getgpst(gnssClock) + 315964800 * 1e9;
                ttRx[0].time = ttRx[0].time/1e9;
                ttRx[0].sec = ttRx[0].time - floor(ttRx[0].time);
                ttRx[0].time = floor(ttRx[0].time);
                obs[Svid - 1].time = ttRx[0];
                obs[Svid - 1].rcv = 1;
                obs[Svid - 1].code[0] = 1;
            }
            else if (carrier == 1.17645005e9 && getPseudorange(measurement, gnssClock) != -1
                    && measurement.getCn0DbHz() > Cn0DbHz) {//如果是L5
                obs[Svid - 1].sat[0] = Svid;
                obs[Svid - 1].P[2] = getPseudorange(measurement, gnssClock);//记录伪距
                if((measurement.getAccumulatedDeltaRangeState() & ADR_STATE_VALID)== ADR_STATE_VALID)
                    obs[Svid - 1].L[2] = (measurement.getAccumulatedDeltaRangeMeters())/gps_l5_lamda;
                obs[Svid - 1].SNR[2] = (int) measurement.getCn0DbHz();
                obs[Svid - 1].D[0] = (float) (-measurement.getPseudorangeRateMetersPerSecond() / gps_l5_lamda);
                gtime_t[] ttRx = new gtime_t[1];
                ttRx[0] = new gtime_t(0, 0);
                ttRx[0].time = getgpst(gnssClock) + 315964800 * 1e9;
                ttRx[0].time = ttRx[0].time/1e9;
                ttRx[0].sec = ttRx[0].time - floor(ttRx[0].time);
                ttRx[0].time = floor(ttRx[0].time);
                obs[Svid - 1].time = ttRx[0];
                obs[Svid - 1].rcv = 1;
                obs[Svid - 1].code[2] = 25;
            }
        }
        if(constellationType == 5) {
            if (carrier == 1.561097984E9&&measurement.getCn0DbHz() > Cn0DbHz) {
                //如果是BDS
                int cor_Svid = Svid + 105;
                obs[cor_Svid - 1].sat[0] = cor_Svid;
                obs[cor_Svid - 1].P[0] = getPseudorange(measurement, gnssClock);//记录伪距
                if((measurement.getAccumulatedDeltaRangeState() & ADR_STATE_VALID) == ADR_STATE_VALID){
                    if(!first_epoch[Svid - 1]){
                        first_fullbiasnanos[Svid - 1] = gnssClock.getFullBiasNanos();
                        first_biasnanos[Svid - 1] = gnssClock.getBiasNanos();
                        first_epoch[Svid - 1]=true;
                    }else{
                        obs[Svid - 1].L[0] = measurement.getAccumulatedDeltaRangeMeters()/bds_b1_lamda
                                -(c/bds_b1_lamda)*(gnssClock.getFullBiasNanos()-first_fullbiasnanos[Svid - 1])*1e-9
                                -(c/bds_b1_lamda)*(gnssClock.getBiasNanos()-first_biasnanos[Svid - 1])*1e-9;
                    }
//                    obs[cor_Svid - 1].L[0] = (measurement.getAccumulatedDeltaRangeMeters())/bds_b1_lamda;
                }
                obs[cor_Svid - 1].D[0] = (float) (-measurement.getPseudorangeRateMetersPerSecond() * measurement.getCarrierFrequencyHz() / c);
                obs[cor_Svid - 1].SNR[0] = (int) measurement.getCn0DbHz();
                gtime_t[] ttRx = new gtime_t[1];
                ttRx[0] = new gtime_t(0, 0);
                ttRx[0].time = getgpst(gnssClock) + 315964800 * 1e9;
                ttRx[0].time = ttRx[0].time / 1e9;
                ttRx[0].sec = ttRx[0].time - floor(ttRx[0].time);
                ttRx[0].time = floor(ttRx[0].time);
                obs[cor_Svid - 1].time = ttRx[0];
                obs[cor_Svid - 1].rcv = 1;
                obs[cor_Svid - 1].code[0] = 40;
            }
            else if(carrier == 1.575420032E9&&measurement.getCn0DbHz() > Cn0DbHz){
                //如果是BDS
                int cor_Svid = Svid + 105;
                obs[cor_Svid - 1].sat[0] = cor_Svid;
                obs[cor_Svid - 1].P[1] = getPseudorange(measurement, gnssClock);//记录伪距
                if((measurement.getAccumulatedDeltaRangeState() & ADR_STATE_VALID)== ADR_STATE_VALID)
                    obs[cor_Svid - 1].L[1] = measurement.getAccumulatedDeltaRangeMeters()/(c/carrier);
                obs[cor_Svid - 1].D[1] = (float) (measurement.getPseudorangeRateMetersPerSecond() * measurement.getCarrierFrequencyHz() / c);
                obs[cor_Svid - 1].SNR[1] = (int) measurement.getCn0DbHz();
                gtime_t[] ttRx = new gtime_t[1];
                ttRx[0] = new gtime_t(0, 0);
                ttRx[0].time = getgpst(gnssClock) + 315964800 * 1e9;
                ttRx[0].time = ttRx[0].time / 1e9;
                ttRx[0].sec = ttRx[0].time - floor(ttRx[0].time);
                ttRx[0].time = floor(ttRx[0].time);
                obs[cor_Svid - 1].time = ttRx[0];
                obs[cor_Svid - 1].rcv = 1;
                obs[cor_Svid - 1].code[1] = 1;
            }
        }
        else if (constellationType == 3) {
            //如果是GLONASS
            if (measurement.getCn0DbHz() > Cn0DbHz) {
                int cor_Svid = Svid + 32;
                obs[cor_Svid - 1].sat[0] = cor_Svid;
                obs[cor_Svid - 1].P[0] = getPseudorange(measurement, gnssClock);//记录伪距
                if(measurement.getAccumulatedDeltaRangeState()== ADR_STATE_VALID)
                    obs[cor_Svid - 1].L[0] = measurement.getAccumulatedDeltaRangeMeters()/(c/carrier);
                obs[cor_Svid - 1].D[0] = (float) (-measurement.getPseudorangeRateMetersPerSecond() * measurement.getCarrierFrequencyHz() / c);
                obs[cor_Svid - 1].SNR[0] = (int) measurement.getCn0DbHz();
                gtime_t[] ttRx = new gtime_t[1];
                ttRx[0] = new gtime_t(0, 0);
                ttRx[0].time = getgpst(gnssClock) + 315964800 * 1e9;
                ttRx[0].time = ttRx[0].time / 1e9;
                ttRx[0].sec = ttRx[0].time - floor(ttRx[0].time);
                ttRx[0].time = floor(ttRx[0].time);
                obs[cor_Svid - 1].time = ttRx[0];
                obs[cor_Svid - 1].rcv = 1;
                obs[cor_Svid - 1].code[0] = 1;
            }
        }
        else if(constellationType == 6&&carrier==1.575420032E9){
            if (measurement.getCn0DbHz() > Cn0DbHz) {
                //如果是Galileo
                int cor_Svid = Svid + 59;
                obs[cor_Svid - 1].sat[0] = cor_Svid;
                obs[cor_Svid - 1].P[0] = getPseudorange(measurement, gnssClock);//记录伪距
                if(measurement.getAccumulatedDeltaRangeState()== ADR_STATE_VALID)
                    obs[cor_Svid - 1].L[0] = measurement.getAccumulatedDeltaRangeMeters()/(c/carrier);
                obs[cor_Svid - 1].D[0] = (float) (-measurement.getPseudorangeRateMetersPerSecond() * measurement.getCarrierFrequencyHz() / c);
                obs[cor_Svid - 1].SNR[0] = (int) measurement.getCn0DbHz();
                gtime_t[] ttRx = new gtime_t[1];
                ttRx[0] = new gtime_t(0, 0);
                ttRx[0].time = getgpst(gnssClock) + 315964800 * 1e9;
                ttRx[0].time = ttRx[0].time / 1e9;
                ttRx[0].sec = ttRx[0].time - floor(ttRx[0].time);
                ttRx[0].time = floor(ttRx[0].time);
                obs[cor_Svid - 1].time = ttRx[0];
                obs[cor_Svid - 1].rcv = 1;
                obs[cor_Svid - 1].code[0] = 40;
            }
        }
    }
}
