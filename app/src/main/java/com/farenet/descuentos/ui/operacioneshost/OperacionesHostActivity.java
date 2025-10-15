package com.farenet.descuentos.ui.operacioneshost;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.farenet.descuentos.Core.Storage.SessionManager;
import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.auth.LoginActivity;
import com.farenet.descuentos.ui.operacioneshost.adapter.OperacionesTabsAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class OperacionesHostActivity extends AppCompatActivity {

    private ViewPager2 pager;
    private BottomNavigationView bottom;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operaciones_host);

        session = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pager = findViewById(R.id.pager);
        bottom = findViewById(R.id.bottomNav);

        // ✅ Usa el adapter propio de Operaciones (2 tabs)
        pager.setAdapter(new OperacionesTabsAdapter(this));
        pager.setOffscreenPageLimit(1);
        pager.setUserInputEnabled(true);
        pager.setPageTransformer(null);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_operaciones, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_logout) {
            session.clear();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setCurrent(int index) {
        if (pager.getCurrentItem() != index) {
            pager.setCurrentItem(index, true);
        }
    }

    public void goToPendingApprovals() {
        if (bottom != null) {
            bottom.setSelectedItemId(R.id.tab_solicitudes); // índice 1
        } else if (pager != null) {
            pager.setCurrentItem(1, true);
        }
    }
}
