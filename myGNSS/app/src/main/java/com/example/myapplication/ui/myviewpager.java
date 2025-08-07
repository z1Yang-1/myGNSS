package com.example.myapplication.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

public class myviewpager extends ViewPager {
    private boolean swipeable = true;

    public myviewpager(@NonNull Context context) {
        super(context);
    }

    public myviewpager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    public void setSwipeable(boolean swipeable) {
        this.swipeable = swipeable;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.swipeable && super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.swipeable && super.onTouchEvent(event);
    }
}
