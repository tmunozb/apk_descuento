package com.farenet.descuentos.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.API.Actual.DTO.bolsa.BolsaConfigDto;
import com.farenet.descuentos.API.Actual.DTO.maestros.AccesoPlantaDto;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto;
import com.farenet.descuentos.Core.Cache.DashboardCache;
import com.farenet.descuentos.Core.Network.NewApiClient;
import com.farenet.descuentos.Core.Storage.SessionManager;
import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.auth.LoginActivity;
import com.farenet.descuentos.ui.operacioneshost.OperacionesHostActivity;
import com.farenet.descuentos.ui.solicitudes.comunes.adapter.SolicitudSimpleAdapter;
import com.farenet.descuentos.ui.solicitudes.model.SolicitudUI;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OperacionesHomeFragment extends Fragment {

    private static final boolean ONLY_THIS_MONTH_FOR_APPROVED = false;

    private SessionManager session;

    // KPI
    private TextView tvKpiPendientes;
    private TextView tvKpiAprobadas;

    // Resumen de bolsa (general)
    private TextView tvTopeTotal, tvUsadoTotal, tvSaldoTotal;

    // Últimas solicitudes
    private RecyclerView rvUltimos;
    private EditText etBuscarUltimos;
    private SolicitudSimpleAdapter ultimosAdapter;
    private final List<SolicitudUI> ultimosData = new ArrayList<>();

    // Llamadas
    private Call<List<SolicitudDto>> callPendAut;
    private Call<List<SolicitudDto>> callAprob;
    private Call<List<SolicitudDto>> callUltimas;
    private Call<List<BolsaConfigDto>> callBolsas;

    private String getUser() {
        String u = session != null ? session.getUsername() : null;
        if (!TextUtils.isEmpty(u)) return u.trim();
        return "tmunoz"; // fallback
    }

    private boolean promptShownThisSession = false;
    private int lastPromptForCount = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_operaciones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session = new SessionManager(requireContext());

        // Toolbar (logout)
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getString(R.string.app_name));
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.nav_logout) {
                    session.clear();
                    Intent i = new Intent(requireContext(), LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    requireActivity().finish();
                    return true;
                }
                return false;
            });
        }

        // Saludo
        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        if (tvWelcome != null) {
            String nombre = session.getNombreVisible();
            tvWelcome.setText("¡Hola, " + (nombre == null ? "Operaciones" : nombre) + "!");
        }

        // KPI
        tvKpiPendientes = view.findViewById(R.id.tvKpiPendientes);
        tvKpiAprobadas  = view.findViewById(R.id.tvKpiAprobadas);

        // Resumen de bolsa (Tope / Usado / Saldo)
        tvTopeTotal  = view.findViewById(R.id.tvTopeTotal);
        tvUsadoTotal = view.findViewById(R.id.tvUsadoTotal);
        tvSaldoTotal = view.findViewById(R.id.tvSaldoTotal);

        // Tile “Pendientes”
        MaterialCardView tilePend = view.findViewById(R.id.tilePendientesAut);
        if (tilePend != null) {
            ((TextView) tilePend.findViewById(R.id.tvTitle)).setText("Pendientes");
            ((TextView) tilePend.findViewById(R.id.tvSubtitle)).setText("Aprobar");
            ((ImageView) tilePend.findViewById(R.id.ivIcon)).setImageResource(R.drawable.ic_pending_actions_24);
        }

        // Recycler “Últimas”
        rvUltimos = view.findViewById(R.id.rvUltimos);
        etBuscarUltimos = view.findViewById(R.id.etBuscarUltimos);
        if (rvUltimos != null) {
            rvUltimos.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvUltimos.setHasFixedSize(true);
            rvUltimos.setNestedScrollingEnabled(false);
            rvUltimos.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
            ultimosAdapter = new SolicitudSimpleAdapter(ultimosData, s -> { /* abrir detalle si aplica */ });
            rvUltimos.setAdapter(ultimosAdapter);
        }

        applyPreloadedDashboard();

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
        loadUltimas(null);
        cargarResumenBolsaGeneral(); // ⬅️ resumen de bolsa visible
    }

    @Override
    public void onResume() {
        super.onResume();
        loadKpis();
        loadUltimas(trimOrEmpty(etBuscarUltimos == null ? null : etBuscarUltimos.getText()));
        cargarResumenBolsaGeneral(); // ⬅️ refresca resumen al volver
    }

    private void applyPreloadedDashboard() {
        int pendAut = DashboardCache.getKpiPendAut();
        int aprob   = DashboardCache.getKpiAprobadas();
        if (tvKpiPendientes != null && pendAut >= 0) tvKpiPendientes.setText(String.valueOf(pendAut));
        if (tvKpiAprobadas  != null && aprob   >= 0) tvKpiAprobadas.setText(String.valueOf(aprob));
    }

    private void updateTextIfChanged(TextView tv, int newVal) {
        if (tv == null) return;
        CharSequence cur = tv.getText();
        String next = String.valueOf(newVal);
        if (cur == null || !next.contentEquals(cur)) tv.setText(next);
    }

    /** KPIs (sin acciones de bolsa) **/
    private void loadKpis() {
        // Pinta “…” siempre para evitar que se vea el dato viejo del caché
        if (tvKpiPendientes != null) tvKpiPendientes.setText("…");
        if (tvKpiAprobadas  != null) tvKpiAprobadas.setText("…");

        // --- Pendientes (OPERACIONES usa "PENDIENTE") ---
        if (callPendAut != null) callPendAut.cancel();
        callPendAut = NewApiClient.get().listarSolicitudes(
                getUser(), "PENDIENTE", null, null, 500, 0, "-creado_en"
        );
        callPendAut.enqueue(new Callback<List<SolicitudDto>>() {
            @Override public void onResponse(Call<List<SolicitudDto>> call, Response<List<SolicitudDto>> rsp) {
                int n = (rsp.isSuccessful() && rsp.body() != null) ? rsp.body().size() : 0;
                DashboardCache.setKpiPendAut(n);
                updateTextIfChanged(tvKpiPendientes, n);
                maybePromptPendientes(n);
            }
            @Override public void onFailure(Call<List<SolicitudDto>> call, Throwable t) {
                DashboardCache.setKpiPendAut(0);
                updateTextIfChanged(tvKpiPendientes, 0);
            }
        });

        // --- “Aprobadas” (aquí realmente: INGRESADAS de tipo BOLSA) ---
        if (callAprob != null) callAprob.cancel();
        callAprob = NewApiClient.get().listarSolicitudes(
                getUser(), "INGRESADA", "BOLSA", null, 500, 0, "-creado_en"
        );
        callAprob.enqueue(new Callback<List<SolicitudDto>>() {
            @Override public void onResponse(Call<List<SolicitudDto>> call, Response<List<SolicitudDto>> rsp) {
                int n = 0;
                if (rsp.isSuccessful() && rsp.body() != null) {
                    // ✅ Revalidamos en cliente por si el API no filtra exactamente
                    n = countIngresadasBolsa(rsp.body(), ONLY_THIS_MONTH_FOR_APPROVED);
                }
                DashboardCache.setKpiAprobadas(n);
                updateTextIfChanged(tvKpiAprobadas, n);
            }
            @Override public void onFailure(Call<List<SolicitudDto>> call, Throwable t) {
                DashboardCache.setKpiAprobadas(0);
                updateTextIfChanged(tvKpiAprobadas, 0);
            }
        });
    }



    private void setKpiLoading() {
        int pend = DashboardCache.getKpiPendAut();
        int apr  = DashboardCache.getKpiAprobadas();
        if (tvKpiPendientes != null && pend < 0) tvKpiPendientes.setText("…");
        if (tvKpiAprobadas  != null && apr  < 0) tvKpiAprobadas.setText("…");
    }

    private int filterThisMonthCount(List<SolicitudDto> list) {
        if (list == null) return 0;
        LocalDate now = LocalDate.now(ZoneOffset.systemDefault());
        int ym = now.getYear() * 100 + now.getMonthValue();
        int count = 0;
        for (SolicitudDto d : list) {
            String s = d.creadoEn;
            if (TextUtils.isEmpty(s)) continue;
            try {
                OffsetDateTime odt = OffsetDateTime.parse(s);
                LocalDate ld = odt.toLocalDate();
                int ymItem = ld.getYear() * 100 + ld.getMonthValue();
                if (ymItem == ym) count++;
            } catch (DateTimeParseException ignore) {}
        }
        return count;
    }

    private int countIngresadasBolsa(List<SolicitudDto> list, boolean onlyThisMonth) {
        if (list == null || list.isEmpty()) return 0;

        int count = 0;
        for (SolicitudDto d : list) {
            if (d == null) continue;

            String est  = d.estado != null ? d.estado.trim().toUpperCase(Locale.ROOT) : "";
            String tipo = d.tipo   != null ? d.tipo.trim().toUpperCase(Locale.ROOT)   : "";

            if (!"INGRESADA".equals(est)) continue;
            if (!"BOLSA".equals(tipo))     continue;

            if (onlyThisMonth) {
                String date = !TextUtils.isEmpty(d.aprobadaEn) ? d.aprobadaEn : d.creadoEn;
                if (!isFromThisMonth(date)) continue;
            }
            count++;
        }
        return count;
    }

    private boolean isFromThisMonth(String isoDate) {
        if (TextUtils.isEmpty(isoDate)) return false;
        try {
            OffsetDateTime odt = OffsetDateTime.parse(isoDate);
            LocalDate item = odt.toLocalDate();
            LocalDate now  = LocalDate.now(ZoneOffset.systemDefault());
            return item.getYear() == now.getYear() && item.getMonthValue() == now.getMonthValue();
        } catch (Exception e) {
            return false;
        }
    }



    /** Últimas **/
    private void loadUltimas(String q) {
        if (callUltimas != null) callUltimas.cancel();

        final boolean hasQuery = q != null && !q.trim().isEmpty();
        final int limit = hasQuery ? 20 : 3;

        callUltimas = NewApiClient.get().listarSolicitudes(
                getUser(), null, null, hasQuery ? q.trim() : null, limit, 0, "-creado_en"
        );
        callUltimas.enqueue(new Callback<List<SolicitudDto>>() {
            @Override public void onResponse(Call<List<SolicitudDto>> call, Response<List<SolicitudDto>> rsp) {
                List<SolicitudUI> mapped = new ArrayList<>();
                if (rsp.isSuccessful() && rsp.body() != null) {
                    for (SolicitudDto d : rsp.body()) {
                        mapped.add(new SolicitudUI(
                                nz(d.codigo),
                                capFirst(nz(d.tipo)),
                                nz(d.placa),
                                nz(d.plantaNombre),
                                nz(d.motivo),
                                capFirst(nz(d.estado)),
                                nz(d.creadoEn)
                        ));
                    }
                }
                applyUltimas(mapped);
            }
            @Override public void onFailure(Call<List<SolicitudDto>> call, Throwable t) {
                applyUltimas(new ArrayList<>());
            }
        });
    }

    private void applyUltimas(List<SolicitudUI> items) {
        ultimosData.clear();
        if (items != null) ultimosData.addAll(items);
        if (ultimosAdapter != null) ultimosAdapter.notifyDataSetChanged();
    }

    /** Prompt para pendientes */
    private void maybePromptPendientes(int n) {
        if (n <= 0) return;
        if (!isAdded() || !isResumed()) return;

        if (promptShownThisSession && lastPromptForCount == n) return;
        promptShownThisSession = true;
        lastPromptForCount = n;

        final boolean uno = (n == 1);
        String title = (uno ? "⚠️  Tienes 1 descuento por aprobar"
                : "⚠️  Tienes " + n + " descuentos por aprobar");

        String msgRaw = (uno
                ? "Hay <b>1</b> solicitud pendiente de autorización. ¿Deseas aprobarla ahora?"
                : "Hay <b>" + n + "</b> solicitudes pendientes de autorización. ¿Deseas revisarlas ahora?");

        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_notifications)
                .setTitle(title)
                .setMessage(HtmlCompat.fromHtml(msgRaw, HtmlCompat.FROM_HTML_MODE_LEGACY))
                .setPositiveButton(uno ? "Sí, aprobar ahora" : "Sí, ir ahora", (d, w) -> {
                    if (requireActivity() instanceof OperacionesHostActivity) {
                        ((OperacionesHostActivity) requireActivity()).goToPendingApprovals();
                    }
                })
                .setNegativeButton("No, luego", (d, w) -> { /* cerrar */ })
                .show();
    }

    // ===== Resumen de bolsa (solo lectura) =====
    private void cargarResumenBolsaGeneral() {
        if (tvTopeTotal == null || tvUsadoTotal == null || tvSaldoTotal == null) return;

        final String periodo = yyyymmHoy();

        if (callBolsas != null) callBolsas.cancel();
        callBolsas = NewApiClient.get().bolsaListConfigs(periodo, null, null, 200);
        callBolsas.enqueue(new Callback<List<BolsaConfigDto>>() {
            @Override public void onResponse(Call<List<BolsaConfigDto>> call, Response<List<BolsaConfigDto>> rsp) {
                if (!isAdded()) return;
                double tope = 0d, usado = 0d, saldo = 0d;

                if (rsp.isSuccessful() && rsp.body() != null) {
                    Set<String> allowed = allowedPlantas();
                    for (BolsaConfigDto b : rsp.body()) {
                        if (b == null) continue;
                        if (!TextUtils.isEmpty(b.planta_key) && !allowed.contains(b.planta_key)) continue;

                        double t = b.monto_tope != null ? b.monto_tope : 0d;
                        double u = readUsado(b);
                        Double disp = readDisponible(b);

                        tope  += t;
                        usado += u;
                        saldo += (disp != null) ? disp : Math.max(0d, t - u);
                    }
                }

                setMoney(tvTopeTotal, tope);
                setMoney(tvUsadoTotal, usado);
                setMoney(tvSaldoTotal, saldo);
            }

            @Override public void onFailure(Call<List<BolsaConfigDto>> call, Throwable t) {
                if (!isAdded()) return;
                setMoney(tvTopeTotal, 0d);
                setMoney(tvUsadoTotal, 0d);
                setMoney(tvSaldoTotal, 0d);
            }
        });
    }

    private Set<String> allowedPlantas() {
        Set<String> keys = new HashSet<>();
        List<AccesoPlantaDto> accesos = session.getAccesos();
        if (accesos != null) {
            for (AccesoPlantaDto a : accesos) {
                if (a != null && a.key != null) keys.add(a.key);
            }
        }
        return keys;
    }

    private String yyyymmHoy() {
        return new SimpleDateFormat("yyyyMM", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
    }

    private double readUsado(BolsaConfigDto b) {
        try { if (b.monto_usado != null) return b.monto_usado; } catch (Throwable ignore) {}
        try {
            Field f = b.getClass().getDeclaredField("monto_consumido");
            f.setAccessible(true);
            Object val = f.get(b);
            Double dv = asDouble(val);
            if (dv != null) return dv;
        } catch (Throwable ignore) {}
        try {
            Field f = b.getClass().getDeclaredField("usado");
            f.setAccessible(true);
            Object val = f.get(b);
            Double dv = asDouble(val);
            if (dv != null) return dv;
        } catch (Throwable ignore) {}
        return 0d;
    }

    @Nullable
    private Double readDisponible(BolsaConfigDto b) {
        try {
            Field f = b.getClass().getDeclaredField("monto_disponible");
            f.setAccessible(true);
            Object val = f.get(b);
            Double dv = asDouble(val);
            if (dv != null) return dv;
        } catch (Throwable ignore) {}
        try {
            Field f = b.getClass().getDeclaredField("saldo");
            f.setAccessible(true);
            Object val = f.get(b);
            Double dv = asDouble(val);
            if (dv != null) return dv;
        } catch (Throwable ignore) {}
        return null;
    }

    @Nullable
    private Double asDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof CharSequence) {
            String s = v.toString().trim();
            if (s.isEmpty()) return null;
            s = s.replace(",", "");
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private void setMoney(TextView tv, double amount) {
        if (tv == null) return;
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "PE"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        tv.setText("S/ " + nf.format(amount));
    }

    // ===== Helpers menores =====
    private static String nz(String s) { return s == null ? "" : s; }
    private static String trimOrEmpty(CharSequence cs) { return cs == null ? "" : cs.toString().trim(); }
    private static String capFirst(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0,1).toUpperCase(Locale.getDefault()) + s.substring(1).toLowerCase(Locale.getDefault());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (callPendAut != null) callPendAut.cancel();
        if (callAprob   != null) callAprob.cancel();
        if (callUltimas != null) callUltimas.cancel();
        if (callBolsas  != null) callBolsas.cancel();
    }
}
