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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

                    // Guarda perfil en SessionManager para las NUEVAS pantallas
                    session.saveUsername(body.username);

                    UsuarioPerfil perfil = new UsuarioPerfil();
                    perfil.username = body.username;
                    perfil.perfilId = body.perfilId;
                    perfil.estado = body.estado;
                    perfil.nroDocumento = body.nroDocumento;
                    perfil.nombres = body.nombres;
                    perfil.apellidos = body.apellidos;
                    session.savePerfil(perfil);
                }

                @Override
                public void onFailure(Call<LoginRsp> call, Throwable t) {
                    // Silencioso: si falla el nuevo, no afecta al flujo legacy.
                }
            });
        } catch (Exception ignored) { }
    }

    // ===== (Opcional) Login nuevo “bloqueante”, ya no lo usamos como principal =====
    @SuppressWarnings("unused")
    private void doLoginNew(final String user, final String pw) {
        if (loginCallNew != null) loginCallNew.cancel();

        setLoading(true);
        LoginReq req = new LoginReq(user, pw);
        loginCallNew = NewApiClient.get().login(req);
        loginCallNew.enqueue(new Callback<LoginRsp>() {
            @Override
            public void onResponse(Call<LoginRsp> call, Response<LoginRsp> response) {
                setLoading(false);
                if (!response.isSuccessful()) {
                    int code = response.code();
                    if (code == 401) {
                        toast("Contraseña incorrecta");
                    } else if (code == 404) {
                        toast("Usuario no encontrado");
                    } else if (code == 403) {
                        toast("Acceso denegado");
                    } else {
                        toast("Error del servidor (" + code + ")");
                    }
                    return;
                }

                LoginRsp body = response.body();
                if (body == null) {
                    toast("Respuesta vacía del servidor");
                    return;
                }
                if (!body.isOk()) {
                    toast(body.error != null ? body.error : "Login fallido");
                    return;
                }

                // Guardar sesión nueva
                session.saveUsername(body.username);

                UsuarioPerfil perfil = new UsuarioPerfil();
                perfil.username = body.username;
                perfil.perfilId = body.perfilId;
                perfil.estado = body.estado;
                perfil.nroDocumento = body.nroDocumento;
                perfil.nombres = body.nombres;
                perfil.apellidos = body.apellidos;
                session.savePerfil(perfil);

                goToMain();
            }

            @Override
            public void onFailure(Call<LoginRsp> call, Throwable t) {
                setLoading(false);
                toast("No se pudo conectar. Intenta de nuevo.");
            }
        });
    }

    private void toast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    private void goToMain() {
        startActivity(new Intent(LoginActivity.this, SelectionActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loginCallLegacy != null) loginCallLegacy.cancel();
        if (loginCallNew != null) loginCallNew.cancel();
    }
}
