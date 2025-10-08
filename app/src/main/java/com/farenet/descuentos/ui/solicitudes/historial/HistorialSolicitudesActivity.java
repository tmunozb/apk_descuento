package com.farenet.descuentos.ui.solicitudes.historial;

import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistorialSolicitudesActivity extends AppCompatActivity {

    private AutoCompleteTextView actTipo, actEstado;
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
        return "jperez";
    }

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

        // Usa el adapter que te paso abajo (asegúrate de importarlo)
        adapter = new SolicitudSimpleAdapter(data, s -> {
            // TODO abrir detalle si aplica
        });
        rv.setAdapter(adapter);

        // Adapters de filtros (dropdowns)
        actTipo.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new String[]{"Todos", "Descuento", "Cortesía"}));

        actEstado.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new String[]{"Todos", "Aprobada", "Rechazada", "Pendiente"}));

        // Defaults
        actTipo.setText("Todos", false);
        actEstado.setText("Aprobada", false);

        // Listeners de dropdowns -> recargar
        actTipo.setOnItemClickListener((p, v, i, id) -> fetch());
        actEstado.setOnItemClickListener((p, v, i, id) -> fetch());

        // Swipe-to-refresh
        if (swipe != null) swipe.setOnRefreshListener(this::fetch);

        // Chips rápidos (usar if/else, no switch, por non-final R)
        if (chipsQuick != null) {
            chipsQuick.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == View.NO_ID) return;

                if (checkedId == R.id.chip_all) {
                    actEstado.setText("Todos", false);
                } else if (checkedId == R.id.chip_aprobadas) {
                    actEstado.setText("Aprobada", false);
                } else if (checkedId == R.id.chip_rechazadas) {
                    actEstado.setText("Rechazada", false);
                } else if (checkedId == R.id.chip_pendientes) {
                    actEstado.setText("Pendiente", false);
                }
                fetch();
            });
        }

        // Primera carga
        fetch();
    }

    private void fetch() {
        String tipoUI   = value(actTipo.getText());
        String estadoUI = value(actEstado.getText());

        String tipo   = mapTipoToApi(tipoUI);
        String estado = mapEstadoToApi(estadoUI);

        setRefreshing(true);
        showLoading(true);

        if (listCall != null) listCall.cancel();

        listCall = NewApiClient.get().listarSolicitudes(
                getUser(),
                estado,
                tipo,
                null,
                50,
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
                List<SolicitudUI> mapped = new ArrayList<>();
                for (SolicitudDto d : rsp.body()) {
                    // FIX: fallback correcto de fecha
                    String fecha = nz(d.creadoEn);
                    if (fecha.isEmpty()) fecha = nz(d.creadoEn);

                    mapped.add(new SolicitudUI(
                            nz(d.id),
                            nz(capFirst(d.tipo)),
                            nz(d.placa),
                            nz(d.plantaNombre),
                            nz(d.motivo),
                            nz(capFirst(d.estado)),
                            nz(fecha)
                    ));
                }
                applyData(mapped);
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

    private String mapTipoToApi(String t) {
        if ("Todos".equalsIgnoreCase(t) || t.isEmpty()) return null;
        if (t.toLowerCase(Locale.ROOT).startsWith("descu")) return "DESCUENTO";
        if (t.toLowerCase(Locale.ROOT).startsWith("corte")) return "CORTESIA";
        return t.toUpperCase(Locale.ROOT);
    }

    private String mapEstadoToApi(String e) {
        if ("Todos".equalsIgnoreCase(e) || e.isEmpty()) return null;
        if (e.toLowerCase(Locale.ROOT).startsWith("apro")) return "APROBADA";
        if (e.toLowerCase(Locale.ROOT).startsWith("recha")) return "RECHAZADA";
        if (e.toLowerCase(Locale.ROOT).startsWith("pend")) return "PENDIENTE";
        return e.toUpperCase(Locale.ROOT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listCall != null) listCall.cancel();
    }
}
