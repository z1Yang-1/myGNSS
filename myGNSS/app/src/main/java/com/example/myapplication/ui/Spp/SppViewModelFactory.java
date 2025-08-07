package com.example.myapplication.ui.Spp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class SppViewModelFactory implements ViewModelProvider.Factory{
    private final Context context;

    public SppViewModelFactory(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SppViewModel.class)) {
            return (T) new SppViewModel(context);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }

}
