package com.example.myapplication.ui.status;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.GnssStatus;
import android.util.AttributeSet;
import android.view.View;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

public class GnssSkyView extends View {
    private static final int Y_TRANSLATION = 100; // 定义Y轴平移量
    private int mWidth, mHeight; // 添加成员变量用于保存视图的宽度和高度
    private Paint mPaintCircleAndLine; // 添加画笔成员变量
    private Paint mPaintDegree; // 添加画笔成员变量用于绘制度数
    private static Paint mPrnIdPaint;
    private static final float SAT_RADIUS = 40.0f; // 定义卫星的半径
    private List<SatelliteInfo> satelliteInfoList = new ArrayList<>(); // 存储卫星信息的列表
    private Bitmap mBitmap; // 成员变量，用于保存绘制结果

    public GnssSkyView(Context context) {
        super(context);
        init();
    }

    public GnssSkyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mPaintCircleAndLine = new Paint();
        mPaintCircleAndLine.setColor(Color.BLACK); // 设置画笔颜色为蓝色
        mPaintCircleAndLine.setStyle(Paint.Style.STROKE); // 设置画笔样式为描边
        mPaintCircleAndLine.setStrokeWidth(5); // 设置画笔宽度

        mPrnIdPaint = new Paint();
        mPrnIdPaint.setColor(Color.BLACK); // 设置卫星文本颜色
        mPrnIdPaint.setTextSize(30); // 设置卫星文本大小
        mPrnIdPaint.setStyle(Paint.Style.FILL); // 设置画笔样式为填充

        mPaintDegree = new Paint();
        mPaintDegree.setColor(Color.RED); // 设置画笔颜色为红色
        mPaintDegree.setTextSize(30); // 设置度数字体大小
        mPaintDegree.setStyle(Paint.Style.FILL); // 设置画笔样式为填充
    }

    // 重写 onSizeChanged 方法以获取视图的宽度和高度
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
    }

    // 重写 onDraw 方法以绘制位图
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int minScreenDimen = Math.min(mWidth, mHeight);
        float radius = minScreenDimen / 2.0f;

        // 在Canvas上执行绘制操作
        drawCircle(canvas, radius);
        drawLine(canvas, minScreenDimen, radius);
        drawDegree(canvas, radius);
        if (mBitmap != null) {
            // 在视图上绘制生成的Bitmap
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }
    }

    // 设置卫星信息并启动后台线程进行绘制
    public void setSatelliteInfo(List<SatelliteInfo> satelliteInfoList) {
        this.satelliteInfoList = satelliteInfoList;
        drawInBackground(); // 在后台线程中绘制
    }

    // 在后台线程中绘制内容并更新视图
    public void drawInBackground() {
        new Thread(() -> {
            // 创建Bitmap
            Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            int minScreenDimen = Math.min(mWidth, mHeight);
//                float radius = minScreenDimen / 2.0f;
//
//                // 在Canvas上执行绘制操作
//                drawCircle(canvas, radius);
//                drawLine(canvas, minScreenDimen, radius);
//                drawDegree(canvas, radius);

            // 绘制所有卫星
            for (SatelliteInfo info : satelliteInfoList) {
                drawSatellite(canvas, minScreenDimen, info.elevation, info.azimuth, info.prn, info.constellationType);
            }

            // 将结果传递回主线程
            post(new Runnable() {
                @Override
                public void run() {
                    mBitmap = bitmap; // 保存绘制结果
                    invalidate(); // 请求重绘视图
                }
            });
        }).start();
    }

    private void drawCircle(Canvas c, float radius) {
        c.drawCircle(radius, radius + Y_TRANSLATION, elevationToRadius(radius, 60.0f), mPaintCircleAndLine);
        c.drawCircle(radius, radius + Y_TRANSLATION, elevationToRadius(radius, 30.0f), mPaintCircleAndLine);
        c.drawCircle(radius, radius + Y_TRANSLATION, elevationToRadius(radius, 0.0f), mPaintCircleAndLine);
    }

    private static float elevationToRadius(float s, float elev) {
        return s * (1.0f - (elev / 90.0f));
    }

    private void drawLine(Canvas c, int s, float radius) {
        c.drawLine(radius, Y_TRANSLATION, radius, s + Y_TRANSLATION, mPaintCircleAndLine);
        c.drawLine(0, radius + Y_TRANSLATION, s, radius + Y_TRANSLATION, mPaintCircleAndLine);

        final float cos45 = (float) Math.cos(Math.PI / 4);
        float d1 = radius * (1 - cos45);
        float d2 = radius * (1 + cos45);
        c.drawLine(d1, d1 + Y_TRANSLATION, d2, d2 + Y_TRANSLATION, mPaintCircleAndLine);
        c.drawLine(d2, d1 + Y_TRANSLATION, d1, d2 + Y_TRANSLATION, mPaintCircleAndLine);
    }

    private void drawDegree(Canvas c, float radius) {
        for (int i = 0; i < 360; i += 15) {
            if (i == 45 || i == 135 || i == 225 || i == 315) {
                c.drawText(String.valueOf(i), radius, 40 + Y_TRANSLATION, mPaintDegree);
            } else if (i == 0) {
                c.drawText("N", radius, 40 + Y_TRANSLATION, mPaintDegree);
            } else if (i == 90) {
                c.drawText("E", radius, 40 + Y_TRANSLATION, mPaintDegree);
            } else if (i == 180) {
                c.drawText("S", radius, 40 + Y_TRANSLATION, mPaintDegree);
            } else if (i == 270) {
                c.drawText("W", radius, 40 + Y_TRANSLATION, mPaintDegree);
            } else {
                c.drawLine(radius, Y_TRANSLATION, radius, 20 + Y_TRANSLATION, mPaintDegree);
            }

            c.rotate(15, radius, radius + Y_TRANSLATION);
        }
    }

    private Bitmap getSatelliteBitmap(int constellationType) {
        Bitmap baseMap;
        switch (constellationType) {
            case GnssStatus.CONSTELLATION_BEIDOU:
                baseMap = BitmapFactory.decodeResource(getResources(), R.drawable.china);
                break;
            case GnssStatus.CONSTELLATION_GPS:
                baseMap = BitmapFactory.decodeResource(getResources(), R.drawable.america);
                break;
            case GnssStatus.CONSTELLATION_GALILEO:
                baseMap = BitmapFactory.decodeResource(getResources(), R.drawable.europe);
                break;
            case GnssStatus.CONSTELLATION_GLONASS:
                baseMap = BitmapFactory.decodeResource(getResources(), R.drawable.russia);
                break;
            default:
                return null;
        }
        // 检查 baseMap 是否为 null
        if (baseMap == null) {
            return null;
        }
        // 缩放卫星图标
        return Bitmap.createScaledBitmap(baseMap, (int) (SAT_RADIUS * 2.0f), (int) (SAT_RADIUS * 2.0f), false);
    }

    private static String getSatelliteText(int prn, int constellationType) {
        StringBuilder builder = new StringBuilder();
        switch (constellationType) {
            case GnssStatus.CONSTELLATION_BEIDOU:
                builder.append("C");
                break;
            case GnssStatus.CONSTELLATION_GPS:
                builder.append("G");
                break;
            case GnssStatus.CONSTELLATION_GALILEO:
                builder.append("E");
                break;
            case GnssStatus.CONSTELLATION_GLONASS:
                builder.append("R");
                break;
            default:
                builder.append("S");
        }
        builder.append(prn);
        return builder.toString();
    }

    public void drawSatellite(Canvas c, int s, float elev, float azim, int prn, int constellationType) {
        double radius, angle;
        float x, y;
        Bitmap satMap;
        satMap = getSatelliteBitmap(constellationType);
        if (satMap == null) {
            return; // 如果没有匹配的星座类型，则不绘制
        }
        String satText = getSatelliteText(prn, constellationType);
        radius = elevationToRadius(s / 2.0f, elev);
        angle = Math.toRadians(azim);
        x = (float) (radius * Math.sin(angle));
        y = (float) (radius * Math.cos(angle));

        x = s / 2.0f + x - SAT_RADIUS;
        y = s / 2.0f - y - SAT_RADIUS + Y_TRANSLATION;
        c.drawBitmap(satMap, x, y, null);
        c.drawText(satText, x, y, mPrnIdPaint);
    }
}
