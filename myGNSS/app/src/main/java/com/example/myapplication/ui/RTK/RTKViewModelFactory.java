package com.example.myapplication.ui.RTK;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class RTKViewModelFactory implements ViewModelProvider.Factory{
    private final Context context;

    public RTKViewModelFactory(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(RtkViewModel.class)) {
            return (T) new RtkViewModel(context);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
