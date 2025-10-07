package com.farenet.descuentos.ui.bolsa;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.R;
import com.farenet.descuentos.API.Actual.DTO.bolsa.BolsaConfigDto;
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

    private final List<BolsaConfigDto> data = new ArrayList<>();
    private final Actions actions;

    // Mapa recibido desde la Activity: key -> nombre (p.ej. "98" -> "Surco")
    private final Map<String, String> plantaKeyToNombre = new HashMap<>();

    // Formateadores
    private final Locale localeEsPE = new Locale("es", "PE");
    private final NumberFormat dfMonto; // "1,234.56" (prefijo S/ se agrega en texto)

    public BolsaEstadoAdapter(Actions actions) {
        this.actions = actions;
        DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(localeEsPE);
        df.applyPattern("#,##0.00");
        this.dfMonto = df;
    }

    /** Permite pasar el mapping key->nombre desde la Activity. */
    public void setPlantaKeyToNombre(Map<String, String> map) {
        this.plantaKeyToNombre.clear();
        if (map != null) this.plantaKeyToNombre.putAll(map);
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

        // Planta: muestra nombre si existe mapping; si no, el key
        String key = it.planta_key != null ? String.valueOf(it.planta_key) : "-";
        String nombrePlanta = plantaKeyToNombre.get(key);
        h.tvPlanta.setText(nombrePlanta != null ? nombrePlanta : key);

        // Período legible
        h.tvPeriodo.setText(formatPeriodoLegible(it.periodo_yyyymm));

        // Estado
        h.tvEstado.setText(it.estado != null ? it.estado : "-");

        // Montos
        double tope  = it.monto_tope != null ? it.monto_tope : 0d;
        double usado = it.monto_usado != null ? it.monto_usado : 0d;
        double saldo = it.saldo != null ? it.saldo : (tope - usado);

        h.tvTUS.setText(
                "Tope: S/ "  + dfMonto.format(tope)  +
                        " | Usado: S/ " + dfMonto.format(usado) +
                        " | Saldo: S/ " + dfMonto.format(saldo)
        );

        // Acciones
        h.btnAuditoria.setOnClickListener(v -> { if (actions != null) actions.onAuditoria(it); });
        h.btnEditarTope.setOnClickListener(v -> { if (actions != null) actions.onEditarTope(it); });
        h.btnToggleEstado.setText("CERRADO".equalsIgnoreCase(it.estado) ? "Abrir" : "Cerrar");
        h.btnToggleEstado.setOnClickListener(v -> { if (actions != null) actions.onToggleEstado(it); });
    }

    @Override
    public int getItemCount() { return data.size(); }

    // ---------- Utils ----------
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
        TextView tvPlanta, tvPeriodo, tvEstado, tvTUS;
        MaterialButton btnAuditoria, btnEditarTope, btnToggleEstado;

        VH(@NonNull View v) {
            super(v);
            tvPlanta       = v.findViewById(R.id.tvPlanta);
            tvPeriodo      = v.findViewById(R.id.tvPeriodo);
            tvEstado       = v.findViewById(R.id.tvEstado);
            tvTUS          = v.findViewById(R.id.tvTopeUsadoSaldo);
            btnAuditoria   = v.findViewById(R.id.btnAuditoria);
            btnEditarTope  = v.findViewById(R.id.btnEditarTope);
            btnToggleEstado= v.findViewById(R.id.btnToggleEstado);
        }
    }
}
