package com.farenet.descuentos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.farenet.descuentos.config.Constante;
import com.farenet.descuentos.domain.Autorizadores;
import com.farenet.descuentos.domain.Conceptoinspeccion;
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.domain.TipoPagoDescuento;
import com.farenet.descuentos.domain.MotivoCortesia;
import com.farenet.descuentos.models.newapi.AccesoPlantaDto;
import com.farenet.descuentos.models.newapi.LoginRsp;
import com.farenet.descuentos.models.newapi.MotivoDescuento;
import com.farenet.descuentos.models.newapi.UsuarioPerfil;
import com.farenet.descuentos.network.newapi.NewApiClient;
import com.farenet.descuentos.network.newapi.NewApiService;
import com.farenet.descuentos.repository.MaestroRepository;
import com.farenet.descuentos.repository.SessionManager;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class BootstrapActivity extends AppCompatActivity {

    private SessionManager session;
    private SharedPreferences legacyPrefs;
    private MaestroRepository maestroRepo;
    private NewApiService newApi;

    // Banderas de gating duro (deben estar true para continuar)
    private final AtomicBoolean perfilOk  = new AtomicBoolean(false);
    private final AtomicBoolean accesosOk = new AtomicBoolean(false);

    // Contador de precargas no críticas (maestros / motivos)
    private final AtomicInteger pending = new AtomicInteger(0);

    // UI moderno
    private TextView tvStep, tvSub, tvTip;
    private com.google.android.material.progressindicator.LinearProgressIndicator progressLinear;
    private com.google.android.material.progressindicator.CircularProgressIndicator progressCircular;
    private ImageView imgLogo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bootstrap);

        // Edge-to-edge sutil
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

        session     = new SessionManager(this);
        legacyPrefs = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);
        maestroRepo = Constante.getMaestroRespository();
        newApi      = NewApiClient.get();

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
        progressLinear = findViewById(R.id.progressLinear);
        progressCircular = findViewById(R.id.progressCircular);

        // Animación sutil al logo (pulse initial)
        if (imgLogo != null) {
            imgLogo.setScaleX(0.9f);
            imgLogo.setScaleY(0.9f);
            imgLogo.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(600)
                    .setStartDelay(150)
                    .start();
        }

        // "latido" suave al tip
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

    private void startTipsRotator() {
        if (tvTip == null) return;
        tvTip.postDelayed(new Runnable() {
            @Override public void run() {
                tipIndex = (tipIndex + 1) % tips.length;
                tvTip.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                    tvTip.setText(tips[tipIndex]);
                    tvTip.animate().alpha(0.9f).setDuration(250).start();
                }).start();
                tvTip.postDelayed(this, 3000);
            }
        }, 3000);
    }

    private void setStep(String title, String subtitle, int progressPercent) {
        if (tvStep != null) tvStep.setText(title);
        if (tvSub  != null) tvSub.setText(subtitle);
        if (progressLinear != null) {
            int p = Math.max(0, Math.min(100, progressPercent));
            progressLinear.setProgressCompat(p, true);
        }
    }

    private void onPerfilInicio() {
        setStep("Validando perfil…", "Comprobando credenciales", 18);
    }
    private void onPerfilOk() {
        setStep("Perfil verificado", "Cargando accesos a plantas", 35);
    }
    private void onAccesosOk() {
        setStep("Accesos listos", "Sincronizando catálogos", 65);
    }
    private void onCatalogosOk() {
        setStep("Catálogos sincronizados", "Listo para despegar", 90);
    }
    private void onFinalizando() {
        setStep("Iniciando aplicación…", "Bienvenido(a)", 100);
        if (tvStep != null) tvStep.postDelayed(this::routeWhenReady, 250);
        else routeWhenReady();
    }

    // ========= FLUJO PRINCIPAL =========

    private void startBootstrap() {
        String token = legacyPrefs.getString("token", null);
        if (token == null || token.trim().isEmpty()) {
            // No hay token legacy -> regresar a login
            setStep("Sesión no encontrada", "Redirigiendo a inicio de sesión…", 5);
            goToLogin();
            return;
        }

        // 1) Asegurar PERFIL y ACCESOS (gating)
        onPerfilInicio();
        ensurePerfilYAccesos();

        // 2) Disparar precargas no críticas (no bloquean el enrutamiento)
        prefetchMaestros(token);
        prefetchMotivos();
    }

    // ===== PERFIL + ACCESOS (gating obligatorio) =====
    private void ensurePerfilYAccesos() {
        UsuarioPerfil up = session.getPerfil();
        boolean needsPerfil  = (up == null || up.perfilId == null || up.perfilId.trim().isEmpty());
        boolean needsAccesos = (session.getAccesos() == null || session.getAccesos().isEmpty());

        if (!needsPerfil) perfilOk.set(true);
        if (!needsAccesos) accesosOk.set(true);

        if (!needsPerfil && !needsAccesos) {
            // Ya estamos listos para enrutar (catálogos seguirán cargando)
            onAccesosOk();
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

                    // Si vinieron accesos, guárdalos ya
                    if (body.accesos != null && !body.accesos.isEmpty()) {
                        session.saveAccesos(body.accesos);
                        accesosOk.set(true);
                        onAccesosOk();
                        onFinalizando();
                    } else {
                        // Faltan accesos, pedirlos
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

    private void routeWhenReady() {
        // Solo avanzamos si PERFIL + ACCESOS están OK
        if (!(perfilOk.get() && accesosOk.get())) return;

        // Siempre debe abrir el menú (SelectionActivity)
        Intent i = new Intent(this, SelectionActivity.class);
        // Si quisieras abrir Cortesía automáticamente DESPUÉS, podrías usar un extra:
        // i.putExtra("jump_to", "cortesia");
        startActivity(i);
        finish();
    }

    // ===== PRECARGAS NO CRÍTICAS (no bloquean navegación) =====
    private void prefetchMaestros(String token) {
        // Estos no bloquean el enrutamiento. Sólo mejoran la experiencia al llegar al menú.
        inc(); maestroRepo.getPlantas(token).enqueue(doneList("Plantas"));
        inc(); maestroRepo.getAutorizadores(token).enqueue(doneList("Autorizadores"));
        inc(); maestroRepo.getConceptoinspeccion(token).enqueue(doneList("Conceptos"));
        inc(); maestroRepo.getTipoPagoDescuento(token).enqueue(doneList("TipoPago"));
    }

    /** Helper genérico para cerrar un pending luego de cualquier llamada que devuelve List<T>. */
    private <T> Callback<List<T>> doneList(String tag) {
        return new Callback<List<T>>() {
            @Override public void onResponse(Call<List<T>> call, Response<List<T>> rsp) { dec(); }
            @Override public void onFailure(Call<List<T>> call, Throwable t) { dec(); }
        };
    }

    private void prefetchMotivos() {
        if (newApi == null) return;

        // Motivos Cortesía
        inc();
        newApi.getMotivosCortesia(true).enqueue(new Callback<List<MotivoCortesia>>() {
            @Override public void onResponse(Call<List<MotivoCortesia>> call, Response<List<MotivoCortesia>> response) { dec(); }
            @Override public void onFailure(Call<List<MotivoCortesia>> call, Throwable t) { dec(); }
        });

        // Motivos Descuento (si existe en tu API)
        try {
            inc();
            newApi.getMotivosDescuento(true).enqueue(new Callback<List<MotivoDescuento>>() {
                @Override public void onResponse(Call<List<MotivoDescuento>> call, Response<List<MotivoDescuento>> response) { dec(); }
                @Override public void onFailure(Call<List<MotivoDescuento>> call, Throwable t) { dec(); }
            });
        } catch (Throwable ignore) {
            // Si no está implementado todavía, no romper
        }
    }

    // ===== pending helpers =====
    private void inc() {
        pending.incrementAndGet();
    }
    private void dec() {
        int left = pending.decrementAndGet();
        if (left <= 0) {
            onCatalogosOk();
        }
    }

    // ===== Util =====
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
}
