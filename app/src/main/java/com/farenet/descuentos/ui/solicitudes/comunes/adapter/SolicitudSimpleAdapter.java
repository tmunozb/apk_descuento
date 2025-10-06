package com.farenet.descuentos.ui.solicitudes.comunes.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.solicitudes.model.SolicitudUI;

import java.util.List;

public class SolicitudSimpleAdapter extends RecyclerView.Adapter<SolicitudSimpleAdapter.VH> {

    public interface OnItemClick {
        void onClick(SolicitudUI s);
    }

    private final List<SolicitudUI> data;
    private final OnItemClick listener;

    public SolicitudSimpleAdapter(List<SolicitudUI> data, OnItemClick listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_solicitud_simple, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        SolicitudUI s = data.get(pos);
        h.tvTitulo.setText(s.codigo + " · " + s.tipo);
        h.tvSub.setText("Placa: " + s.placa + " · Planta: " + s.planta + " · Motivo: " + s.motivo);
        h.tvEstado.setText(s.estado);
        h.tvFecha.setText(s.fecha == null ? "" : s.fecha);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(s);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvSub, tvEstado, tvFecha;
        VH(@NonNull View v) {
            super(v);
            tvTitulo = v.findViewById(R.id.tv_titulo);
            tvSub    = v.findViewById(R.id.tv_sub);
            tvEstado = v.findViewById(R.id.tv_estado);
            tvFecha  = v.findViewById(R.id.tv_fecha);
        }
    }
}
