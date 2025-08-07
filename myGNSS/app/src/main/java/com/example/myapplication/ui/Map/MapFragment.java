package com.example.myapplication.ui.Map;

import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentSppBinding;
import com.example.myapplication.positiondatabase;

import java.util.Timer;
import java.util.TimerTask;

public class MapFragment extends Fragment {

    private @NonNull FragmentSppBinding binding;
    private MapViewModel mViewModel;
    MapView mMapView = null;
    private LocationClient mLocationClient;
    private boolean isFirstLoc = true;
    private LocationClientOption option;




    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.setAgreePrivacy(requireContext().getApplicationContext(),true);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        SDKInitializer.initialize(requireContext().getApplicationContext());
        //自4.3.0起，百度地图SDK所有接口均支持百度坐标和国测局坐标，用此方法设置您使用的坐标类型.
        //包括BD09LL和GCJ02两种坐标，默认是BD09LL坐标。
        SDKInitializer.setCoordType(CoordType.BD09LL);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(MapViewModel.class);
        binding = FragmentSppBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        mMapView = new MapView(requireContext());
        if (isAdded()) {
            Activity activity = getActivity(); // 确保 Fragment 已经与 Activity 关联
            if (activity != null) {
                try {
                    LocationClient.setAgreePrivacy(true);
                    mLocationClient = new LocationClient(requireContext().getApplicationContext());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                // 进行相关操作
            }
        }
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            // MapView 销毁后不在处理新接收的位置
//            if (mMapView == null) {
//                return;
//            }
//            locData = new MyLocationData.Builder()
//                    .accuracy(location.getRadius())// 设置定位数据的精度信息，单位：米
//                    .direction(location.getDirection()) // 此处设置开发者获取到的方向信息，顺时针0-360
//                    .latitude(location.getLatitude())
//                    .longitude(location.getLongitude())
//                    .build();
//            // 设置定位数据, 只有先允许定位图层后设置数据才会生效
//            mBaiduMap.setMyLocationData(locData);
//            if (isFirstLoc) {
//                isFirstLoc = false;
//                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//                MapStatus.Builder builder = new MapStatus.Builder();
//                builder.target(latLng).zoom(25.0f);
//                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
//            }
        }

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // TODO: Use the ViewModel
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        if (mViewModel.timer == null) {
            mViewModel.timer = new Timer(); // 创建新的 Timer 实例
        }
        mViewModel.timerTask = new TimerTask() {
            @SuppressLint("Range")
            @Override
            public void run() {
                // 执行定时任务
                if (mViewModel.cursor.moveToFirst()) {
                    // 读取数据
                    mViewModel.strdsm1 = mViewModel.cursor.getString(mViewModel.cursor.getColumnIndex("dsm1"));
                    mViewModel.strdsm2 = mViewModel.cursor.getString(mViewModel.cursor.getColumnIndex("dsm2"));

                    mViewModel.dsm1 = strtodouble(mViewModel.strdsm1);
                    mViewModel.dsm2 = strtodouble(mViewModel.strdsm2);
                    mViewModel.wgs84lat = mViewModel.dsm1[0] + mViewModel.dsm1[1]/60 + mViewModel.dsm1[2]/3600;
                    mViewModel.wgs84lng = mViewModel.dsm2[0] + mViewModel.dsm2[1]/60 + mViewModel.dsm2[2]/3600;
                    LatLng wgs84Point = new LatLng(mViewModel.wgs84lat, mViewModel.wgs84lng);
                    // 创建一个CoordinateConverter对象，进行坐标系转换
                    CoordinateConverter converter = new CoordinateConverter();
                    converter.from(CoordinateConverter.CoordType.GPS); // 从GPS坐标系转换，即WGS84坐标系
                    converter.coord(wgs84Point); // 从GPS坐标系转换，即WGS84坐标系
                    // 进行坐标转换，得到BD09LL坐标系下的坐标点
                    mViewModel.bd09llPoint = converter.convert();
                    mViewModel.locData = new MyLocationData.Builder()
                            .latitude(mViewModel.bd09llPoint.latitude)
                            .longitude(mViewModel.bd09llPoint.longitude)
                            .build();
                    // 设置定位数据, 只有先允许定位图层后设置数据才会生效
                    mViewModel.mBaiduMap.setMyLocationData(mViewModel.locData);
                }
            }
        };
        mViewModel.timer.schedule(mViewModel.timerTask, 0, 1000); // 重新安排 TimerTask
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        if (mViewModel.timer != null) {
            mViewModel.timer.cancel(); // 取消 Timer
            mViewModel.timer = null; // 将 Timer 设置为 null
        }
        super.onPause();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 关闭 cursor 和数据库连接
        if (mViewModel.timer != null) {
            mViewModel.timer.cancel();
        }
        mViewModel.cursor.close();
        mViewModel.db.close();
        if(mMapView!=null)
            mMapView.onDestroy();
        mLocationClient.stop();
        mViewModel.mBaiduMap.setMyLocationEnabled(false);
    }
    static double[] strtodouble(String str){

        // 按逗号分割字符串并得到子字符串数组
        String[] numberStrings = str.split(", ");

        // 创建对应大小的 double 数组
        double[] doubleArray = new double[numberStrings.length];

        // 将字符串转换为 double 并存储到数组中
        for (int i = 0; i < numberStrings.length; i++) {
            doubleArray[i] = Double.parseDouble(numberStrings[i]);
        }
        return doubleArray;
    }

    @SuppressLint("Range")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMapView = view.findViewById(R.id.bmapView);
        mViewModel.mBaiduMap = mMapView.getMap();
        mViewModel.mBaiduMap.setMyLocationEnabled(true);
        // 注册监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        option = new LocationClientOption();

        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);

        option.setCoorType("bd09ll");

        option.setFirstLocType(LocationClientOption.FirstLocType.SPEED_IN_FIRST_LOC);

        option.setScanSpan(1000);

        option.setOpenGnss(true);

        option.setLocationNotify(true);

        option.setIgnoreKillProcess(false);

        option.SetIgnoreCacheException(false);

        option.setWifiCacheTimeOut(5*60*1000);

        option.setEnableSimulateGnss(false);

        option.setNeedNewVersionRgc(true);

        if(mLocationClient != null) mLocationClient.setLocOption(option);

        assert mLocationClient != null;
        mLocationClient.start();

        positiondatabase mydb = new positiondatabase(getContext());

        mViewModel.db = mydb.getReadableDatabase();

        // 执行查询获取最新的一条数据
        mViewModel.cursor = mViewModel.db.rawQuery("SELECT * FROM my_table ORDER BY id DESC LIMIT 1", null);

        // 检查是否有数据
        if (mViewModel.cursor.moveToFirst()) {
            // 读取数据
            mViewModel.strdsm1 = mViewModel.cursor.getString(mViewModel.cursor.getColumnIndex("dsm1"));
            mViewModel.strdsm2 = mViewModel.cursor.getString(mViewModel.cursor.getColumnIndex("dsm2"));

            mViewModel.dsm1 = strtodouble(mViewModel.strdsm1);
            mViewModel.dsm2 = strtodouble(mViewModel.strdsm2);
            mViewModel.wgs84lat = mViewModel.dsm1[0] + mViewModel.dsm1[1]/60 + mViewModel.dsm1[2]/3600;
            mViewModel.wgs84lng = mViewModel.dsm2[0] + mViewModel.dsm2[1]/60 + mViewModel.dsm2[2]/3600;
            LatLng wgs84Point = new LatLng(mViewModel.wgs84lat, mViewModel.wgs84lng);
            // 创建一个CoordinateConverter对象，进行坐标系转换
            CoordinateConverter converter = new CoordinateConverter();
            converter.from(CoordinateConverter.CoordType.GPS); // 从GPS坐标系转换，即WGS84坐标系
            converter.coord(wgs84Point); // 从GPS坐标系转换，即WGS84坐标系
            // 进行坐标转换，得到BD09LL坐标系下的坐标点
            mViewModel.bd09llPoint = converter.convert();

            // 加载并缩放位图
            mViewModel.originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.position_icon);
            mViewModel.scaledBitmap = scaleBitmap(mViewModel.originalBitmap, 0.06f, 0.06f); // 缩放比例为0.5
            // 创建 BitmapDescriptor
            mViewModel.bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(mViewModel.scaledBitmap);
//            MarkerOptions markerOptions = new MarkerOptions()
//                    .position(mViewModel.bd09llPoint)
//                    .icon(mViewModel.bitmapDescriptor);
//            mViewModel.mBaiduMap.addOverlay(markerOptions);
            MyLocationConfiguration mLocationConfiguration = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.FOLLOWING,false,
                    mViewModel.bitmapDescriptor,mViewModel.accuracyCircleFillColor,mViewModel.accuracyCircleStrokeColor);
            mViewModel.mBaiduMap.setMyLocationConfiguration(mLocationConfiguration);

            mViewModel.locData = new MyLocationData.Builder()
                    .latitude(mViewModel.bd09llPoint.latitude)
                    .longitude(mViewModel.bd09llPoint.longitude)
                    .build();
            // 设置定位数据, 只有先允许定位图层后设置数据才会生效
            mViewModel.mBaiduMap.setMyLocationData(mViewModel.locData);
//            if (isFirstLoc) {
//                isFirstLoc = false;
//                LatLng latLng = new LatLng(mViewModel.bd09llPoint.latitude, mViewModel.bd09llPoint.longitude);
//                MapStatus.Builder builder = new MapStatus.Builder();
//                builder.target(latLng).zoom(20.0f);
//                mViewModel.mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
//            }
        }
        mViewModel.timer = new Timer();
        mViewModel.timerTask = new TimerTask() {
            @Override
            public void run() {
                // 执行查询获取最新的一条数据
                mViewModel.cursor = mViewModel.db.rawQuery("SELECT * FROM my_table ORDER BY id DESC LIMIT 1", null);
                // 执行定时任务
                if (mViewModel.cursor.moveToFirst()) {
                    // 读取数据
                    mViewModel.strdsm1 = mViewModel.cursor.getString(mViewModel.cursor.getColumnIndex("dsm1"));
                    mViewModel.strdsm2 = mViewModel.cursor.getString(mViewModel.cursor.getColumnIndex("dsm2"));

                    mViewModel.dsm1 = strtodouble(mViewModel.strdsm1);
                    mViewModel.dsm2 = strtodouble(mViewModel.strdsm2);
                    mViewModel.wgs84lat = mViewModel.dsm1[0] + mViewModel.dsm1[1]/60 + mViewModel.dsm1[2]/3600;
                    mViewModel.wgs84lng = mViewModel.dsm2[0] + mViewModel.dsm2[1]/60 + mViewModel.dsm2[2]/3600;
                    LatLng wgs84Point = new LatLng(mViewModel.wgs84lat, mViewModel.wgs84lng);
                    // 创建一个CoordinateConverter对象，进行坐标系转换
                    CoordinateConverter converter = new CoordinateConverter();
                    converter.from(CoordinateConverter.CoordType.GPS); // 从GPS坐标系转换，即WGS84坐标系
                    converter.coord(wgs84Point); // 从GPS坐标系转换，即WGS84坐标系
                    // 进行坐标转换，得到BD09LL坐标系下的坐标点
                    mViewModel.bd09llPoint = converter.convert();
                    mViewModel.locData = new MyLocationData.Builder()
                            .latitude(mViewModel.bd09llPoint.latitude)
                            .longitude(mViewModel.bd09llPoint.longitude)
                            .build();
                    // 设置定位数据, 只有先允许定位图层后设置数据才会生效
                    if(mViewModel.mBaiduMap!=null)
                        mViewModel.mBaiduMap.setMyLocationData(mViewModel.locData);
//                    MarkerOptions markerOptions = new MarkerOptions()
//                            .position(mViewModel.bd09llPoint)
//                            .icon(mViewModel.bitmapDescriptor);
//                    mViewModel.mBaiduMap.addOverlay(markerOptions);
//                    LatLng latLng = new LatLng(mViewModel.bd09llPoint.latitude, mViewModel.bd09llPoint.longitude);
//                    MapStatus.Builder builder = new MapStatus.Builder();
//                    builder.target(latLng);
//                    mViewModel.mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                }
            }
        };
        mViewModel.timer.schedule(new TimerTask() {
            @SuppressLint("Range")
            @Override
            public void run() {
                // 执行查询获取最新的一条数据
                mViewModel.cursor = mViewModel.db.rawQuery("SELECT * FROM my_table ORDER BY id DESC LIMIT 1", null);
//                positiondatabase mydb = new positiondatabase(getContext());
//
//                SQLiteDatabase db = mydb.getReadableDatabase();
//                // 执行查询获取最新的一条数据
//                Cursor cursor = db.rawQuery("SELECT * FROM my_table ORDER BY id DESC LIMIT 1", null);
                // 检查是否有数据
                if (mViewModel.cursor.moveToFirst()) {
                    // 读取数据
                    mViewModel.strdsm1 = mViewModel.cursor.getString(mViewModel.cursor.getColumnIndex("dsm1"));
                    mViewModel.strdsm2 = mViewModel.cursor.getString(mViewModel.cursor.getColumnIndex("dsm2"));

                    mViewModel.dsm1 = strtodouble(mViewModel.strdsm1);
                    mViewModel.dsm2 = strtodouble(mViewModel.strdsm2);
                    mViewModel.wgs84lat = mViewModel.dsm1[0] + mViewModel.dsm1[1]/60 + mViewModel.dsm1[2]/3600;
                    mViewModel.wgs84lng = mViewModel.dsm2[0] + mViewModel.dsm2[1]/60 + mViewModel.dsm2[2]/3600;
                    LatLng wgs84Point = new LatLng(mViewModel.wgs84lat, mViewModel.wgs84lng);
                    // 创建一个CoordinateConverter对象，进行坐标系转换
                    CoordinateConverter converter = new CoordinateConverter();
                    converter.from(CoordinateConverter.CoordType.GPS); // 从GPS坐标系转换，即WGS84坐标系
                    converter.coord(wgs84Point); // 从GPS坐标系转换，即WGS84坐标系
                    // 进行坐标转换，得到BD09LL坐标系下的坐标点
                    mViewModel.bd09llPoint = converter.convert();
                    // 清理之前的标记
//                    if (mViewModel.currentMarker != null) {
//                        mViewModel.mBaiduMap.removeOverLays((List<Overlay>) mViewModel.currentMarker.getTitleOptions());
//                    }
                    mViewModel.locData = new MyLocationData.Builder()
                            .latitude(mViewModel.bd09llPoint.latitude)
                            .longitude(mViewModel.bd09llPoint.longitude)
                            .build();
                    // 设置定位数据, 只有先允许定位图层后设置数据才会生效
                    if(mViewModel.mBaiduMap!=null)
                        mViewModel.mBaiduMap.setMyLocationData(mViewModel.locData);
//                    MarkerOptions markerOptions = new MarkerOptions()
//                            .position(mViewModel.bd09llPoint)
//                            .icon(mViewModel.bitmapDescriptor);
//                    mViewModel.currentMarker = (Marker) mViewModel.mBaiduMap.addOverlay(markerOptions);
//                    mViewModel.mBaiduMap.addOverlay(mViewModel.currentMarker.getTitleOptions());
//
//                    LatLng latLng = new LatLng(mViewModel.bd09llPoint.latitude, mViewModel.bd09llPoint.longitude);
//                    MapStatus.Builder builder = new MapStatus.Builder();
//                    builder.target(latLng);
//                    mViewModel.mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                }
            }
        }, 2000, 500); // 初始延迟为2秒，间隔为1秒
    }
    // 缩放位图
    private Bitmap scaleBitmap(Bitmap bitmap, float scaleX, float scaleY) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scaleX, scaleY);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }
}