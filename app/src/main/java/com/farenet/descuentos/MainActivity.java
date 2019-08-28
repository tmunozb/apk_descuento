package com.farenet.descuentos;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.farenet.descuentos.adapter.FragmentPageAdapter;
import com.farenet.descuentos.config.Constante;
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.domain.Usuario;
import com.farenet.descuentos.fragment.FragmentCortesia;
import com.farenet.descuentos.fragment.FragmentDescuento;
import com.farenet.descuentos.repository.LoginRepository;
import com.farenet.descuentos.repository.MaestroRepository;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private LoginRepository loginRepository;
    private MaestroRepository maestroRepository;
    private SharedPreferences sharedPreferences;
    private Usuario usuario;
    private List<Planta> plantas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT > 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        loginRepository = Constante.getLoginRespository();
        maestroRepository = Constante.getMaestroRespository();
        sharedPreferences = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);

        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        viewPager = (ViewPager) findViewById(R.id.viewPager);

        setupViewPager();

        tabLayout.setupWithViewPager(viewPager);

        setupTabIcons();
    }

    private void setupTabIcons() {
        View v = LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        Drawable top = getResources().getDrawable(R.drawable.ic_descuento_selected);
        TextView txt = (TextView) v.findViewById(R.id.txt);
        txt.setText("Descuento");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            txt.setTextColor(getColor(R.color.selected));
            txt.setTypeface(null, Typeface.BOLD);
        }
        txt.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
        tabLayout.getTabAt(0).setCustomView(v);

        v = LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        top = getResources().getDrawable(R.drawable.ic_cortesia);
        txt = (TextView) v.findViewById(R.id.txt);
        txt.setText("Cortesia");
        txt.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
        tabLayout.getTabAt(1).setCustomView(v);
    }

    private void setupViewPager() {

        FragmentPageAdapter pagerAdapter = new FragmentPageAdapter(getSupportFragmentManager());
        pagerAdapter.addFragment(new FragmentDescuento(), "Descuento");
        pagerAdapter.addFragment(new FragmentCortesia(), "Cortesia");
        viewPager.setAdapter(pagerAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    View v = tabLayout.getTabAt(0).getCustomView();
                    TextView txt = (TextView) v.findViewById(R.id.txt);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        txt.setTextColor(getColor(R.color.selected));
                        txt.setTypeface(null, Typeface.BOLD);
                    }
                    Drawable top = getResources().getDrawable(R.drawable.ic_descuento_selected);
                    txt.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
                    tabLayout.getTabAt(0).setCustomView(v);
                } else if (tab.getPosition() == 1) {
                    View v = tabLayout.getTabAt(1).getCustomView();
                    TextView txt = (TextView) v.findViewById(R.id.txt);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        txt.setTextColor(getColor(R.color.selected));
                        txt.setTypeface(null, Typeface.BOLD);
                    }
                    Drawable top = getResources().getDrawable(R.drawable.ic_cortesia_selected);
                    txt.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
                    tabLayout.getTabAt(1).setCustomView(v);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    View v = tabLayout.getTabAt(0).getCustomView();
                    TextView txt = (TextView) v.findViewById(R.id.txt);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        txt.setTextColor(getColor(R.color.unselected));
                        txt.setTypeface(null, Typeface.NORMAL);
                    }
                    Drawable top = getResources().getDrawable(R.drawable.ic_descuento);
                    txt.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
                    tabLayout.getTabAt(0).setCustomView(v);
                } else if (tab.getPosition() == 1) {
                    View v = tabLayout.getTabAt(1).getCustomView();
                    TextView txt = (TextView) v.findViewById(R.id.txt);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        txt.setTextColor(getColor(R.color.unselected));
                        txt.setTypeface(null, Typeface.NORMAL);
                    }
                    Drawable top = getResources().getDrawable(R.drawable.ic_cortesia);
                    txt.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
                    tabLayout.getTabAt(1).setCustomView(v);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

    }
}
