package com.farenet.descuentos.config;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.farenet.descuentos.domain.Autorizadores;
import com.farenet.descuentos.domain.Conceptoinspeccion;
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.domain.TipoPagoDescuento;
import com.farenet.descuentos.domain.Usuario;
import com.farenet.descuentos.repository.LoginRepository;
import com.farenet.descuentos.repository.MaestroRepository;
import com.farenet.descuentos.sql.QueryRealm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BaseApplication extends Application {

    // Broadcast actions para notificar fin de sync
    public static final String ACTION_PLANTAS_UPDATED       = "com.farenet.descuentos.PLANTAS_UPDATED";
    public static final String ACTION_CONCEPTOS_UPDATED     = "com.farenet.descuentos.CONCEPTOS_UPDATED";
    public static final String ACTION_TIPOPAGO_UPDATED      = "com.farenet.descuentos.TIPOPAGO_UPDATED";
    public static final String ACTION_AUTORIZADORES_UPDATED = "com.farenet.descuentos.AUTORIZADORES_UPDATED";

    private LoginRepository loginRepository;
    private MaestroRepository maestroRepository;
    private Usuario usuario;
    private SharedPreferences sharedPreferences;

    // Cola única para evitar colisiones de escritura en Realm
    private static final ExecutorService REALM_WRITE_EXECUTOR =
            Executors.newSingleThreadExecutor();

    // Caches en memoria (opcionales)
    private List<Planta> plantas = new ArrayList<>();
    private List<Conceptoinspeccion> conceptoinspeccions = new ArrayList<>();
    private List<TipoPagoDescuento> tipoPagoDescuentos = new ArrayList<>();
    private List<Autorizadores> autorizadores = new ArrayList<>();

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

        // Si ya hay token, sincroniza maestros; si no, intenta login y luego sincroniza
        String token = sharedPreferences.getString("token", null);
        if (token == null) {
            String user = sharedPreferences.getString("user", null);
            String pw   = sharedPreferences.getString("pw", null);
            if (user != null && pw != null) {
                login(user, pw, this::syncMaestrosIfMissing);
            }
        } else {
            syncMaestrosIfMissing();
        }
    }

    // ========= Login =========
    private void login(String user, String password, Runnable afterLogin) {
        try {
            Call<Usuario> call = loginRepository.getUsuario(user, password);
            call.enqueue(new Callback<Usuario>() {
                @Override
                public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        usuario = response.body();
                        sharedPreferences.edit()
                                .putString("token", usuario.getToken())
                                .apply();
                        if (afterLogin != null) afterLogin.run();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "No se pudo iniciar sesión", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Usuario> call, Throwable t) {
                    Toast.makeText(getApplicationContext(),
                            "Error de red en login", Toast.LENGTH_SHORT).show();
                    Log.e("error", String.valueOf(t.getMessage()));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Público para disparar sync tras login desde LoginActivity
    public void forceSyncMaestros() {
        syncMaestrosIfMissing();
    }

    // ========= Sincronización de maestros =========
    private void syncMaestrosIfMissing() {
        // Lee copias (unmanaged) para no dejar instancias Realm abiertas
        plantas = QueryRealm.copyAllPlantas();
        conceptoinspeccions = QueryRealm.copyAllConceptos();
        tipoPagoDescuentos = QueryRealm.copyAllTipoPagos();
        autorizadores = QueryRealm.copyAllAutorizadores();

        if (plantas == null || plantas.isEmpty())                         getPlantas();
        if (conceptoinspeccions == null || conceptoinspeccions.isEmpty()) getConceptos();
        if (tipoPagoDescuentos == null || tipoPagoDescuentos.isEmpty())   getTipoPagoDescuento();
        if (autorizadores == null || autorizadores.isEmpty())             getAutorizadores();
    }

    // ========= Helpers de escritura encolada + broadcast =========
    private void enqueueRealmWrite(RealmWriteTask task, @Nullable String broadcastAction) {
        REALM_WRITE_EXECUTOR.execute(() -> {
            try (Realm realm = Realm.getDefaultInstance()) {
                realm.executeTransaction(r -> task.run(r));
            } catch (Throwable t) {
                Log.e("RealmWrite", "Error en escritura Realm", t);
            } finally {
                if (broadcastAction != null) {
                    sendBroadcast(new android.content.Intent(broadcastAction));
                }
            }
        });
    }

    // Overload para usos sin broadcast
    private void enqueueRealmWrite(RealmWriteTask task) {
        enqueueRealmWrite(task, null);
    }

    @FunctionalInterface
    private interface RealmWriteTask {
        void run(Realm r);
    }

    // ========= Descargas =========

    private void getPlantas() {
        String token = sharedPreferences.getString("token", null);
        if (token == null) return;

        maestroRepository.getPlantas(token).enqueue(new Callback<List<Planta>>() {
            @Override
            public void onResponse(Call<List<Planta>> call, Response<List<Planta>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Planta> data = new ArrayList<>(response.body());
                    // Agregar "TODOS"
                    Planta p = new Planta();
                    p.setKey("todos");
                    p.setNombre("TODOS");
                    data.add(p);

                    enqueueRealmWrite(r -> QueryRealm.insertOrUpdatePlantas(r, data),
                            ACTION_PLANTAS_UPDATED);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error al obtener plantas", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Planta>> call, Throwable t) {
                Toast.makeText(getApplicationContext(),
                        "Red: plantas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getConceptos() {
        String token = sharedPreferences.getString("token", null);
        if (token == null) return;

        maestroRepository.getConceptoinspeccion(token).enqueue(new Callback<List<Conceptoinspeccion>>() {
            @Override
            public void onResponse(Call<List<Conceptoinspeccion>> call, Response<List<Conceptoinspeccion>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Conceptoinspeccion> data = response.body();
                    enqueueRealmWrite(r -> QueryRealm.insertOrUpdateConceptos(r, data),
                            ACTION_CONCEPTOS_UPDATED);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error al obtener conceptos", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Conceptoinspeccion>> call, Throwable t) {
                Toast.makeText(getApplicationContext(),
                        "Red: conceptos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getTipoPagoDescuento() {
        String token = sharedPreferences.getString("token", null);
        if (token == null) return;

        maestroRepository.getTipoPagoDescuento(token).enqueue(new Callback<List<TipoPagoDescuento>>() {
            @Override
            public void onResponse(Call<List<TipoPagoDescuento>> call, Response<List<TipoPagoDescuento>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TipoPagoDescuento> data = response.body();
                    enqueueRealmWrite(r -> QueryRealm.insertOrUpdateTipoPago(r, data),
                            ACTION_TIPOPAGO_UPDATED);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error al obtener tipo pago", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<TipoPagoDescuento>> call, Throwable t) {
                Toast.makeText(getApplicationContext(),
                        "Red: tipo pago", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getAutorizadores() {
        String token = sharedPreferences.getString("token", null);
        if (token == null) return;

        maestroRepository.getAutorizadores(token).enqueue(new Callback<List<Autorizadores>>() {
            @Override
            public void onResponse(Call<List<Autorizadores>> call, Response<List<Autorizadores>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Autorizadores> data = response.body();
                    enqueueRealmWrite(r -> QueryRealm.insertOrUpdateAutorizadores(r, data),
                            ACTION_AUTORIZADORES_UPDATED);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error al obtener autorizadores", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Autorizadores>> call, Throwable t) {
                Toast.makeText(getApplicationContext(),
                        "Red: autorizadores", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
