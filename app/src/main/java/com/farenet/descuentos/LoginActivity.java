package com.farenet.descuentos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.farenet.descuentos.config.Constante;
import com.farenet.descuentos.domain.Usuario;
import com.farenet.descuentos.models.newapi.LoginReq;
import com.farenet.descuentos.models.newapi.LoginRsp;
import com.farenet.descuentos.models.newapi.UsuarioPerfil;
import com.farenet.descuentos.network.newapi.NewApiClient;
import com.farenet.descuentos.repository.LoginRepository;
import com.farenet.descuentos.repository.SessionManager;
import com.farenet.descuentos.prefetch.Prefetcher; // <-- IMPORTANTE

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.farenet.descuentos.models.newapi.AccesoPlantaDto;

public class LoginActivity extends AppCompatActivity {

    private EditText etUser, etPass;
    private Button btnIniciar;
    private ProgressBar progress;

    // Legacy
    private SharedPreferences sharedPreferences;
    private LoginRepository loginRepository;
    private Call<Usuario> loginCallLegacy;

    // New API
    private SessionManager session;
    private Call<LoginRsp> loginCallNew;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT > 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        etUser = findViewById(R.id.etUser);
        etPass = findViewById(R.id.etPass);
        btnIniciar = findViewById(R.id.btnLogin);
        progress = findViewById(R.id.progress);

        // legacy prefs para el login antiguo (Descuentos/Cortesías usan este token)
        sharedPreferences = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);
        loginRepository = Constante.getLoginRespository();

        // sesión nueva (para nuevas pantallas)
        session = new SessionManager(this);

        // Autologin: PRIORIDAD token legacy (porque lo usan las pantallas existentes)
        String legacyToken = sharedPreferences.getString("token", null);
        if (legacyToken != null && !legacyToken.isEmpty()) {
            // Lanzamos prefetch en background ANTES de ir al main
            Prefetcher.warmUpAfterLogin(
                    legacyToken,
                    Constante.getMaestroRespository(),
                    NewApiClient.get()
            );
            goToMain();
            return;
        }
        // Si no hay token legacy, intenta sesión nueva
        UsuarioPerfil up = session.getPerfil();
        if (up != null && up.username != null && !up.username.isEmpty()) {
            goToMain();
            return;
        }

        btnIniciar.setOnClickListener(v -> {
            if (validar()) {
                // 1) Siempre login legacy primero (para garantizar el token)
                doLoginLegacyFirst(etUser.getText().toString().trim(), etPass.getText().toString());
            }
        });
    }

    private boolean validar() {
        boolean ok = true;
        if (etUser.getText() == null || etUser.getText().toString().trim().isEmpty()) {
            etUser.setError("Ingrese usuario");
            ok = false;
        }
        if (etPass.getText() == null || etPass.getText().toString().isEmpty()) {
            etPass.setError("Ingrese contraseña");
            ok = false;
        }
        return ok;
    }

    private void setLoading(boolean loading) {
        btnIniciar.setEnabled(!loading);
        if (progress != null) {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    // ===== LOGIN LEGACY (PRIMERO) =====
    private void doLoginLegacyFirst(final String user, final String pw) {
        if (loginCallLegacy != null) loginCallLegacy.cancel();

        setLoading(true);
        loginCallLegacy = loginRepository.getUsuario(user, pw);
        loginCallLegacy.enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Usuario usuario = response.body();

                    // Guarda token y user para las pantallas viejas
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("token", usuario.getToken());
                    editor.putString("user", user);
                    editor.apply();

                    // Prefetch inmediato tras login OK (no bloquea UI)
                    Prefetcher.warmUpAfterLogin(
                            usuario.getToken(),
                            Constante.getMaestroRespository(),
                            NewApiClient.get()
                    );

                    // Navega de inmediato: Descuento/Cortesía ya tienen token
                    goToMain();

                    // 2) En paralelo, intenta el login NUEVO para guardar perfil (no bloquea)
                    doLoginNewFireAndForget(user, pw);

                } else {
                    toast("Usuario o contraseña incorrectos");
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                setLoading(false);
                toast("No se pudo conectar (legacy). Intenta de nuevo.");
            }
        });
    }

    // ===== LOGIN NUEVO (fire & forget) =====
    private void doLoginNewFireAndForget(final String user, final String pw) {
        try {
            if (loginCallNew != null) loginCallNew.cancel();
            LoginReq req = new LoginReq(user, pw);
            loginCallNew = NewApiClient.get().login(req);
            loginCallNew.enqueue(new Callback<LoginRsp>() {
                @Override
                public void onResponse(Call<LoginRsp> call, Response<LoginRsp> response) {
                    if (!response.isSuccessful()) return;
                    LoginRsp body = response.body();
                    if (body == null || !body.isOk()) return;

                    // 1) Guarda username y perfil primero (para snapshot consistente)
                    session.saveUsername(body.username);

                    UsuarioPerfil perfil = new UsuarioPerfil();
                    perfil.username     = body.username;
                    perfil.perfilId     = body.perfilId;
                    perfil.estado       = body.estado;
                    perfil.nroDocumento = body.nroDocumento;
                    perfil.nombres      = body.nombres;
                    perfil.apellidos    = body.apellidos;
                    session.savePerfil(perfil);

                    // 2) Accesos del login (si vinieron)
                    if (body.accesos != null && !body.accesos.isEmpty()) {
                        session.saveAccesos(body.accesos);
                        android.util.Log.d("SESSION_TEST",
                                "Accesos guardados=" + body.accesos.size()
                                        + " / Accesos leidos=" + session.getAccesos().size());
                    } else {
                        // 3) Fallback: obtener accesos por endpoint dedicado
                        NewApiClient.get().obtenerAccesos(body.username)
                                .enqueue(new Callback<List<AccesoPlantaDto>>() {
                                    @Override
                                    public void onResponse(Call<List<AccesoPlantaDto>> call,
                                                           Response<List<AccesoPlantaDto>> rsp) {
                                        if (!rsp.isSuccessful() || rsp.body() == null) {
                                            // Segundo intento por si el backend espera ?user=
                                            NewApiClient.get().obtenerAccesosPorUser(body.username)
                                                    .enqueue(new Callback<List<AccesoPlantaDto>>() {
                                                        @Override
                                                        public void onResponse(Call<List<AccesoPlantaDto>> call2,
                                                                               Response<List<AccesoPlantaDto>> rsp2) {
                                                            if (!rsp2.isSuccessful() || rsp2.body() == null) return;
                                                            session.saveAccesos(rsp2.body());
                                                            android.util.Log.d("SESSION_TEST",
                                                                    "Accesos por fetch(user)= " + session.getAccesos().size());
                                                        }
                                                        @Override public void onFailure(Call<List<AccesoPlantaDto>> call2, Throwable t2) { }
                                                    });
                                            return;
                                        }
                                        session.saveAccesos(rsp.body());
                                        android.util.Log.d("SESSION_TEST",
                                                "Accesos por fetch(usuario)= " + session.getAccesos().size());
                                    }
                                    @Override public void onFailure(Call<List<AccesoPlantaDto>> call, Throwable t) { }
                                });
                    }

                    // 4) (Opcional) snapshot compacto para tus logs
                    android.util.Log.d("SESSION_TEST", "Snapshot -> " + session.debugSnapshot());

                    // 5) (Opcional) guardar raw para inspección
                    session.saveLoginRaw(body);
                }

                @Override
                public void onFailure(Call<LoginRsp> call, Throwable t) {
                    // Silencioso: si falla el nuevo, no afecta al flujo legacy.
                }
            });
        } catch (Exception ignored) { }
    }

    private void toast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    private void goToMain() {
        startActivity(new Intent(LoginActivity.this, BootstrapActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loginCallLegacy != null) loginCallLegacy.cancel();
        if (loginCallNew != null) loginCallNew.cancel();
    }
}
