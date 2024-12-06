package com.example.myapplication.ui.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.List;

public class myFragmentAdapter extends FragmentPagerAdapter {
    private List<Fragment> myFragmentList;
    private List<String> title;
    public myFragmentAdapter(@NonNull FragmentManager fm, List<Fragment> myFragmentList, List<String> title) {
        super(fm);
        this.myFragmentList = myFragmentList;
        this.title = title;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return myFragmentList.get(position);
    }

    @Override
    public int getCount() {
        return myFragmentList == null ? 0 : myFragmentList.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return title == null? "":title.get(position);
    }

}
