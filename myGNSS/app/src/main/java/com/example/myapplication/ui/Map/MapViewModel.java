package com.example.myapplication.ui.Map;

import static com.example.myapplication.ui.Map.MapFragment.strtodouble;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;

import androidx.lifecycle.ViewModel;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;

import java.util.Timer;
import java.util.TimerTask;

public class MapViewModel extends ViewModel {
    TimerTask timerTask; // 定义 TimerTask
    double[] dsm1 = new double[3];
    double[] dsm2 = new double[3];
    double wgs84lat;
    double wgs84lng;
    String strdsm1;
    String strdsm2;
    Timer timer;
    LatLng bd09llPoint;
    Cursor cursor;
    BaiduMap mBaiduMap;
    BitmapDescriptor bitmapDescriptor;
    Bitmap originalBitmap;
    Bitmap scaledBitmap;
    SQLiteDatabase db;
    // 声明一个成员变量来保存当前标记
    Marker mcurrentMarker;
    int accuracyCircleFillColor = 0xAAFFFF88;
    //自定义精度圈边框颜色
    int accuracyCircleStrokeColor = 0xAA00FF00;
    MyLocationData locData;
    // TODO: Implement the ViewModel
    public MapViewModel(){
        timer = new Timer();
        timerTask = new TimerTask() {
            @SuppressLint("Range")
            @Override
            public void run() {
                // 执行查询获取最新的一条数据
                cursor = db.rawQuery("SELECT * FROM my_table ORDER BY id DESC LIMIT 1", null);
                // 执行定时任务
                if (cursor.moveToFirst()) {
                    // 读取数据
                    strdsm1 = cursor.getString(cursor.getColumnIndex("dsm1"));
                    strdsm2 = cursor.getString(cursor.getColumnIndex("dsm2"));

                    dsm1 = strtodouble(strdsm1);
                    dsm2 = strtodouble(strdsm2);
                    wgs84lat = dsm1[0] + dsm1[1]/60 + dsm1[2]/3600;
                    wgs84lng = dsm2[0] + dsm2[1]/60 + dsm2[2]/3600;
                    LatLng wgs84Point = new LatLng(wgs84lat, wgs84lng);
                    // 创建一个CoordinateConverter对象，进行坐标系转换
                    CoordinateConverter converter = new CoordinateConverter();
                    converter.from(CoordinateConverter.CoordType.GPS); // 从GPS坐标系转换，即WGS84坐标系
                    converter.coord(wgs84Point); // 从GPS坐标系转换，即WGS84坐标系
                    // 进行坐标转换，得到BD09LL坐标系下的坐标点
                    bd09llPoint = converter.convert();
//                    locData = new MyLocationData.Builder()
//                            .latitude(bd09llPoint.latitude)
//                            .longitude(bd09llPoint.longitude)
//                            .build();
//                    // 设置定位数据, 只有先允许定位图层后设置数据才会生效
//                    if(mBaiduMap!=null)
//                        mBaiduMap.setMyLocationData(locData);
//                    MarkerOptions markerOptions = new MarkerOptions()
//                            .position(bd09llPoint)
//                            .icon(bitmapDescriptor);
//                    mBaiduMap.addOverlay(markerOptions);
//                    LatLng latLng = new LatLng(bd09llPoint.latitude, bd09llPoint.longitude);
//                    MapStatus.Builder builder = new MapStatus.Builder();
//                    builder.target(latLng);
//                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                }
            }
        };
    }
}