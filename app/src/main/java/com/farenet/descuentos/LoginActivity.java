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
import com.farenet.descuentos.repository.LoginRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUser, etPass;
    private Button btnIniciar;
    private ProgressBar progress; // añade un ProgressBar en tu layout
    private SharedPreferences sharedPreferences;
    private LoginRepository loginRepository;
    private Call<Usuario> loginCall; // para evitar llamadas en paralelo

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
        progress = findViewById(R.id.progress); // asegúrate de tenerlo en el layout

        sharedPreferences = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);
        loginRepository = Constante.getLoginRespository();

        // Si ya hay token válido, entra directo
        if (sharedPreferences.getString("token", null) != null) {
            goToMain();
            return;
        }

        btnIniciar.setOnClickListener(v -> {
            if (validar()) {
                // evita doble clic / llamadas en paralelo
                if (loginCall == null || loginCall.isCanceled()) {
                    doLogin(etUser.getText().toString().trim(),
                            etPass.getText().toString());
                }
            }
        });
    }

    private boolean validar() {
        boolean pasa = true;
        if (etUser.getText() == null || etUser.getText().toString().trim().isEmpty()) {
            etUser.setError("Ingrese usuario");
            pasa = false;
        }
        if (etPass.getText() == null || etPass.getText().toString().isEmpty()) {
            etPass.setError("Ingrese contraseña");
            pasa = false;
        }
        return pasa;
    }

    private void setLoading(boolean loading) {
        btnIniciar.setEnabled(!loading);
        if (progress != null) {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void doLogin(final String user, final String pw) {
        setLoading(true);
        loginCall = loginRepository.getUsuario(user, pw);
        loginCall.enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Usuario usuario = response.body();

                    // Guarda token y usuario; evita guardar el password en texto plano
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("token", usuario.getToken());
                    editor.putString("user", user);
                    // Si de verdad necesitas el pw, encripta/hashea; de lo contrario, NO lo guardes
                    editor.apply();

                    // Opcional: dispara una sincronización *serializada* antes de ir a Main
                    // pero si ya sincronizas en BaseApplication, no dupliques.
                    goToMain();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Usuario o contraseña incorrectos",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                setLoading(false);
                Toast.makeText(getApplicationContext(),
                        "No se pudo conectar. Intenta de nuevo.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loginCall != null) {
            loginCall.cancel();
        }
    }
}
