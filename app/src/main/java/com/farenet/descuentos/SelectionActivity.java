package com.farenet.descuentos;

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

import com.farenet.descuentos.config.Constante;
import com.farenet.descuentos.models.newapi.AccesoPlantaDto;
import com.farenet.descuentos.models.newapi.LoginRsp;
import com.farenet.descuentos.models.newapi.UsuarioPerfil;
import com.farenet.descuentos.models.newapi.UsuarioResumenDto;
import com.farenet.descuentos.network.newapi.NewApiClient;
import com.farenet.descuentos.repository.SessionManager;
import com.farenet.descuentos.sql.QueryRealm;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    // refs de UI del dashboard
    private TextView tvWelcome;
    private View cardDescuentos, cardCortesias, cardReportes, cardDatos, cardFeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        session = new SessionManager(getApplicationContext());
        legacyPrefs = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);

        logSession("onCreate()");
        logLegacyToken("onCreate()");

        String legacyToken = legacyPrefs.getString("token", null);
        UsuarioPerfil up = session.getPerfil();
        if ((legacyToken == null || legacyToken.isEmpty())
                && (up == null || up.username == null || up.username.isEmpty())) {
            Log.w(TAG, "No hay sesión válida (legacy ni nueva). Redirigiendo a Login.");
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

        // refs del dashboard
        tvWelcome      = findViewById(R.id.tv_welcome);
        cardDescuentos = findViewById(R.id.card_descuentos);
        cardCortesias  = findViewById(R.id.card_cortesias);
        cardReportes   = findViewById(R.id.card_reportes);
        cardDatos      = findViewById(R.id.card_datos);
        cardFeed       = findViewById(R.id.card_feed);

        // listeners de cards (los habilitamos solo si el perfil aplica)
        if (cardDescuentos != null) {
            cardDescuentos.setOnClickListener(v -> {
                if (tienePerfil("sistemas")) {
                    startActivity(new Intent(this, DescuentoActivity.class));
                }
            });
        }
        if (cardCortesias != null) {
            cardCortesias.setOnClickListener(v -> {
                if (tienePerfil("sistemas")) {
                    startActivity(new Intent(this, CortesiaActivity.class));
                }
            });
        }

        updateHeader();
        aplicarVisibilidadPorPerfil(); // menú lateral
        aplicarSaludoYVisibilidadDeCards(); // saludo + cards del dashboard

        // Si falta perfil/accesos, intenta bootstrap
        if (needsBootstrap(up)) {
            tryBootstrapPerfilPorServicios();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        logSession("onResume()");
        logLegacyToken("onResume()");
        aplicarVisibilidadPorPerfil();
        aplicarSaludoYVisibilidadDeCards();
    }

    /** Saludo “Hola, {Nombre}” y visibilidad/uso de cards según perfil */
    private void aplicarSaludoYVisibilidadDeCards() {
        UsuarioPerfil up = session.getPerfil();
        String nombre = (up != null) ? up.getNombreCompleto() : session.getUsername();
        if (tvWelcome != null) {
            tvWelcome.setText("¡Hola, " + (nombre != null && !nombre.isEmpty() ? nombre : "Usuario") + "!");
        }

        boolean isSistemas = tienePerfil("sistemas");

        // Solo “sistemas” ve y puede entrar a Descuentos/Cortesías.
        setVisible(cardDescuentos, isSistemas);
        setVisible(cardCortesias,  isSistemas);

        // El resto de cards pueden quedarse visibles (ajusta a gusto)
        setVisible(cardReportes, true);
        setVisible(cardDatos,    true);
        setVisible(cardFeed,     true);
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

        List<LoginRsp.PlantaAcceso> acc = session.getAccesos();
        Log.d(TAG, "updateHeader() -> accesos=" + (acc != null ? acc.size() : 0));
    }

    private void aplicarVisibilidadPorPerfil() {
        if (navView == null || navView.getMenu() == null) return;
        Menu menu = navView.getMenu();

        UsuarioPerfil up = session.getPerfil();
        if (up == null || up.perfilId == null || up.perfilId.trim().isEmpty()) {
            Log.w(TAG, "Sin perfil nuevo. Mostrando todo (fallback legacy).");
            setAllVisible(menu, true);
            return;
        }

        String perfilId = up.perfilId.toLowerCase(Locale.ROOT);

        boolean puedeDescuentoCortesia = any(perfilId, "sistemas","administrador","ventas","supervisor");
        boolean puedeDescuento         = any(perfilId, "sistemas","administrador","ventas","supervisor");
        boolean puedeReportes          = any(perfilId, "sistemas","administrador","reportes");
        boolean puedeDatosVehiculares  = any(perfilId, "sistemas","administrador","soporte","datos");
        boolean puedePlantas           = any(perfilId, "sistemas","administrador","planta");

        setVisible(menu, R.id.nav_descuento,         puedeDescuento);
        setVisible(menu, R.id.nav_cortesia,          puedeDescuentoCortesia);
        setVisible(menu, R.id.nav_reportes,          puedeReportes);
        setVisible(menu, R.id.nav_datos_vehiculares, puedeDatosVehiculares);
        setVisible(menu, R.id.nav_plantas,           puedePlantas);
        setVisible(menu, R.id.nav_soporte,           true);
        setVisible(menu, R.id.nav_logout,            true);

        Log.d(TAG, "Visibilidad aplicada. perfilId=" + perfilId);
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

    // ====== Bootstrap (perfil/accesos) ======
    private boolean needsBootstrap(UsuarioPerfil up) {
        boolean perfilVacio   = (up == null || up.perfilId == null || up.perfilId.trim().isEmpty());
        boolean accesosVacios = (session.getAccesos() == null || session.getAccesos().isEmpty());
        return perfilVacio || accesosVacios;
    }

    private void tryBootstrapPerfilPorServicios() {
        final String user = session.getUsername();
        if (user == null || user.trim().isEmpty()) {
            Log.w(TAG, "Bootstrap cancelado: username vacío.");
            return;
        }
        Log.d(TAG, "Bootstrap -> buscar_usuarios(filtro=" + user + ")");

        NewApiClient.get().buscarUsuarios(user).enqueue(new Callback<List<UsuarioResumenDto>>() {
            @Override
            public void onResponse(Call<List<UsuarioResumenDto>> call, Response<List<UsuarioResumenDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.w(TAG, "buscar_usuarios sin datos (" + response.code() + ")");
                    cargarAccesosSolo(user);
                    return;
                }
                UsuarioResumenDto elegido = null;
                for (UsuarioResumenDto u : response.body()) {
                    if (u != null && u.username != null && u.username.equalsIgnoreCase(user)) {
                        elegido = u; break;
                    }
                }
                if (elegido != null) {
                    UsuarioPerfil p = new UsuarioPerfil();
                    p.username     = elegido.username;
                    p.perfilId     = elegido.perfilId; // String
                    p.estado       = elegido.estado;
                    p.nroDocumento = elegido.dni;
                    p.nombres      = elegido.nombres;
                    p.apellidos    = elegido.apellidos;
                    session.savePerfil(p);

                    Log.d(TAG, "Bootstrap perfil OK -> " + session.debugSnapshot());
                    updateHeader();
                    aplicarVisibilidadPorPerfil();
                    aplicarSaludoYVisibilidadDeCards();
                } else {
                    Log.w(TAG, "No se encontró usuario exacto en buscar_usuarios (filtro=" + user + ")");
                }
                cargarAccesosSolo(user);
            }

            @Override
            public void onFailure(Call<List<UsuarioResumenDto>> call, Throwable t) {
                Log.e(TAG, "buscar_usuarios error", t);
                cargarAccesosSolo(user);
            }
        });
    }

    private void cargarAccesosSolo(final String user) {
        Log.d(TAG, "Bootstrap -> obtener_accesos_usuario(usuario=" + user + ")");
        // Intento #1: ?usuario=
        NewApiClient.get().obtenerAccesos(user).enqueue(new Callback<List<AccesoPlantaDto>>() {
            @Override
            public void onResponse(Call<List<AccesoPlantaDto>> call, Response<List<AccesoPlantaDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    guardarAccesosYRefrescar(response.body());
                } else {
                    Log.w(TAG, "obtener_accesos_usuario (?usuario) code=" + response.code() + " — probando ?user");
                    obtenerAccesosFallbackUser(user);
                }
            }
            @Override
            public void onFailure(Call<List<AccesoPlantaDto>> call, Throwable t) {
                Log.e(TAG, "obtener_accesos_usuario (?usuario) error", t);
                obtenerAccesosFallbackUser(user);
            }
        });
    }

    private void obtenerAccesosFallbackUser(final String user) {
        Log.d(TAG, "Fallback -> obtener_accesos_usuario(user=" + user + ")");
        // Intento #2: ?user=
        NewApiClient.get().obtenerAccesosPorUser(user).enqueue(new Callback<List<AccesoPlantaDto>>() {
            @Override
            public void onResponse(Call<List<AccesoPlantaDto>> call, Response<List<AccesoPlantaDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    guardarAccesosYRefrescar(response.body());
                } else {
                    Log.w(TAG, "Fallback ?user sin datos (" + response.code() + ")");
                    logSession("post-bootstrap (sin accesos)");
                }
            }
            @Override
            public void onFailure(Call<List<AccesoPlantaDto>> call, Throwable t) {
                Log.e(TAG, "Fallback ?user error", t);
                logSession("post-bootstrap (onFailure accesos)");
            }
        });
    }

    private void guardarAccesosYRefrescar(List<AccesoPlantaDto> body) {
        List<LoginRsp.PlantaAcceso> list = new ArrayList<>();
        for (AccesoPlantaDto a : body) {
            if (a == null) continue;
            LoginRsp.PlantaAcceso pa = new LoginRsp.PlantaAcceso();
            pa.key = a.key;
            pa.planta = a.planta;
            list.add(pa);
        }
        session.saveAccesos(list);
        Log.d(TAG, "Bootstrap accesos OK -> accesos=" + list.size());
        updateHeader();
        aplicarVisibilidadPorPerfil();
        aplicarSaludoYVisibilidadDeCards();
        logSession("post-bootstrap OK");
    }

    // ====== Navegación ======
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
        } else if (id == R.id.nav_plantas) {
            showInfo("Plantas", "Abrir administración de plantas.");
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

    // ====== Logs ======
    private void logSession(String where) {
        try {
            String snapshot = session != null ? session.debugSnapshot() : "session=null";
            Log.d(TAG, where + " -> " + snapshot);
        } catch (Throwable t) {
            Log.e(TAG, where + " -> error al leer snapshot", t);
        }
    }

    private void logLegacyToken(String where) {
        try {
            String token = legacyPrefs != null ? legacyPrefs.getString("token", null) : null;
            Log.d(TAG, where + " -> Legacy token presente? " + (token != null && !token.isEmpty()));
        } catch (Throwable t) {
            Log.e(TAG, where + " -> error al leer legacy token", t);
        }
    }
}
