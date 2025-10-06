package com.farenet.descuentos.ui.bolsa;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.R;
import com.farenet.descuentos.API.Actual.DTO.bolsa.BolsaConfigDto;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BolsaEstadoAdapter extends RecyclerView.Adapter<BolsaEstadoAdapter.VH> {

    public interface Actions {
        void onAuditoria(BolsaConfigDto item);
        void onEditarTope(BolsaConfigDto item);
        void onToggleEstado(BolsaConfigDto item);
    }

    private final List<BolsaConfigDto> data = new ArrayList<>();
    private final Actions actions;

    public BolsaEstadoAdapter(Actions actions) {
        this.actions = actions;
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
        BolsaConfigDto it = data.get(pos);
        h.tvPlanta.setText(String.valueOf(it.planta_key));
        h.tvPeriodo.setText(it.periodo_yyyymm != null ? it.periodo_yyyymm : "");
        h.tvEstado.setText(it.estado != null ? it.estado : "-");

        double tope  = it.monto_tope != null ? it.monto_tope : 0d;
        double usado = it.monto_usado != null ? it.monto_usado : 0d;
        double saldo = it.saldo != null ? it.saldo : (tope - usado);

        h.tvTUS.setText(String.format(Locale.getDefault(),
                "Tope: S/ %,.2f | Usado: S/ %,.2f | Saldo: S/ %,.2f", tope, usado, saldo));

        h.btnAuditoria.setOnClickListener(v -> actions.onAuditoria(it));
        h.btnEditarTope.setOnClickListener(v -> actions.onEditarTope(it));
        h.btnToggleEstado.setText("CERRADO".equalsIgnoreCase(it.estado) ? "Abrir" : "Cerrar");
        h.btnToggleEstado.setOnClickListener(v -> actions.onToggleEstado(it));
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvPlanta, tvPeriodo, tvEstado, tvTUS;
        MaterialButton btnAuditoria, btnEditarTope, btnToggleEstado;

        VH(@NonNull View v) {
            super(v);
            tvPlanta = v.findViewById(R.id.tvPlanta);
            tvPeriodo= v.findViewById(R.id.tvPeriodo);
            tvEstado = v.findViewById(R.id.tvEstado);
            tvTUS    = v.findViewById(R.id.tvTopeUsadoSaldo);
            btnAuditoria   = v.findViewById(R.id.btnAuditoria);
            btnEditarTope  = v.findViewById(R.id.btnEditarTope);
            btnToggleEstado= v.findViewById(R.id.btnToggleEstado);
        }
    }
}
