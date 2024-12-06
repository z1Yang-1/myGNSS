package com.example.myapplication.ui.status;

import static android.location.GnssStatus.CONSTELLATION_BEIDOU;
import static android.location.GnssStatus.CONSTELLATION_GPS;

import static com.example.myapplication.MainActivity.newFolder2;
import static com.example.myapplication.ui.Sol.SolViewModel.prcopt;
import static com.example.myapplication.ui.measure_util.resetobs;
import static com.example.myapplication.ui.measure_util.solopt_default;
import static com.rtklib.bean.nav_t.getMaxSat;
import static com.rtklib.readobsnav.satSys.sat_sys;
import static com.rtklib.util.util1.time2epoch;

import android.annotation.SuppressLint;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.os.Build;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.ui.measure_util;
import com.rtklib.bean.gtime_t;
import com.rtklib.bean.obsd_t;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HomeViewModel extends ViewModel {
    StringBuilder sb1 = new StringBuilder("卫星类型").append('\n').append('\n');
    StringBuilder sb2 = new StringBuilder("可见卫星数目").append('\n').append('\n');
    static obsd_t[] obs = new obsd_t[getMaxSat()];
    static BufferedWriter bufferedWriter;
    static boolean support_CarrierPhase = true;
    double nr=0;
    int bds_sat_b1i=0;
    int bds_sat_b1c=0;
    int gps_sat_l1=0;
    int gps_sat_l5=0;
    int glonass_sat=0;
    int galileo_sat=0;
    private final MutableLiveData<String> bds_sat_count;
    private final MutableLiveData<String> gps_sat_count;
    private final MutableLiveData<String> glonass_sat_count;
    private final MutableLiveData<String> galileo_sat_count;
    boolean firstpaint;

    boolean start_log = false;
    private final MutableLiveData<String> mText;

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("home fragment");
        for(int i=0;i<getMaxSat();i++){
            obs[i] = new obsd_t();
        }
        firstpaint = true;
        bds_sat_count = new MutableLiveData<>();
        gps_sat_count = new MutableLiveData<>();
        glonass_sat_count = new MutableLiveData<>();
        galileo_sat_count = new MutableLiveData<>();

    }

    public static void write_log_head() {
        // 获取当前的UTC时间
        LocalDateTime utcTime = LocalDateTime.now(java.time.ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_M_d_H_mm_ss");
        String formattedTime = utcTime.format(formatter);
        try {
            File out_folder = new File(newFolder2, "gnss_log" + formattedTime + ".txt");
            FileWriter fileWriter = new FileWriter(out_folder);
            bufferedWriter = new BufferedWriter(fileWriter);
            out_loghead(bufferedWriter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressLint("DefaultLocale")
    public static void out_loghead(BufferedWriter bufferedWriter) throws IOException
    {
        Date now = new Date();
        //RINEX Version Type
        bufferedWriter.write(String.format("     %3.2f           OBSERVATION DATA    M                   RINEX VERSION / TYPE",3.03));
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
        String X = String.format("%14.4f", 0.0);
        String Y = String.format("%14.4f", 0.0);
        String Z = String.format("%14.4f", 0.0);
        bufferedWriter.write(X + Y + Z + "                  " + "APPROX POSITION XYZ");
        bufferedWriter.newLine();
        bufferedWriter.write("        0.0000        0.0000        0.0000                  ANTENNA: DELTA H/E/N");
        bufferedWriter.newLine();
        bufferedWriter.write("     0                                                      RCV CLOCK OFFS APPL ");
        bufferedWriter.newLine();
        if(support_CarrierPhase){
            bufferedWriter.write("G    8 C1C L1C D1C S1C C5Q L5Q D5Q S5Q                      SYS / # / OBS TYPES ");
            bufferedWriter.newLine();
        }else {
            bufferedWriter.write("G    4 C1C S1C C5X S5X                                      SYS / # / OBS TYPES ");
            bufferedWriter.newLine();
        }
        if (support_CarrierPhase) {
            bufferedWriter.write("R    4 L1C C1C D1C S1C                                      SYS / # / OBS TYPES ");
            bufferedWriter.newLine();
        } else {
            bufferedWriter.write("R    3 C1C D1C S1C                                          SYS / # / OBS TYPES ");
            bufferedWriter.newLine();
        }
        if (support_CarrierPhase) {
            bufferedWriter.write("J    8 C1C L1C D1C S1C C5Q L5Q D5Q S5Q                      SYS / # / OBS TYPES ");
            bufferedWriter.newLine();
        } else {
            bufferedWriter.write("J    4 C1C S1C C5X S5X                                      SYS / # / OBS TYPES ");
            bufferedWriter.newLine();
        }
        if (support_CarrierPhase) {
            bufferedWriter.write("E    8 C1C L1C D1C S1C C5Q L5Q D5Q S5Q                      SYS / # / OBS TYPES ");
            bufferedWriter.newLine();
        } else {
            bufferedWriter.write("E    4 C1C S1C C5X S5X                                      SYS / # / OBS TYPES ");
            bufferedWriter.newLine();
        }
        if (support_CarrierPhase) {
            bufferedWriter.write("C    4 C2I L2I D2I S2I                                      SYS / # / OBS TYPES ");
            bufferedWriter.newLine();
        } else {
            bufferedWriter.write("C    3 C2I D2I S2I                                          SYS / # / OBS TYPES ");
            bufferedWriter.newLine();
        }
        bufferedWriter.write("                                                            END OF HEADER");
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    //把卫星类型转换为对应的字符串
    public static String Gtypetostring(int constellationType)
    {
        // 将卫星类型转换为字符串
        String constellationName;
        switch (constellationType) {
            case CONSTELLATION_GPS:
                constellationName = "GPS";
                break;
            case GnssStatus.CONSTELLATION_SBAS:
                constellationName = "SBAS";
                break;
            case GnssStatus.CONSTELLATION_GLONASS:
                constellationName = "GLONASS";
                break;
            case CONSTELLATION_BEIDOU:
                constellationName = "BeiDou";
                break;
            case GnssStatus.CONSTELLATION_GALILEO:
                constellationName = "Galileo";
                break;
            case GnssStatus.CONSTELLATION_QZSS:
                constellationName = "QZSS";
                break;
            case GnssStatus.CONSTELLATION_IRNSS:
                constellationName = "IRNSS";
                break;
            default:
                constellationName = "Unknown";
                break;
        }
        return constellationName;
    }
    void processGnssMeasurements(GnssMeasurementsEvent eventArgs){
        // 获取GNSS测量数据列表
        List<GnssMeasurement> measurements = new ArrayList<>(eventArgs.getMeasurements());
        // 遍历测量数据列表
        bds_sat_b1i=0;
        bds_sat_b1c=0;
        gps_sat_l1=0;
        gps_sat_l5=0;
        glonass_sat=0;
        galileo_sat=0;
        for (GnssMeasurement measurement : measurements) {
            int type = measurement.getConstellationType();
            double carrier = measurement.getCarrierFrequencyHz();
            if(type==CONSTELLATION_GPS&&carrier==1.575420032e9)
                gps_sat_l1++;
            if(type==CONSTELLATION_GPS&&carrier==1.17645005e9)
                gps_sat_l5++;
            else if(type==CONSTELLATION_BEIDOU&& carrier == 1.561097984E9)
                bds_sat_b1i++;
            else if(type==CONSTELLATION_BEIDOU&& carrier == 1.575420032E9)
                bds_sat_b1c++;
            else if(type == GnssStatus.CONSTELLATION_GLONASS)
                glonass_sat++;
            else if(type == GnssStatus.CONSTELLATION_GALILEO&&carrier==1.575420032E9)
                galileo_sat++;
        }
        bds_sat_count.postValue(bds_sat_b1i+"/"+bds_sat_b1c);
        gps_sat_count.postValue(gps_sat_l1+"/"+gps_sat_l5);
        glonass_sat_count.postValue(String.valueOf(glonass_sat));
        galileo_sat_count.postValue(String.valueOf(galileo_sat));
        nr=0;
        for(obsd_t obss : obs){
            if(obss.sat[0]!=0){
                nr++;
            }
        }
    }
    void processSatelliteStatus(android.location.GnssStatus status) {
        int satelliteCount = status.getSatelliteCount();
        int gps_num = 0;
        int sbas_num = 0;
        int glonass_num = 0;
        int beidou_num = 0;
        int qzss_num = 0;
        int irnss_num = 0;

        for (int i = 0; i < satelliteCount; i++) {
            boolean hasEphemerisData = status.hasEphemerisData(i);
            int ConstellationType = status.getConstellationType(i);
            // 将卫星类型转换为字符串
            String constellationName = Gtypetostring(ConstellationType);
            switch (constellationName) {
                case "GPS":
                    gps_num++;
                    break;
                case "SBAS":
                    sbas_num++;
                    break;
                case "GLONASS":
                    glonass_num++;
                    break;
                case "BeiDou":
                    beidou_num++;
                    break;
                case "QZSS":
                    qzss_num++;
                    break;
                case "IRNSS":
                    irnss_num++;
                    break;
            }
        }
        if(gps_num!=0){
            sb1.append("GPS").append('\n').append('\n');
            sb2.append(gps_num).append('\n').append('\n');
        }
        if(sbas_num!=0){
            sb1.append("SBAS").append('\n').append('\n');
            sb2.append(sbas_num).append('\n').append('\n');
        }
        if(glonass_num!=0){
            sb1.append("GLONASS").append('\n').append('\n');
            sb2.append(glonass_num).append('\n').append('\n');
        }
        if(beidou_num!=0){
            sb1.append("BeiDou").append('\n').append('\n');
            sb2.append(beidou_num).append('\n').append('\n');
        }
        if(qzss_num!=0){
            sb1.append("QZSS").append('\n').append('\n');
            sb2.append(qzss_num).append('\n').append('\n');
        }
        if(irnss_num!=0){
            sb1.append("IRNSS").append('\n').append('\n');
            sb2.append(irnss_num).append('\n').append('\n');
        }
    }

    public void log_measurememts(GnssMeasurementsEvent eventArgs){
        gtime_t firstObsTime = null; // 初始化为 null，表示尚未设置
        int num_sat = 0;
        // 获取GNSS测量数据列表
        List<GnssMeasurement> measurements = new ArrayList<>(eventArgs.getMeasurements());
        // 遍历测量数据列表
        for (GnssMeasurement measurement : measurements) {
            measure_util.readobs(measurement, eventArgs.getClock(), obs);
        }
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
                bufferedWriter.write(sb.toString());
                bufferedWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public LiveData<String> getText() {
        return mText;
    }
    public LiveData<String> getbds() {
        return bds_sat_count;
    }
    public LiveData<String> getgps() {
        return gps_sat_count;
    }
    public LiveData<String> getglonass() {
        return glonass_sat_count;
    }
    public LiveData<String> getgalileo() {
        return galileo_sat_count;
    }


}