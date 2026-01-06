package com.farenet.descuentos.ui.solicitudes.comunes.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.solicitudes.model.SolicitudUI;
import com.google.android.material.chip.Chip;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SolicitudSimpleAdapter extends RecyclerView.Adapter<SolicitudSimpleAdapter.VH> {

    public interface OnItemClick {
        void onClick(SolicitudUI s);
    }

    private final List<SolicitudUI> data;
    private final OnItemClick listener;
    private final NumberFormat moneyPE;

    public SolicitudSimpleAdapter(List<SolicitudUI> data, OnItemClick listener) {
        this.data = data;
        this.listener = listener;
        moneyPE = NumberFormat.getNumberInstance(new Locale("es", "PE"));
        moneyPE.setMinimumFractionDigits(2);
        moneyPE.setMaximumFractionDigits(2);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_solicitud_historial, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        final SolicitudUI s = data.get(pos);

        // ===== Campos básicos =====
        h.tvCodigo.setText(nz(s.codigo));
        h.tvFecha.setText(nz(s.fecha));

        h.chEstado.setText(nz(s.estado));
        tintEstado(h.chEstado, s.estado);

        h.tvTipo.setText(nz(s.tipo));
        h.tvPlaca.setText(nz(s.placa));
        h.tvPlanta.setText(nz(s.planta));

        // ===== Concepto (opcional) =====
        // Muestra SOLO si hay valor real. Si viene "LIV..." sin prefijo, agrega "Concepto: ".
        if (h.tvConcepto != null) {
            String c = (s.conceptoDisplay == null) ? "" : s.conceptoDisplay.trim();

            // Evita mostrar placeholders tipo "Concepto"
            if (c.equalsIgnoreCase("concepto")) c = "";

            if (!c.isEmpty() && !c.toLowerCase(Locale.ROOT).startsWith("concepto")) {
                c = "Concepto: " + c;
            }
            setOrGone(h.tvConcepto, c.isEmpty() ? null : c);
        }

        // ===== Monto y Aprobado (opcional) =====
        // 1) Preferimos s.monto si viene en el modelo.
        // 2) Si el historial inyectó "Monto: ..." y "Aprobó: ..." dentro de motivo, los extraemos.
        String motivo = nz(s.motivo);

        Extracted extrasFromMotivo = extractMontoYAprobador(motivo);

        Double montoFinal = (s.monto != null && s.monto > 0) ? s.monto : extrasFromMotivo.monto;
        String aprobadorFinal = extrasFromMotivo.aprobador;

        String motivoLimpio = cleanMotivo(motivo);
        h.tvMotivo.setText(nz(motivoLimpio));

        if (h.tvMonto != null) {
            if (montoFinal != null && montoFinal > 0) {
                h.tvMonto.setVisibility(View.VISIBLE);
                h.tvMonto.setText("Monto: S/ " + moneyPE.format(montoFinal));
            } else {
                h.tvMonto.setVisibility(View.GONE);
                h.tvMonto.setText(null);
            }
        }

        if (h.tvAprobado != null) {
            setOrGone(h.tvAprobado, (TextUtils.isEmpty(aprobadorFinal) ? null : "Aprobó: " + aprobadorFinal));
        }

        // ===== Click =====
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(s);
        });
    }

    @Override
    public int getItemCount() {
        return (data == null) ? 0 : data.size();
    }

    // ====== ViewHolder ======
    static class VH extends RecyclerView.ViewHolder {
        TextView tvCodigo, tvFecha, tvTipo, tvPlaca, tvPlanta, tvMotivo;

        // Opcionales (si existen en el layout)
        TextView tvConcepto;
        TextView tvMonto;
        TextView tvAprobado;

        Chip chEstado;

        VH(@NonNull View v) {
            super(v);
            tvCodigo  = v.findViewById(R.id.tvCodigo);
            tvFecha   = v.findViewById(R.id.tvFecha);
            chEstado  = v.findViewById(R.id.chEstado);
            tvTipo    = v.findViewById(R.id.tvTipo);
            tvPlaca   = v.findViewById(R.id.tvPlaca);
            tvPlanta  = v.findViewById(R.id.tvPlanta);
            tvMotivo  = v.findViewById(R.id.tvMotivo);

            tvConcepto = safeFind(v, R.id.tvConcepto);
            tvMonto    = safeFind(v, R.id.tvMonto);
            tvAprobado = safeFind(v, R.id.tvAprobado);
        }

        @SuppressWarnings("unchecked")
        private static <T extends View> T safeFind(View root, int id) {
            try { return root.findViewById(id); } catch (Throwable ignore) { return null; }
        }
    }

    // ====== Helpers ======
    private static String nz(String s) { return (s == null) ? "" : s; }

    private static void setOrGone(TextView tv, String text) {
        if (tv == null) return;
        if (TextUtils.isEmpty(text)) {
            tv.setVisibility(View.GONE);
            tv.setText(null);
        } else {
            tv.setVisibility(View.VISIBLE);
            tv.setText(text);
        }
    }

    /** Extrae "Monto: S/ 123.45" y "Aprobó: Nombre" si fueron inyectados en el motivo */
    private static Extracted extractMontoYAprobador(String motivo) {
        Extracted ex = new Extracted();
        if (TextUtils.isEmpty(motivo)) return ex;

        // Monto
        int idxMonto = indexOfIgnoreCase(motivo, "Monto:");
        if (idxMonto >= 0) {
            int nextSep = motivo.indexOf("·", idxMonto + 6);
            String montoChunk = (nextSep > idxMonto) ? motivo.substring(idxMonto, nextSep) : motivo.substring(idxMonto);
            Double parsed = parseMontoFromChunk(montoChunk);
            if (parsed != null) ex.monto = parsed;
        }

        // Aprobó
        int idxApr = indexOfIgnoreCase(motivo, "Aprobó:");
        if (idxApr >= 0) {
            int nextSep = motivo.indexOf("·", idxApr + 7);
            String aprChunk = (nextSep > idxApr) ? motivo.substring(idxApr, nextSep) : motivo.substring(idxApr);
            String name = aprChunk.replaceFirst("(?i)Aprobó:\\s*", "").trim();
            if (!name.isEmpty()) ex.aprobador = name;
        }

        return ex;
    }

    private static int indexOfIgnoreCase(String text, String needle) {
        if (text == null || needle == null) return -1;
        final String t = text.toLowerCase(Locale.ROOT);
        final String n = needle.toLowerCase(Locale.ROOT);
        return t.indexOf(n);
    }

    private static Double parseMontoFromChunk(String chunk) {
        if (TextUtils.isEmpty(chunk)) return null;

        String s = chunk.replace("Monto:", "")
                .replace("S/", "")
                .replace("s/", "")
                .replace("S/.", "")
                .replace("s/.", "")
                .replace(" ", "")
                .replace(",", "")
                .trim();

        if (s.isEmpty()) return null;

        try {
            int cut = s.indexOf("·");
            if (cut >= 0) s = s.substring(0, cut).trim();
            return Double.parseDouble(s);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    /** Limpia del motivo los trozos " · Monto: ..." y " · Aprobó: ..." si existen. */
    private static String cleanMotivo(String motivo) {
        if (TextUtils.isEmpty(motivo)) return "";
        String out = motivo;
        out = out.replaceAll("\\s*·\\s*Monto:\\s*S/?\\.?\\s*[0-9.,]+", "");
        out = out.replaceAll("\\s*·\\s*Aprobó:\\s*[^·]+", "");
        return out.trim();
    }

    /** Colorea el chip según estado (simple; ajusta colores si quieres). */
    private static void tintEstado(Chip ch, String estado) {
        if (ch == null) return;
        String e = (estado == null ? "" : estado.trim().toLowerCase(Locale.ROOT));

        if (e.startsWith("aproba")) {
            ch.setChipBackgroundColorResource(R.color.fn_accent_success);
        } else if (e.startsWith("rechaza")) {
            ch.setChipBackgroundColorResource(R.color.gg_error);
        } else if (e.startsWith("procesa") || e.startsWith("ingresa")) {
            ch.setChipBackgroundColorResource(R.color.fn_accent_success);
        } else if (e.startsWith("pend") || e.startsWith("envia")) {
            ch.setChipBackgroundColorResource(R.color.bottom_nav_active);
        } else {
            ch.setChipBackgroundColorResource(R.color.fn_accent_success);
        }
    }

    // Contenedor para datos extraídos
    private static class Extracted {
        Double monto = null;
        String aprobador = null;
    }
}
