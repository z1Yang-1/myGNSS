package com.example.myapplication.ui.Sol;

import static com.example.myapplication.ui.measure_util.prcopt_default;

import android.content.SharedPreferences;

import androidx.lifecycle.ViewModel;

import com.rtklib.bean.prcopt_t;

import java.util.List;

public class SolViewModel extends ViewModel {
    String[] ionosphere_model = {"OFF", "Klobuchar"};
    String[] troposphere_model = {"OFF", "Saastamoinen"};
    String[] azel = new String[19]; // 从0到90，每隔5一个元素，共有19个元素
    String[] cn0 = new String[10];
    String[] ambres = {"OFF", "Continuous", "Instantaneous", "Fix and Hold"};
    List<String> ambrestest = List.of("OFF", "Continuous", "Instantaneous", "Fix and Hold");
    String[] ambres_bds = {"OFF", "On"};
    String[] ambres_glo = {"OFF", "On","auto cal"};
    String[] Freq = {"L1"};
    public static boolean open;
    public static prcopt_t prcopt = prcopt_default;
    SharedPreferences solSettingPrefer;
    public SolViewModel(){
        for (int i = 0; i < azel.length; i++) {
            azel[i] = i * 5 +"°"; // 将索引乘以5，然后转换为字符串
        }
        for (int i = 0; i < cn0.length; i++) {
            cn0[i] = i * 5 +" "+"dBHz"; // 将索引乘以5，然后转换为字符串
        }
    }
}
