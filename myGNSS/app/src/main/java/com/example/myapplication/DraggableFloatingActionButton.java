package com.example.myapplication;

import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class DraggableFloatingActionButton extends FloatingActionButton implements View.OnTouchListener{
    private float _xDelta;
    private float _yDelta;
    private GestureDetector gestureDetector;
    private Vibrator vibrator;
    private boolean isDragging = false; // 用于标记是否正在拖拽
    public DraggableFloatingActionButton(Context context) {
        super(context);
        init(context);
    }

    public DraggableFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DraggableFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 初始化 GestureDetector
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                // 在长按时震动
                if (vibrator != null) {
                    vibrator.vibrate(100); // 震动 100 毫秒
                }
                isDragging = true; // 设置为正在拖拽
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // 处理单击事件
                performClick(); // 调用 performClick() 方法以触发点击事件
                return true;
            }
        });
        // 初始化 Vibrator
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        gestureDetector.onTouchEvent(motionEvent); // 处理手势事件
        final int X = (int) motionEvent.getRawX();
        final int Y = (int) motionEvent.getRawY();
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // 计算偏移量
                _xDelta = X - getX();
                _yDelta = Y - getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) { // 只有在拖拽时才更新位置
                    // 更新按钮位置
                    float newX = X - _xDelta;
                    float newY = Y - _yDelta;

                    // 确保按钮在屏幕内
                    newX = Math.max(0, Math.min(newX, ((View) getParent()).getWidth() - getWidth()));
                    newY = Math.max(0, Math.min(newY, ((View) getParent()).getHeight() - getHeight()));

                    setX(newX);
                    setY(newY);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false; // 拖拽结束
                break;
        }
        return true; // 返回 true 表示事件已处理
    }
}
