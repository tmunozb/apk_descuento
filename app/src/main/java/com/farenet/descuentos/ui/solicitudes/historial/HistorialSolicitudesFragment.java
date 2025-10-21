package com.farenet.descuentos.ui.solicitudes.historial;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto;
import com.farenet.descuentos.Core.Network.NewApiClient;
import com.farenet.descuentos.Core.Storage.SessionManager;
import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.solicitudes.comunes.adapter.SolicitudSimpleAdapter;
import com.farenet.descuentos.ui.solicitudes.model.SolicitudUI;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Historial de solicitudes (como Fragment).
 * Reutiliza los mismos IDs del layout de la actividad anterior.
 */
public class HistorialSolicitudesFragment extends Fragment {

    private MaterialAutoCompleteTextView actTipo, actEstado;
    private TextView tvEmpty;
    private RecyclerView rv;
    private View progress;
    private SwipeRefreshLayout swipe;
    private ChipGroup chipsQuick;

    private SolicitudSimpleAdapter adapter;
    private final List<SolicitudUI> data = new ArrayList<>();

    private Call<List<SolicitudDto>> listCall;
    private SessionManager session;

    // Fuente del dropdown (mismo orden que verás en UI)
    private static final String[] TIPOS_UI   = {"Todos", "Descuento", "Cortesía", "Bolsa"};
    private static final String[] ESTADOS_UI = {"Todos","Ingresada", "Aprobada", "Procesada", "Rechazada", "Pendiente", "Enviada"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Puedes crear un nuevo layout "fragment_historial_solicitudes" con los mismos IDs,
        // o referenciar directamente R.layout.activity_historial_solicitudes si ya te sirve.
        return inflater.inflate(R.layout.fragment_historial_solicitudes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session = new SessionManager(requireContext());

        actTipo    = view.findViewById(R.id.act_tipo);
        actEstado  = view.findViewById(R.id.act_estado);
        tvEmpty    = view.findViewById(R.id.tv_empty);
        rv         = view.findViewById(R.id.rv);
        swipe      = view.findViewById(R.id.swipe);
        chipsQuick = view.findViewById(R.id.chips_quick);
        progress   = view.findViewById(android.R.id.progress);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(true);
        adapter = new SolicitudSimpleAdapter(data, s -> {
            // TODO: abrir detalle si aplica
        });
        rv.setAdapter(adapter);

        // MaterialAutoCompleteTextView (M3)
        if (actTipo != null)   actTipo.setSimpleItems(TIPOS_UI);
        if (actEstado != null) actEstado.setSimpleItems(ESTADOS_UI);

        // Valores por defecto
        if (actTipo != null)   actTipo.setText("Todos", false);
        if (actEstado != null) actEstado.setText("Todos", false);

        // Pull-to-refresh
        if (swipe != null) swipe.setOnRefreshListener(this::fetch);

        // Dropdown listeners -> recargar y sincronizar chips
        if (actTipo != null) {
            actTipo.setOnItemClickListener((p, v, i, id) -> fetch());
        }
        if (actEstado != null) {
            actEstado.setOnItemClickListener((p, v, i, id) -> {
                syncChipsWithEstado(value(actEstado.getText()));
                fetch();
            });
        }

        // Chips rápidos -> sincronizar dropdown
        if (chipsQuick != null) {
            chipsQuick.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == View.NO_ID) return;
                String sel = "Todos";
                if (checkedId == R.id.chip_aprobadas)     sel = "Aprobada";
                else if (checkedId == R.id.chip_rechazadas) sel = "Ingresada";
                else if (checkedId == R.id.chip_rechazadas) sel = "Rechazada";
                else if (checkedId == R.id.chip_procesadas) sel = "Procesada";
                else if (checkedId == R.id.chip_pendientes) sel = "Pendiente";
                else if (checkedId == R.id.chip_enviadas)   sel = "Enviada";
                if (actEstado != null) actEstado.setText(sel, false);
                fetch();
            });
            chipsQuick.check(R.id.chip_all);
        }

        // Primera carga
        fetch();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listCall != null) listCall.cancel();
    }

    private String getUser() {
        String u = (session != null) ? session.getUsername() : null;
        if (!TextUtils.isEmpty(u)) return u.trim();
        return "tmunoz"; // fallback
    }

    private void fetch() {
        final String tipoUI   = value(actTipo == null ? "" : actTipo.getText());
        final String estadoUI = value(actEstado == null ? "" : actEstado.getText());

        final String tipoApi   = mapTipoToApi(tipoUI);     // null si "Todos"
        final String estadoApi = mapEstadoToApi(estadoUI); // null si "Todos"

        setRefreshing(true);
        showLoading(true);

        if (listCall != null) listCall.cancel();

        // Orden por más reciente (pide amplio y filtramos en cliente para consistencia inmediata)
        listCall = NewApiClient.get().listarSolicitudes(
                getUser(),      // usuario para server (si aplica en tu backend)
                null,           // estado en server (filtramos aquí abajo)
                null,           // tipo en server   (filtramos aquí abajo)
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
                    applyData(new ArrayList<>());
                    toast("No se pudo cargar historial");
                    return;
                }

                // 1) Mapeo DTO -> UI
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

                // 2) Filtro en memoria (por si el API no filtra exactamente)
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
                applyData(new ArrayList<>());
                toast("Error de red");
            }
        });
    }

    private void applyData(List<SolicitudUI> items) {
        data.clear();
        if (items != null) data.addAll(items);
        if (adapter != null) adapter.notifyDataSetChanged();

        boolean empty = data.isEmpty();
        if (tvEmpty != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (rv != null) rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void syncChipsWithEstado(String estadoUI) {
        if (chipsQuick == null) return;
        if ("Aprobada".equalsIgnoreCase(estadoUI)) {
            chipsQuick.check(R.id.chip_aprobadas);
        } else if ("Rechazada".equalsIgnoreCase(estadoUI)) {
            chipsQuick.check(R.id.chip_ingresadas);
        } else if ("Ingresada".equalsIgnoreCase(estadoUI)) {
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

    // ===== Helpers =====
    private void toast(String s) {
        if (!isAdded()) return;
        android.widget.Toast.makeText(requireContext(), s, android.widget.Toast.LENGTH_LONG).show();
    }

    private static String value(CharSequence cs) { return cs == null ? "" : cs.toString().trim(); }
    private static String nz(String s) { return s == null ? "" : s; }

    private static String capFirst(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0,1).toUpperCase(Locale.getDefault())
                + s.substring(1).toLowerCase(Locale.getDefault());
    }

    // Normalización UI -> API
    private static String mapTipoToApi(String t) {
        if ("Todos".equalsIgnoreCase(t) || t.isEmpty()) return null;
        String x = t.toLowerCase(Locale.ROOT);
        if (x.startsWith("descu")) return "DESCUENTO";
        if (x.startsWith("corte")) return "CORTESIA";
        if (x.startsWith("bolsa")) return "BOLSA";
        return t.toUpperCase(Locale.ROOT);
    }

    private static String mapEstadoToApi(String e) {
        if ("Todos".equalsIgnoreCase(e) || e.isEmpty()) return null;
        String x = e.toLowerCase(Locale.ROOT);
        if (x.startsWith("apro")) return "APROBADA";
        if (x.startsWith("ingre")) return "INGRESADA";
        if (x.startsWith("recha")) return "RECHAZADA";
        if (x.startsWith("proce")) return "PROCESADA";
        if (x.startsWith("pend")) return "PENDIENTE";
        if (x.startsWith("envi")) return "ENVIADA";
        return e.toUpperCase(Locale.ROOT);
    }
}
