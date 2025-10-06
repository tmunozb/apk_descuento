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
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BolsaEstadoActivity extends AppCompatActivity implements BolsaEstadoAdapter.Actions {

    private TextInputEditText etPeriodo;
    private AutoCompleteTextView actPlanta;
    private RecyclerView rv;
    private View progress;

    private final List<String> plantasNombres = new ArrayList<>();
    private final Map<String,String> plantaNombreToKey = new LinkedHashMap<>();

    private BolsaEstadoAdapter adapter;
    private SessionManager session;

    private Call<List<BolsaConfigDto>> listCall;
    private Call<BolsaConfigDto> upsertCall;
    private Call<Map<String,Object>> estadoCall;
    private Call<List<BolsaAuditoriaDto>> auditCall;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bolsa_estado);

        session = new SessionManager(getApplicationContext());

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        etPeriodo = findViewById(R.id.etPeriodo);
        actPlanta = findViewById(R.id.actPlanta);
        rv        = findViewById(R.id.rvBolsas);
        progress  = findViewById(R.id.progress);

        // Defaults
        etPeriodo.setText(yyyymmHoy());
        cargarPlantasDesdeSesion();
        actPlanta.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, plantasNombres));
        if (!plantasNombres.isEmpty()) actPlanta.setText(plantasNombres.get(0), false);

        findViewById(R.id.btnBuscar).setOnClickListener(v -> buscar());

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BolsaEstadoAdapter(this);
        rv.setAdapter(adapter);

        buscar(); // primera carga
    }

    private String yyyymmHoy() {
        return new SimpleDateFormat("yyyyMM", Locale.getDefault()).format(new Date());
    }

    private void cargarPlantasDesdeSesion() {
        plantasNombres.clear();
        plantaNombreToKey.clear();

        List<AccesoPlantaDto> accesos = session.getAccesos();
        if (accesos == null) return;

        for (AccesoPlantaDto a : accesos) {
            if (a == null) continue;
            String nombre = safe(a.planta);
            String key    = safe(a.key);
            if (TextUtils.isEmpty(nombre)) continue;
            if (!plantaNombreToKey.containsKey(nombre)) {
                plantaNombreToKey.put(nombre, key);
                plantasNombres.add(nombre);
            }
        }
        Collections.sort(plantasNombres);
    }

    private void buscar() {
        String periodo = safe(etPeriodo.getText() != null ? etPeriodo.getText().toString() : "");
        String plantaNombre = safe(actPlanta.getText() != null ? actPlanta.getText().toString() : "");
        String plantaKey = plantaNombreToKey.get(plantaNombre);

        if (periodo.length() != 6) {
            Toast.makeText(this, "Período inválido (use YYYYMM)", Toast.LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(plantaKey)) {
            Toast.makeText(this, "Seleccione una planta válida", Toast.LENGTH_LONG).show();
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
                adapter.setItems(rsp.body());
            }
            @Override public void onFailure(Call<List<BolsaConfigDto>> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(BolsaEstadoActivity.this, "Error de red", Toast.LENGTH_LONG).show();
            }
        });
    }

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
