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
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
import com.farenet.descuentos.ui.solicitudes.comunes.adapter.SolicitudSimpleAdapter;
import com.farenet.descuentos.ui.solicitudes.model.SolicitudUI;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

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

import com.google.android.material.appbar.MaterialToolbar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.core.text.HtmlCompat;


public class HomeFragment extends Fragment {

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

    // Resumen Descuentos (mes)
    private TextView tvDescCountMes, tvDescMontoMes, tvDescPromMes;
    private Call<List<SolicitudDto>> callDescMes;

    private String getUser() {
        String u = session != null ? session.getUsername() : null;
        if (u != null && !u.trim().isEmpty()) return u.trim();
        return "tmunoz"; // fallback
    }

    private boolean promptShownThisSession = false;
    private int lastPromptForCount = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_comercial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session = new SessionManager(requireContext());

        // Usa la toolbar de la Activity
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            // Título para esta pantalla (opcional)
            toolbar.setTitle(getString(R.string.app_name));

            // Si la activity deja el menú por XML, no inflamos nada aquí para evitar duplicados.
            // Solo manejamos el click:
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.nav_logout) {
                    session.clear();
                    // Si también limpias caches propios:
                    // DashboardCache.clear(); BolsaCache.clear(); ...

                    // Vuelve al Login
                    Intent i = new Intent(requireContext(), com.farenet.descuentos.ui.auth.LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    requireActivity().finish();
                    return true;
                }
                return false;
            });
        }


        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        if (tvWelcome != null) {
            String nombre = session.getNombreVisible();
            tvWelcome.setText("¡Hola, " + (nombre == null ? "Comercial" : nombre) + "!");
        }

        // KPI
        tvKpiPendientes = view.findViewById(R.id.tvKpiPendientes);
        tvKpiAprobadas  = view.findViewById(R.id.tvKpiAprobadas);

        // Resumen de bolsa
        tvTopeTotal  = view.findViewById(R.id.tvTopeTotal);
        tvUsadoTotal = view.findViewById(R.id.tvUsadoTotal);
        tvSaldoTotal = view.findViewById(R.id.tvSaldoTotal);

        // Resumen Descuentos (mes actual)
        tvDescCountMes = view.findViewById(R.id.tvDescCountMes);
        tvDescMontoMes = view.findViewById(R.id.tvDescMontoMes);
        tvDescPromMes  = view.findViewById(R.id.tvDescPromMes);

        // Tiles (solo UI)
        MaterialCardView tileNueva = view.findViewById(R.id.tileBolsaEstado);
        if (tileNueva != null) {
            ((TextView) tileNueva.findViewById(R.id.tvTitle)).setText("Bolsa");
            ((TextView) tileNueva.findViewById(R.id.tvSubtitle)).setText("Plantas");
            ((ImageView) tileNueva.findViewById(R.id.ivIcon)).setImageResource(R.drawable.ic_add_circle_24);
        }
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
        cargarResumenBolsaGeneral();
        cargarResumenDescuentosMes();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadKpis();
        loadUltimas(trimOrEmpty(etBuscarUltimos == null ? null : etBuscarUltimos.getText()));
        cargarResumenBolsaGeneral();
        cargarResumenDescuentosMes();
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

    /** KPIs **/
    private void loadKpis() {
        setKpiLoading();

        if (callPendAut != null) callPendAut.cancel();
        callPendAut = NewApiClient.get().listarSolicitudes(
                getUser(), "PENDIENTE_AUT", null, null, 500, 0, "-creado_en"
        );
        callPendAut.enqueue(new Callback<List<SolicitudDto>>() {
            @Override public void onResponse(Call<List<SolicitudDto>> call, Response<List<SolicitudDto>> rsp) {
                int n = (rsp.isSuccessful() && rsp.body() != null) ? rsp.body().size() : 0;
                DashboardCache.setKpiPendAut(n);
                updateTextIfChanged(tvKpiPendientes, n);
                maybePromptPendientes(n);
            }
            @Override public void onFailure(Call<List<SolicitudDto>> call, Throwable t) {
                if (DashboardCache.getKpiPendAut() < 0 && tvKpiPendientes != null) tvKpiPendientes.setText("–");
            }
        });

        if (callAprob != null) callAprob.cancel();
        callAprob = NewApiClient.get().listarSolicitudes(
                getUser(), "APROBADA", "DESCUENTO", null, 500, 0, "-creado_en"
        );
        callAprob.enqueue(new Callback<List<SolicitudDto>>() {
            @Override public void onResponse(Call<List<SolicitudDto>> call, Response<List<SolicitudDto>> rsp) {
                int n = 0;
                if (rsp.isSuccessful() && rsp.body() != null) {
                    n = ONLY_THIS_MONTH_FOR_APPROVED ? filterThisMonthCount(rsp.body()) : rsp.body().size();
                }
                DashboardCache.setKpiAprobadas(n);
                updateTextIfChanged(tvKpiAprobadas, n);
            }
            @Override public void onFailure(Call<List<SolicitudDto>> call, Throwable t) {
                if (DashboardCache.getKpiAprobadas() < 0 && tvKpiAprobadas != null) tvKpiAprobadas.setText("–");
            }
        });
    }

    private void maybePromptPendientes(int n) {
        if (n <= 0) return;
        if (!isAdded() || !isResumed()) return;

        if (promptShownThisSession && lastPromptForCount == n) return;
        promptShownThisSession = true;
        lastPromptForCount = n;

        // Título y mensaje dinámicos (singular/plural) + numerito en negrita
        final boolean uno = (n == 1);
        String title = (uno ? "⚠️  Tienes 1 descuento por aprobar"
                : "⚠️  Tienes " + n + " descuentos por aprobar");

        String msgRaw = (uno
                ? "Hay <b>1</b> solicitud pendiente de autorización. ¿Deseas aprobarla ahora?"
                : "Hay <b>" + n + "</b> solicitudes pendientes de autorización. ¿Deseas revisarlas ahora?");

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_notifications) // usa tu ícono de alerta (o android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(HtmlCompat.fromHtml(msgRaw, HtmlCompat.FROM_HTML_MODE_LEGACY))
                .setPositiveButton(uno ? "Sí, aprobar ahora" : "Sí, ir ahora", (d, w) -> {
                    if (requireActivity() instanceof com.farenet.descuentos.ui.comercialhost.ComercialHostActivity) {
                        ((com.farenet.descuentos.ui.comercialhost.ComercialHostActivity) requireActivity())
                                .goToPendingApprovals();
                    }
                })
                .setNegativeButton("No, luego", (d, w) -> { /* cerrar */ })
                .show();
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

    // ===== Resumen de bolsa (general) =====
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

    // ===== Resumen DESCUENTOS (mes actual) =====
    private void cargarResumenDescuentosMes() {
        if (callDescMes != null) callDescMes.cancel();

        // Trae DESCUENTO y filtramos localmente por estado y por mes (aprobadaEn si existe; si no, creadoEn)
        callDescMes = NewApiClient.get().listarSolicitudes(
                getUser(),
                null,            // estado -> filtramos local
                "DESCUENTO",     // solo descuentos
                null,            // q
                1000,            // limit
                0,               // offset
                "-creado_en"     // orden
        );

        callDescMes.enqueue(new Callback<List<SolicitudDto>>() {
            @Override public void onResponse(Call<List<SolicitudDto>> call, Response<List<SolicitudDto>> rsp) {
                int count = 0;
                double total = 0d;

                if (rsp.isSuccessful() && rsp.body() != null) {
                    for (SolicitudDto d : rsp.body()) {
                        if (d == null) continue;

                        // Estados válidos
                        String est = d.estado != null ? d.estado.trim().toUpperCase(Locale.ROOT) : "";
                        if (!("APROBADA".equals(est) || "PROCESADA".equals(est))) continue;

                        // Mes actual (usa aprobadaEn si existe; si no, creadoEn)
                        if (!isFromThisMonthForResumen(d)) continue;

                        total += readMonto(d);
                        count++;
                    }
                }

                double prom = count > 0 ? (total / count) : 0d;
                setInt(tvDescCountMes, count);
                setMoney(tvDescMontoMes, total);
                setMoney(tvDescPromMes,  prom);
            }

            @Override public void onFailure(Call<List<SolicitudDto>> call, Throwable t) {
                setInt(tvDescCountMes, 0);
                setMoney(tvDescMontoMes, 0d);
                setMoney(tvDescPromMes,  0d);
            }
        });
    }

    // ===== Helpers =====
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

    // Usa aprobadaEn si existe; si no, creadoEn
    private boolean isFromThisMonthForResumen(SolicitudDto d) {
        String candidate = (d != null && !TextUtils.isEmpty(d.aprobadaEn)) ? d.aprobadaEn : (d != null ? d.creadoEn : null);
        return isFromThisMonth(candidate);
    }

    // Lee “usado” con tolerancia de nombre de campo
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

    // Lee “disponible/saldo” si viene desde backend
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

    // === MONTO de la solicitud (acepta Number o String) ===
    private double readMonto(SolicitudDto d) {
        if (d == null) return 0d;

        // Getters comunes (camelCase y snake_case)
        Double viaGetter = callNumericGetter(d, "getMonto");
        if (viaGetter != null) return viaGetter;

        viaGetter = callNumericGetter(d, "getMontoAprobado");
        if (viaGetter != null) return viaGetter;
        viaGetter = callNumericGetter(d, "getMonto_aprobado");
        if (viaGetter != null) return viaGetter;

        viaGetter = callNumericGetter(d, "getMontoSolicitado");
        if (viaGetter != null) return viaGetter;
        viaGetter = callNumericGetter(d, "getMonto_solicitado");
        if (viaGetter != null) return viaGetter;

        viaGetter = callNumericGetter(d, "getImporte");
        if (viaGetter != null) return viaGetter;

        viaGetter = callNumericGetter(d, "getTotal");
        if (viaGetter != null) return viaGetter;

        // Campos (camelCase / snake_case)
        Double viaField = readNumericField(d, "monto"); // API actual lo trae como String "60.00"
        if (viaField != null) return viaField;

        viaField = readNumericField(d, "montoAprobado");
        if (viaField != null) return viaField;
        viaField = readNumericField(d, "monto_aprobado");
        if (viaField != null) return viaField;

        viaField = readNumericField(d, "montoSolicitado");
        if (viaField != null) return viaField;
        viaField = readNumericField(d, "monto_solicitado");
        if (viaField != null) return viaField;

        viaField = readNumericField(d, "importe");
        if (viaField != null) return viaField;

        viaField = readNumericField(d, "total");
        if (viaField != null) return viaField;

        return 0d;
    }

    @Nullable
    private Double callNumericGetter(Object obj, String getterName) {
        try {
            Method m = obj.getClass().getMethod(getterName);
            Object v = m.invoke(obj);
            return asDouble(v);
        } catch (Throwable ignore) {}
        return null;
    }

    @Nullable
    private Double readNumericField(Object obj, String fieldName) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object v = f.get(obj);
            return asDouble(v);
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
            // tolera comas de miles
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

    private void setInt(TextView tv, int val) {
        if (tv == null) return;
        tv.setText(String.valueOf(val));
    }

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
        if (callDescMes != null) callDescMes.cancel();
    }
}
