package com.example.myapplication.ui.Ntrip;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;


public class NtripViewModelFactory implements ViewModelProvider.Factory{
    private final Context context;

    public NtripViewModelFactory(Context context) {
        this.context = context;
    }
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(NtripViewModel.class)) {
            return (T) new NtripViewModel(context);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
