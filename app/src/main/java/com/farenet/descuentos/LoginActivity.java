package com.farenet.descuentos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.farenet.descuentos.config.Constante;
import com.farenet.descuentos.domain.Usuario;
import com.farenet.descuentos.repository.LoginRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Author by Alexis Pumayalla on 28/08/19.
 * Email apumayallag@gmail.com
 * Phone 961778965
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etUser, etPass;
    private Button btnIniciar;
    private SharedPreferences sharedPreferences;
    private LoginRepository loginRepository;
    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT > 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        etUser = (EditText) findViewById(R.id.etUser);
        etPass = (EditText) findViewById(R.id.etPass);
        btnIniciar = (Button) findViewById(R.id.btnLogin);
        sharedPreferences = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);
        loginRepository = Constante.getLoginRespository();
        if (sharedPreferences.getString("token", null) != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }


    }

    @Override
    protected void onResume() {
        super.onResume();

        btnIniciar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validar()) {
                    login(etUser.getText().toString(), etPass.getText().toString());
                }
            }
        });

    }

    private boolean validar() {
        boolean pasa = true;
        if (etUser.getText() == null || etUser.getText().toString().isEmpty()) {
            etUser.setError("Ingrese usuario");
            pasa = false;
        }
        if (etPass.getText() == null || etPass.getText().toString().isEmpty()) {
            etPass.setError("Ingrese contraeña");
            pasa = false;
        }
        return pasa;
    }

    private void login(final String user, final String pw) {
        try {
            Call<Usuario> call = loginRepository.getUsuario(user, pw);
            call.enqueue(new Callback<Usuario>() {
                @Override
                public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                    if (response.isSuccessful() && response.code() == 200) {
                        usuario = response.body();
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("token", usuario.getToken());
                        editor.putString("user", user);
                        editor.putString("pw", pw);
                        editor.commit();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "Error al iniciar", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Usuario> call, Throwable t) {
                    Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT).show();
                    Log.e("error", t.getMessage());

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
