package com.farenet.descuentos.ui.bolsa;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.API.Actual.DTO.maestros.AccesoPlantaDto;
import com.farenet.descuentos.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter robusto para carga masiva de Bolsas:
 * - IDs estables por plantaKey (evita "saltos" al reciclar)
 * - TextWatcher único por EditText (no se duplica al reciclar)
 * - Payloads para actualizar solo el checkbox cuando se “marcan todas”
 */
public class BolsaBulkAdapter extends RecyclerView.Adapter<BolsaBulkAdapter.VH> {

    public static class Row {
        public String plantaKey;
        public String plantaNombre;
        public boolean seleccionado = false;
        public Double montoTope = null;

        public Row(String key, String nombre) {
            this.plantaKey = key;
            this.plantaNombre = nombre;
        }
    }

    private final List<Row> items = new ArrayList<>();

    public BolsaBulkAdapter() {
        // IDs estables para que RecyclerView mantenga la identidad de cada fila
        setHasStableIds(true);
    }

    public void setData(List<AccesoPlantaDto> accesos) {
        items.clear();
        if (accesos != null) {
            for (AccesoPlantaDto a : accesos) {
                if (a == null) continue;
                String key = a.key != null ? a.key.trim() : "";
                String nombre = a.planta != null ? a.planta.trim() : "";
                if (!key.isEmpty()) {
                    items.add(new Row(key, nombre.isEmpty() ? key : nombre));
                }
            }
        }
        notifyDataSetChanged();
    }

    /** Marca o desmarca todas las filas sin tocar los EditText (usa payload). */
    public void marcarTodas(boolean value) {
        for (Row r : items) r.seleccionado = value;
        if (!items.isEmpty()) {
            notifyItemRangeChanged(0, items.size(), "checked-only");
        }
    }

    public List<Row> getSeleccionadosConMonto() {
        List<Row> out = new ArrayList<>();
        for (Row r : items) {
            if (r.seleccionado && r.montoTope != null && r.montoTope >= 0) {
                out.add(r);
            }
        }
        return out;
    }

    // ===== RecyclerView =====

    @Override
    public long getItemId(int position) {
        // hash estable por plantaKey; si falta, usa posición (caso extremo)
        String k = items.get(position).plantaKey;
        return (k == null) ? position : k.hashCode();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_bolsa_bulk_planta, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        bindFull(h, items.get(pos));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            Object p = payloads.get(0);
            if ("checked-only".equals(p)) {
                // Solo actualizar el CheckBox sin tocar EditText (evita rebind del texto)
                Row r = items.get(pos);
                h.chkSel.setOnCheckedChangeListener(null);
                h.chkSel.setChecked(r.seleccionado);
                h.chkSel.setOnCheckedChangeListener((buttonView, isChecked) -> r.seleccionado = isChecked);
                return;
            }
        }
        super.onBindViewHolder(h, pos, payloads);
    }

    private void bindFull(@NonNull VH h, @NonNull Row r) {
        h.tvPlanta.setText(!TextUtils.isEmpty(r.plantaNombre) ? r.plantaNombre : r.plantaKey);
        h.tvPlantaKey.setText(r.plantaKey);

        // CheckBox: quitar listener, setear estado, volver a poner listener
        h.chkSel.setOnCheckedChangeListener(null);
        h.chkSel.setChecked(r.seleccionado);
        h.chkSel.setOnCheckedChangeListener((buttonView, isChecked) -> r.seleccionado = isChecked);

        // TextWatcher: desmonta el previo
        TextWatcher oldTw = (TextWatcher) h.etMonto.getTag(R.id.tag_text_watcher);
        if (oldTw != null) {
            h.etMonto.removeTextChangedListener(oldTw);
        }

        // Setea el texto solo si cambió para no disparar watchers innecesariamente
        String show = (r.montoTope == null) ? "" : plainNumber(r.montoTope);
        if (!TextUtils.equals(h.etMonto.getText(), show)) {
            h.etMonto.setText(show);
            if (!h.etMonto.hasFocus()) {
                h.etMonto.setSelection(h.etMonto.getText().length());
            }
        }

        // Nuevo watcher que solo actualiza el modelo (no hace notify)
        TextWatcher tw = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String v = (s == null) ? "" : s.toString().trim();
                if (v.isEmpty()) {
                    r.montoTope = null;
                    return;
                }
                // Acepta coma o punto como decimal
                v = v.replace(',', '.');
                try {
                    r.montoTope = Double.parseDouble(v);
                } catch (Exception e) {
                    r.montoTope = null;
                }
            }
        };
        h.etMonto.addTextChangedListener(tw);
        h.etMonto.setTag(R.id.tag_text_watcher, tw);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** Limpia listeners al reciclar para evitar duplicados y fugas. */
    @Override
    public void onViewRecycled(@NonNull VH holder) {
        super.onViewRecycled(holder);
        TextWatcher oldTw = (TextWatcher) holder.etMonto.getTag(R.id.tag_text_watcher);
        if (oldTw != null) {
            holder.etMonto.removeTextChangedListener(oldTw);
            holder.etMonto.setTag(R.id.tag_text_watcher, null);
        }
        holder.chkSel.setOnCheckedChangeListener(null);
    }

    // ===== Utils =====

    private static String plainNumber(Double d) {
        if (d == null) return "";
        // No formatear con separadores para no reescribir mientras el usuario digita
        // (preserva exactamente lo que el usuario espera ver)
        if (d == Math.rint(d)) return String.valueOf(d.intValue());
        return String.valueOf(d);
    }

    // Watcher base para no implementar métodos que no usamos
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {}
    }

    // ===== ViewHolder =====

    static class VH extends RecyclerView.ViewHolder {
        final CheckBox chkSel;
        final TextView tvPlanta;
        final TextView tvPlantaKey;
        final EditText etMonto;

        VH(@NonNull View v) {
            super(v);
            chkSel = v.findViewById(R.id.chk_sel);
            tvPlanta = v.findViewById(R.id.tv_planta);
            tvPlantaKey = v.findViewById(R.id.tv_planta_key);
            etMonto = v.findViewById(R.id.et_monto);
        }
    }
}
