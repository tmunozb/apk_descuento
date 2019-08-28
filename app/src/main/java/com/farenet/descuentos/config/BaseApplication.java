package com.farenet.descuentos.config;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.farenet.descuentos.domain.Conceptoinspeccion;
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.domain.TipoPagoDescuento;
import com.farenet.descuentos.domain.Usuario;
import com.farenet.descuentos.repository.LoginRepository;
import com.farenet.descuentos.repository.MaestroRepository;
import com.farenet.descuentos.sql.QueryRealm;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Author by Alexis Pumayalla on 28/08/19.
 * Email apumayallag@gmail.com
 * Phone 961778965
 */
public class BaseApplication extends Application {

    private LoginRepository loginRepository;
    private MaestroRepository maestroRepository;
    private Usuario usuario;
    private SharedPreferences sharedPreferences;
    private List<Planta> plantas = new ArrayList<>();
    private List<Conceptoinspeccion> conceptoinspeccions = new ArrayList<>();
    private List<TipoPagoDescuento> tipoPagoDescuentos = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        RealmConfiguration configuration = new RealmConfiguration.Builder()
                .name(Realm.DEFAULT_REALM_NAME)
                .schemaVersion(0)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(configuration);

        loginRepository = Constante.getLoginRespository();
        maestroRepository = Constante.getMaestroRespository();

        sharedPreferences = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);
        login();

        plantas = QueryRealm.getAllPlantas();
        conceptoinspeccions = QueryRealm.getAllConcepto();
        tipoPagoDescuentos = QueryRealm.getAllTipoPagos();
        if(!(plantas != null && !plantas.isEmpty())){
            getPlantas();
        }
        if(!(conceptoinspeccions != null && !conceptoinspeccions.isEmpty())){
            getConceptos();
        }
        if(!(tipoPagoDescuentos != null && !tipoPagoDescuentos.isEmpty())){
            getTipoPagoDescuento();
        }
    }

    private void login() {
        try {
            Call<Usuario> call = loginRepository.getUsuario(Constante.user, Constante.password);
            call.enqueue(new Callback<Usuario>() {
                @Override
                public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                    if (response.isSuccessful() && response.code() == 200) {
                        usuario = response.body();
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("token", usuario.getToken());
                        editor.commit();
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

    private void getPlantas(){
        Call<List<Planta>> listCall = maestroRepository.getPlantas(sharedPreferences.getString("token",null));
        listCall.enqueue(new Callback<List<Planta>>() {
            @Override
            public void onResponse(Call<List<Planta>> call, Response<List<Planta>> response) {
                if (response.isSuccessful() && response.code() == 200) {
                    plantas = response.body();
                    Planta planta = new Planta();
                    planta.setKey("todos");
                    planta.setNombre("TODOS");
                    plantas.add(planta);
                    QueryRealm.savePlanta(plantas);
                } else {
                    Toast.makeText(getApplicationContext(), "Error al iniciar", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Planta>> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("error", t.getMessage());
            }
        });
    }

    private void getConceptos(){
        Call<List<Conceptoinspeccion>> listCall = maestroRepository.getConceptoinspeccion(sharedPreferences.getString("token",null));
        listCall.enqueue(new Callback<List<Conceptoinspeccion>>() {
            @Override
            public void onResponse(Call<List<Conceptoinspeccion>> call, Response<List<Conceptoinspeccion>> response) {
                if (response.isSuccessful() && response.code() == 200) {
                    conceptoinspeccions = response.body();
                    QueryRealm.saveConceptos(conceptoinspeccions);
                } else {
                    Toast.makeText(getApplicationContext(), "Error al iniciar", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Conceptoinspeccion>> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Error al iniciar", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getTipoPagoDescuento(){
        Call<List<TipoPagoDescuento>> listCall = maestroRepository.getTipoPagoDescuento(sharedPreferences.getString("token",null));
        listCall.enqueue(new Callback<List<TipoPagoDescuento>>() {
            @Override
            public void onResponse(Call<List<TipoPagoDescuento>> call, Response<List<TipoPagoDescuento>> response) {
                if (response.isSuccessful() && response.code() == 200) {
                    tipoPagoDescuentos= response.body();
                    QueryRealm.saveTipoPago(tipoPagoDescuentos);
                } else {
                    Toast.makeText(getApplicationContext(), "Error al iniciar", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<TipoPagoDescuento>> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Error al iniciar", Toast.LENGTH_LONG).show();
            }
        });
    }
}
