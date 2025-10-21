package com.farenet.descuentos.ui.solicitudes.pendientes.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.solicitudes.model.SolicitudUI;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class PendienteSolicitudAdapter extends RecyclerView.Adapter<PendienteSolicitudAdapter.VH> {

    public interface Actions {
        void onAprobar(SolicitudUI s);
        void onRechazar(SolicitudUI s);
    }

    private final List<SolicitudUI> data;
    private final Actions actions;

    public PendienteSolicitudAdapter(List<SolicitudUI> data, Actions actions) {
        this.data = data;
        this.actions = actions;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_solicitud_pendiente, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        SolicitudUI s = data.get(pos);
        h.tvTitulo.setText(s.codigo + " · " + s.tipo);
        h.tvPlanta.setText("Planta: " + s.planta);
        h.tvSub.setText("Placa: " + s.placa);
        h.tvSub2.setText("Concepto Vehicular: " + s.conceptoDisplay);
        h.tvMonto.setText("Monto Descuento: S/." + s.monto);
        h.tvMotivo.setText("Motivo: " + s.motivo);
        h.tvEstado.setText("Estado: " + s.estado);

        h.btnAprobar.setOnClickListener(v -> {
            if (actions != null) actions.onAprobar(s);
        });
        h.btnRechazar.setOnClickListener(v -> {
            if (actions != null) actions.onRechazar(s);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitulo,tvPlanta, tvSub, tvSub2,tvMonto,tvMotivo, tvEstado;
        MaterialButton btnAprobar, btnRechazar;
        VH(@NonNull View v) {
            super(v);
            tvTitulo = v.findViewById(R.id.tv_titulo);
            tvPlanta = v.findViewById(R.id.tv_planta);
            tvSub    = v.findViewById(R.id.tv_sub);
            tvSub2    = v.findViewById(R.id.tv_sub2);
            tvMonto  = v.findViewById(R.id.tv_monto);
            tvMotivo    = v.findViewById(R.id.tv_motivo);
            tvEstado = v.findViewById(R.id.tv_estado);
            btnAprobar  = v.findViewById(R.id.btn_aprobar);
            btnRechazar = v.findViewById(R.id.btn_rechazar);
        }
    }
}
