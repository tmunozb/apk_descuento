// ui/selection/comercial/SelectionComercialActivity.java
package com.farenet.descuentos.ui.selection.comercial;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudPendienteDto;
import com.farenet.descuentos.Core.Network.NewApiClient;
import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.bolsa.BolsaEstadoActivity;
import com.farenet.descuentos.ui.selection.base.BaseSelectionActivity;
import com.farenet.descuentos.ui.solicitudes.comunes.adapter.SolicitudSimpleAdapter;
import com.farenet.descuentos.ui.solicitudes.historial.HistorialSolicitudesActivity;
import com.farenet.descuentos.ui.solicitudes.model.SolicitudUI;
import com.farenet.descuentos.ui.solicitudes.pendientes.SolicitudesPendientesActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.appbar.MaterialToolbar;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectionComercialActivity extends BaseSelectionActivity {

    // Cambia a true si quieres que "Aprobadas" sea SOLO del mes actual.
    private static final boolean ONLY_THIS_MONTH_FOR_APPROVED = false;

    // Views KPI
    private TextView tvKpiPendientes;
    private TextView tvKpiAprobadas;

    // “Últimas solicitudes”
    private RecyclerView rvUltimos;
    private EditText etBuscarUltimos;
    private SolicitudSimpleAdapter ultimosAdapter;
    private final List<SolicitudUI> ultimosData = new ArrayList<>();

    // Llamadas en curso
    private Call<List<SolicitudPendienteDto>> callPend;
    private Call<List<SolicitudDto>>          callAprob;
    private Call<List<SolicitudDto>>          callUltimas;

    private String getUser() {
        // TODO: reemplazar por usuario logueado real
        return "tmunoz";
    }

    @Override
    protected Config provideConfig() {
        Config c = new Config();
        c.layoutRes = R.layout.activity_selection_comercial;
        c.menuRes   = R.menu.menu_selection_comercial;
        return c;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Toolbar: logout (igual que tu versión)
        MaterialToolbar tb = toolbar;
        if (tb != null) {
            tb.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.nav_logout) {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Cerrar sesión")
                            .setMessage("¿Desea cerrar sesión y borrar el caché local?")
                            .setPositiveButton("Sí", (d, w) -> {
                                session.clear();
                                startActivity(new Intent(this, com.farenet.descuentos.ui.auth.LoginActivity.class)
                                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                | Intent.FLAG_ACTIVITY_NEW_TASK
                                                | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                finish();
                            })
                            .setNegativeButton("No", null)
                            .show();
                    return true;
                }
                return false;
            });
        }

        // Saludo
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        if (tvWelcome != null) {
            String nombre = session.getNombreVisible();
            tvWelcome.setText("¡Hola, " + (nombre == null ? "Comercial" : nombre) + "!");
        }

        // KPIs
        tvKpiPendientes = findViewById(R.id.tvKpiPendientes);
        tvKpiAprobadas  = findViewById(R.id.tvKpiAprobadas);

        // Tiles
        MaterialCardView tileNueva = findViewById(R.id.tileBolsaEstado);
        if (tileNueva != null) {
            ((TextView) tileNueva.findViewById(R.id.tvTitle)).setText("Bolsa");
            ((TextView) tileNueva.findViewById(R.id.tvSubtitle)).setText("Descuento Autorizado");
            ((ImageView) tileNueva.findViewById(R.id.ivIcon)).setImageResource(R.drawable.ic_add_circle_24);
            tileNueva.setOnClickListener(v -> startActivity(new Intent(this, BolsaEstadoActivity.class)));
        }

        MaterialCardView tilePend = findViewById(R.id.tilePendientesAut);
        if (tilePend != null) {
            ((TextView) tilePend.findViewById(R.id.tvTitle)).setText("Pendientes (Aut.)");
            ((TextView) tilePend.findViewById(R.id.tvSubtitle)).setText("Aprobar / Autorizar");
            ((ImageView) tilePend.findViewById(R.id.ivIcon)).setImageResource(R.drawable.ic_pending_actions_24);
            tilePend.setOnClickListener(v -> startActivity(new Intent(this, SolicitudesPendientesActivity.class)));
        }

        MaterialCardView tileHist = findViewById(R.id.tileHistorial);
        if (tileHist != null) {
            ((TextView) tileHist.findViewById(R.id.tvTitle)).setText("Historial");
            ((TextView) tileHist.findViewById(R.id.tvSubtitle)).setText("Aprobadas / Rechazadas");
            ((ImageView) tileHist.findViewById(R.id.ivIcon)).setImageResource(R.drawable.ic_history_24);
            tileHist.setOnClickListener(v -> startActivity(new Intent(this, HistorialSolicitudesActivity.class)));
        }

        MaterialCardView tileRep = findViewById(R.id.tileReportes);
        if (tileRep != null) {
            ((TextView) tileRep.findViewById(R.id.tvTitle)).setText("Reportes");
            ((TextView) tileRep.findViewById(R.id.tvSubtitle)).setText("KPIs / Exportar");
            ((ImageView) tileRep.findViewById(R.id.ivIcon)).setImageResource(R.drawable.ic_bar_chart);
            tileRep.setOnClickListener(v -> { /* abrir módulo reportes */ });
        }

        // Bottom nav
        BottomNavigationView bottom = findViewById(R.id.bottomNav);
        if (bottom != null) {
            bottom.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.tab_home) return true;
                if (id == R.id.tab_solicitudes) {
                    startActivity(new Intent(this, SolicitudesPendientesActivity.class));
                    return true;
                }
                if (id == R.id.tab_reportes) {
                    // abrir reportes
                    return true;
                }
                return false;
            });
            bottom.setSelectedItemId(R.id.tab_home);
        }

        // ====== “ÚLTIMAS SOLICITUDES” ======
        rvUltimos = findViewById(R.id.rvUltimos);
        etBuscarUltimos = findViewById(R.id.etBuscarUltimos);

        if (rvUltimos != null) {
            rvUltimos.setLayoutManager(new LinearLayoutManager(this));
            rvUltimos.setHasFixedSize(true);
            rvUltimos.setNestedScrollingEnabled(false);  // ← clave para que no “salte”
            rvUltimos.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
            ultimosAdapter = new SolicitudSimpleAdapter(ultimosData, s -> { /* detalle */ });
            rvUltimos.setAdapter(ultimosAdapter);
        }


        // Buscar al pulsar “buscar” en el teclado
        if (etBuscarUltimos != null) {
            etBuscarUltimos.setOnEditorActionListener((v, actionId, event) -> {
                boolean isSearch = actionId == EditorInfo.IME_ACTION_SEARCH
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
                if (isSearch) {
                    loadUltimas(trimOrEmpty(etBuscarUltimos.getText()));
                    return true;
                }
                return false;
            });
        }

        // Cargar datos al abrir
        loadKpis();
        loadUltimas(null); // sin filtro
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarga por si cambió algo mientras estabas en otra pantalla
        loadKpis();
        loadUltimas(trimOrEmpty(etBuscarUltimos == null ? null : etBuscarUltimos.getText()));
    }

    /** KPIs **/
    private void loadKpis() {
        setKpiLoading();

        if (callPend != null) callPend.cancel();
// usamos la misma firma que para aprobadas
        Call<List<SolicitudDto>> callPendAut = NewApiClient.get().listarSolicitudes(
                getUser(),              // o null si quieres global
                "PENDIENTE_AUT",        // ← solo las de autorización
                null, null,
                500, 0,
                "-creado_en"
        );
        callPendAut.enqueue(new Callback<List<SolicitudDto>>() {
            @Override public void onResponse(Call<List<SolicitudDto>> call, Response<List<SolicitudDto>> rsp) {
                if (!rsp.isSuccessful() || rsp.body() == null) {
                    tvKpiPendientes.setText("–");
                    return;
                }
                tvKpiPendientes.setText(String.valueOf(rsp.body().size()));
            }
            @Override public void onFailure(Call<List<SolicitudDto>> call, Throwable t) {
                if (call.isCanceled()) return;
                tvKpiPendientes.setText("–");
            }
        });


        // 2) Aprobadas: /solicitudes?estado=APROBADA
        if (callAprob != null) callAprob.cancel();
        callAprob = NewApiClient.get().listarSolicitudes(
                getUser(),      // user del comercial
                "APROBADA",
                "DESCUENTO",           // tipo
                null,           // q
                500,            // limit
                0,              // offset
                "-creado_en"    // recientes primero
        );
        callAprob.enqueue(new Callback<List<SolicitudDto>>() {
            @Override public void onResponse(Call<List<SolicitudDto>> call,
                                             Response<List<SolicitudDto>> rsp) {
                if (!rsp.isSuccessful() || rsp.body() == null) {
                    tvKpiAprobadas.setText("–");
                    return;
                }
                List<SolicitudDto> list = rsp.body();
                if (ONLY_THIS_MONTH_FOR_APPROVED) {
                    tvKpiAprobadas.setText(String.valueOf(filterThisMonthCount(list)));
                } else {
                    tvKpiAprobadas.setText(String.valueOf(list.size()));
                }
            }
            @Override public void onFailure(Call<List<SolicitudDto>> call, Throwable t) {
                if (call.isCanceled()) return;
                tvKpiAprobadas.setText("–");
            }
        });
    }

    /** Últimas solicitudes (todas), orden -creado_en, con búsqueda opcional por q */
    private void loadUltimas(String q) {
        if (callUltimas != null) callUltimas.cancel();

        final boolean hasQuery = q != null && !q.trim().isEmpty();
        final int limit = hasQuery ? 20 : 3;   // ← 3 recientes; si buscas, muestra más

        callUltimas = NewApiClient.get().listarSolicitudes(
                getUser(),            // o null si quieres global
                null,                 // estado => todos
                null,                 // tipo   => todos
                hasQuery ? q.trim() : null, // q (placa/código)
                limit,                // ← aquí
                0,
                "-creado_en"          // más recientes primero
        );

        callUltimas.enqueue(new Callback<List<SolicitudDto>>() {
            @Override public void onResponse(Call<List<SolicitudDto>> call, Response<List<SolicitudDto>> rsp) {
                if (!rsp.isSuccessful() || rsp.body() == null) {
                    applyUltimas(new ArrayList<>());
                    return;
                }
                List<SolicitudUI> mapped = new ArrayList<>();
                for (SolicitudDto d : rsp.body()) {
                    mapped.add(new SolicitudUI(
                            nz(d.codigo),
                            nz(capFirst(d.tipo)),
                            nz(d.placa),
                            nz(d.plantaNombre),
                            nz(d.motivo),
                            nz(capFirst(d.estado)),
                            nz(d.creadoEn)
                    ));
                }
                applyUltimas(mapped);
            }
            @Override public void onFailure(Call<List<SolicitudDto>> call, Throwable t) {
                if (call.isCanceled()) return;
                applyUltimas(new ArrayList<>());
            }
        });
    }


    private void applyUltimas(List<SolicitudUI> items) {
        ultimosData.clear();
        if (items != null) ultimosData.addAll(items);
        if (ultimosAdapter != null) ultimosAdapter.notifyDataSetChanged();
    }

    /** Utils **/
    private void setKpiLoading() {
        if (tvKpiPendientes != null) tvKpiPendientes.setText("…");
        if (tvKpiAprobadas  != null) tvKpiAprobadas.setText("…");
    }

    private int filterThisMonthCount(List<SolicitudDto> list) {
        if (list == null) return 0;
        LocalDate now = LocalDate.now(ZoneOffset.systemDefault());
        int ym = now.getYear() * 100 + now.getMonthValue();
        int count = 0;
        for (SolicitudDto d : list) {
            String s = d.creadoEn; // ISO8601
            if (s == null || s.isEmpty()) continue;
            try {
                OffsetDateTime odt = OffsetDateTime.parse(s);
                LocalDate ld = odt.toLocalDate();
                int ymItem = ld.getYear() * 100 + ld.getMonthValue();
                if (ymItem == ym) count++;
            } catch (DateTimeParseException ignore) {}
        }
        return count;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }

    private static String trimOrEmpty(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private static String capFirst(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0,1).toUpperCase(Locale.getDefault()) +
                s.substring(1).toLowerCase(Locale.getDefault());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (callPend    != null) callPend.cancel();
        if (callAprob   != null) callAprob.cancel();
        if (callUltimas != null) callUltimas.cancel();
    }
}
