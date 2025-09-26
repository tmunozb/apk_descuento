package com.farenet.descuentos;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.farenet.descuentos.models.newapi.AccesoPlantaDto;
import com.farenet.descuentos.models.newapi.ConceptoPlantaDto;
import com.farenet.descuentos.models.newapi.ConceptosResponse;
import com.farenet.descuentos.models.newapi.TipoPagoDto;
import com.farenet.descuentos.models.newapi.UsuarioPerfil;
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

    // Panels
    private View step1, step2, step3;
    private MaterialButton btnAtras, btnSiguiente, btnEnviar;

    // Stepper (círculos/labels/conectores)
    private View step1Circle, step2Circle, step3Circle;
    private TextView tvStep1Num, tvStep2Num, tvStep3Num;
    private View line12Fg, line23Fg;

    // Paso 1
    private AutoCompleteTextView actTipo, actPlanta, actConcepto, actTipoPago, actTipoDescuento;
    private TextInputLayout tilConcepto, tilTipoPago, tilTipoDescuento;

    // Paso 2
    private TextInputLayout tilMonto;
    private TextInputEditText etPlaca, etMonto, etMotivo;

    // Paso 3
    private TextView tvResumen;

    private int step = 1;

    // Sesión / plantas
    private SessionManager session;
    private final List<String> plantasNombres = new ArrayList<>();
    private final Map<String, String> plantaNombreToKey = new LinkedHashMap<>();

    // Conceptos
    private final List<String> conceptosLabel = new ArrayList<>();
    private final Map<String, ConceptoPlantaDto> conceptoByLabel = new LinkedHashMap<>();
    private Call<ConceptosResponse> conceptosCall;

    // Tipos de pago
    private final List<String> tiposPagoLabel = new ArrayList<>();
    private final Map<String, TipoPagoDto> tipoPagoByLabel = new LinkedHashMap<>();
    private Call<List<TipoPagoDto>> tiposPagoCall;

    // Flags para selección diferida de FLAT
    private boolean pendingSelectFlat = false;
    private static final String TIPO_PAGO_FLAT_KEY = "FLA";
    private static final String TIPO_PAGO_FLAT_NOMBRE = "Flat";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitud_crear);

        session = new SessionManager(getApplicationContext());

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        // Panels
        step1 = findViewById(R.id.panel_step1);
        step2 = findViewById(R.id.panel_step2);
        step3 = findViewById(R.id.panel_step3);

        // Stepper refs (asegúrate que existan en el XML nuevo)
        step1Circle = findViewById(R.id.step1_container) != null ? ((View)((android.view.ViewGroup)findViewById(R.id.step1_container)).getChildAt(0)) : null;
        step2Circle = findViewById(R.id.step2_container) != null ? ((View)((android.view.ViewGroup)findViewById(R.id.step2_container)).getChildAt(0)) : null;
        step3Circle = findViewById(R.id.step3_container) != null ? ((View)((android.view.ViewGroup)findViewById(R.id.step3_container)).getChildAt(0)) : null;

        tvStep1Num = findViewById(R.id.tv_step1_num);
        tvStep2Num = findViewById(R.id.tv_step2_num);
        tvStep3Num = findViewById(R.id.tv_step3_num);
        line12Fg   = findViewById(R.id.line_1_2_fg);
        line23Fg   = findViewById(R.id.line_2_3_fg);

        // Inputs
        actTipo          = findViewById(R.id.act_tipo);
        actPlanta        = findViewById(R.id.act_planta);
        actConcepto      = findViewById(R.id.act_concepto);
        actTipoPago      = findViewById(R.id.act_tipopago);
        actTipoDescuento = findViewById(R.id.act_tipodescuento);

        tilConcepto      = findViewById(R.id.til_concepto);
        tilTipoPago      = findViewById(R.id.til_tipopago);
        tilTipoDescuento = findViewById(R.id.til_tipodescuento);

        etPlaca   = findViewById(R.id.et_placa);
        etMonto   = findViewById(R.id.et_monto);
        etMotivo  = findViewById(R.id.et_motivo);
        tilMonto  = findViewById(R.id.til_monto);

        tvResumen = findViewById(R.id.tv_resumen);

        btnAtras     = findViewById(R.id.btn_atras);
        btnSiguiente = findViewById(R.id.btn_siguiente);
        btnEnviar    = findViewById(R.id.btn_enviar);

        // Tipo de solicitud
        setAdapter(actTipo, new String[]{"Descuento", "Cortesía"});
        actTipo.setText("Descuento", false);
        actTipo.setOnClickListener(v -> actTipo.showDropDown());
        actTipo.setOnFocusChangeListener((v, f) -> { if (f) actTipo.showDropDown(); });
        actTipo.setOnItemClickListener((p, v, pos, id) -> {
            applyTipoSolicitudUI();
            if (isCortesia()) {
                actConcepto.setText("", false);
                actTipoPago.setText("", false);
                actTipoDescuento.setText("", false);
            } else {
                String key = obtenerPlantaKeySeleccionada();
                if (!TextUtils.isEmpty(key)) cargarConceptosPorPlanta(key);
            }
        });

        // Plantas
        cargarPlantasDesdeSesion();
        setAdapter(actPlanta, plantasNombres.toArray(new String[0]));
        actPlanta.setOnClickListener(v -> actPlanta.showDropDown());
        actPlanta.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actPlanta.showDropDown(); });
        actPlanta.setOnItemClickListener((parent, view, position, id) -> {
            actPlanta.setError(null);
            String key = obtenerPlantaKeySeleccionada();
            if (TextUtils.isEmpty(key)) {
                resetConceptos();
            } else if (!isCortesia()) {
                cargarConceptosPorPlanta(key);
            }
        });
        if (plantasNombres.size() == 1) {
            actPlanta.setText(plantasNombres.get(0), false);
            String key = obtenerPlantaKeySeleccionada();
            if (!TextUtils.isEmpty(key) && !isCortesia()) cargarConceptosPorPlanta(key);
        }

        // Tipo de pago
        actTipoPago.setOnClickListener(v -> {
            if (tiposPagoLabel.isEmpty()) cargarTiposPago();
            actTipoPago.showDropDown();
        });
        actTipoPago.setOnFocusChangeListener((v, f) -> {
            if (f) {
                if (tiposPagoLabel.isEmpty()) cargarTiposPago();
                actTipoPago.showDropDown();
            }
        });
        actTipoPago.setOnItemClickListener((p, v, pos, id) -> actTipoPago.setError(null));

        // Tipo de descuento
        setAdapter(actTipoDescuento, new String[]{"Autorizado", "Carta", "Campaña"});
        actTipoDescuento.setOnClickListener(v -> actTipoDescuento.showDropDown());
        actTipoDescuento.setOnFocusChangeListener((v, f) -> { if (f) actTipoDescuento.showDropDown(); });

        // Concepto disabled hasta elegir planta (si no es cortesía)
        actConcepto.setEnabled(false);
        if (tilConcepto != null) tilConcepto.setEnabled(false);
        actConcepto.setOnClickListener(v -> actConcepto.showDropDown());
        actConcepto.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actConcepto.showDropDown(); });
        actConcepto.setOnItemClickListener((p, v, pos, id) -> actConcepto.setError(null));

        // UI inicial por tipo/rol
        applyTipoSolicitudUI();

        // Nav con stepper
        btnAtras.setOnClickListener(v -> setStep(step - 1));
        btnSiguiente.setOnClickListener(v -> {
            if (step == 1 && !validStep1()) return;
            if (step == 2 && !validStep2()) return;
            setStep(step + 1);
        });
        btnEnviar.setOnClickListener(v -> enviar());

        // Estado inicial
        setStep(1);
    }

    /* =========================
            STEPPER UI
       ========================= */

    private void setStep(int target) {
        step = Math.max(1, Math.min(3, target));
        // Panels & botones
        step1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        step2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        step3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        btnAtras.setEnabled(step > 1);
        btnSiguiente.setVisibility(step < 3 ? View.VISIBLE : View.GONE);
        btnEnviar.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        // Stepper circles + connectors
        // Fallback si no existen (por si estás migrando el XML en fases)
        if (tvStep1Num == null || tvStep2Num == null || tvStep3Num == null) {
            render(); // al menos genera el resumen en paso 3
            return;
        }

        switch (step) {
            case 1:
                setCircle(step1Circle, R.drawable.bg_step_active);  tvStep1Num.setText("1");
                setCircle(step2Circle, R.drawable.bg_step_inactive);tvStep2Num.setText("2");
                setCircle(step3Circle, R.drawable.bg_step_inactive);tvStep3Num.setText("3");
                animateConnector(line12Fg, 0f);
                animateConnector(line23Fg, 0f);
                break;

            case 2:
                setCircle(step1Circle, R.drawable.bg_step_done);     tvStep1Num.setText("✓");
                setCircle(step2Circle, R.drawable.bg_step_active);   tvStep2Num.setText("2");
                setCircle(step3Circle, R.drawable.bg_step_inactive); tvStep3Num.setText("3");
                animateConnector(line12Fg, 1f);
                animateConnector(line23Fg, 0f);
                break;

            case 3:
                setCircle(step1Circle, R.drawable.bg_step_done);     tvStep1Num.setText("✓");
                setCircle(step2Circle, R.drawable.bg_step_done);     tvStep2Num.setText("✓");
                setCircle(step3Circle, R.drawable.bg_step_active);   tvStep3Num.setText("3");
                animateConnector(line12Fg, 1f);
                animateConnector(line23Fg, 1f);
                break;
        }

        render();
    }

    private void setCircle(View circle, int drawable) {
        if (circle != null) circle.setBackgroundResource(drawable);
    }

    // Anima el ancho del conector foreground (0..1 del ancho del parent)
    private void animateConnector(View fg, float fraction) {
        if (fg == null || fg.getParent() == null) return;
        View parent = (View) fg.getParent();
        parent.post(() -> {
            int target = (int) (parent.getWidth() * Math.max(0f, Math.min(1f, fraction)));
            int start = fg.getWidth();
            if (start == target) return;

            ValueAnimator va = ValueAnimator.ofInt(start, target);
            va.setDuration(220);
            va.addUpdateListener(a -> {
                ViewGroup.LayoutParams lp = fg.getLayoutParams();
                lp.width = (int) a.getAnimatedValue();
                fg.setLayoutParams(lp);
            });
            va.start();
        });
    }

    /* =========================
          LÓGICA EXISTENTE
       ========================= */

    /** Mostrar/ocultar controles según tipo (Cortesía/Descuento) y bloquear por rol */
    private void applyTipoSolicitudUI() {
        boolean cortesia = isCortesia();

        // Paso 1: ocultar Concepto / TipoPago / TipoDescuento en Cortesía
        setVisible(tilConcepto, !cortesia);
        setVisible(tilTipoPago, !cortesia);
        setVisible(tilTipoDescuento, !cortesia);

        actConcepto.setEnabled(!cortesia && !conceptosLabel.isEmpty());
        if (tilConcepto != null) tilConcepto.setEnabled(!cortesia && !conceptosLabel.isEmpty());

        enableTipoPago(!cortesia);
        enableTipoDescuento(!cortesia);

        // Paso 2: ocultar Monto si es Cortesía
        if (tilMonto != null) tilMonto.setVisibility(cortesia ? View.GONE : View.VISIBLE);
        if (cortesia) {
            etMonto.setText("");
            etMonto.setError(null);
        }

        // Defaults/bloqueos por rol (solo si es Descuento)
        if (!cortesia) applyRoleDefaultsAndLocks();
    }

    /** Defaults/bloqueos por rol cuando es Descuento */
    private void applyRoleDefaultsAndLocks() {
        if (!isAdminOrOps()) {
            enableTipoPago(true);
            enableTipoDescuento(true);
            return;
        }
        // Tipo de descuento = Autorizado (bloqueado)
        actTipoDescuento.setText("Autorizado", false);
        enableTipoDescuento(false);

        // Tipo de pago = Flat (bloqueado)
        if (tiposPagoLabel.isEmpty()) {
            pendingSelectFlat = true;
            cargarTiposPago();
        } else {
            String labelToSet = findTipoPagoLabelByKeyOrNombre(TIPO_PAGO_FLAT_KEY, TIPO_PAGO_FLAT_NOMBRE);
            if (!TextUtils.isEmpty(labelToSet)) {
                actTipoPago.setText(labelToSet, false);
            } else if (!tiposPagoLabel.isEmpty()) {
                actTipoPago.setText(tiposPagoLabel.get(0), false);
            }
            enableTipoPago(false);
        }
    }

    private void enableTipoPago(boolean enable) {
        actTipoPago.setEnabled(enable);
        if (tilTipoPago != null) tilTipoPago.setEnabled(enable);
    }

    private void enableTipoDescuento(boolean enable) {
        actTipoDescuento.setEnabled(enable);
        if (tilTipoDescuento != null) tilTipoDescuento.setEnabled(enable);
    }

    private boolean isCortesia() { return "Cortesía".equalsIgnoreCase(v(actTipo)); }

    private boolean isAdminOrOps() {
        UsuarioPerfil up = session.getPerfil();
        String p = (up != null ? up.perfilId : null);
        if (p == null) return false;
        p = p.trim().toLowerCase();
        return p.equals("administrador") || p.equals("operaciones");
    }

    private void setVisible(TextInputLayout til, boolean visible) {
        if (til != null) til.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /** Plantas desde sesión */
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

    /** Conceptos por planta */
    private void cargarConceptosPorPlanta(String plantaKey) {
        if (conceptosCall != null) conceptosCall.cancel();
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
                actConcepto.setEnabled(habilitar && !isCortesia());
                if (tilConcepto != null) tilConcepto.setEnabled(habilitar && !isCortesia());
                if (conceptosLabel.size() == 1) actConcepto.setText(conceptosLabel.get(0), false);
            }
            @Override
            public void onFailure(Call<ConceptosResponse> call, Throwable t) {
                if (call.isCanceled()) return;
                Toast.makeText(SolicitudCrearActivity.this, "Error al cargar conceptos", Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Tipos de pago */
    private void cargarTiposPago() {
        if (tiposPagoCall != null) tiposPagoCall.cancel();

        tiposPagoLabel.clear();
        tipoPagoByLabel.clear();
        setAdapter(actTipoPago, new String[0]);

        tiposPagoCall = NewApiClient.get().obtenerTiposPago(null);
        tiposPagoCall.enqueue(new Callback<List<TipoPagoDto>>() {
            @Override
            public void onResponse(Call<List<TipoPagoDto>> call, Response<List<TipoPagoDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(SolicitudCrearActivity.this, "No se pudieron cargar tipos de pago", Toast.LENGTH_LONG).show();
                    return;
                }
                for (TipoPagoDto t : response.body()) {
                    if (t == null) continue;
                    String label = !TextUtils.isEmpty(t.nombre) ? t.nombre : (t.key != null ? t.key : "—");
                    tiposPagoLabel.add(label);
                    tipoPagoByLabel.put(label, t);
                }
                setAdapter(actTipoPago, tiposPagoLabel.toArray(new String[0]));

                if (pendingSelectFlat) {
                    pendingSelectFlat = false;
                    String labelToSet = findTipoPagoLabelByKeyOrNombre(TIPO_PAGO_FLAT_KEY, TIPO_PAGO_FLAT_NOMBRE);
                    if (!TextUtils.isEmpty(labelToSet)) {
                        actTipoPago.setText(labelToSet, false);
                    }
                    enableTipoPago(false);
                }
            }

            @Override
            public void onFailure(Call<List<TipoPagoDto>> call, Throwable t) {
                if (call.isCanceled()) return;
                Toast.makeText(SolicitudCrearActivity.this, "Error al cargar tipos de pago", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String findTipoPagoLabelByKeyOrNombre(String key, String nombre) {
        for (String label : tiposPagoLabel) {
            if (label.equalsIgnoreCase(nombre)) return label;
        }
        for (Map.Entry<String, TipoPagoDto> e : tipoPagoByLabel.entrySet()) {
            TipoPagoDto dto = e.getValue();
            if (dto != null && key.equalsIgnoreCase(dto.key)) return e.getKey();
        }
        return null;
    }

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
        if (step == 3) {
            StringBuilder sb = new StringBuilder();
            sb.append("Tipo: ").append(v(actTipo)).append("\n");
            sb.append("Planta: ").append(v(actPlanta)).append("\n");

            if (!isCortesia()) {
                sb.append("Concepto: ").append(v(actConcepto)).append("\n");
                sb.append("Tipo de pago: ").append(v(actTipoPago)).append("\n");
                sb.append("Tipo de descuento: ").append(v(actTipoDescuento)).append("\n");
            }

            sb.append("Placa: ").append(t(etPlaca)).append("\n");
            if (!isCortesia()) {
                sb.append("Monto: ").append(t(etMonto)).append("\n");
            }
            sb.append("Motivo: ").append(t(etMotivo)).append("\n");
            sb.append("PlantaKey: ").append(obtenerPlantaKeySeleccionada());

            tvResumen.setText(sb.toString());
        }
    }

    private boolean validStep1() {
        if (empty(actTipo))   { actTipo.setError("Selecciona el tipo"); actTipo.requestFocus(); return false; }
        if (empty(actPlanta)) { actPlanta.setError("Selecciona la planta"); actPlanta.requestFocus(); return false; }
        String key = obtenerPlantaKeySeleccionada();
        if (TextUtils.isEmpty(key)) { actPlanta.setError("Planta inválida"); actPlanta.requestFocus(); return false; }

        if (!isCortesia()) {
            if (!actConcepto.isEnabled() || empty(actConcepto)) {
                actConcepto.setError("Selecciona el concepto"); actConcepto.requestFocus(); return false;
            }
            if (empty(actTipoPago)) {
                actTipoPago.setError("Selecciona el tipo de pago"); actTipoPago.requestFocus(); return false;
            }
            if (empty(actTipoDescuento)) {
                actTipoDescuento.setError("Selecciona el tipo de descuento"); actTipoDescuento.requestFocus(); return false;
            }
        }
        return true;
    }

    private boolean validStep2() {
        if (TextUtils.isEmpty(t(etPlaca))) {
            etPlaca.setError("Ingresa la placa"); etPlaca.requestFocus(); return false;
        }
        if (!isCortesia()) {
            if (TextUtils.isEmpty(t(etMonto))) {
                etMonto.setError("Ingresa el monto"); etMonto.requestFocus(); return false;
            }
            try {
                double val = Double.parseDouble(t(etMonto));
                if (val <= 0) { etMonto.setError("Monto debe ser mayor a 0"); etMonto.requestFocus(); return false; }
            } catch (Exception e) {
                etMonto.setError("Monto inválido"); etMonto.requestFocus(); return false;
            }
        } else {
            etMonto.setError(null);
        }
        if (TextUtils.isEmpty(t(etMotivo))) {
            etMotivo.setError("Ingresa el motivo"); etMotivo.requestFocus(); return false;
        }
        return true;
    }

    private void enviar() {
        SolicitudCrearReq req = new SolicitudCrearReq();

        // Paso 1
        req.tipoSolicitud = v(actTipo);
        req.planta        = v(actPlanta);
        req.plantaKey     = obtenerPlantaKeySeleccionada();

        if (!isCortesia()) {
            // Concepto
            String conceptoLabel = v(actConcepto);
            req.concepto = conceptoLabel;
            ConceptoPlantaDto dtoSel = conceptoByLabel.get(conceptoLabel);
            if (dtoSel != null) {
                req.conceptoKey   = dtoSel.conceptoKey;
                req.conceptoValor = dtoSel.valor;
            }

            // Tipo de pago
            String tpLabel = v(actTipoPago);
            TipoPagoDto tpSel = tipoPagoByLabel.get(tpLabel);
            if (tpSel != null) {
                req.tipoPagoKey = tpSel.key;     // "POR" | "MON" | "FLA"
                req.tipoPago    = tpSel.nombre;  // "Porcentaje" | "Monto" | "Flat"
            } else {
                req.tipoPago = tpLabel;
            }

            // Tipo de descuento
            req.tipoDescuento = v(actTipoDescuento);
        }

        // Paso 2
        req.placa  = t(etPlaca);
        req.motivo = t(etMotivo);
        if (!isCortesia()) {
            try { req.monto = Double.parseDouble(t(etMonto)); }
            catch (Exception e) { req.monto = null; }
        } else {
            req.monto = null;
        }

        // TODO: NewApiClient.get().crearSolicitud(req).enqueue(...)

        Toast.makeText(this,
                "Solicitud lista:\nTipo=" + req.tipoSolicitud
                        + "\nplantaKey=" + req.plantaKey
                        + (isCortesia() ? "" : ("\nconceptoKey=" + req.conceptoKey
                        + "\ntipoPago=" + req.tipoPago + " (" + req.tipoPagoKey + ")"
                        + "\nTipoDesc=" + req.tipoDescuento
                        + "\nMonto=" + req.monto))
                        + "\nPlaca=" + req.placa
                        + "\nMotivo=" + req.motivo,
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
        if (tiposPagoCall != null) tiposPagoCall.cancel();
    }
}
