package com.farenet.descuentos.ui.auth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.farenet.descuentos.Core.config.Constante;
import com.farenet.descuentos.R;
import com.farenet.descuentos.domain.model.Usuario;
import com.farenet.descuentos.API.Actual.DTO.maestros.AccesoPlantaDto;
import com.farenet.descuentos.API.Actual.DTO.auth.LoginReq;
import com.farenet.descuentos.API.Actual.DTO.auth.LoginRsp;
import com.farenet.descuentos.API.Actual.DTO.auth.UsuarioPerfil;
import com.farenet.descuentos.Core.Network.NewApiClient;
import com.farenet.descuentos.Core.bootstrap.Prefetcher;
import com.farenet.descuentos.API.Antigua.Service.LoginRepository;
import com.farenet.descuentos.Core.Storage.SessionManager;
import com.farenet.descuentos.ui.bootstrap.BootstrapActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUser, etPass;
    private Button btnIniciar;
    private ProgressBar progress;
    private FrameLayout progressOverlay;
    private TextInputLayout tilUser, tilPass;
    private MaterialCardView cardForm;

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

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_login);

        etUser = findViewById(R.id.etUser);
        etPass = findViewById(R.id.etPass);
        btnIniciar = findViewById(R.id.btnLogin);
        progress = findViewById(R.id.progress);
        progressOverlay = findViewById(R.id.progressOverlay);
        tilUser = findViewById(R.id.tilUser);
        tilPass = findViewById(R.id.tilPass);
        cardForm = findViewById(R.id.cardForm);

        // Animación sutil
        cardForm.setAlpha(0f);
        cardForm.setTranslationY(24f);
        cardForm.post(() -> {
            ObjectAnimator a1 = ObjectAnimator.ofFloat(cardForm, View.ALPHA, 0f, 1f);
            a1.setDuration(300);
            a1.setInterpolator(new DecelerateInterpolator());
            ObjectAnimator a2 = ObjectAnimator.ofFloat(cardForm, View.TRANSLATION_Y, 24f, 0f);
            a2.setDuration(300);
            a2.setInterpolator(new DecelerateInterpolator());
            a1.start(); a2.start();
        });

        // Legacy prefs
        sharedPreferences = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);
        loginRepository = Constante.getLoginRespository();

        // Sesión nueva
        session = new SessionManager(this);

        // Autologin: token legacy
        String legacyToken = sharedPreferences.getString("token", null);
        if (legacyToken != null && !legacyToken.isEmpty()) {
            Prefetcher.warmUpAfterLogin(legacyToken, Constante.getMaestroRespository(), NewApiClient.get());
            goToMain();
            return;
        }
        // Autologin: perfil nuevo
        UsuarioPerfil up = session.getPerfil();
        if (up != null && up.username != null && !up.username.isEmpty()) {
            goToMain();
            return;
        }

        btnIniciar.setOnClickListener(v -> {
            if (validar()) {
                doLoginLegacyFirst(etUser.getText().toString().trim(), etPass.getText().toString());
            }
        });

        findViewById(R.id.tvForgot).setOnClickListener(v ->
                Toast.makeText(this, "Contacta a TI para restablecer tu clave.", Toast.LENGTH_SHORT).show());
    }

    private boolean validar() {
        boolean ok = true;
        tilUser.setError(null);
        tilPass.setError(null);

        String u = etUser.getText() != null ? etUser.getText().toString().trim() : "";
        String p = etPass.getText() != null ? etPass.getText().toString() : "";

        if (u.isEmpty()) { tilUser.setError("Ingrese usuario"); ok = false; }
        if (p.isEmpty()) { tilPass.setError("Ingrese contraseña"); ok = false; }
        return ok;
    }

    private void setLoading(boolean loading) {
        btnIniciar.setEnabled(!loading);
        progressOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
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

                    // Guarda token y user para pantallas viejas
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("token", usuario.getToken());
                    editor.putString("user", user);
                    editor.apply();

                    Prefetcher.warmUpAfterLogin(usuario.getToken(),
                            Constante.getMaestroRespository(), NewApiClient.get());

                    goToMain();

                    // En paralelo: login nuevo
                    doLoginNewFireAndForget(user, pw);
                } else {
                    tilPass.setError("Usuario o contraseña incorrectos");
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                setLoading(false);
                Toast.makeText(getApplicationContext(),
                        "No se pudo conectar (legacy). Intenta de nuevo.",
                        Toast.LENGTH_LONG).show();
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

                    session.saveUsername(body.username);
                    UsuarioPerfil perfil = new UsuarioPerfil();
                    perfil.username = body.username;
                    perfil.perfilId = body.perfilId;
                    perfil.estado = body.estado;
                    perfil.nroDocumento = body.nroDocumento;
                    perfil.nombres = body.nombres;
                    perfil.apellidos = body.apellidos;
                    session.savePerfil(perfil);

                    if (body.accesos != null && !body.accesos.isEmpty()) {
                        session.saveAccesos(body.accesos);
                    } else {
                        NewApiClient.get().obtenerAccesos(body.username)
                                .enqueue(new Callback<List<AccesoPlantaDto>>() {
                                    @Override
                                    public void onResponse(Call<List<AccesoPlantaDto>> call,
                                                           Response<List<AccesoPlantaDto>> rsp) {
                                        if (rsp.isSuccessful() && rsp.body() != null)
                                            session.saveAccesos(rsp.body());
                                    }
                                    @Override public void onFailure(Call<List<AccesoPlantaDto>> call, Throwable t) { }
                                });
                    }
                    session.saveLoginRaw(body);
                }
                @Override public void onFailure(Call<LoginRsp> call, Throwable t) { }
            });
        } catch (Exception ignored) { }
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
