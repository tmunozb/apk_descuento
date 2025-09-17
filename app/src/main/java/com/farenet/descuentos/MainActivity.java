package com.farenet.descuentos;

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

import com.farenet.descuentos.adapter.FragmentPageAdapter;
import com.farenet.descuentos.config.Constante;
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.fragment.FragmentCortesia;
import com.farenet.descuentos.fragment.FragmentDescuento;
import com.farenet.descuentos.repository.LoginRepository;
import com.farenet.descuentos.repository.MaestroRepository;
import com.farenet.descuentos.sql.QueryRealm;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Author by Alexis Pumayalla on 28/08/19.
 * Email apumayallag@gmail.com
 * Phone 961778965
 */
public class MainActivity extends AppCompatActivity {

    private static final int GROUP_PLANTAS = 1001;

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private LoginRepository loginRepository;
    private MaestroRepository maestroRepository;
    private SharedPreferences sharedPreferences;

    private List<Planta> plantas = new ArrayList<>();
    private String plantaSeleccionadaId; // persistimos aquí la selección actual

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginRepository = Constante.getLoginRespository();
        maestroRepository = Constante.getMaestroRespository();
        sharedPreferences = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);

        // 1) Toolbar como ActionBar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.app_name));
            // Subtítulo opcional con usuario/planta
            String plantaNombre = sharedPreferences.getString("planta_nombre", null);
            if (plantaNombre != null) {
                getSupportActionBar().setSubtitle("Planta: " + plantaNombre);
            }
        }

        // 2) Tabs + ViewPager
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        setupViewPager();
        tabLayout.setupWithViewPager(viewPager);
        setupTabIcons();

        // 3) Cargar plantas desde Realm (unmanaged)
        plantas = QueryRealm.copyAllPlantas();
        plantaSeleccionadaId = sharedPreferences.getString("planta_id", null);

        // 4) Listener visual de tabs (igual que tu código original)
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    View v = tabLayout.getTabAt(0).getCustomView();
                    TextView txt = v.findViewById(R.id.txt);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        txt.setTextColor(getColor(R.color.selected));
                        txt.setTypeface(null, Typeface.BOLD);
                    }
                    tabLayout.getTabAt(0).setCustomView(v);
                } else if (tab.getPosition() == 1) {
                    View v = tabLayout.getTabAt(1).getCustomView();
                    TextView txt = v.findViewById(R.id.txt);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        txt.setTextColor(getColor(R.color.selected));
                        txt.setTypeface(null, Typeface.BOLD);
                    }
                    tabLayout.getTabAt(1).setCustomView(v);
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    View v = tabLayout.getTabAt(0).getCustomView();
                    TextView txt = v.findViewById(R.id.txt);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        txt.setTextColor(getColor(R.color.unselected));
                        txt.setTypeface(null, Typeface.NORMAL);
                    }
                    tabLayout.getTabAt(0).setCustomView(v);
                } else if (tab.getPosition() == 1) {
                    View v = tabLayout.getTabAt(1).getCustomView();
                    TextView txt = v.findViewById(R.id.txt);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        txt.setTextColor(getColor(R.color.unselected));
                        txt.setTypeface(null, Typeface.NORMAL);
                    }
                    tabLayout.getTabAt(1).setCustomView(v);
                }
            }

            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    private void setupViewPager() {
        FragmentPageAdapter pagerAdapter = new FragmentPageAdapter(getSupportFragmentManager());
        pagerAdapter.addFragment(new FragmentDescuento(), "Descuento");
        pagerAdapter.addFragment(new FragmentCortesia(), "Cortesia");
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

    // ===== Menú =====

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);

        // Configurar SearchView
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView sv = (SearchView) searchItem.getActionView();
        if (sv != null) {
            sv.setQueryHint("Buscar…");
            sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String query) {
                    // TODO: aplica la búsqueda (por placa, autorizador, etc.)
                    Toast.makeText(MainActivity.this, "Buscar: " + query, Toast.LENGTH_SHORT).show();
                    return true;
                }
                @Override public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
        }
        return true;
    }

    // Llenamos el submenú “Cambiar planta” dinámicamente
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem parent = menu.findItem(R.id.action_change_plant_parent);
        if (parent != null) {
            SubMenu sub = parent.getSubMenu();
            if (sub != null) {
                sub.clear(); // evita duplicados
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

        // Acciones fijas
        if (id == R.id.action_notifications) {
            // TODO: abre Activity/Fragment de notificaciones
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


        // Ítems del grupo dinámico “Cambiar planta”
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

                // TODO: notificar a fragments si filtran por planta (SharedViewModel / callback)
                Toast.makeText(this, "Planta: " + seleccion.getNombre(), Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void cerrarSesionYBorrarCache() {
        // 1) Deshabilita interacciones si quieres (opcional)
        // 2) Limpia SharedPreferences
        sharedPreferences.edit().clear().apply();

        // 3) Limpia Realm (caché de maestros, etc.)
        com.farenet.descuentos.sql.QueryRealm.wipeAllAsync(new com.farenet.descuentos.sql.QueryRealm.TxCallback() {
            @Override public void onSuccess() {
                // 4) Navega a Login y limpia backstack
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
            @Override public void onError(Throwable error) {
                // Si algo va mal limpiando, igual permite salir para no bloquear al usuario
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
