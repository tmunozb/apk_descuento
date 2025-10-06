package com.farenet.descuentos.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.viewpager.widget.ViewPager;

import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.auth.LoginActivity;
import com.farenet.descuentos.ui.common.adapter.FragmentPageAdapter;
import com.farenet.descuentos.Core.config.Constante;
import com.farenet.descuentos.data.local.realm.entity.Planta;
import com.farenet.descuentos.ui.solicitudes.crear.FragmentCortesia;
import com.farenet.descuentos.ui.solicitudes.crear.FragmentDescuento;
import com.farenet.descuentos.API.Antigua.Service.LoginRepository;
import com.farenet.descuentos.API.Antigua.Service.MaestroRepository;
import com.farenet.descuentos.data.local.realm.dao.QueryRealm;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int GROUP_PLANTAS = 1001;

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private LoginRepository loginRepository;
    private MaestroRepository maestroRepository;
    private SharedPreferences sharedPreferences;

    private List<Planta> plantas = new ArrayList<>();
    private String plantaSeleccionadaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginRepository = Constante.getLoginRespository();
        maestroRepository = Constante.getMaestroRespository();
        sharedPreferences = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);

        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Tabs + ViewPager
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        setupViewPager();
        tabLayout.setupWithViewPager(viewPager);
        setupTabIcons();
        setupTabSelectionColors();

        // Cargar plantas de Realm (unmanaged)
        plantas = QueryRealm.copyAllPlantas();

        // Cargar selección previa o establecer una por defecto
        plantaSeleccionadaId = sharedPreferences.getString("planta_id", null);
        if (plantaSeleccionadaId == null) {
            if (plantas != null && !plantas.isEmpty()) {
                Planta primera = plantas.get(0);
                plantaSeleccionadaId = primera.getKey();
                sharedPreferences.edit()
                        .putString("planta_id", primera.getKey())
                        .putString("planta_nombre", primera.getNombre())
                        .apply();
            }
        }

        // Título/subtítulo
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.app_name));
            String plantaNombre = sharedPreferences.getString("planta_nombre", null);
            if (plantaNombre != null) {
                getSupportActionBar().setSubtitle("Planta: " + plantaNombre);
            }
        }
    }

    private void setupViewPager() {
        FragmentPageAdapter pagerAdapter = new FragmentPageAdapter(getSupportFragmentManager());
        pagerAdapter.addFragment(new FragmentDescuento(), "Descuento");
        pagerAdapter.addFragment(new FragmentCortesia(), "Cortesía");
        viewPager.setAdapter(pagerAdapter);
    }

    private void setupTabIcons() {
        View v = LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        TextView txt = v.findViewById(R.id.txt);
        txt.setText("Descuentos");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            txt.setTextColor(getColor(R.color.selected));
            txt.setTypeface(null, Typeface.BOLD);
        }
        tabLayout.getTabAt(0).setCustomView(v);

        v = LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        txt = v.findViewById(R.id.txt);
        txt.setText("Cortesía");
        tabLayout.getTabAt(1).setCustomView(v);
    }

    private void setupTabSelectionColors() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                View v = tab.getCustomView();
                if (v == null) return;
                TextView txt = v.findViewById(R.id.txt);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    txt.setTextColor(getColor(R.color.selected));
                    txt.setTypeface(null, Typeface.BOLD);
                }
                tab.setCustomView(v);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {
                View v = tab.getCustomView();
                if (v == null) return;
                TextView txt = v.findViewById(R.id.txt);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    txt.setTextColor(getColor(R.color.unselected));
                    txt.setTypeface(null, Typeface.NORMAL);
                }
                tab.setCustomView(v);
            }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    // ===== Menú =====

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);

        // Search
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView sv = (SearchView) searchItem.getActionView();
        if (sv != null) {
            sv.setQueryHint("Buscar…");
            sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String query) {
                    Toast.makeText(MainActivity.this, "Buscar: " + query, Toast.LENGTH_SHORT).show();
                    return true;
                }
                @Override public boolean onQueryTextChange(String newText) { return false; }
            });
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem parent = menu.findItem(R.id.action_change_plant_parent);
        if (parent != null) {
            SubMenu sub = parent.getSubMenu();
            if (sub != null) {
                sub.clear();
                if (plantas != null && !plantas.isEmpty()) {
                    int order = 0;
                    for (Planta p : plantas) {
                        final String id = p.getKey();
                        final String nombre = p.getNombre();
                        if (nombre == null) continue;

                        int itemId = ("planta_" + (id != null ? id : nombre)).hashCode();
                        MenuItem mi = sub.add(GROUP_PLANTAS, itemId, order++, nombre);
                        mi.setCheckable(true);
                        if (id != null && id.equals(plantaSeleccionadaId)) {
                            mi.setChecked(true);
                        }
                    }
                    sub.setGroupCheckable(GROUP_PLANTAS, true, true);
                } else {
                    sub.add(GROUP_PLANTAS, View.generateViewId(), 0, "Sin plantas");
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();

        if (id == R.id.action_notifications) {
            Toast.makeText(this, "Notificaciones", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_help) {
            new AlertDialog.Builder(this)
                    .setTitle("Ayuda")
                    .setMessage("Escríbenos si necesitas soporte.")
                    .setPositiveButton("OK", null)
                    .show();
            return true;
        } else if (id == R.id.action_logout) {
            new AlertDialog.Builder(this)
                    .setTitle("Cerrar sesión")
                    .setMessage("¿Desea cerrar sesión y borrar el caché local?")
                    .setPositiveButton("Sí", (d, w) -> cerrarSesionYBorrarCache())
                    .setNegativeButton("No", null)
                    .show();
            return true;
        }

        // Cambiar planta
        if (item.getGroupId() == GROUP_PLANTAS) {
            item.setChecked(true);
            String nombrePlanta = item.getTitle().toString();

            Planta seleccion = null;
            for (Planta p : plantas) {
                if (nombrePlanta.equals(p.getNombre())) {
                    seleccion = p;
                    break;
                }
            }
            if (seleccion != null) {
                plantaSeleccionadaId = seleccion.getKey();

                sharedPreferences.edit()
                        .putString("planta_id", seleccion.getKey())
                        .putString("planta_nombre", seleccion.getNombre())
                        .apply();

                if (getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle("Planta: " + seleccion.getNombre());
                }

                // TODO: notificar a fragments si filtran por planta
                Toast.makeText(this, "Planta: " + seleccion.getNombre(), Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void cerrarSesionYBorrarCache() {
        sharedPreferences.edit().clear().apply();

        QueryRealm.wipeAllAsync(new QueryRealm.TxCallback() {
            @Override public void onSuccess() {
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
            @Override public void onError(Throwable error) {
                Toast.makeText(MainActivity.this,
                        "Error limpiando caché: " + (error != null && error.getMessage()!=null ? error.getMessage() : "desconocido"),
                        Toast.LENGTH_LONG).show();

                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
        });
    }
}
