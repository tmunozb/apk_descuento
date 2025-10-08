package com.farenet.descuentos.ui.solicitudes.comunes.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.solicitudes.model.SolicitudUI;
import com.google.android.material.chip.Chip;

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

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_solicitud_historial, parent, false); // ← usar tu layout real
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        SolicitudUI s = data.get(pos);

        // Bindeo 1:1 con los IDs de item_solicitud_creado.xml
        h.tvCodigo.setText(nz(s.codigo));
        h.tvFecha.setText(nz(s.fecha));
        h.chEstado.setText(nz(s.estado));

        h.tvTipo.setText(nz(s.tipo));
        h.tvPlaca.setText(nz(s.placa));
        h.tvPlanta.setText(nz(s.planta));
        h.tvMotivo.setText(nz(s.motivo));

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(s);
        });
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCodigo, tvFecha, tvTipo, tvPlaca, tvPlanta, tvMotivo;
        Chip chEstado;

        VH(@NonNull View v) {
            super(v);
            tvCodigo = v.findViewById(R.id.tvCodigo);
            tvFecha  = v.findViewById(R.id.tvFecha);
            chEstado = v.findViewById(R.id.chEstado);
            tvTipo   = v.findViewById(R.id.tvTipo);
            tvPlaca  = v.findViewById(R.id.tvPlaca);
            tvPlanta = v.findViewById(R.id.tvPlanta);
            tvMotivo = v.findViewById(R.id.tvMotivo);
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
