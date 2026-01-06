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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Historial de solicitudes (Fragment).
 * Muestra Monto y "Aprobó" (desde d.aprobadoPorNombre) dentro de 'motivo'.
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

    private static final String[] TIPOS_UI   = {"Todos", "Descuento", "Bolsa"};
    private static final String[] ESTADOS_UI = {"Todos", "Ingresada", "Procesada", "Rechazada", "Pendiente", "Enviada"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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
        adapter = new SolicitudSimpleAdapter(data, s -> { /* abrir detalle si aplica */ });
        rv.setAdapter(adapter);

        if (actTipo != null)   actTipo.setSimpleItems(TIPOS_UI);
        if (actEstado != null) actEstado.setSimpleItems(ESTADOS_UI);

        if (actTipo != null)   actTipo.setText("Todos", false);
        if (actEstado != null) actEstado.setText("Todos", false);

        if (swipe != null) swipe.setOnRefreshListener(this::fetch);

        if (actTipo != null)   actTipo.setOnItemClickListener((p, v, i, id) -> fetch());
        if (actEstado != null) {
            actEstado.setOnItemClickListener((p, v, i, id) -> {
                syncChipsWithEstado(value(actEstado.getText()));
                fetch();
            });
        }

        if (chipsQuick != null) {
            chipsQuick.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == View.NO_ID) return;
                String sel = "Todos";
                if      (checkedId == R.id.chip_ingresadas)  sel = "Ingresada";
                else if (checkedId == R.id.chip_rechazadas)  sel = "Rechazada";
                else if (checkedId == R.id.chip_procesadas)  sel = "Procesada";
                else if (checkedId == R.id.chip_pendientes)  sel = "Pendiente";
                else if (checkedId == R.id.chip_enviadas)    sel = "Enviada";
                else                                         sel = "Todos";
                if (actEstado != null) actEstado.setText(sel, false);
                fetch();
            });
            chipsQuick.check(R.id.chip_all);
        }

        fetch();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listCall != null) listCall.cancel();
    }

    private String getUser() {
        String u = (session != null) ? session.getUsername() : null;
        return TextUtils.isEmpty(u) ? "tmunoz" : u.trim();
    }

    private void fetch() {
        final String tipoUI   = value(actTipo == null ? "" : actTipo.getText());
        final String estadoUI = value(actEstado == null ? "" : actEstado.getText());

        final String tipoApi   = mapTipoToApi(tipoUI);     // null si "Todos"
        final String estadoApi = mapEstadoToApi(estadoUI); // null si "Todos"

        setRefreshing(true);
        showLoading(true);

        if (listCall != null) listCall.cancel();

        listCall = NewApiClient.get().listarSolicitudes(
                getUser(),
                null,      // estado servidor
                null,      // tipo servidor
                null,      // q
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

                // Mapear e inyectar "Monto" y "Aprobó" (desde aprobadoPorNombre) en 'motivo'
                List<SolicitudUI> mapped = new ArrayList<>();
                for (SolicitudDto d : rsp.body()) {
                    String conceptoDisplay = "";
                    if (!TextUtils.isEmpty(d.conceptoAbreviatura)) {
                        conceptoDisplay = "Concepto: " + d.conceptoAbreviatura;
                    } else if (!TextUtils.isEmpty(d.conceptoKey)) {
                        conceptoDisplay = "Concepto: " + d.conceptoKey;
                    } else if (!TextUtils.isEmpty(d.conceptoNombre)) {
                        conceptoDisplay = "Concepto: " + d.conceptoNombre;
                    }
                    String motivoBase = nz(d.motivo);
                    Double monto      = readMonto(d);
                    // 👇 USAR DIRECTO EL CAMPO MAPEADO POR GSON
                    String aprobador  = safe(d.aprobadoPorNombre);

                    StringBuilder motivoDisplay = new StringBuilder();
                    if (!TextUtils.isEmpty(motivoBase)) motivoDisplay.append(motivoBase);
                    if (monto != null) {
                        if (motivoDisplay.length() > 0) motivoDisplay.append(" · ");
                        motivoDisplay.append("Monto: ").append(formatMoney(monto));
                    }
                    if (!TextUtils.isEmpty(aprobador)) {
                        if (motivoDisplay.length() > 0) motivoDisplay.append(" · ");
                        motivoDisplay.append("Aprobó: ").append(aprobador);
                    }

                    SolicitudUI ui = new SolicitudUI(
                            nz(d.codigo),
                            nz(capFirst(d.tipo)),
                            nz(d.placa),
                            nz(d.plantaNombre),
                            motivoDisplay.toString(),
                            nz(capFirst(d.estado)),
                            nz(d.creadoEn)
                    );

// ✅ Aquí se estaba perdiendo el concepto
                    ui.conceptoDisplay = conceptoDisplay;

                    mapped.add(ui);


                }

                // Filtro en memoria
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
        if ("Ingresada".equalsIgnoreCase(estadoUI)  && chipsQuick.findViewById(R.id.chip_ingresadas)  != null)
            chipsQuick.check(R.id.chip_ingresadas);
        else if ("Rechazada".equalsIgnoreCase(estadoUI))
            chipsQuick.check(R.id.chip_rechazadas);
        else if ("Procesada".equalsIgnoreCase(estadoUI))
            chipsQuick.check(R.id.chip_procesadas);
        else if ("Pendiente".equalsIgnoreCase(estadoUI))
            chipsQuick.check(R.id.chip_pendientes);
        else if ("Enviada".equalsIgnoreCase(estadoUI))
            chipsQuick.check(R.id.chip_enviadas);
        else
            chipsQuick.check(R.id.chip_all);
    }

    private void setRefreshing(boolean refreshing) {
        if (swipe != null) swipe.setRefreshing(refreshing);
    }

    private void showLoading(boolean show) {
        if (progress != null) progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ========= Lectura de campos =========

    @Nullable
    private static Double readMonto(SolicitudDto d) {
        try {
            if (d.monto != null) return d.monto;
        } catch (Throwable ignore) {}
        // Fallbacks si el backend cambia
        Double v = tryParseDouble(d.importe);
        if (v != null) return v;
        return tryParseDouble(d.total);
    }

    @Nullable
    private static Double tryParseDouble(Double n) {
        return n; // ya viene como Double por Gson si existe
    }

    private String formatMoney(double amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "PE"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "S/ " + nf.format(amount);
    }

    // ========= Helpers =========
    private void toast(String s) {
        if (!isAdded()) return;
        android.widget.Toast.makeText(requireContext(), s, android.widget.Toast.LENGTH_LONG).show();
    }

    private static String value(CharSequence cs) { return cs == null ? "" : cs.toString().trim(); }
    private static String nz(String s) { return s == null ? "" : s; }
    private static String safe(String s) { return (s == null) ? "" : s.trim(); }

    private static String capFirst(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0,1).toUpperCase(Locale.getDefault())
                + s.substring(1).toLowerCase(Locale.getDefault());
    }

    private static String mapTipoToApi(String t) {
        if ("Todos".equalsIgnoreCase(t) || t.isEmpty()) return null;
        String x = t.toLowerCase(Locale.ROOT);
        if (x.startsWith("descu")) return "DESCUENTO";
        if (x.startsWith("bolsa")) return "BOLSA";
        return t.toUpperCase(Locale.ROOT);
    }

    private static String mapEstadoToApi(String e) {
        if ("Todos".equalsIgnoreCase(e) || e.isEmpty()) return null;
        String x = e.toLowerCase(Locale.ROOT);
        if (x.startsWith("ingre")) return "INGRESADA";
        if (x.startsWith("proce")) return "PROCESADA";
        if (x.startsWith("recha")) return "RECHAZADA";
        if (x.startsWith("pend"))  return "PENDIENTE";
        if (x.startsWith("envi"))  return "ENVIADA";
        return e.toUpperCase(Locale.ROOT);
    }
}
