package com.example.myapplication.ui.status;

public class SatelliteInfo {
    public float elevation;
    public float azimuth;
    public int prn;
    public int constellationType;

    public SatelliteInfo(float elevation, float azimuth, int prn, int constellationType) {
        this.elevation = elevation;
        this.azimuth = azimuth;
        this.prn = prn;
        this.constellationType = constellationType;
    }
}
