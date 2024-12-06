package com.example.myapplication.ui.DGPS;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class DgpsViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;

    public DgpsViewModelFactory(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(DgpsViewModel.class)) {
            return (T) new DgpsViewModel(context);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
