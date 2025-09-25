package com.farenet.descuentos;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.farenet.descuentos.models.newapi.LoginRsp;
import com.farenet.descuentos.models.req.SolicitudCrearReq;
import com.farenet.descuentos.repository.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.farenet.descuentos.models.newapi.AccesoPlantaDto;


public class SolicitudCrearActivity extends AppCompatActivity {

    private View step1, step2, step3;
    private MaterialButton btnAtras, btnSiguiente, btnEnviar;

    // Paso 1
    private AutoCompleteTextView actTipo, actPlanta, actConcepto, actTipoCampania;
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
        // Autoselección si solo hay una planta
        if (plantasNombres.size() == 1) {
            actPlanta.setText(plantasNombres.get(0), false);
        }

        // 3) Resto de combos (por ahora mock; reemplaza por tus catálogos/API)
        setAdapter(actConcepto, new String[]{"Inspección CI", "Reinspección", "Revisión técnica", "Otros"});
        setAdapter(actTipoCampania, new String[]{"N/A", "Campaña XX", "Alianza Y"});
        actTipoCampania.setText("N/A", false);

        setAdapter(actAutoriza, new String[]{"Jefe Operaciones", "Gerente Operaciones", "Sistemas"});

        // listeners
        btnAtras.setOnClickListener(v -> goBack());
        btnSiguiente.setOnClickListener(v -> goNext());
        btnEnviar.setOnClickListener(v -> enviar());

        render();
    }

    /** Usa accesos guardados en la sesión para poblar el combo de plantas. */
    /** Usa accesos guardados en la sesión para poblar el combo de plantas. */
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
            // Armar resumen
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
        if (empty(actTipo))     { actTipo.setError("Selecciona el tipo"); actTipo.requestFocus(); return false; }
        if (empty(actPlanta))   { actPlanta.setError("Selecciona la planta"); actPlanta.requestFocus(); return false; }
        if (empty(actConcepto)) { actConcepto.setError("Selecciona el concepto"); actConcepto.requestFocus(); return false; }
        if (empty(actTipoCampania)) { actTipoCampania.setError("Selecciona el tipo de campaña"); actTipoCampania.requestFocus(); return false; }
        // Validar que la planta seleccionada exista en el mapa
        String key = obtenerPlantaKeySeleccionada();
        if (TextUtils.isEmpty(key)) {
            actPlanta.setError("Planta inválida para tu sesión");
            actPlanta.requestFocus();
            return false;
        }
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
        // Construye el DTO listo para Retrofit
        SolicitudCrearReq req = new SolicitudCrearReq();
        req.tipoSolicitud = v(actTipo);          // "Descuento" | "Cortesía"
        req.planta        = v(actPlanta);
        req.plantaKey     = obtenerPlantaKeySeleccionada(); // CLAVE real
        req.concepto      = v(actConcepto);
        req.tipoCampania  = v(actTipoCampania);
        req.placa         = t(etPlaca);
        req.monto         = Double.parseDouble(t(etMonto));
        req.motivo        = t(etMotivo);
        req.autoriza      = v(actAutoriza);

        // TODO: Retrofit -> NewApiClient.get().crearSolicitud(req).enqueue(...)
        Toast.makeText(this, "Solicitud lista para enviar con plantaKey=" + req.plantaKey, Toast.LENGTH_LONG).show();
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
}
