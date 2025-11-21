package com.farenet.descuentos.ui.sistemashost;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
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
import com.farenet.descuentos.ui.bolsa.BolsaBulkActivity;
import com.farenet.descuentos.ui.sistemashost.adapter.SistemasTabsAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class SistemasHostActivity extends AppCompatActivity {

    private ViewPager2 pager;
    private BottomNavigationView bottom;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sistemas_host);

        session = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pager  = findViewById(R.id.pager);
        bottom = findViewById(R.id.bottomNav);

        // Adapter del host (AHORA: 4 páginas -> Home, Descuento, Bolsa, Historial)
        pager.setAdapter(new SistemasTabsAdapter(this));
        pager.setOffscreenPageLimit(3);
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
            } else if (id == R.id.tab_Descuento) {
                setCurrent(1);
                return true;
            } else if (id == R.id.tab_Bolsa) {
                setCurrent(2);
                return true;
            } else if (id == R.id.tab_solicitudes) {
                setCurrent(3);
                return true;
            } else if (id == R.id.tab_mas) {
                showMasSheet(); // Cortesías y Bolsa Masivo aquí
                return false;   // No “selecciones” el tab Más
            }
            return false;
        });

        // Pager -> BottomNav
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                int target = position == 0 ? R.id.tab_home
                        : position == 1 ? R.id.tab_Descuento
                        : position == 2 ? R.id.tab_Bolsa
                        : R.id.tab_solicitudes;
                if (bottom.getSelectedItemId() != target) {
                    bottom.setSelectedItemId(target);
                }
            }
        });

        // Tab inicial
        bottom.setSelectedItemId(R.id.tab_home);
    }

    private void showMasSheet() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this).inflate(R.layout.bs_mas, null, false);
        bs.setContentView(v);

        // Cortesías (Activity actual de tu app)
        v.findViewById(R.id.btnCortesias).setOnClickListener(x -> {
            bs.dismiss();
            startActivity(new Intent(this, com.farenet.descuentos.ui.solicitudes.cortesia.CortesiaActivity.class));
        });

        // Bolsa Masivo (sólo visible para 'sistemas' si quieres)
        View btnMasivo = v.findViewById(R.id.btnBolsaMasivo);
        boolean isSistemas = false;
        try {
            String pid = session.getPerfil() != null ? session.getPerfil().perfilId : null;
            isSistemas = pid != null && pid.equalsIgnoreCase("sistemas");
        } catch (Exception ignore) {}
        btnMasivo.setVisibility(isSistemas ? View.VISIBLE : View.GONE);

        btnMasivo.setOnClickListener(x -> {
            bs.dismiss();
            startActivity(new Intent(this, BolsaBulkActivity.class));
        });

        bs.show();
    }

    private void setCurrent(int index) {
        if (pager.getCurrentItem() != index) {
            pager.setCurrentItem(index, true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_sistemas, menu);
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

    public void goToPendingApprovals() {
        if (bottom != null) {
            bottom.setSelectedItemId(R.id.tab_solicitudes);
        } else if (pager != null) {
            pager.setCurrentItem(3, true);
        }
    }
}
