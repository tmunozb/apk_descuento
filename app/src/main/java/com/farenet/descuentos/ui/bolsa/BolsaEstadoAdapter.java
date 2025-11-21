package com.farenet.descuentos.ui.bolsa;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.API.Actual.DTO.bolsa.BolsaConfigDto;
import com.farenet.descuentos.BuildConfig;
import com.farenet.descuentos.R;
import com.google.android.material.button.MaterialButton;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/** Adapter para la pantalla de Estado de Bolsas. */
public class BolsaEstadoAdapter extends RecyclerView.Adapter<BolsaEstadoAdapter.VH> {

    public interface Actions {
        void onAuditoria(BolsaConfigDto item);
        void onEditarTope(BolsaConfigDto item);
        void onToggleEstado(BolsaConfigDto item);
    }

    private static final String TAG = "BolsaEstadoAdapter";

    private final List<BolsaConfigDto> data = new ArrayList<>();
    private final Actions actions;

    // Mapa normalizado: key(normalizada) -> nombre
    private final Map<String, String> plantaKeyToNombre = new HashMap<>();

    // Formateadores
    private final Locale localeEsPE = new Locale("es", "PE");
    private final NumberFormat dfMonto;

    // Control de permisos de UI (lo setea el Fragment)
    private boolean adminMode = false;

    public BolsaEstadoAdapter(Actions actions) {
        this.actions = actions;
        DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(localeEsPE);
        df.applyPattern("#,##0.00");
        this.dfMonto = df;
    }

    /** Habilita/Deshabilita botones de admin (auditoría / editar / abrir-cerrar). */
    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
        android.util.Log.i("BolsaEstadoAdapter", "setAdminMode -> " + adminMode + " (items=" + data.size() + ")");
        notifyDataSetChanged();
    }

    /** Para depuración en el Fragment, si quieres verificar el valor actual. */
    public boolean isAdminMode() { return adminMode; }

    /** Pasa el mapping de planta_key -> planta_nombre desde el Fragment. */
    public void setPlantaKeyToNombre(Map<String, String> map) {
        this.plantaKeyToNombre.clear();
        if (map != null) {
            for (Map.Entry<String, String> e : map.entrySet()) {
                String k = normalizeKey(e.getKey());
                String v = e.getValue();
                if (k != null && !k.isEmpty()) {
                    this.plantaKeyToNombre.put(k, v);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setItems(List<BolsaConfigDto> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bolsa_estado, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        final BolsaConfigDto it = data.get(pos);

        // -------- Planta: lookup con clave normalizada + fallback
        String rawKey = it != null && it.planta_key != null ? it.planta_key : "";
        String normKey = normalizeKey(rawKey);

        String nombrePlanta = null;
        if (normKey != null) {
            nombrePlanta = plantaKeyToNombre.get(normKey);
            if (nombrePlanta == null) {
                String alt = tryAlternateNumericKeys(normKey);
                if (alt != null) nombrePlanta = plantaKeyToNombre.get(alt);
            }
        }

        h.tvPlanta.setText(
                (nombrePlanta != null && !nombrePlanta.trim().isEmpty())
                        ? nombrePlanta
                        : (rawKey == null || rawKey.trim().isEmpty() ? "-" : rawKey.trim())
        );

        // Período legible
        h.tvPeriodo.setText(formatPeriodoLegible(it != null ? it.periodo_yyyymm : null));

        // Estado + color si está CERRADO
        String estado = it != null && it.estado != null ? it.estado : "-";
        h.tvEstado.setText(estado);
        if ("CERRADO".equalsIgnoreCase(estado)) {
            int red = ContextCompat.getColor(h.itemView.getContext(),
                    com.google.android.material.R.color.design_default_color_error);
            h.tvEstado.setTextColor(red);
        } else {
            h.tvEstado.setTextColor(h.defaultEstadoColor);
        }

        // ---- MONTOS robustos (pueden venir como String en la API) ----
        double tope  = parseDoubleSafe(it != null ? it.monto_tope  : null);
        double usado = parseDoubleSafe(it != null ? it.monto_usado : null);
        double saldo = (it != null && it.saldo != null) ? parseDoubleSafe(it.saldo) : (tope - usado);

        h.tvTUS.setText(
                "Tope: S/ "  + dfMonto.format(tope)  +
                        " | Usado: S/ " + dfMonto.format(usado) +
                        " | Saldo: S/ " + dfMonto.format(Math.max(0d, saldo))
        );

        // ---------- Acciones ----------
        // IMPORTANTE: el valor depende de setAdminMode() llamado por el Fragment
        if (adminMode) {
            h.btnAuditoria.setVisibility(View.VISIBLE);
            h.btnEditarTope.setVisibility(View.VISIBLE);
            h.btnToggleEstado.setVisibility(View.VISIBLE);

            h.btnAuditoria.setOnClickListener(v -> { if (actions != null) actions.onAuditoria(it); });
            h.btnEditarTope.setOnClickListener(v -> { if (actions != null) actions.onEditarTope(it); });

            h.btnToggleEstado.setText("CERRADO".equalsIgnoreCase(estado) ? "Abrir" : "Cerrar");
            h.btnToggleEstado.setOnClickListener(v -> { if (actions != null) actions.onToggleEstado(it); });
        } else {
            h.btnAuditoria.setVisibility(View.GONE);
            h.btnEditarTope.setVisibility(View.GONE);
            h.btnToggleEstado.setVisibility(View.GONE);

            h.btnAuditoria.setOnClickListener(null);
            h.btnEditarTope.setOnClickListener(null);
            h.btnToggleEstado.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() { return data.size(); }

    // ---------- Utils ----------

    /** Acepta Number, String o null. */
    private double parseDoubleSafe(Object val) {
        if (val == null) return 0d;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            String s = String.valueOf(val).trim().replace(",", ".");
            if (s.isEmpty() || s.equalsIgnoreCase("null")) return 0d;
            return Double.parseDouble(s);
        } catch (Exception ignore) {
            return 0d;
        }
    }

    /** Normaliza claves: trim, mayúsculas, si es numérico elimina ceros a la izquierda. */
    private String normalizeKey(String key) {
        if (key == null) return null;
        String k = key.trim();
        if (k.isEmpty()) return "";
        if (k.matches("^0*\\d+$")) {
            try {
                long n = Long.parseLong(k);
                return String.valueOf(n);
            } catch (NumberFormatException ignore) { }
        }
        return k.toUpperCase(Locale.ROOT);
    }

    private String tryAlternateNumericKeys(String normalizedKey) {
        if (normalizedKey == null) return null;
        if (normalizedKey.matches("^\\d+$")) {
            // Aquí podrías devolver una versión zero-padded si tu mapa se cargara así.
            return null;
        }
        return null;
    }

    private String formatPeriodoLegible(String yyyymm) {
        if (yyyymm == null || yyyymm.length() != 6) return yyyymm == null ? "-" : yyyymm;
        try {
            Date d = new SimpleDateFormat("yyyyMM", Locale.getDefault()).parse(yyyymm);
            if (d == null) return yyyymm;
            String mes = new SimpleDateFormat("MMMM yyyy", new Locale("es", "PE")).format(d);
            mes = capitalize(mes);
            return mes + " (" + yyyymm + ")";
        } catch (ParseException e) {
            return yyyymm;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    // ---------- ViewHolder ----------
    static class VH extends RecyclerView.ViewHolder {
        final TextView tvPlanta, tvPeriodo, tvEstado, tvTUS;
        final MaterialButton btnAuditoria, btnEditarTope, btnToggleEstado;
        final int defaultEstadoColor;

        VH(@NonNull View v) {
            super(v);
            tvPlanta        = v.findViewById(R.id.tvPlanta);
            tvPeriodo       = v.findViewById(R.id.tvPeriodo);
            tvEstado        = v.findViewById(R.id.tvEstado);
            tvTUS           = v.findViewById(R.id.tvTopeUsadoSaldo);
            btnAuditoria    = v.findViewById(R.id.btnAuditoria);
            btnEditarTope   = v.findViewById(R.id.btnEditarTope);
            btnToggleEstado = v.findViewById(R.id.btnToggleEstado);
            defaultEstadoColor = tvEstado.getTextColors().getDefaultColor();
        }
    }
}
