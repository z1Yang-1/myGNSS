package com.example.myapplication.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ToastBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.example.myapplication.SHOW_TOAST".equals(intent.getAction())) {
            String message = intent.getStringExtra("toastMessage");
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}
