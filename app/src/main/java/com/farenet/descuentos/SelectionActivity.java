package com.farenet.descuentos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.farenet.descuentos.config.Constante;
import com.farenet.descuentos.fragment.FragmentDescuento;
import com.farenet.descuentos.models.newapi.UsuarioPerfil;
import com.farenet.descuentos.repository.SessionManager;
import com.farenet.descuentos.sql.QueryRealm;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

public class SelectionActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private MaterialToolbar toolbar;

    private SessionManager session;
    private SharedPreferences legacyPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        session = new SessionManager(this);
        legacyPrefs = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);

        // Si no hay sesión (ni legacy ni nueva), vuelve a Login
        String legacyToken = legacyPrefs.getString("token", null);
        UsuarioPerfil up = session.getPerfil();
        if ((legacyToken == null || legacyToken.isEmpty())
                && (up == null || up.username == null || up.username.isEmpty())) {
            goToLogin();
            return;
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navView.setNavigationItemSelectedListener(this);

        // Poblar header de forma segura
        if (navView.getHeaderCount() > 0) {
            View header = navView.getHeaderView(0);
            TextView tvName = header.findViewById(R.id.tvHeaderName);
            TextView tvRole = header.findViewById(R.id.tvHeaderRole);
            if (up != null) {
                if (tvName != null) tvName.setText(up.username != null ? up.username : "Usuario");
                if (tvRole != null) tvRole.setText(up.perfilId != null ? up.perfilId : "Perfil");
            }
        }

        // (OPCIONAL) Card "Descuentos" en el dashboard
        View cardDescuentos = findViewById(R.id.card_descuentos);
        if (cardDescuentos != null) {
            cardDescuentos.setOnClickListener(v ->
                    startActivity(new Intent(this, DescuentoActivity.class)));
        }

        // Card "Cortesías" en el dashboard
        View cardCortesias = findViewById(R.id.card_cortesias);
        if (cardCortesias != null) {
            cardCortesias.setOnClickListener(v ->
                    startActivity(new Intent(this, CortesiaActivity.class)));
        }


        // Control de visibilidad por perfil
        aplicarVisibilidadPorPerfil();
    }

    @Override
    protected void onResume() {
        super.onResume();
        aplicarVisibilidadPorPerfil();
    }

    private void aplicarVisibilidadPorPerfil() {
        if (navView == null || navView.getMenu() == null) return;
        Menu menu = navView.getMenu();

        // Si no hay perfil nuevo, muestra todo (fallback para login legacy)
        UsuarioPerfil up = session.getPerfil();
        if (up == null || up.perfilId == null || up.perfilId.trim().isEmpty()) {
            setAllVisible(menu, true);
            return;
        }

        String perfilId = up.perfilId.toLowerCase();

        boolean puedeDescuentoCortesia = any(perfilId, "sistemas","administrador","ventas","supervisor");
        boolean puedeDescuento = any(perfilId, "sistemas","administrador","ventas","supervisor");
        boolean puedeReportes         = any(perfilId, "sistemas","administrador","reportes");
        boolean puedeDatosVehiculares = any(perfilId, "sistemas","administrador","soporte","datos");
        boolean puedePlantas          = any(perfilId, "sistemas","administrador","planta");

        setVisible(menu, R.id.nav_descuento, puedeDescuento);
        setVisible(menu, R.id.nav_cortesia, puedeDescuentoCortesia);
        setVisible(menu, R.id.nav_reportes,           puedeReportes);
        setVisible(menu, R.id.nav_datos_vehiculares,  puedeDatosVehiculares);
        setVisible(menu, R.id.nav_plantas,            puedePlantas);
        setVisible(menu, R.id.nav_soporte,            true); // siempre visible
        setVisible(menu, R.id.nav_logout,             true);
    }

    private void setAllVisible(Menu menu, boolean visible) {
        int size = menu.size();
        for (int i = 0; i < size; i++) menu.getItem(i).setVisible(visible);
    }

    private void setVisible(Menu menu, int id, boolean visible) {
        MenuItem it = menu.findItem(id);
        if (it != null) it.setVisible(visible);
    }

    private boolean any(String perfilId, String... allowed) {
        if (perfilId == null) return false;
        for (String a : allowed) {
            if (perfilId.equalsIgnoreCase(a)) return true;
        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();

        if (id == R.id.nav_soporte) {
            showInfo("Soporte", "Abrir herramientas de soporte.");
        }
        // ✅ Abrir la pantalla de Descuentos (ya no MainActivity)
        else if (id == R.id.nav_descuento /* o nav_descuentos si lo tienes separado */) {
            startActivity(new Intent(this, DescuentoActivity.class));
        }
        else if (id == R.id.nav_cortesia /* o nav_descuentos si lo tienes separado */) {
            startActivity(new Intent(this, CortesiaActivity.class));
        }
        else if (id == R.id.nav_reportes) {
            showInfo("Reportes", "Abrir módulo de reportes.");
        } else if (id == R.id.nav_datos_vehiculares) {
            showInfo("Datos vehiculares", "Abrir módulo de datos vehiculares.");
        } else if (id == R.id.nav_plantas) {
            showInfo("Plantas", "Abrir administración de plantas.");
        } else if (id == R.id.nav_logout) {
            confirmarLogout();
        }

        drawerLayout.closeDrawers();
        return true;
    }

    private void showInfo(String title, String msg) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    private void confirmarLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Desea cerrar sesión y borrar el caché local?")
                .setPositiveButton("Sí", (d, w) -> cerrarSesionYBorrarCache())
                .setNegativeButton("No", null)
                .show();
    }

    private void cerrarSesionYBorrarCache() {
        legacyPrefs.edit().clear().apply();
        session.clear();
        QueryRealm.wipeAllAsync(new QueryRealm.TxCallback() {
            @Override public void onSuccess() { goToLogin(); }
            @Override public void onError(Throwable error) { goToLogin(); }
        });
    }

    private void goToLogin() {
        Intent i = new Intent(SelectionActivity.this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
