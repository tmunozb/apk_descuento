package com.farenet.descuentos.ui.bolsa;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.R;
import com.farenet.descuentos.API.Actual.DTO.bolsa.BolsaAuditoriaDto;
import com.farenet.descuentos.API.Actual.DTO.bolsa.BolsaConfigDto;
import com.farenet.descuentos.API.Actual.DTO.maestros.AccesoPlantaDto;
import com.farenet.descuentos.Core.Network.NewApiClient;
import com.farenet.descuentos.Core.Storage.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BolsaFragment extends Fragment implements BolsaEstadoAdapter.Actions {

    private RecyclerView rv;
    private View progress;

    private final List<String> periodoLabels = new ArrayList<>();
    private final Map<String, String> labelToYyyymm = new LinkedHashMap<>();

    private final List<String> plantasNombres = new ArrayList<>();
    private final Map<String, String> plantaNombreToKey = new LinkedHashMap<>();
    private final Map<String, String> plantaKeyToNombre = new LinkedHashMap<>();

    private BolsaEstadoAdapter adapter;
    private SessionManager session;

    private Call<List<BolsaConfigDto>> listCall;
    private Call<BolsaConfigDto> upsertCall;
    private Call<Map<String, Object>> estadoCall;
    private Call<List<BolsaAuditoriaDto>> auditCall;

    private String selectedPeriodoLabel = "";
    private String selectedPeriodoKey   = "";
    private String selectedPlantaNombre = "";
    private String selectedPlantaKey    = null;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bolsa_estado, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rv = view.findViewById(R.id.rvBolsas);
        progress = view.findViewById(R.id.progress);
        Chip chipPeriodo = view.findViewById(R.id.chipPeriodo);
        Chip chipPlanta  = view.findViewById(R.id.chipPlanta);
        View btnBuscar   = view.findViewById(R.id.btnBuscar);
        TextView tvHintResumen = view.findViewById(R.id.tvHintResumen);

        session = new SessionManager(requireContext());
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        // ✳️ Modo admin solo si el usuario es “sistemas”
        boolean adminMode = isUserSistemas();

        adapter = new BolsaEstadoAdapter(this);
        adapter.setAdminMode(adminMode); // <-- el adapter debe ocultar botones/menú cuando sea false
        adapter.setPlantaKeyToNombre(plantaKeyToNombre);
        rv.setAdapter(adapter);

        buildPeriodoOptions();
        cargarPlantasDesdeSesion();

        adapter.setPlantaKeyToNombre(plantaKeyToNombre);

        selectedPeriodoLabel = periodoLabels.isEmpty() ? "" : periodoLabels.get(0);
        selectedPeriodoKey   = labelToYyyymm.getOrDefault(selectedPeriodoLabel, yyyymmHoy());

        if (!plantasNombres.isEmpty()) {
            selectedPlantaNombre = plantasNombres.get(0);
            selectedPlantaKey    = plantaNombreToKey.get(selectedPlantaNombre);
        }

        chipPeriodo.setText(selectedPeriodoLabel.isEmpty() ? "Mes de trabajo" : selectedPeriodoLabel);
        chipPlanta.setText(selectedPlantaNombre.isEmpty() ? "Planta" : selectedPlantaNombre);
        tvHintResumen.setText(makeHintResumen());

        chipPeriodo.setOnClickListener(v -> showFiltrosBottomSheet(true, false));
        chipPlanta.setOnClickListener(v  -> showFiltrosBottomSheet(false, true));
        btnBuscar.setOnClickListener(v -> buscar());

        buscar();
    }

    /** Devuelve true si el usuario tiene perfil/rol "sistemas". Robusto a distintas implementaciones de SessionManager. */
    private boolean isUserSistemas() {
        if (session == null) return false;

        // 1) Métodos comunes en SessionManager (si existen)
        try {
            Method m = session.getClass().getMethod("getPerfilNombre");
            Object v = m.invoke(session);
            if (v != null && "sistemas".equalsIgnoreCase(v.toString())) return true;
        } catch (Throwable ignore) {}

        try {
            Method m = session.getClass().getMethod("getPerfilId");
            Object v = m.invoke(session);
            if (v != null && "sistemas".equalsIgnoreCase(v.toString())) return true;
        } catch (Throwable ignore) {}

        try {
            Method m = session.getClass().getMethod("getRoles");
            Object v = m.invoke(session);
            if (v instanceof List) {
                for (Object r : ((List<?>) v)) {
                    if (r != null) {
                        String s = r.toString();
                        if ("sistemas".equalsIgnoreCase(s) || "admin".equalsIgnoreCase(s)) return true;
                    }
                }
            }
        } catch (Throwable ignore) {}

        // 2) A falta de lo anterior, nunca habilitar admin
        return false;
    }

    private void buildPeriodoOptions() {
        periodoLabels.clear();
        labelToYyyymm.clear();

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat yyyymm = new SimpleDateFormat("yyyyMM", Locale.getDefault());
        SimpleDateFormat label  = new SimpleDateFormat("MMMM yyyy", new Locale("es", "PE"));

        for (int i = 0; i < 6; i++) {
            String key = yyyymm.format(cal.getTime());
            String lbl = capitalize(label.format(cal.getTime()));
            periodoLabels.add(lbl);
            labelToYyyymm.put(lbl, key);
            cal.add(Calendar.MONTH, -1);
        }
    }

    private void showFiltrosBottomSheet(boolean editPeriodo, boolean editPlanta) {
        BottomSheetDialog bs = new BottomSheetDialog(requireContext());
        View v = getLayoutInflater().inflate(R.layout.bottomsheet_filtros_bolsa, null, false);
        bs.setContentView(v);

        MaterialAutoCompleteTextView bsPeriodo = v.findViewById(R.id.bs_actPeriodo);
        MaterialAutoCompleteTextView bsPlanta  = v.findViewById(R.id.bs_actPlanta);
        MaterialButton btnCancelar = v.findViewById(R.id.bs_btnCancelar);
        MaterialButton btnAplicar  = v.findViewById(R.id.bs_btnAplicar);

        bsPeriodo.setAdapter(new android.widget.ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, periodoLabels));
        bsPlanta.setAdapter(new android.widget.ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, plantasNombres));

        if (!TextUtils.isEmpty(selectedPeriodoLabel)) bsPeriodo.setText(selectedPeriodoLabel, false);
        if (!TextUtils.isEmpty(selectedPlantaNombre)) bsPlanta.setText(selectedPlantaNombre, false);

        View periodoContainer = (View) bsPeriodo.getParent().getParent();
        View plantaContainer  = (View) bsPlanta.getParent().getParent();
        if (periodoContainer != null) periodoContainer.setVisibility(editPeriodo ? View.VISIBLE : View.GONE);
        if (plantaContainer  != null) plantaContainer.setVisibility(editPlanta  ? View.VISIBLE : View.GONE);

        btnCancelar.setOnClickListener(x -> bs.dismiss());
        btnAplicar.setOnClickListener(x -> {
            String newLblPeriodo = safe(bsPeriodo.getText() != null ? bsPeriodo.getText().toString() : "");
            String newPlantaNom  = safe(bsPlanta.getText()   != null ? bsPlanta.getText().toString()   : "");

            if (editPeriodo && !TextUtils.isEmpty(newLblPeriodo)) {
                selectedPeriodoLabel = newLblPeriodo;
                selectedPeriodoKey   = labelToYyyymm.get(selectedPeriodoLabel);
            }
            if (editPlanta && !TextUtils.isEmpty(newPlantaNom)) {
                selectedPlantaNombre = newPlantaNom;
                selectedPlantaKey    = plantaNombreToKey.get(selectedPlantaNombre);
            }

            Chip chipPeriodo = requireView().findViewById(R.id.chipPeriodo);
            Chip chipPlanta  = requireView().findViewById(R.id.chipPlanta);
            TextView tvHintResumen = requireView().findViewById(R.id.tvHintResumen);

            chipPeriodo.setText(TextUtils.isEmpty(selectedPeriodoLabel) ? "Mes de trabajo" : selectedPeriodoLabel);
            chipPlanta.setText(TextUtils.isEmpty(selectedPlantaNombre) ? "Planta" : selectedPlantaNombre);
            tvHintResumen.setText(makeHintResumen());

            bs.dismiss();
        });

        bs.show();
    }

    private String makeHintResumen() {
        String p = TextUtils.isEmpty(selectedPeriodoLabel) ? "Mes" : selectedPeriodoLabel;
        String s = TextUtils.isEmpty(selectedPlantaNombre) ? "Planta" : selectedPlantaNombre;
        return p + " • " + s;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String yyyymmHoy() {
        return new SimpleDateFormat("yyyyMM", Locale.getDefault()).format(new java.util.Date());
    }

    private void cargarPlantasDesdeSesion() {
        plantasNombres.clear();
        plantaNombreToKey.clear();
        plantaKeyToNombre.clear();

        plantasNombres.add("Todos");
        plantaNombreToKey.put("Todos", null);

        List<AccesoPlantaDto> accesos = session.getAccesos();
        if (accesos != null) {
            for (AccesoPlantaDto a : accesos) {
                if (a == null) continue;
                String nombre = safe(a.planta);
                String key    = safe(a.key);
                if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(key)) continue;

                if (!plantaNombreToKey.containsKey(nombre)) {
                    plantaNombreToKey.put(nombre, key);
                    plantasNombres.add(nombre);
                }
                if (!plantaKeyToNombre.containsKey(key)) {
                    plantaKeyToNombre.put(key, nombre);
                }
            }
        }
        if (plantasNombres.size() > 1) {
            Collections.sort(plantasNombres.subList(1, plantasNombres.size()));
        }
    }

    private void buscar() {
        String periodo   = safe(selectedPeriodoKey);
        String plantaKey = selectedPlantaKey;

        if (TextUtils.isEmpty(periodo) || periodo.length() != 6) {
            Toast.makeText(requireContext(), "Período inválido (use el selector)", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading(true);
        if (listCall != null) listCall.cancel();
        listCall = NewApiClient.get().bolsaListConfigs(periodo, plantaKey, null, 50);
        listCall.enqueue(new Callback<List<BolsaConfigDto>>() {
            @Override public void onResponse(Call<List<BolsaConfigDto>> call, Response<List<BolsaConfigDto>> rsp) {
                showLoading(false);
                if (!rsp.isSuccessful() || rsp.body() == null) {
                    Toast.makeText(requireContext(), "Error al obtener bolsas", Toast.LENGTH_LONG).show();
                    return;
                }
                Set<String> allowedKeys = new HashSet<>(plantaKeyToNombre.keySet());
                List<BolsaConfigDto> soloAcceso = new ArrayList<>();
                for (BolsaConfigDto b : rsp.body()) {
                    if (plantaKey == null) {
                        if (allowedKeys.contains(b.planta_key)) soloAcceso.add(b);
                    } else if (TextUtils.equals(plantaKey, b.planta_key)) {
                        soloAcceso.add(b);
                    }
                }
                adapter.setItems(soloAcceso);
            }
            @Override public void onFailure(Call<List<BolsaConfigDto>> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(requireContext(), "Error de red", Toast.LENGTH_LONG).show();
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
        // Guard extra: si no es sistemas, no hace nada
        if (!isUserSistemas()) return;
        if (item == null) return;

        showLoading(true);
        if (auditCall != null) auditCall.cancel();
        auditCall = NewApiClient.get().bolsaAuditoria(item.planta_key, item.periodo_yyyymm, 200);
        auditCall.enqueue(new Callback<List<BolsaAuditoriaDto>>() {
            @Override public void onResponse(Call<List<BolsaAuditoriaDto>> call, Response<List<BolsaAuditoriaDto>> rsp) {
                showLoading(false);
                if (!rsp.isSuccessful() || rsp.body() == null) {
                    Toast.makeText(requireContext(), "No se pudo cargar auditoría", Toast.LENGTH_LONG).show();
                    return;
                }
                mostrarDialogoAuditoria(rsp.body());
            }
            @Override public void onFailure(Call<List<BolsaAuditoriaDto>> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(requireContext(), "Error de red", Toast.LENGTH_LONG).show();
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
                        safe(a.created_at), safe(a.tipo_mov),
                        a.monto != null ? a.monto : 0d,
                        safe(a.actor_username),
                        a.descuento_id != null ? a.descuento_id : "-"
                ));
            }
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Auditoría")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onEditarTope(BolsaConfigDto item) {
        // Guard extra
        if (!isUserSistemas()) return;
        if (item == null) return;

        final View dialog = getLayoutInflater().inflate(R.layout.dialog_edit_tope, null, false);
        final com.google.android.material.textfield.TextInputEditText et =
                dialog.findViewById(R.id.etNuevoTope);
        if (item.monto_tope != null) {
            et.setText(String.format(Locale.getDefault(), "%.2f", item.monto_tope));
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Editar tope")
                .setView(dialog)
                .setPositiveButton("Guardar", (d, w) -> {
                    String sVal = et.getText() != null ? et.getText().toString().trim() : "";
                    double nuevo;
                    try { nuevo = Double.parseDouble(sVal); }
                    catch (Exception ex) {
                        Toast.makeText(requireContext(), "Monto inválido", Toast.LENGTH_LONG).show();
                        return;
                    }
                    upsertTope(item.planta_key, item.periodo_yyyymm, nuevo, item.estado);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void upsertTope(String plantaKey, String periodo, double tope, String estado) {
        showLoading(true);
        Map<String, Object> body = new LinkedHashMap<>();
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
                    Toast.makeText(requireContext(), "No se pudo guardar tope", Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(requireContext(), "Actualizado", Toast.LENGTH_SHORT).show();
                buscar();
            }
            @Override public void onFailure(Call<BolsaConfigDto> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(requireContext(), "Error de red", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onToggleEstado(BolsaConfigDto item) {
        // Guard extra
        if (!isUserSistemas()) return;
        if (item == null) return;

        final String nuevo = "CERRADO".equalsIgnoreCase(item.estado) ? "ACTIVO" : "CERRADO";
        new AlertDialog.Builder(requireContext())
                .setTitle(("CERRADO".equalsIgnoreCase(item.estado) ? "Abrir" : "Cerrar") + " período")
                .setMessage("¿Seguro que deseas cambiar a: " + nuevo + "?")
                .setPositiveButton("Sí", (d, w) -> setEstado(item.planta_key, item.periodo_yyyymm, nuevo))
                .setNegativeButton("No", null)
                .show();
    }

    private void setEstado(String plantaKey, String periodo, String estado) {
        showLoading(true);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("planta_key", plantaKey);
        body.put("periodo", periodo);
        body.put("estado", estado);

        if (estadoCall != null) estadoCall.cancel();
        estadoCall = NewApiClient.get().bolsaSetEstado(body);
        estadoCall.enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> rsp) {
                showLoading(false);
                if (!rsp.isSuccessful() || rsp.body() == null) {
                    Toast.makeText(requireContext(), "No se pudo actualizar estado", Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(requireContext(), "Estado actualizado", Toast.LENGTH_SHORT).show();
                buscar();
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(requireContext(), "Error de red", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listCall != null) listCall.cancel();
        if (upsertCall != null) upsertCall.cancel();
        if (estadoCall != null) estadoCall.cancel();
        if (auditCall != null) auditCall.cancel();
    }
}
