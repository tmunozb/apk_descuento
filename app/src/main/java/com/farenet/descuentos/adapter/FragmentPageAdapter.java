package com.farenet.descuentos.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class FragmentPageAdapter extends FragmentPagerAdapter {

    private List<Fragment> lstFragment = new ArrayList<>();
    private List<String> lstTittle = new ArrayList<>();

    public FragmentPageAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int i) {
        return lstFragment.get(i);
    }

    @Override
    public int getCount() {
        return lstFragment.size();
    }

    public void addFragment(Fragment fragment, String title) {
        lstFragment.add(fragment);
        lstTittle.add(title);
    }

    public CharSequence getPageTitle(int i) {
        return lstTittle.get(i);
    }
}
