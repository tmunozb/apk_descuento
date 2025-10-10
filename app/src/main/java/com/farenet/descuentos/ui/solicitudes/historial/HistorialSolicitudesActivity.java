package com.farenet.descuentos.ui.solicitudes.historial;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto;
import com.farenet.descuentos.Core.Network.NewApiClient;
import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.solicitudes.comunes.adapter.SolicitudSimpleAdapter;
import com.farenet.descuentos.ui.solicitudes.model.SolicitudUI;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistorialSolicitudesActivity extends AppCompatActivity {

    private MaterialAutoCompleteTextView actTipo, actEstado;
    private TextView tvEmpty;
    private RecyclerView rv;
    private View progress;
    private SwipeRefreshLayout swipe;
    private ChipGroup chipsQuick;

    private SolicitudSimpleAdapter adapter;
    private final List<SolicitudUI> data = new ArrayList<>();

    private Call<List<SolicitudDto>> listCall;

    private String getUser() {
        // TODO: reemplazar por usuario logueado real
        return "tmunoz";
    }

    // Fuente del dropdown (mismo orden que verás en UI)
    private static final String[] TIPOS_UI = {"Todos", "Descuento", "Cortesía", "Bolsa"};
    private static final String[] ESTADOS_UI = {"Todos", "Aprobada","Procesada", "Rechazada", "Pendiente", "Enviada"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_solicitudes);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        if (tb != null) tb.setNavigationOnClickListener(v -> finish());

        actTipo    = findViewById(R.id.act_tipo);
        actEstado  = findViewById(R.id.act_estado);
        tvEmpty    = findViewById(R.id.tv_empty);
        rv         = findViewById(R.id.rv);
        swipe      = findViewById(R.id.swipe);
        chipsQuick = findViewById(R.id.chips_quick);
        progress   = findViewById(android.R.id.progress);

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setHasFixedSize(true);
        adapter = new SolicitudSimpleAdapter(data, s -> {
            // TODO: abrir detalle si aplica
        });
        rv.setAdapter(adapter);

        // --- DROPDOWNS Material (usa layout/material correcto) ---
        // ¡Más limpio que ArrayAdapter! y asegura estilos M3
        actTipo.setSimpleItems(TIPOS_UI);
        actEstado.setSimpleItems(ESTADOS_UI);

        // Valores por defecto
        actTipo.setText("Todos", false);
        actEstado.setText("Todos", false);

        // Pull-to-refresh
        if (swipe != null) swipe.setOnRefreshListener(this::fetch);

        // Dropdown listeners -> recargar y sincronizar chips
        actTipo.setOnItemClickListener((p, v, i, id) -> fetch());
        actEstado.setOnItemClickListener((p, v, i, id) -> {
            syncChipsWithEstado(value(actEstado.getText()));
            fetch();
        });

        // Chips rápidos -> sincronizar dropdown
        if (chipsQuick != null) {
            chipsQuick.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == View.NO_ID) return;
                String sel = "Todos";
                if (checkedId == R.id.chip_aprobadas)   sel = "Aprobada";
                else if (checkedId == R.id.chip_rechazadas) sel = "Rechazada";
                else if (checkedId == R.id.chip_procesadas) sel = "Procesada";
                else if (checkedId == R.id.chip_pendientes) sel = "Pendiente";
                else if (checkedId == R.id.chip_enviadas)   sel = "Enviada";
                actEstado.setText(sel, false);
                fetch();
            });
            chipsQuick.check(R.id.chip_all);
        }

        // Primera carga
        fetch();
    }

    private void fetch() {
        final String tipoUI   = value(actTipo == null ? "" : actTipo.getText());
        final String estadoUI = value(actEstado == null ? "" : actEstado.getText());

        final String tipoApi   = mapTipoToApi(tipoUI);     // null si "Todos"
        final String estadoApi = mapEstadoToApi(estadoUI); // null si "Todos"

        setRefreshing(true);
        showLoading(true);

        if (listCall != null) listCall.cancel();

        // Orden por más reciente
        listCall = NewApiClient.get().listarSolicitudes(
                getUser(),           // user global (evitas limitar por usuario)
                null,           // estado en server (lo filtramos cliente para ver efecto inmediato)
                null,           // tipo    en server (ídem)
                null,           // q
                200,
                0,
                "-creado_en"
        );

        listCall.enqueue(new Callback<List<SolicitudDto>>() {
            @Override public void onResponse(Call<List<SolicitudDto>> call, Response<List<SolicitudDto>> rsp) {
                setRefreshing(false);
                showLoading(false);
                if (!rsp.isSuccessful() || rsp.body() == null) {
                    toast("No se pudo cargar historial");
                    applyData(new ArrayList<>());
                    return;
                }

                // 1) Mapeo
                List<SolicitudUI> mapped = new ArrayList<>();
                for (SolicitudDto d : rsp.body()) {
                    mapped.add(new SolicitudUI(
                            nz(d.codigo),                 // mostrar CÓDIGO
                            nz(capFirst(d.tipo)),
                            nz(d.placa),
                            nz(d.plantaNombre),
                            nz(d.motivo),
                            nz(capFirst(d.estado)),
                            nz(d.creadoEn)
                    ));
                }

                // 2) Filtro en memoria (fallback si server no filtra)
                List<SolicitudUI> filtered = new ArrayList<>();
                for (SolicitudUI s : mapped) {
                    if (tipoApi != null && !tipoApi.equalsIgnoreCase(mapTipoToApi(s.tipo))) continue;
                    if (estadoApi != null && !estadoApi.equalsIgnoreCase(mapEstadoToApi(s.estado))) continue;
                    filtered.add(s);
                }

                applyData(filtered);
            }
            @Override public void onFailure(Call<List<SolicitudDto>> call, Throwable t) {
                if (call.isCanceled()) return;
                setRefreshing(false);
                showLoading(false);
                toast("Error de red");
                applyData(new ArrayList<>());
            }
        });
    }

    private void applyData(List<SolicitudUI> items) {
        data.clear();
        if (items != null) data.addAll(items);
        adapter.notifyDataSetChanged();

        boolean empty = data.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void syncChipsWithEstado(String estadoUI) {
        if (chipsQuick == null) return;
        if ("Aprobada".equalsIgnoreCase(estadoUI)) {
            chipsQuick.check(R.id.chip_aprobadas);
        } else if ("Rechazada".equalsIgnoreCase(estadoUI)) {
            chipsQuick.check(R.id.chip_rechazadas);
        } else if ("Procesada".equalsIgnoreCase(estadoUI)) {
            chipsQuick.check(R.id.chip_procesadas);
        } else if ("Pendiente".equalsIgnoreCase(estadoUI)) {
            chipsQuick.check(R.id.chip_pendientes);
        } else if ("Enviada".equalsIgnoreCase(estadoUI)) {
            chipsQuick.check(R.id.chip_enviadas);
        } else {
            chipsQuick.check(R.id.chip_all);
        }
    }

    private void setRefreshing(boolean refreshing) {
        if (swipe != null) swipe.setRefreshing(refreshing);
    }

    private void showLoading(boolean show) {
        if (progress != null) progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void toast(String s) {
        android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_LONG).show();
    }

    private String value(CharSequence cs) { return cs == null ? "" : cs.toString().trim(); }
    private String nz(String s) { return s == null ? "" : s; }

    private String capFirst(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0,1).toUpperCase(Locale.getDefault()) +
                s.substring(1).toLowerCase(Locale.getDefault());
    }

    // Normalización UI -> API
    private String mapTipoToApi(String t) {
        if ("Todos".equalsIgnoreCase(t) || t.isEmpty()) return null;
        String x = t.toLowerCase(Locale.ROOT);
        if (x.startsWith("descu")) return "DESCUENTO";
        if (x.startsWith("corte")) return "CORTESIA";
        return t.toUpperCase(Locale.ROOT);
    }

    private String mapEstadoToApi(String e) {
        if ("Todos".equalsIgnoreCase(e) || e.isEmpty()) return null;
        String x = e.toLowerCase(Locale.ROOT);
        if (x.startsWith("apro")) return "APROBADA";
        if (x.startsWith("recha")) return "RECHAZADA";
        if (x.startsWith("proce")) return "PROCESADA";
        if (x.startsWith("pend")) return "PENDIENTE";
        if (x.startsWith("envi")) return "ENVIADA";
        return e.toUpperCase(Locale.ROOT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listCall != null) listCall.cancel();
    }
}
