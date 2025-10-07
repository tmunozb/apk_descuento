// ui/selection/base/BaseSelectionActivity.java
package com.farenet.descuentos.ui.selection.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.farenet.descuentos.Core.Storage.SessionManager;
import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.auth.LoginActivity;
import com.farenet.descuentos.ui.bolsa.BolsaBulkActivity;
import com.farenet.descuentos.ui.bolsa.BolsaEstadoActivity;
import com.farenet.descuentos.ui.solicitudes.cortesia.CortesiaActivity;
import com.farenet.descuentos.ui.solicitudes.crear.SolicitudCrearActivity;
import com.farenet.descuentos.ui.solicitudes.descuento.DescuentoActivity;
import com.farenet.descuentos.ui.solicitudes.historial.HistorialSolicitudesActivity;
import com.farenet.descuentos.ui.solicitudes.lista.SolicitudesListaActivity;
import com.farenet.descuentos.ui.solicitudes.pendientes.SolicitudesPendientesActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

public abstract class BaseSelectionActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;          // opcional
    protected NavigationView navView;             // opcional
    protected MaterialToolbar toolbar;            // recomendado
    protected SessionManager session;

    // Views del layout “clásico” (si no existen, quedan null y no pasa nada)
    protected TextView tvWelcome;
    protected View cardNuevaSolicitud, cardMisSolicitudes, cardPendientesAprobar, cardHistorial;
    protected View cardDescuentos, cardCortesias, cardReportes, cardDatos, cardFeed;
    protected View cardBolsasEstado, cardBolsasBulk;

    // ----------------- Config -----------------
    public static class Config {
        @MenuRes   public int menuRes   = 0;                           // menú del Drawer o Toolbar
        @LayoutRes public int layoutRes = R.layout.activity_selection;  // layout por defecto

        // Visibilidad de cards (solo si el layout las define)
        public boolean vNueva=false, vMis=false, vPend=false, vHist=false;
        public boolean vDesc=false, vCort=false, vRep=false, vDatos=false, vFeed=false;
        public boolean vBolsaEstado=false, vBolsaBulk=false;

        // Habilitar listeners (solo si las views existen)
        public boolean canNueva=false, canMis=false, canPend=false, canHist=false;
        public boolean canDesc=false, canCort=false, canBolsaEstado=false, canBolsaBulk=false;
    }

    /** Devuelve la configuración específica del perfil (layout, menú, flags). */
    protected abstract Config provideConfig();

    /** Hook opcional: los hijos pueden configurar tiles/bottom nav, etc. */
    protected void onAfterViewsBound(Config c) { /* no-op */ }

    // BaseSelectionActivity.java  (añade)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Config cfg = provideConfig();
        setContentView(cfg.layoutRes);

        session = new SessionManager(getApplicationContext());
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView      = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);


        if (navView != null) {
            navView.getMenu().clear();
            if (cfg.menuRes != 0) navView.inflateMenu(cfg.menuRes);
            navView.setNavigationItemSelectedListener(this);
            if (drawerLayout != null && toolbar != null) {
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        this, drawerLayout, toolbar,
                        R.string.navigation_drawer_open,
                        R.string.navigation_drawer_close
                );
                drawerLayout.addDrawerListener(toggle);
                toggle.syncState();
            }
        } else if (toolbar != null && cfg.menuRes != 0) {
            // Evita doble inflate si ya pusiste app:menu en el XML (ver nota en sección 3)
            // toolbar.getMenu().clear();
            // toolbar.inflateMenu(cfg.menuRes);
            toolbar.setOnMenuItemClickListener(this::onToolbarMenuItemClick);
        }

        bindDashboardViews();
        applyConfig(cfg);

        // ⬇️ NUEVO: bootstrap de perfil/accesos
        bootstrapPerfilYAccesos(this::onAfterViewsBound, cfg);
    }

    private interface AfterBind { void run(Config c); }

    private void bootstrapPerfilYAccesos(AfterBind cont, Config cfg) {
        boolean perfilVacio   = session.getPerfil() == null || session.getPerfil().perfilId == null || session.getPerfil().perfilId.trim().isEmpty();
        boolean accesosVacios = session.getAccesos() == null || session.getAccesos().isEmpty();

        if (!perfilVacio && !accesosVacios) {
            cont.run(cfg); // ya todo cargado
            return;
        }

        String username = session.getUsername();
        if (username == null || username.trim().isEmpty()) {
            cont.run(cfg); // sin usuario, continúa igual (pantalla mostrará mínimo)
            return;
        }

        com.farenet.descuentos.Core.Network.NewApiClient.get()
                .loginPerfilesGet(username)
                .enqueue(new retrofit2.Callback<com.farenet.descuentos.API.Actual.DTO.auth.LoginRsp>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.farenet.descuentos.API.Actual.DTO.auth.LoginRsp> call,
                                           retrofit2.Response<com.farenet.descuentos.API.Actual.DTO.auth.LoginRsp> resp) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            var body = resp.body();

                            var p = new com.farenet.descuentos.API.Actual.DTO.auth.UsuarioPerfil();
                            p.username     = body.username;
                            p.perfilId     = body.perfilId;
                            p.estado       = body.estado;
                            p.nroDocumento = body.nroDocumento;
                            p.nombres      = body.nombres;
                            p.apellidos    = body.apellidos;

                            session.saveUsername(body.username);
                            session.savePerfil(p);
                            if (body.accesos != null && !body.accesos.isEmpty()) {
                                session.saveAccesos(body.accesos);
                            }
                        }
                        cont.run(cfg);
                    }
                    @Override
                    public void onFailure(retrofit2.Call<com.farenet.descuentos.API.Actual.DTO.auth.LoginRsp> call, Throwable t) {
                        cont.run(cfg); // continúa aunque falle; la UI no se bloquea
                    }
                });
    }


    // Permite reutilizar el mismo handler para el menú del Toolbar
    private boolean onToolbarMenuItemClick(MenuItem item) {
        return onNavigationItemSelected(item);
    }

    private void bindDashboardViews() {
        tvWelcome            = findViewById(R.id.tv_welcome);
        cardNuevaSolicitud   = findViewById(R.id.card_nueva_solicitud);
        cardMisSolicitudes   = findViewById(R.id.card_mis_solicitudes);
        cardPendientesAprobar= findViewById(R.id.card_pendientes_aprobar);
        cardHistorial        = findViewById(R.id.card_historial);
        cardDescuentos       = findViewById(R.id.card_descuentos);
        cardCortesias        = findViewById(R.id.card_cortesias);
        cardReportes         = findViewById(R.id.card_reportes);
        cardDatos            = findViewById(R.id.card_datos);
        cardFeed             = findViewById(R.id.card_feed);
        cardBolsasEstado     = findViewById(R.id.card_bolsas_estado);
        cardBolsasBulk       = findViewById(R.id.card_bolsas_bulk);
    }

    private static void setVisible(View v, boolean visible) {
        if (v != null) v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    protected void applyConfig(Config c) {
        String nombre = session.getNombreVisible();
        if (tvWelcome != null) {
            tvWelcome.setText("¡Hola, " + (nombre == null ? "Usuario" : nombre) + "!");
        }

        // Visibilidad (si existen)
        setVisible(cardNuevaSolicitud,   c.vNueva);
        setVisible(cardMisSolicitudes,   c.vMis);
        setVisible(cardPendientesAprobar,c.vPend);
        setVisible(cardHistorial,        c.vHist);
        setVisible(cardDescuentos,       c.vDesc);
        setVisible(cardCortesias,        c.vCort);
        setVisible(cardReportes,         c.vRep);
        setVisible(cardDatos,            c.vDatos);
        setVisible(cardFeed,             c.vFeed);
        setVisible(cardBolsasEstado,     c.vBolsaEstado);
        setVisible(cardBolsasBulk,       c.vBolsaBulk);

        // Listeners (si existen)
        if (c.canNueva && cardNuevaSolicitud != null)
            cardNuevaSolicitud.setOnClickListener(v ->
                    startActivity(new Intent(this, SolicitudCrearActivity.class)));

        if (c.canMis && cardMisSolicitudes != null)
            cardMisSolicitudes.setOnClickListener(v ->
                    startActivity(new Intent(this, SolicitudesListaActivity.class)));

        if (c.canPend && cardPendientesAprobar != null)
            cardPendientesAprobar.setOnClickListener(v ->
                    startActivity(new Intent(this, SolicitudesPendientesActivity.class)));

        if (c.canHist && cardHistorial != null)
            cardHistorial.setOnClickListener(v ->
                    startActivity(new Intent(this, HistorialSolicitudesActivity.class)));

        if (c.canDesc && cardDescuentos != null)
            cardDescuentos.setOnClickListener(v ->
                    startActivity(new Intent(this, DescuentoActivity.class)));

        if (c.canCort && cardCortesias != null)
            cardCortesias.setOnClickListener(v ->
                    startActivity(new Intent(this, CortesiaActivity.class)));

        if (c.canBolsaEstado && cardBolsasEstado != null)
            cardBolsasEstado.setOnClickListener(v ->
                    startActivity(new Intent(this, BolsaEstadoActivity.class)));

        if (c.canBolsaBulk && cardBolsasBulk != null)
            cardBolsasBulk.setOnClickListener(v ->
                    startActivity(new Intent(this, BolsaBulkActivity.class)));
    }

    // Handler común de opciones de Drawer/Toolbar
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_descuento) {
            startActivity(new Intent(this, DescuentoActivity.class));
        } else if (id == R.id.nav_cortesia) {
            startActivity(new Intent(this, CortesiaActivity.class));
        } else if (id == R.id.nav_reportes) {
            new AlertDialog.Builder(this)
                    .setTitle("Reportes")
                    .setMessage("Abrir módulo de reportes.")
                    .setPositiveButton("OK", null)
                    .show();
        } else if (id == R.id.nav_datos_vehiculares) {
            new AlertDialog.Builder(this)
                    .setTitle("Datos vehiculares")
                    .setMessage("Abrir módulo de datos vehiculares.")
                    .setPositiveButton("OK", null)
                    .show();
        } else if (id == R.id.nav_logout) {
            new AlertDialog.Builder(this)
                    .setTitle("Cerrar sesión")
                    .setMessage("¿Desea cerrar sesión y borrar el caché local?")
                    .setPositiveButton("Sí", (d, w) -> {
                        session.clear();
                        startActivity(new Intent(this, LoginActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        | Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        }

        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // UX: cerrar Drawer con back
    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
