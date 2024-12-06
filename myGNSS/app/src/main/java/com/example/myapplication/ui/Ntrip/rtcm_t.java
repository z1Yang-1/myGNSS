package com.example.myapplication.ui.Ntrip;

import static com.rtklib.bean.nav_t.getMaxSat;
import static com.rtklib.bean.obsd_t.NEXOBS;
import static com.rtklib.bean.obsd_t.NFREQ;

import com.rtklib.bean.dgps_t;
import com.rtklib.bean.gtime_t;
import com.rtklib.bean.nav_t;
import com.rtklib.bean.obs_t;
import com.rtklib.bean.ssr_t;
import com.rtklib.bean.sta_t;

import java.io.Serializable;
import java.util.Objects;

public class rtcm_t implements Serializable {
    private static final long serialVersionUID = 1L;
    public int staid;//基站ID
    private int stah;//基站状态
    private int seqno;
    private int outtype;
    private gtime_t time;
    private gtime_t time_s;
    private obs_t obs;
    private nav_t nav;
    private sta_t sta;
    private dgps_t dgps;
    private ssr_t[] ssr = new ssr_t[getMaxSat()];
    String msg;
    String msgtype;
    String[] msmtype = new String[7];
    private int obsflag;
    private int ephsat;
    private int ephset;
    private double[][] cp = new double[getMaxSat()][NFREQ+NEXOBS];
    private int[][] lock = new int[getMaxSat()][NFREQ+NEXOBS];
    private int[][] loss = new int[getMaxSat()][NFREQ+NEXOBS];
    private gtime_t[][] lltime = new gtime_t[getMaxSat()][NFREQ+NEXOBS];
    private int nbyte;
    private int nbit;
    private int len;
    private int[] buffer = new int[1200];
    private int word;
    private int[] nmsg2 = new int[100];
    private int[] nmsg3 = new int[400];

    public int getStaid() {
        return staid;
    }

    public void setStaid(int staid) {
        this.staid = staid;
    }

    public int getStah() {
        return stah;
    }

    public void setStah(int stah) {
        this.stah = stah;
    }

    public int getSeqno() {
        return seqno;
    }

    public void setSeqno(int seqno) {
        this.seqno = seqno;
    }

    public int getOuttype() {
        return outtype;
    }

    public void setOuttype(int outtype) {
        this.outtype = outtype;
    }

    public gtime_t getTime() {
        return time;
    }

    public void setTime(gtime_t time) {
        this.time = time;
    }

    public gtime_t getTime_s() {
        return time_s;
    }

    public void setTime_s(gtime_t time_s) {
        this.time_s = time_s;
    }

    public obs_t getObs() {
        return obs;
    }

    public void setObs(obs_t obs) {
        this.obs = obs;
    }

    public nav_t getNav() {
        return nav;
    }

    public void setNav(nav_t nav) {
        this.nav = nav;
    }

    public sta_t getSta() {
        return sta;
    }

    public void setSta(sta_t sta) {
        this.sta = sta;
    }

    public dgps_t getDgps() {
        return dgps;
    }

    public void setDgps(dgps_t dgps) {
        this.dgps = dgps;
    }

    public ssr_t[] getSsr() {
        return ssr;
    }

    public void setSsr(ssr_t[] ssr) {
        this.ssr = ssr;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMsgtype() {
        return msgtype;
    }

    public void setMsgtype(String msgtype) {
        this.msgtype = msgtype;
    }

    public String[] getMsmtype() {
        return msmtype;
    }

    public void setMsmtype(String[] msmtype) {
        this.msmtype = msmtype;
    }

    public int getObsflag() {
        return obsflag;
    }

    public void setObsflag(int obsflag) {
        this.obsflag = obsflag;
    }

    public int getEphsat() {
        return ephsat;
    }

    public void setEphsat(int ephsat) {
        this.ephsat = ephsat;
    }

    public int getEphset() {
        return ephset;
    }

    public void setEphset(int ephset) {
        this.ephset = ephset;
    }

    public double[][] getCp() {
        return cp;
    }

    public void setCp(double[][] cp) {
        this.cp = cp;
    }

    public int[][] getLock() {
        return lock;
    }

    public void setLock(int[][] lock) {
        this.lock = lock;
    }

    public int[][] getLoss() {
        return loss;
    }

    public void setLoss(int[][] loss) {
        this.loss = loss;
    }

    public gtime_t[][] getLltime() {
        return lltime;
    }

    public void setLltime(gtime_t[][] lltime) {
        this.lltime = lltime;
    }

    public int getNbyte() {
        return nbyte;
    }

    public void setNbyte(int nbyte) {
        this.nbyte = nbyte;
    }

    public int getNbit() {
        return nbit;
    }

    public void setNbit(int nbit) {
        this.nbit = nbit;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public int[] getBuffer() {
        return buffer;
    }

    public void setBuffer(int[] buffer) {
        this.buffer = buffer;
    }

    public int getWord() {
        return word;
    }

    public void setWord(int word) {
        this.word = word;
    }

    public int[] getNmsg2() {
        return nmsg2;
    }

    public void setNmsg2(int[] nmsg2) {
        this.nmsg2 = nmsg2;
    }

    public int[] getNmsg3() {
        return nmsg3;
    }

    public void setNmsg3(int[] nmsg3) {
        this.nmsg3 = nmsg3;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        rtcm_t other = (rtcm_t) obj;
        // 在这里根据类的属性进行比较，如果所有属性都相等则返回true，否则返回false
        return staid == other.staid && obsflag == other.obsflag; // 以staid和obsflag为例
    }

    @Override
    public int hashCode() {
        return Objects.hash(staid, obsflag); // 选择类的属性作为hashCode的计算依据
    }
}
