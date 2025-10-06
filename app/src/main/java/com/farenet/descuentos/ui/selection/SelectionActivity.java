package com.farenet.descuentos.ui.selection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

import com.farenet.descuentos.Core.config.Constante;
import com.farenet.descuentos.API.Actual.DTO.maestros.AccesoPlantaDto;
import com.farenet.descuentos.API.Actual.DTO.auth.LoginRsp;
import com.farenet.descuentos.API.Actual.DTO.auth.UsuarioPerfil;
import com.farenet.descuentos.Core.Network.NewApiClient;
import com.farenet.descuentos.Core.Storage.SessionManager;
import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.solicitudes.crear.SolicitudCrearActivity;
import com.farenet.descuentos.ui.solicitudes.lista.SolicitudesListaActivity;
import com.farenet.descuentos.ui.solicitudes.pendientes.SolicitudesPendientesActivity;
import com.farenet.descuentos.data.local.realm.dao.QueryRealm;
import com.farenet.descuentos.ui.auth.LoginActivity;
import com.farenet.descuentos.ui.bolsa.BolsaBulkActivity;
import com.farenet.descuentos.ui.bolsa.BolsaEstadoActivity; // TODO: implementar pantalla
import com.farenet.descuentos.ui.solicitudes.cortesia.CortesiaActivity;
import com.farenet.descuentos.ui.solicitudes.descuento.DescuentoActivity;
import com.farenet.descuentos.ui.solicitudes.historial.HistorialSolicitudesActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectionActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "SESSION_TEST";

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private MaterialToolbar toolbar;

    private SessionManager session;
    private SharedPreferences legacyPrefs;

    // Dashboard
    private TextView tvWelcome;
    private View cardNuevaSolicitud, cardMisSolicitudes, cardPendientesAprobar, cardHistorial;
    private View cardDescuentos, cardCortesias, cardReportes, cardDatos, cardFeed;

    // NUEVAS: Bolsas
    private View cardBolsasEstado, cardBolsasBulk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        session = new SessionManager(getApplicationContext());
        legacyPrefs = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);

        String legacyToken = legacyPrefs.getString("token", null);
        UsuarioPerfil up = session.getPerfil();
        if ((legacyToken == null || legacyToken.isEmpty())
                && (up == null || up.username == null || up.username.isEmpty())) {
            goToLogin();
            return;
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        // refs dashboard
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

        // NUEVAS bolsAs
        cardBolsasEstado     = findViewById(R.id.card_bolsas_estado);
        cardBolsasBulk       = findViewById(R.id.card_bolsas_bulk);

        // Listeners
        if (cardNuevaSolicitud != null) {
            cardNuevaSolicitud.setOnClickListener(v -> {
                if (tienePerfil("sistemas") || tienePerfil("administrador")) {
                    startActivity(new Intent(this, SolicitudCrearActivity.class));
                } else {
                    showInfo("Acceso restringido", "Solo Administrador o Sistemas pueden crear solicitudes.");
                }
            });
        }
        if (cardMisSolicitudes != null) {
            cardMisSolicitudes.setOnClickListener(v ->
                    startActivity(new Intent(this, SolicitudesListaActivity.class))
            );
        }
        if (cardPendientesAprobar != null) {
            cardPendientesAprobar.setOnClickListener(v -> {
                if (tienePerfil("operaciones") || tienePerfil("sistemas")
                        || tienePerfil("administrador") || tienePerfil("comercial")
                        || tienePerfil("mecanico") || tienePerfil("asistente_servicio")) {
                    startActivity(new Intent(this, SolicitudesPendientesActivity.class));
                } else {
                    showInfo("Acceso restringido", "No tienes permiso para aprobar.");
                }
            });
        }
        if (cardHistorial != null) {
            cardHistorial.setOnClickListener(v -> {
                if (tienePerfil("sistemas") || tienePerfil("comercial")) {
                    startActivity(new Intent(this, HistorialSolicitudesActivity.class));
                } else {
                    showInfo("Acceso restringido", "Solo Sistemas o Comercial.");
                }
            });
        }
        if (cardDescuentos != null) {
            cardDescuentos.setOnClickListener(v -> {
                if (tienePerfil("sistemas")) {
                    startActivity(new Intent(this, DescuentoActivity.class));
                } else {
                    showInfo("Acceso restringido", "Solo Sistemas gestiona descuentos directamente.");
                }
            });
        }
        if (cardCortesias != null) {
            cardCortesias.setOnClickListener(v -> {
                if (tienePerfil("operaciones") || tienePerfil("sistemas") || tienePerfil("administrador")) {
                    startActivity(new Intent(this, CortesiaActivity.class));
                } else {
                    showInfo("Acceso restringido", "No tienes permisos para cortesías.");
                }
            });
        }

        // NUEVOS listeners (SOLO SISTEMAS)
        if (cardBolsasEstado != null) {
            cardBolsasEstado.setOnClickListener(v -> {
                if (tienePerfil("sistemas")) {
                    startActivity(new Intent(this, BolsaEstadoActivity.class)); // la implementamos después
                } else {
                    showInfo("Acceso restringido", "Solo Sistemas puede ver el estado de bolsas.");
                }
            });
        }
        if (cardBolsasBulk != null) {
            cardBolsasBulk.setOnClickListener(v -> {
                if (tienePerfil("sistemas")) {
                    startActivity(new Intent(this, BolsaBulkActivity.class));
                } else {
                    showInfo("Acceso restringido", "Solo Sistemas puede gestionar carga masiva de bolsas.");
                }
            });
        }

        updateHeader();
        aplicarVisibilidadPorPerfil();
        aplicarSaludoYVisibilidadDeCards();

        if (needsBootstrap(up)) {
            tryBootstrapPerfilPorServicios();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        aplicarVisibilidadPorPerfil();
        aplicarSaludoYVisibilidadDeCards();
    }

    /** Saludo y visibilidad de cards según perfil */
    private void aplicarSaludoYVisibilidadDeCards() {
        UsuarioPerfil up = session.getPerfil();
        String nombre = (up != null) ? up.getNombreCompleto() : session.getUsername();
        if (tvWelcome != null) {
            tvWelcome.setText("¡Hola, " + (nombre != null && !nombre.isEmpty() ? nombre : "Usuario") + "!");
        }

        boolean isSistemas     = tienePerfil("sistemas");
        boolean isAdmin        = tienePerfil("administrador");
        boolean isOperaciones  = tienePerfil("operaciones");
        boolean isComercial    = tienePerfil("comercial");
        boolean isMecanico     = tienePerfil("mecanico");
        boolean isAsistente    = tienePerfil("asistente_servicio");

        setVisible(cardNuevaSolicitud,        isSistemas);
        setVisible(cardMisSolicitudes,        isSistemas);
        setVisible(cardHistorial,             isSistemas || isComercial);
        setVisible(cardPendientesAprobar,     isSistemas || isOperaciones || isAdmin || isComercial || isMecanico || isAsistente);
        setVisible(cardDescuentos,            isSistemas);
        setVisible(cardCortesias,             isSistemas || isOperaciones || isAdmin);
        setVisible(cardReportes,              isSistemas);
        setVisible(cardDatos,                 isSistemas);
        setVisible(cardFeed,                  isSistemas);

        // NUEVOS (solo sistemas)
        setVisible(cardBolsasEstado,          isSistemas);
        setVisible(cardBolsasBulk,            isSistemas);
    }

    private boolean tienePerfil(String perfilEsperado) {
        UsuarioPerfil up = session.getPerfil();
        return up != null && up.perfilId != null
                && up.perfilId.trim().equalsIgnoreCase(perfilEsperado);
    }

    private void setVisible(View v, boolean visible) {
        if (v != null) v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateHeader() {
        if (navView == null || navView.getHeaderCount() == 0) return;

        View header = navView.getHeaderView(0);
        TextView tvName = header.findViewById(R.id.tvHeaderName);
        TextView tvRole = header.findViewById(R.id.tvHeaderRole);

        UsuarioPerfil up = session.getPerfil();
        String nombre = (up != null) ? up.getNombreCompleto() : session.getUsername();
        String perfil = (up != null && up.perfilId != null) ? up.perfilId : "Perfil";

        if (tvName != null) tvName.setText(nombre != null && !nombre.isEmpty() ? nombre : "Usuario");
        if (tvRole != null) tvRole.setText(perfil);

        List<AccesoPlantaDto> acc = session.getAccesos();
        Log.d(TAG, "updateHeader() -> accesos=" + (acc != null ? acc.size() : 0));
    }

    private String lastPerfilIdShown;
    private void aplicarVisibilidadPorPerfil() {
        if (navView == null || navView.getMenu() == null) return;
        Menu menu = navView.getMenu();

        UsuarioPerfil up = session.getPerfil();
        String perfilId = (up != null && up.perfilId != null) ? up.perfilId.trim().toLowerCase() : "";
        if (perfilId.equals(lastPerfilIdShown)) return;

        boolean puedeDescuento         = any(perfilId, "sistemas");
        boolean puedeCortesia          = any(perfilId, "sistemas","administrador","operaciones");
        boolean puedeReportes          = any(perfilId, "sistemas");
        boolean puedeDatosVehiculares  = any(perfilId, "sistemas");

        setVisible(menu, R.id.nav_descuento,         puedeDescuento);
        setVisible(menu, R.id.nav_cortesia,          puedeCortesia);
        setVisible(menu, R.id.nav_reportes,          puedeReportes);
        setVisible(menu, R.id.nav_datos_vehiculares, puedeDatosVehiculares);
        setVisible(menu, R.id.nav_soporte,           puedeDatosVehiculares);
        setVisible(menu, R.id.nav_logout,            true);
        lastPerfilIdShown = perfilId;
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

    // ====== Bootstrap (perfil/accesos) ======
    private boolean needsBootstrap(UsuarioPerfil up) {
        boolean perfilVacio   = (up == null || up.perfilId == null || up.perfilId.trim().isEmpty());
        boolean accesosVacios = (session.getAccesos() == null || session.getAccesos().isEmpty());
        return perfilVacio || accesosVacios;
    }

    private String resolveUsername() {
        String u = session.getUsername();
        if (u != null && !u.trim().isEmpty()) return u.trim();
        if (legacyPrefs != null) {
            String legacyUser = legacyPrefs.getString("user", null);
            if (legacyUser != null && !legacyUser.trim().isEmpty()) return legacyUser.trim();
        }
        UsuarioPerfil up = session.getPerfil();
        if (up != null && up.username != null && !up.username.trim().isEmpty()) return up.username.trim();
        return null;
    }

    private void tryBootstrapPerfilPorServicios() {
        final String user = resolveUsername();
        if (user == null) return;

        NewApiClient.get().loginPerfilesGet(user).enqueue(new Callback<LoginRsp>() {
            @Override
            public void onResponse(Call<LoginRsp> call, Response<LoginRsp> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                LoginRsp body = response.body();
                session.saveUsername(body.username);

                UsuarioPerfil p = new UsuarioPerfil();
                p.username     = body.username;
                p.perfilId     = body.perfilId;
                p.estado       = body.estado;
                p.nroDocumento = body.nroDocumento;
                p.nombres      = body.nombres;
                p.apellidos    = body.apellidos;
                session.savePerfil(p);

                if (body.accesos != null && !body.accesos.isEmpty()) {
                    session.saveAccesos(body.accesos);
                }

                updateHeader();
                aplicarVisibilidadPorPerfil();
                aplicarSaludoYVisibilidadDeCards();
            }

            @Override
            public void onFailure(Call<LoginRsp> call, Throwable t) {
                Log.e(TAG, "login_perfiles(GET) error", t);
            }
        });
    }

    // ====== Drawer ======
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.nav_soporte) {
            showInfo("Soporte", "Abrir herramientas de soporte.");
        } else if (id == R.id.nav_descuento) {
            startActivity(new Intent(this, DescuentoActivity.class));
        } else if (id == R.id.nav_cortesia) {
            startActivity(new Intent(this, CortesiaActivity.class));
        } else if (id == R.id.nav_reportes) {
            showInfo("Reportes", "Abrir módulo de reportes.");
        } else if (id == R.id.nav_datos_vehiculares) {
            showInfo("Datos vehiculares", "Abrir módulo de datos vehiculares.");
        } else if (id == R.id.nav_logout) {
            confirmarLogout();
        }
        if (drawerLayout != null) drawerLayout.closeDrawers();
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
