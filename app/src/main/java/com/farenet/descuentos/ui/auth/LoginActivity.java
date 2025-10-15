package com.farenet.descuentos.ui.auth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.farenet.descuentos.Core.Storage.SessionManager;
import com.farenet.descuentos.Core.bootstrap.Prefetcher;
import com.farenet.descuentos.Core.config.Constante;
import com.farenet.descuentos.Core.Network.NewApiClient;
import com.farenet.descuentos.API.Antigua.Service.LoginRepository;
import com.farenet.descuentos.API.Actual.DTO.auth.LoginReq;
import com.farenet.descuentos.API.Actual.DTO.auth.LoginRsp;
import com.farenet.descuentos.API.Actual.DTO.auth.UsuarioPerfil;
import com.farenet.descuentos.API.Actual.DTO.maestros.AccesoPlantaDto;
import com.farenet.descuentos.R;
import com.farenet.descuentos.domain.model.Usuario;
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
    private FrameLayout progressOverlay;
    private TextInputLayout tilUser, tilPass;
    private MaterialCardView cardForm;
    private ImageView imgLogo;
    private Animation rotateAnim;

    // Legacy
    private SharedPreferences sharedPreferences;
    private LoginRepository loginRepository;
    private Call<Usuario> loginCallLegacy;

    // New API
    private SessionManager session;
    private Call<LoginRsp> loginCallNew;

    // Debounce
    private long lastClickTs = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge; como el fondo es blanco, íconos oscuros en status bar
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_login);

        // refs UI
        etUser = findViewById(R.id.etUser);
        etPass = findViewById(R.id.etPass);
        btnIniciar = findViewById(R.id.btnLogin);
        progressOverlay = findViewById(R.id.progressOverlay);
        tilUser = findViewById(R.id.tilUser);
        tilPass = findViewById(R.id.tilPass);
        cardForm = findViewById(R.id.cardForm);
        imgLogo = findViewById(R.id.imgLogo);

        // Animación sutil de aparición de la card
        cardForm.setAlpha(0f);
        cardForm.setTranslationY(18f);
        cardForm.post(() -> {
            ObjectAnimator a1 = ObjectAnimator.ofFloat(cardForm, View.ALPHA, 0f, 1f);
            a1.setDuration(260);
            a1.setInterpolator(new DecelerateInterpolator());
            ObjectAnimator a2 = ObjectAnimator.ofFloat(cardForm, View.TRANSLATION_Y, 18f, 0f);
            a2.setDuration(260);
            a2.setInterpolator(new DecelerateInterpolator());
            a1.start(); a2.start();
        });

        // Logo: rotación lenta tipo “cargando”
        rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_slow);
        imgLogo.startAnimation(rotateAnim);

        // Limpieza de errores al escribir
        TextWatcher clearErrorsWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { tilUser.setError(null); tilPass.setError(null); }
        };
        etUser.addTextChangedListener(clearErrorsWatcher);
        etPass.addTextChangedListener(clearErrorsWatcher);

        // IME action "Done" => login
        etPass.setOnEditorActionListener((v, actionId, event) -> {
            boolean pressedEnter = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_DONE || pressedEnter) {
                if (btnIniciar != null) btnIniciar.performClick();
                return true;
            }
            return false;
        });

        // Legacy prefs y repos
        sharedPreferences = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);
        loginRepository = Constante.getLoginRespository();

        // Sesión nueva
        session = new SessionManager(this);

        // Autologin (legacy o nueva)
        if (tryAutologinIfPossible()) return;

        // Click Iniciar sesión
        btnIniciar.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (now - lastClickTs < 750) return; // debounce
            lastClickTs = now;

            if (validar()) {
                String user = safeTrim(etUser.getText());
                String pass = safe(etPass.getText());
                doLoginLegacyFirst(user, pass);
            }
        });

        // Olvidé mi contraseña
        View tvForgot = findViewById(R.id.tvForgot);
        if (tvForgot != null) {
            tvForgot.setOnClickListener(v ->
                    Toast.makeText(this, "Contacta a TI para restablecer tu clave.", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private boolean tryAutologinIfPossible() {
        String legacyToken = sharedPreferences.getString("token", null);
        if (!TextUtils.isEmpty(legacyToken)) {
            Prefetcher.warmUpAfterLogin(legacyToken, Constante.getMaestroRespository(), NewApiClient.get());
            goToMain(); return true;
        }
        UsuarioPerfil up = session.getPerfil();
        if (up != null && !TextUtils.isEmpty(up.username)) {
            goToMain(); return true;
        }
        return false;
    }

    private boolean validar() {
        boolean ok = true;
        tilUser.setError(null); tilPass.setError(null);
        String u = safeTrim(etUser.getText());
        String p = safe(etPass.getText());
        if (TextUtils.isEmpty(u)) { tilUser.setError("Ingrese usuario"); ok = false; }
        if (TextUtils.isEmpty(p)) { tilPass.setError("Ingrese contraseña"); ok = false; }
        return ok;
    }

    private void setLoading(boolean loading) {
        btnIniciar.setEnabled(!loading);
        etUser.setEnabled(!loading);
        etPass.setEnabled(!loading);
        tilUser.setEnabled(!loading);
        tilPass.setEnabled(!loading);

        if (loading) {
            progressOverlay.setVisibility(View.VISIBLE);
            progressOverlay.setAlpha(0f);
            progressOverlay.animate().alpha(1f).setDuration(150).start();
        } else {
            progressOverlay.animate().alpha(0f).setDuration(150)
                    .withEndAction(() -> progressOverlay.setVisibility(View.GONE))
                    .start();
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
                if (!isSafe()) return;
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    Usuario usuario = response.body();

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("token", usuario.getToken());
                    editor.putString("user", user);
                    editor.apply();

                    Prefetcher.warmUpAfterLogin(usuario.getToken(),
                            Constante.getMaestroRespository(), NewApiClient.get());

                    goToMain();
                    doLoginNewFireAndForget(user, pw);
                } else {
                    tilPass.setError("Usuario o contraseña incorrectos");
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                if (!isSafe()) return;
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
                    if (!isSafe()) return;
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
                                        if (!isSafe()) return;
                                        if (rsp.isSuccessful() && rsp.body() != null) {
                                            session.saveAccesos(rsp.body());
                                        }
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

    // Helpers
    private String safeTrim(Editable e) { return e == null ? "" : e.toString().trim(); }
    private String safe(Editable e) { return e == null ? "" : e.toString(); }
    private boolean isSafe() { return !isFinishing() && !isDestroyed(); }

    @Override
    protected void onResume() {
        super.onResume();
        if (imgLogo != null && rotateAnim != null) imgLogo.startAnimation(rotateAnim);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (imgLogo != null) imgLogo.clearAnimation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loginCallLegacy != null) loginCallLegacy.cancel();
        if (loginCallNew != null) loginCallNew.cancel();
        if (imgLogo != null) imgLogo.clearAnimation();
    }
}
