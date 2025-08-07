package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapUtils {
    public static Bitmap loadAndScaleBitmap(Context context, int resourceId, int width, int height) {
        // 加载原始位图
        Bitmap originalBitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);

        // 缩放位图
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true);

        // 回收原始位图以释放内存
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
        }

        return scaledBitmap;
    }
}
