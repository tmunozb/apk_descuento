// ui/bootstrap/BootstrapActivity.java
package com.farenet.descuentos.ui.bootstrap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.farenet.descuentos.API.Antigua.Service.MaestroRepository;
import com.farenet.descuentos.API.Actual.DTO.auth.LoginRsp;
import com.farenet.descuentos.API.Actual.DTO.auth.UsuarioPerfil;
import com.farenet.descuentos.API.Actual.DTO.maestros.AccesoPlantaDto;
import com.farenet.descuentos.API.Actual.DTO.maestros.MotivoDescuento;
import com.farenet.descuentos.Core.Cache.BolsaCache;
import com.farenet.descuentos.Core.Cache.DashboardCache;
import com.farenet.descuentos.Core.Network.NewApiClient;
import com.farenet.descuentos.Core.Storage.SessionManager;
import com.farenet.descuentos.Core.config.Constante;
import com.farenet.descuentos.R;
import com.farenet.descuentos.data.local.realm.entity.MotivoCortesia;
import com.farenet.descuentos.ui.auth.LoginActivity;
// ⬇️ Asegúrate que esta ruta es la correcta de tu host Activity (ViewPager2)
import com.farenet.descuentos.ui.comercialhost.ComercialHostActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BootstrapActivity extends AppCompatActivity {

    private SessionManager session;
    private SharedPreferences legacyPrefs;
    private MaestroRepository maestroRepo;

    // Gating
    private final AtomicBoolean perfilOk  = new AtomicBoolean(false);
    private final AtomicBoolean accesosOk = new AtomicBoolean(false);

    // Precargas no críticas
    private final AtomicInteger pending = new AtomicInteger(0);

    // UI
    private TextView tvStep, tvSub, tvTip;
    private LinearProgressIndicator progressLinear;
    private CircularProgressIndicator progressCircular;
    private ImageView imgLogo;

    // Evitar doble enrutamiento
    private final AtomicBoolean routed = new AtomicBoolean(false);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bootstrap);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

        session     = new SessionManager(this);
        legacyPrefs = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);
        maestroRepo = Constante.getMaestroRespository();

        bindUi();
        startTipsRotator();
        startBootstrap();
    }

    // ========= UI =========

    private void bindUi() {
        tvStep = findViewById(R.id.tvStep);
        tvSub  = findViewById(R.id.tvSub);
        tvTip  = findViewById(R.id.tvTip);
        imgLogo = findViewById(R.id.imgLogo);
        progressLinear   = findViewById(R.id.progressLinear);
        progressCircular = findViewById(R.id.progressCircular);

        if (imgLogo != null) {
            imgLogo.setScaleX(0.9f);
            imgLogo.setScaleY(0.9f);
            imgLogo.animate().scaleX(1f).scaleY(1f).setDuration(600).setStartDelay(150).start();
        }
        if (tvTip != null) {
            tvTip.setAlpha(0f);
            tvTip.animate().alpha(0.9f).setDuration(600).setStartDelay(500).start();
        }
        setStep("Validando perfil…", "Un momento por favor", 8);
    }

    private final String[] tips = {
            "Tip: puedes abrir Cortesías desde el menú lateral",
            "Tip: desliza desde el borde para abrir el menú",
            "Tip: mantén tu sesión activa para autologin"
    };
    private int tipIndex = 0;
    private final Runnable tipsRunnable = new Runnable() {
        @Override public void run() {
            if (tvTip == null) return;
            tipIndex = (tipIndex + 1) % tips.length;
            tvTip.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                tvTip.setText(tips[tipIndex]);
                tvTip.animate().alpha(0.9f).setDuration(250).start();
            }).start();
            tvTip.postDelayed(this, 3000);
        }
    };

    private void startTipsRotator() {
        if (tvTip == null) return;
        tvTip.postDelayed(tipsRunnable, 3000);
    }

    private void stopTipsRotator() {
        if (tvTip != null) tvTip.removeCallbacks(tipsRunnable);
    }

    private void setStep(String title, String subtitle, int progressPercent) {
        if (tvStep != null) tvStep.setText(title);
        if (tvSub  != null) tvSub.setText(subtitle);
        if (progressLinear != null) {
            int p = Math.max(0, Math.min(100, progressPercent));
            progressLinear.setProgressCompat(p, true);
        }
    }

    private void onPerfilInicio() { setStep("Validando perfil…", "Comprobando credenciales", 18); }
    private void onPerfilOk()     { setStep("Perfil verificado", "Cargando accesos a plantas", 35); }
    private void onAccesosOk()    { setStep("Accesos listos", "Sincronizando catálogos", 65); }
    private void onCatalogosOk()  { setStep("Catálogos sincronizados", "Listo para despegar", 90); }
    private void onFinalizando()  {
        setStep("Iniciando aplicación…", "Bienvenido(a)", 100);
        if (tvStep != null) tvStep.postDelayed(this::routeWhenReady, 250);
        else routeWhenReady();
    }

    // ========= FLUJO PRINCIPAL =========

    private void startBootstrap() {
        String token = legacyPrefs.getString("token", null);
        if (token == null || token.trim().isEmpty()) {
            setStep("Sesión no encontrada", "Redirigiendo a inicio de sesión…", 5);
            goToLogin();
            return;
        }

        // 1) Gating: PERFIL + ACCESOS
        onPerfilInicio();
        ensurePerfilYAccesos();

        // 2) Precargas no críticas
        prefetchMaestros(token);
        prefetchMotivos();
        prefetchDashboard();
    }

    // ===== PERFIL + ACCESOS =====
    private void ensurePerfilYAccesos() {
        UsuarioPerfil up = session.getPerfil();
        boolean needsPerfil  = (up == null || up.perfilId == null || up.perfilId.trim().isEmpty());
        boolean needsAccesos = (session.getAccesos() == null || session.getAccesos().isEmpty());

        if (!needsPerfil)  perfilOk.set(true);
        if (!needsAccesos) accesosOk.set(true);

        if (!needsPerfil && !needsAccesos) {
            onAccesosOk();
            prefetchBolsaEstadoSilencioso();
            onFinalizando();
            return;
        }

        if (needsPerfil) {
            String username = resolveUsername();
            if (username == null) {
                setStep("Error de perfil", "No se pudo resolver el usuario de sesión", 10);
                goToLogin();
                return;
            }
            NewApiClient.get().loginPerfilesGet(username).enqueue(new Callback<LoginRsp>() {
                @Override public void onResponse(Call<LoginRsp> call, Response<LoginRsp> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        setStep("Error de perfil", "HTTP " + response.code(), 12);
                        goToLogin();
                        return;
                    }
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

                    perfilOk.set(true);
                    onPerfilOk();

                    if (body.accesos != null && !body.accesos.isEmpty()) {
                        session.saveAccesos(body.accesos);
                        accesosOk.set(true);
                        onAccesosOk();
                        prefetchBolsaEstadoSilencioso();
                        onFinalizando();
                    } else {
                        fetchAccesos(body.username);
                    }
                }
                @Override public void onFailure(Call<LoginRsp> call, Throwable t) {
                    setStep("Fallo de red", "Obteniendo perfil", 12);
                    goToLogin();
                }
            });
        } else if (needsAccesos) {
            String username = resolveUsername();
            if (username == null) {
                setStep("Error de accesos", "Usuario de sesión vacío", 12);
                goToLogin();
                return;
            }
            fetchAccesos(username);
        }
    }

    private void fetchAccesos(String username) {
        setStep("Cargando accesos…", "Consultando permisos de plantas", 42);
        NewApiClient.get().obtenerAccesos(username).enqueue(new Callback<List<AccesoPlantaDto>>() {
            @Override public void onResponse(Call<List<AccesoPlantaDto>> call, Response<List<AccesoPlantaDto>> rsp) {
                if (rsp.isSuccessful() && rsp.body() != null && !rsp.body().isEmpty()) {
                    session.saveAccesos(rsp.body());
                    accesosOk.set(true);
                    onAccesosOk();
                    prefetchBolsaEstadoSilencioso();
                    onFinalizando();
                } else {
                    // Fallback ?user=
                    NewApiClient.get().obtenerAccesosPorUser(username)
                            .enqueue(new Callback<List<AccesoPlantaDto>>() {
                                @Override public void onResponse(Call<List<AccesoPlantaDto>> call2, Response<List<AccesoPlantaDto>> rsp2) {
                                    if (rsp2.isSuccessful() && rsp2.body() != null && !rsp2.body().isEmpty()) {
                                        session.saveAccesos(rsp2.body());
                                        accesosOk.set(true);
                                        onAccesosOk();
                                        prefetchBolsaEstadoSilencioso();
                                        onFinalizando();
                                    } else {
                                        setStep("Sin accesos", "No se encontraron accesos para el usuario", 48);
                                        goToLogin();
                                    }
                                }
                                @Override public void onFailure(Call<List<AccesoPlantaDto>> call2, Throwable t2) {
                                    setStep("Fallo de red", "Obteniendo accesos", 48);
                                    goToLogin();
                                }
                            });
                }
            }
            @Override public void onFailure(Call<List<AccesoPlantaDto>> call, Throwable t) {
                setStep("Fallo de red", "Obteniendo accesos", 48);
                goToLogin();
            }
        });
    }

    /** ⬇️ CORRECCIÓN: Enrutar SIEMPRE a una Activity, no a un Fragment */
    private void routeWhenReady() {
        if (!(perfilOk.get() && accesosOk.get())) return;
        if (routed.getAndSet(true)) return; // evita doble start

        String perfil = session.getPerfil() != null ? session.getPerfil().perfilId : "";
        if (perfil == null) perfil = "";
        perfil = perfil.trim();

        Class<?> next;

        if ("sistemas".equalsIgnoreCase(perfil)) {
            next = com.farenet.descuentos.ui.selection.sistemas.SelectionSistemasActivity.class;
        } else if ("operaciones".equalsIgnoreCase(perfil)) {
            next = com.farenet.descuentos.ui.selection.operaciones.SelectionOperacionesActivity.class;
        } else if ("comercial".equalsIgnoreCase(perfil)) {
            // ⬅️ ANTES: HomeFragment.class (crash)
            // AHORA: host Activity con ViewPager2
            next = ComercialHostActivity.class;
        } else if ("asistente_servicio".equalsIgnoreCase(perfil)) {
            next = com.farenet.descuentos.ui.selection.asistente.SelectionAsistenteActivity.class;
        } else {
            // Fallback razonable
            next = com.farenet.descuentos.ui.selection.asistente.SelectionAsistenteActivity.class;
        }

        Intent i = new Intent(this, next);
        // Si Bootstrap es tu launcher, limpia el back stack:
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    // ===== PRECARGAS NO CRÍTICAS =====

    private void prefetchMaestros(String token) {
        inc(); maestroRepo.getPlantas(token)           .enqueue(doneList("Plantas"));
        inc(); maestroRepo.getAutorizadores(token)     .enqueue(doneList("Autorizadores"));
        inc(); maestroRepo.getConceptoinspeccion(token).enqueue(doneList("Conceptos"));
        inc(); maestroRepo.getTipoPagoDescuento(token) .enqueue(doneList("TipoPago"));
    }

    private void prefetchMotivos() {
        inc();
        NewApiClient.get().getMotivosCortesia(true).enqueue(new Callback<List<MotivoCortesia>>() {
            @Override public void onResponse(Call<List<MotivoCortesia>> call, Response<List<MotivoCortesia>> response) { dec(); }
            @Override public void onFailure(Call<List<MotivoCortesia>> call, Throwable t) { dec(); }
        });

        try {
            inc();
            NewApiClient.get().getMotivosDescuento(true).enqueue(new Callback<List<MotivoDescuento>>() {
                @Override public void onResponse(Call<List<MotivoDescuento>> call, Response<List<MotivoDescuento>> response) { dec(); }
                @Override public void onFailure(Call<List<MotivoDescuento>> call, Throwable t) { dec(); }
            });
        } catch (Throwable ignore) {
            // endpoint aún no disponible
        }
    }

    private void prefetchDashboard() {
        NewApiClient.get().listarSolicitudes(
                resolveUsername(), "PENDIENTE_AUT",
                null, null, 500, 0, "-creado_en"
        ).enqueue(new retrofit2.Callback<java.util.List<com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto>>() {
            @Override public void onResponse(retrofit2.Call<java.util.List<com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto>> call,
                                             retrofit2.Response<java.util.List<com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto>> rsp) {
                int n = (rsp.isSuccessful() && rsp.body() != null) ? rsp.body().size() : 0;
                DashboardCache.setKpiPendAut(n);
            }
            @Override public void onFailure(retrofit2.Call<java.util.List<com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto>> call, Throwable t) {
                DashboardCache.setKpiPendAut(0);
            }
        });

        NewApiClient.get().listarSolicitudes(
                resolveUsername(), "APROBADA",
                "DESCUENTO", null, 500, 0, "-creado_en"
        ).enqueue(new retrofit2.Callback<java.util.List<com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto>>() {
            @Override public void onResponse(retrofit2.Call<java.util.List<com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto>> call,
                                             retrofit2.Response<java.util.List<com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto>> rsp) {
                int n = (rsp.isSuccessful() && rsp.body() != null) ? rsp.body().size() : 0;
                DashboardCache.setKpiAprobadas(n);
            }
            @Override public void onFailure(retrofit2.Call<java.util.List<com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto>> call, Throwable t) {
                DashboardCache.setKpiAprobadas(0);
            }
        });

        NewApiClient.get().listarSolicitudes(
                resolveUsername(), null, null, null,
                3, 0, "-creado_en"
        ).enqueue(new retrofit2.Callback<java.util.List<com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto>>() {
            @Override public void onResponse(retrofit2.Call<java.util.List<com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto>> call,
                                             retrofit2.Response<java.util.List<com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto>> rsp) {
                if (rsp.isSuccessful() && rsp.body() != null) {
                    DashboardCache.setUltimas(rsp.body());
                }
            }
            @Override public void onFailure(retrofit2.Call<java.util.List<com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto>> call, Throwable t) { }
        });
    }

    /** Precarga de Bolsa por reflexión (best-effort). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void prefetchBolsaEstadoSilencioso() {
        try {
            Object service = NewApiClient.get();
            java.lang.reflect.Method m;
            Object callObj;

            try {
                m = service.getClass().getMethod("bolsaEstado", String.class);
                callObj = m.invoke(service, resolveUsername());
            } catch (NoSuchMethodException noUser) {
                try {
                    m = service.getClass().getMethod("bolsaEstado");
                    callObj = m.invoke(service);
                } catch (NoSuchMethodException noParam) {
                    try {
                        m = service.getClass().getMethod("obtenerBolsaEstado", String.class);
                        callObj = m.invoke(service, resolveUsername());
                    } catch (NoSuchMethodException nope) {
                        try {
                            m = service.getClass().getMethod("obtenerBolsaEstado");
                            callObj = m.invoke(service);
                        } catch (NoSuchMethodException nope2) {
                            return; // no hay endpoint
                        }
                    }
                }
            }

            if (!(callObj instanceof retrofit2.Call)) return;

            retrofit2.Call call = (retrofit2.Call) callObj;
            inc();
            call.enqueue(new retrofit2.Callback() {
                @Override public void onResponse(retrofit2.Call c, retrofit2.Response rsp) {
                    try {
                        if (rsp.isSuccessful()) {
                            BolsaCache.set(rsp.body());
                        }
                    } finally {
                        dec();
                    }
                }
                @Override public void onFailure(retrofit2.Call c, Throwable t) { dec(); }
            });
        } catch (Throwable ignore) {
            // best-effort
        }
    }

    private <T> Callback<List<T>> doneList(String tag) {
        return new Callback<List<T>>() {
            @Override public void onResponse(Call<List<T>> call, Response<List<T>> rsp) { dec(); }
            @Override public void onFailure(Call<List<T>> call, Throwable t) { dec(); }
        };
    }

    // pending helpers
    private void inc() { pending.incrementAndGet(); }
    private void dec() {
        int left = pending.decrementAndGet();
        if (left <= 0) onCatalogosOk();
    }

    // Util
    private String resolveUsername() {
        String u = session.getUsername();
        if (u != null && !u.trim().isEmpty()) return u.trim();
        String legacyUser = legacyPrefs.getString("user", null);
        if (legacyUser != null && !legacyUser.trim().isEmpty()) return legacyUser.trim();
        UsuarioPerfil up = session.getPerfil();
        if (up != null && up.username != null && !up.username.trim().isEmpty()) return up.username.trim();
        return null;
    }

    private void goToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTipsRotator();
    }
}
