package com.farenet.descuentos.ui.bolsa;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.R;
import com.farenet.descuentos.API.Actual.DTO.maestros.AccesoPlantaDto;
import com.farenet.descuentos.API.Actual.DTO.bolsa.BolsaAuditoriaDto;
import com.farenet.descuentos.API.Actual.DTO.bolsa.BolsaConfigDto;
import com.farenet.descuentos.Core.Network.NewApiClient;
import com.farenet.descuentos.Core.Storage.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BolsaEstadoActivity extends AppCompatActivity implements BolsaEstadoAdapter.Actions {

    // UI
    private MaterialAutoCompleteTextView actPeriodo;
    private AutoCompleteTextView actPlanta;
    private RecyclerView rv;
    private View progress;

    // Períodos (label amigable -> yyyymm)
    private final LinkedHashMap<String, String> periodLabelToValue = new LinkedHashMap<>();
    private final List<String> periodLabels = new ArrayList<>();

    // Plantas
    private final List<String> plantasNombres = new ArrayList<>();
    private final Map<String, String> plantaNombreToKey = new LinkedHashMap<>();
    private final Map<String, String> plantaKeyToNombre = new LinkedHashMap<>();
    private final Set<String> allowedPlantaKeys = new HashSet<>();

    // Infra
    private BolsaEstadoAdapter adapter;
    private SessionManager session;

    // Calls
    private Call<List<BolsaConfigDto>> listCall;
    private Call<BolsaConfigDto> upsertCall;
    private Call<Map<String,Object>> estadoCall;
    private Call<List<BolsaAuditoriaDto>> auditCall;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bolsa_estado);

        session   = new SessionManager(getApplicationContext());
        actPeriodo= findViewById(R.id.actPeriodo);
        actPlanta = findViewById(R.id.actPlanta);
        rv        = findViewById(R.id.rvBolsas);
        progress  = findViewById(R.id.progress);

        // Toolbar back
        MaterialToolbar tb = findViewById(R.id.toolbar);
        if (tb != null) tb.setNavigationOnClickListener(v -> finish());

        // Períodos (ej. "Oct 2025 (202510)" -> "202510")
        buildPeriods(/*mesesHaciaAtrás*/ 24, /*mesesHaciaAdelante*/ 2);
        actPeriodo.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, periodLabels));
        // Default: hoy
        String hoyYYYYMM = yyyymmHoy();
        String defaultLabel = labelForYYYYMM(hoyYYYYMM);
        if (defaultLabel == null && !periodLabels.isEmpty()) {
            defaultLabel = periodLabels.get(0);
        }
        if (defaultLabel != null) actPeriodo.setText(defaultLabel, false);

        // Plantas (con "Todos")
        cargarPlantasDesdeSesion();
        actPlanta.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, plantasNombres));
        if (!plantasNombres.isEmpty()) actPlanta.setText(plantasNombres.get(0), false);

        // Recycler
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BolsaEstadoAdapter(this);
        adapter.setPlantaKeyToNombre(plantaKeyToNombre); // para mostrar nombre de planta
        rv.setAdapter(adapter);

        // Acciones
        findViewById(R.id.btnBuscar).setOnClickListener(v -> buscar());

        // Primera carga
        buscar();
    }

    // ---------------- Util Períodos ----------------

    private void buildPeriods(int backMonths, int forwardMonths) {
        periodLabelToValue.clear();
        periodLabels.clear();

        Calendar cal = Calendar.getInstance(); // hoy
        // Generamos desde -back hasta +forward
        Calendar start = (Calendar) cal.clone();
        start.add(Calendar.MONTH, -backMonths);

        int total = backMonths + forwardMonths + 1;
        for (int i = 0; i < total; i++) {
            Calendar c = (Calendar) start.clone();
            c.add(Calendar.MONTH, i);
            String yyyymm = new SimpleDateFormat("yyyyMM", Locale.getDefault()).format(c.getTime());
            String label = buildFriendlyLabel(c, yyyymm);
            periodLabelToValue.put(label, yyyymm);
            periodLabels.add(label);
        }
    }

    private String buildFriendlyLabel(Calendar c, String yyyymm) {
        String[] months = new DateFormatSymbols(Locale.getDefault()).getMonths();
        String mesCorto = months[c.get(Calendar.MONTH)];
        if (mesCorto.length() > 3) mesCorto = mesCorto.substring(0, 3);
        int year = c.get(Calendar.YEAR);
        return mesCorto + " " + year + " (" + yyyymm + ")";
    }

    private String labelForYYYYMM(String yyyymm) {
        for (Map.Entry<String, String> e : periodLabelToValue.entrySet()) {
            if (yyyymm.equals(e.getValue())) return e.getKey();
        }
        return null;
    }

    private String yyyymmFromLabel(String label) {
        if (label == null) return null;
        String val = periodLabelToValue.get(label.trim());
        if (!TextUtils.isEmpty(val)) return val;
        // fallback: si el usuario tecleó directamente 202510
        String s = label.replaceAll("[^0-9]", "");
        return s.length() == 6 ? s : null;
    }

    private String yyyymmHoy() {
        return new SimpleDateFormat("yyyyMM", Locale.getDefault()).format(new Date());
    }

    // ---------------- Plantas ----------------

    private void cargarPlantasDesdeSesion() {
        plantasNombres.clear();
        plantaNombreToKey.clear();
        plantaKeyToNombre.clear();
        allowedPlantaKeys.clear();

        // "Todos"
        plantasNombres.add("Todos");
        plantaNombreToKey.put("Todos", null);

        List<AccesoPlantaDto> accesos = session.getAccesos();
        if (accesos != null) {
            for (AccesoPlantaDto a : accesos) {
                if (a == null) continue;
                String nombre = safe(a.planta); // p.ej. "Surco"
                String key    = safe(a.key);    // p.ej. "98"
                if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(key)) continue;

                if (!plantaNombreToKey.containsKey(nombre)) {
                    plantaNombreToKey.put(nombre, key);
                    plantasNombres.add(nombre);
                }
                plantaKeyToNombre.put(key, nombre);
                allowedPlantaKeys.add(key);
            }
        }
        if (plantasNombres.size() > 1) {
            Collections.sort(plantasNombres.subList(1, plantasNombres.size()));
        }
    }

    // ---------------- Buscar ----------------

    private void buscar() {
        String periodoLabel = safe(actPeriodo.getText() != null ? actPeriodo.getText().toString() : "");
        String periodo = yyyymmFromLabel(periodoLabel); // <-- convierte etiqueta amigable a YYYYMM

        String plantaNombre = safe(actPlanta.getText() != null ? actPlanta.getText().toString() : "");
        String plantaKey = plantaNombreToKey.get(plantaNombre); // null si "Todos"

        if (TextUtils.isEmpty(periodo) || periodo.length() != 6) {
            Toast.makeText(this, "Período inválido (elige de la lista o usa formato YYYYMM)", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading(true);
        if (listCall != null) listCall.cancel();
        listCall = NewApiClient.get().bolsaListConfigs(periodo, plantaKey, null, 50);
        listCall.enqueue(new Callback<List<BolsaConfigDto>>() {
            @Override public void onResponse(Call<List<BolsaConfigDto>> call, Response<List<BolsaConfigDto>> rsp) {
                showLoading(false);
                if (!rsp.isSuccessful() || rsp.body() == null) {
                    Toast.makeText(BolsaEstadoActivity.this, "Error al obtener bolsas", Toast.LENGTH_LONG).show();
                    return;
                }
                List<BolsaConfigDto> all = rsp.body();

                // Filtrar por acceso
                List<BolsaConfigDto> filtered = new ArrayList<>();
                for (BolsaConfigDto b : all) {
                    String key = b.planta_key != null ? String.valueOf(b.planta_key).trim() : null;
                    if (key != null && allowedPlantaKeys.contains(key)) {
                        filtered.add(b);
                    }
                }

                adapter.setPlantaKeyToNombre(plantaKeyToNombre); // aseguramos nombres
                adapter.setItems(filtered);
            }
            @Override public void onFailure(Call<List<BolsaConfigDto>> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(BolsaEstadoActivity.this, "Error de red", Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---------------- UI helpers ----------------

    private void showLoading(boolean show) {
        if (progress != null) progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    // ====== Actions del Adapter ======
    @Override
    public void onAuditoria(BolsaConfigDto item) {
        if (item == null) return;
        showLoading(true);
        if (auditCall != null) auditCall.cancel();
        auditCall = NewApiClient.get().bolsaAuditoria(item.planta_key, item.periodo_yyyymm, 200);
        auditCall.enqueue(new Callback<List<BolsaAuditoriaDto>>() {
            @Override public void onResponse(Call<List<BolsaAuditoriaDto>> call, Response<List<BolsaAuditoriaDto>> rsp) {
                showLoading(false);
                if (!rsp.isSuccessful() || rsp.body() == null) {
                    Toast.makeText(BolsaEstadoActivity.this, "No se pudo cargar auditoría", Toast.LENGTH_LONG).show();
                    return;
                }
                mostrarDialogoAuditoria(rsp.body());
            }
            @Override public void onFailure(Call<List<BolsaAuditoriaDto>> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(BolsaEstadoActivity.this, "Error de red", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarDialogoAuditoria(List<BolsaAuditoriaDto> items) {
        StringBuilder sb = new StringBuilder();
        if (items == null || items.isEmpty()) {
            sb.append("Sin movimientos.");
        } else {
            for (BolsaAuditoriaDto a : items) {
                sb.append(String.format(Locale.getDefault(),
                        "[%s] %s: S/ %,.2f  por %s  (desc:%s)\n",
                        safe(a.created_at), safe(a.tipo_mov), a.monto != null ? a.monto : 0d,
                        safe(a.actor_username), a.descuento_id != null ? a.descuento_id : "-"
                ));
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Auditoría")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onEditarTope(BolsaConfigDto item) {
        if (item == null) return;
        final View dialog = getLayoutInflater().inflate(R.layout.dialog_edit_tope, null, false);
        final com.google.android.material.textfield.TextInputEditText et = dialog.findViewById(R.id.etNuevoTope);
        if (item.monto_tope != null) et.setText(String.format(Locale.getDefault(), "%.2f", item.monto_tope));

        new AlertDialog.Builder(this)
                .setTitle("Editar tope")
                .setView(dialog)
                .setPositiveButton("Guardar", (d, w) -> {
                    String sVal = et.getText() != null ? et.getText().toString().trim() : "";
                    double nuevo;
                    try { nuevo = Double.parseDouble(sVal); } catch (Exception ex) {
                        Toast.makeText(this, "Monto inválido", Toast.LENGTH_LONG).show(); return;
                    }
                    upsertTope(item.planta_key, item.periodo_yyyymm, nuevo, item.estado);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void upsertTope(String plantaKey, String periodo, double tope, String estado) {
        showLoading(true);
        Map<String, Object> body = new HashMap<>();
        body.put("planta_key", plantaKey);
        body.put("periodo", periodo);
        body.put("monto_tope", tope);
        if (!TextUtils.isEmpty(estado)) body.put("estado", estado);

        if (upsertCall != null) upsertCall.cancel();
        upsertCall = NewApiClient.get().bolsaUpsertConfig(body);
        upsertCall.enqueue(new Callback<BolsaConfigDto>() {
            @Override public void onResponse(Call<BolsaConfigDto> call, Response<BolsaConfigDto> rsp) {
                showLoading(false);
                if (!rsp.isSuccessful() || rsp.body() == null) {
                    Toast.makeText(BolsaEstadoActivity.this, "No se pudo guardar tope", Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(BolsaEstadoActivity.this, "Actualizado", Toast.LENGTH_SHORT).show();
                buscar();
            }
            @Override public void onFailure(Call<BolsaConfigDto> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(BolsaEstadoActivity.this, "Error de red", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onToggleEstado(BolsaConfigDto item) {
        if (item == null) return;
        final String nuevo = "CERRADO".equalsIgnoreCase(item.estado) ? "ACTIVO" : "CERRADO";
        new AlertDialog.Builder(this)
                .setTitle(("CERRADO".equalsIgnoreCase(item.estado) ? "Abrir" : "Cerrar") + " período")
                .setMessage("¿Seguro que deseas cambiar a: " + nuevo + "?")
                .setPositiveButton("Sí", (d,w) -> setEstado(item.planta_key, item.periodo_yyyymm, nuevo))
                .setNegativeButton("No", null)
                .show();
    }

    private void setEstado(String plantaKey, String periodo, String estado) {
        showLoading(true);
        Map<String, Object> body = new HashMap<>();
        body.put("planta_key", plantaKey);
        body.put("periodo", periodo);
        body.put("estado", estado);

        if (estadoCall != null) estadoCall.cancel();
        estadoCall = NewApiClient.get().bolsaSetEstado(body);
        estadoCall.enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> rsp) {
                showLoading(false);
                if (!rsp.isSuccessful() || rsp.body() == null) {
                    Toast.makeText(BolsaEstadoActivity.this, "No se pudo actualizar estado", Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(BolsaEstadoActivity.this, "Estado actualizado", Toast.LENGTH_SHORT).show();
                buscar();
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(BolsaEstadoActivity.this, "Error de red", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listCall != null) listCall.cancel();
        if (upsertCall != null) upsertCall.cancel();
        if (estadoCall != null) estadoCall.cancel();
        if (auditCall != null) auditCall.cancel();
    }
}
