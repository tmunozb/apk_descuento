package com.farenet.descuentos;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.farenet.descuentos.models.newapi.AccesoPlantaDto;
import com.farenet.descuentos.models.newapi.ConceptoPlantaDto;
import com.farenet.descuentos.models.newapi.ConceptosResponse;
import com.farenet.descuentos.models.req.SolicitudCrearReq;
import com.farenet.descuentos.network.newapi.NewApiClient;
import com.farenet.descuentos.repository.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SolicitudCrearActivity extends AppCompatActivity {

    private View step1, step2, step3;
    private MaterialButton btnAtras, btnSiguiente, btnEnviar;

    // Paso 1
    private AutoCompleteTextView actTipo, actPlanta, actConcepto, actTipoCampania;
    private TextInputLayout tilConcepto;
    // Paso 2
    private TextInputEditText etPlaca, etMonto, etMotivo;
    private AutoCompleteTextView actAutoriza;
    // Paso 3
    private android.widget.TextView tvResumen;

    private int step = 1;

    // Sesión / plantas
    private SessionManager session;
    private final List<String> plantasNombres = new ArrayList<>();
    private final Map<String, String> plantaNombreToKey = new LinkedHashMap<>(); // nombre -> key

    // Conceptos
    private final List<String> conceptosLabel = new ArrayList<>();
    private final Map<String, ConceptoPlantaDto> conceptoByLabel = new LinkedHashMap<>();
    private Call<ConceptosResponse> conceptosCall;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitud_crear);

        session = new SessionManager(getApplicationContext());

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        // refs
        step1 = findViewById(R.id.panel_step1);
        step2 = findViewById(R.id.panel_step2);
        step3 = findViewById(R.id.panel_step3);

        actTipo         = findViewById(R.id.act_tipo);
        actPlanta       = findViewById(R.id.act_planta);
        actConcepto     = findViewById(R.id.act_concepto);
        actTipoCampania = findViewById(R.id.act_tipocampana);
        tilConcepto     = findViewById(R.id.til_concepto);

        etPlaca   = findViewById(R.id.et_placa);
        etMonto   = findViewById(R.id.et_monto);
        etMotivo  = findViewById(R.id.et_motivo);
        actAutoriza = findViewById(R.id.act_autoriza);

        tvResumen = findViewById(R.id.tv_resumen);

        btnAtras     = findViewById(R.id.btn_atras);
        btnSiguiente = findViewById(R.id.btn_siguiente);
        btnEnviar    = findViewById(R.id.btn_enviar);

        // 1) Tipo de solicitud: combo fijo
        setAdapter(actTipo, new String[]{"Descuento", "Cortesía"});
        actTipo.setText("Descuento", false);

        // 2) Plantas desde SessionManager
        cargarPlantasDesdeSesion(); // llena plantasNombres + mapa nombre->key
        if (plantasNombres.isEmpty()) {
            Toast.makeText(this, "No tienes plantas asignadas en tu sesión.", Toast.LENGTH_LONG).show();
        }
        setAdapter(actPlanta, plantasNombres.toArray(new String[0]));

        // UX: mostrar dropdown al tocar/enfocar
        actPlanta.setOnClickListener(v -> actPlanta.showDropDown());
        actPlanta.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actPlanta.showDropDown(); });
        actPlanta.setOnItemClickListener((parent, view, position, id) -> {
            actPlanta.setError(null);
            String key = obtenerPlantaKeySeleccionada();
            if (TextUtils.isEmpty(key)) {
                resetConceptos();
            } else {
                cargarConceptosPorPlanta(key);
            }
        });

        // Autoselección si solo hay una planta (y carga sus conceptos)
        if (plantasNombres.size() == 1) {
            actPlanta.setText(plantasNombres.get(0), false);
            String key = obtenerPlantaKeySeleccionada();
            if (!TextUtils.isEmpty(key)) cargarConceptosPorPlanta(key);
        }

        // 3) Otros combos (puedes reemplazarlos por APIs reales cuando gustes)
        setAdapter(actTipoCampania, new String[]{"N/A", "Campaña XX", "Alianza Y"});
        actTipoCampania.setText("N/A", false);
        setAdapter(actAutoriza, new String[]{"Jefe Operaciones", "Gerente Operaciones", "Sistemas"});

        // Concepto deshabilitado hasta elegir planta
        actConcepto.setEnabled(false);
        if (tilConcepto != null) tilConcepto.setEnabled(false);
        actConcepto.setOnClickListener(v -> actConcepto.showDropDown());
        actConcepto.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actConcepto.showDropDown(); });
        actConcepto.setOnItemClickListener((p, v, pos, id) -> actConcepto.setError(null));

        // listeners navegación
        btnAtras.setOnClickListener(v -> goBack());
        btnSiguiente.setOnClickListener(v -> goNext());
        btnEnviar.setOnClickListener(v -> enviar());

        render();
    }

    /** Pobla combo de plantas desde la sesión. */
    private void cargarPlantasDesdeSesion() {
        plantasNombres.clear();
        plantaNombreToKey.clear();

        List<AccesoPlantaDto> accesos = session.getAccesos();
        if (accesos == null) return;

        for (AccesoPlantaDto a : accesos) {
            if (a == null) continue;
            String nombre = safe(a.planta);
            String key    = safe(a.key);
            if (nombre.isEmpty()) continue;
            if (!plantaNombreToKey.containsKey(nombre)) {
                plantaNombreToKey.put(nombre, key);
                plantasNombres.add(nombre);
            }
        }
    }

    /** Llama API y pobla combo de conceptos según planta. */
    private void cargarConceptosPorPlanta(String plantaKey) {
        // Cancela llamadas previas
        if (conceptosCall != null) conceptosCall.cancel();

        // Limpia UI
        resetConceptos();

        conceptosCall = NewApiClient.get().conceptosPorPlanta(plantaKey);
        conceptosCall.enqueue(new Callback<ConceptosResponse>() {
            @Override
            public void onResponse(Call<ConceptosResponse> call, Response<ConceptosResponse> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().items == null) {
                    Toast.makeText(SolicitudCrearActivity.this, "No se pudieron cargar conceptos", Toast.LENGTH_LONG).show();
                    return;
                }

                for (ConceptoPlantaDto dto : response.body().items) {
                    if (dto == null) continue;
                    // Etiqueta visible: "ABREVIATURA (S/ valor)"
                    String label = (dto.abreviatura != null ? dto.abreviatura : dto.conceptoKey);
                    if (dto.valor != null) {
                        boolean entero = Math.abs(dto.valor - Math.rint(dto.valor)) < 1e-9;
                        label += " (S/ " + (entero ? String.valueOf(dto.valor.intValue()) : String.valueOf(dto.valor)) + ")";
                    }
                    conceptosLabel.add(label);
                    conceptoByLabel.put(label, dto);
                }

                setAdapter(actConcepto, conceptosLabel.toArray(new String[0]));
                boolean habilitar = !conceptosLabel.isEmpty();
                actConcepto.setEnabled(habilitar);
                if (tilConcepto != null) tilConcepto.setEnabled(habilitar);

                // Autoselección si hay uno solo
                if (conceptosLabel.size() == 1) {
                    actConcepto.setText(conceptosLabel.get(0), false);
                }
            }

            @Override
            public void onFailure(Call<ConceptosResponse> call, Throwable t) {
                if (call.isCanceled()) return;
                Toast.makeText(SolicitudCrearActivity.this, "Error al cargar conceptos", Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Limpia y deshabilita el combo de conceptos. */
    private void resetConceptos() {
        actConcepto.setEnabled(false);
        if (tilConcepto != null) tilConcepto.setEnabled(false);
        actConcepto.setText("", false);
        conceptosLabel.clear();
        conceptoByLabel.clear();
        setAdapter(actConcepto, new String[0]);
    }

    private void setAdapter(AutoCompleteTextView view, String[] arr) {
        view.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, arr));
    }

    private void render() {
        step1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        step2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        step3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        btnAtras.setEnabled(step > 1);
        btnSiguiente.setVisibility(step < 3 ? View.VISIBLE : View.GONE);
        btnEnviar.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        if (step == 3) {
            String resumen = ""
                    + "Tipo: " + v(actTipo) + "\n"
                    + "Planta: " + v(actPlanta) + "\n"
                    + "Concepto: " + v(actConcepto) + "\n"
                    + "Tipo de campaña: " + v(actTipoCampania) + "\n"
                    + "Placa: " + t(etPlaca) + "\n"
                    + "Monto: " + t(etMonto) + "\n"
                    + "Motivo: " + t(etMotivo) + "\n"
                    + "Autoriza: " + v(actAutoriza) + "\n"
                    + "PlantaKey: " + obtenerPlantaKeySeleccionada();
            tvResumen.setText(resumen);
        }
    }

    private void goNext() {
        if (step == 1) {
            if (!validStep1()) return;
            step = 2;
        } else if (step == 2) {
            if (!validStep2()) return;
            step = 3;
        }
        render();
    }

    private void goBack() {
        if (step > 1) { step--; render(); }
    }

    private boolean validStep1() {
        if (empty(actTipo))         { actTipo.setError("Selecciona el tipo"); actTipo.requestFocus(); return false; }
        if (empty(actPlanta))       { actPlanta.setError("Selecciona la planta"); actPlanta.requestFocus(); return false; }

        String key = obtenerPlantaKeySeleccionada();
        if (TextUtils.isEmpty(key)) {
            actPlanta.setError("Planta inválida para tu sesión");
            actPlanta.requestFocus();
            return false;
        }

        if (!actConcepto.isEnabled() || empty(actConcepto)) {
            actConcepto.setError("Selecciona el concepto");
            actConcepto.requestFocus();
            return false;
        }

        if (empty(actTipoCampania)) { actTipoCampania.setError("Selecciona el tipo de campaña"); actTipoCampania.requestFocus(); return false; }
        return true;
    }

    private boolean validStep2() {
        if (TextUtils.isEmpty(t(etPlaca))) {
            etPlaca.setError("Ingresa la placa"); etPlaca.requestFocus(); return false;
        }
        if (TextUtils.isEmpty(t(etMonto))) {
            etMonto.setError("Ingresa el monto"); etMonto.requestFocus(); return false;
        }
        try {
            double val = Double.parseDouble(t(etMonto));
            if (val <= 0) { etMonto.setError("Monto debe ser mayor a 0"); etMonto.requestFocus(); return false; }
        } catch (Exception e) {
            etMonto.setError("Monto inválido"); etMonto.requestFocus(); return false;
        }
        if (TextUtils.isEmpty(t(etMotivo))) {
            etMotivo.setError("Ingresa el motivo"); etMotivo.requestFocus(); return false;
        }
        if (empty(actAutoriza)) {
            actAutoriza.setError("Selecciona quién autoriza"); actAutoriza.requestFocus(); return false;
        }
        return true;
    }

    private void enviar() {
        SolicitudCrearReq req = new SolicitudCrearReq();
        req.tipoSolicitud = v(actTipo);          // "Descuento" | "Cortesía"
        req.planta        = v(actPlanta);
        req.plantaKey     = obtenerPlantaKeySeleccionada(); // CLAVE real

        String conceptoLabel = v(actConcepto);
        req.concepto      = conceptoLabel;

        // Recupera clave y valor del concepto seleccionado
        ConceptoPlantaDto dtoSel = conceptoByLabel.get(conceptoLabel);
        if (dtoSel != null) {
            req.conceptoKey   = dtoSel.conceptoKey;
            req.conceptoValor = dtoSel.valor;
        }

        req.tipoCampania  = v(actTipoCampania);
        req.placa         = t(etPlaca);
        req.monto         = Double.parseDouble(t(etMonto));
        req.motivo        = t(etMotivo);
        req.autoriza      = v(actAutoriza);

        // TODO: Retrofit -> NewApiClient.get().crearSolicitud(req).enqueue(...)
        Toast.makeText(this,
                "Solicitud lista: plantaKey=" + req.plantaKey
                        + " conceptoKey=" + req.conceptoKey
                        + " valor=" + req.conceptoValor,
                Toast.LENGTH_LONG).show();
        finish();
    }

    private String obtenerPlantaKeySeleccionada() {
        String nombre = v(actPlanta);
        if (TextUtils.isEmpty(nombre)) return "";
        String key = plantaNombreToKey.get(nombre);
        return key == null ? "" : key.trim();
    }

    // helpers
    private boolean empty(AutoCompleteTextView v) { return TextUtils.isEmpty(v.getText()); }
    private String v(AutoCompleteTextView v) { return v.getText() == null ? "" : v.getText().toString().trim(); }
    private String t(TextInputEditText v) { return v.getText() == null ? "" : v.getText().toString().trim(); }
    private String safe(String s) { return s == null ? "" : s.trim(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conceptosCall != null) conceptosCall.cancel();
    }
}
