package com.farenet.descuentos.ui.comercialhost;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.comercialhost.adapter.TabsAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.View;

public class ComercialHostActivity extends AppCompatActivity {

    private ViewPager2 pager;
    private BottomNavigationView bottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comercial_host);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pager = findViewById(R.id.pager);
        bottom = findViewById(R.id.bottomNav);

        pager.setAdapter(new TabsAdapter(this));
        pager.setOffscreenPageLimit(2);
        pager.setUserInputEnabled(true);

        // ❌ Quitar cualquier PageTransformer que mueva en X
        pager.setPageTransformer(null);

        // ✅ Evitar el “glow”/elastic del RecyclerView interno (a veces hace ver bordes)
        RecyclerView rv = (RecyclerView) pager.getChildAt(0);
        if (rv != null) rv.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // BottomNav -> Pager
        bottom.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.tab_home) {
                setCurrent(0);
                return true;
            } else if (id == R.id.tab_Bolsa) {
                setCurrent(1);
                return true;
            } else if (id == R.id.tab_solicitudes) {
                setCurrent(2);
                return true;
            }
            return false;
        });

        // Pager -> BottomNav
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                int target = position == 0 ? R.id.tab_home :
                        position == 1 ? R.id.tab_Bolsa : R.id.tab_solicitudes;
                if (bottom.getSelectedItemId() != target) bottom.setSelectedItemId(target);
            }
        });

        // Tab inicial
        bottom.setSelectedItemId(R.id.tab_home);
    }

    private void setCurrent(int index) {
        if (pager.getCurrentItem() != index) {
            pager.setCurrentItem(index, true);
        }
    }
}
