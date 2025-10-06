package com.farenet.descuentos.ui.bolsa;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.R;
import com.farenet.descuentos.API.Actual.DTO.maestros.AccesoPlantaDto;

import java.util.ArrayList;
import java.util.List;

public class BolsaBulkAdapter extends RecyclerView.Adapter<BolsaBulkAdapter.VH> {

    public static class Row {
        public String plantaKey;
        public String plantaNombre;
        public boolean seleccionado = false;
        public Double montoTope = null;
        public Row(String key, String nombre) {
            this.plantaKey = key; this.plantaNombre = nombre;
        }
    }

    private final List<Row> items = new ArrayList<>();

    public void setData(List<AccesoPlantaDto> accesos) {
        items.clear();
        if (accesos != null) {
            for (AccesoPlantaDto a : accesos) {
                if (a == null) continue;
                String key = a.key != null ? a.key.trim() : "";
                String nombre = a.planta != null ? a.planta.trim() : "";
                if (!key.isEmpty() && !nombre.isEmpty()) {
                    items.add(new Row(key, nombre));
                }
            }
        }
        notifyDataSetChanged();
    }

    public void marcarTodas(boolean value) {
        for (Row r : items) r.seleccionado = value;
        notifyDataSetChanged();
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

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_bolsa_bulk_planta, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Row r = items.get(pos);
        h.tvPlanta.setText(r.plantaNombre);
        h.tvPlantaKey.setText(r.plantaKey);

        h.chkSel.setOnCheckedChangeListener(null);
        h.chkSel.setChecked(r.seleccionado);
        h.chkSel.setOnCheckedChangeListener((buttonView, isChecked) -> r.seleccionado = isChecked);

        h.etMonto.setText(r.montoTope == null ? "" : String.valueOf(r.montoTope));
        h.etMonto.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    String v = s.toString().trim();
                    r.montoTope = v.isEmpty() ? null : Double.parseDouble(v);
                } catch (Exception e) {
                    r.montoTope = null;
                }
            }
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox chkSel; TextView tvPlanta; TextView tvPlantaKey; EditText etMonto;
        VH(@NonNull View v) {
            super(v);
            chkSel = v.findViewById(R.id.chk_sel);
            tvPlanta = v.findViewById(R.id.tv_planta);
            tvPlantaKey = v.findViewById(R.id.tv_planta_key);
            etMonto = v.findViewById(R.id.et_monto);
        }
    }
}
