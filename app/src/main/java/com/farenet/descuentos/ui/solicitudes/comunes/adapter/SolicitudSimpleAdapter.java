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
                // Asegúrate que este sea tu layout real del ítem
                .inflate(R.layout.item_solicitud_historial, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        final SolicitudUI s = data.get(pos);

        // Campos básicos
        h.tvCodigo.setText(nz(s.codigo));
        h.tvFecha.setText(nz(s.fecha));
        h.chEstado.setText(nz(s.estado));
        tintEstado(h.chEstado, s.estado);

        h.tvTipo.setText(nz(s.tipo));
        h.tvPlaca.setText(nz(s.placa));
        h.tvPlanta.setText(nz(s.planta));

        // ===== Concepto (opcional) =====
        // Si tu layout tiene un TextView con id tvConcepto y tu SolicitudUI expone nombre/abreviatura
        // del concepto (p.ej. s.conceptoNombre o similar), lo mostramos. Si no, lo ocultamos.
        if (h.tvConcepto != null) {
            // Intenta usar un campo "conceptoNombre" si lo tienes en tu SolicitudUI; si no, cae a la key
            String conceptoDisplay = null;
            try {
                // Si añadiste un método/field en SolicitudUI para nombre bonito del concepto, úsalo aquí
                // ejemplo: conceptoDisplay = s.getConceptoDisplay();
                // si no tienes, usa la key sólo si aporta algo:
                if (!TextUtils.isEmpty(s.conceptoKey)) {
                    conceptoDisplay = s.conceptoKey;
                }
            } catch (Throwable ignore) {}
            setOrGone(h.tvConcepto, conceptoDisplay);
        }

        // ===== Monto y Aprobado (opcional) =====
        // 1) Preferimos el valor de s.monto si viene en el modelo (p.ej. en Pendientes)
        // 2) Si vienes del historial donde inyectaste "Monto: ..." y "Aprobó: ..." dentro de motivo,
        //    los extraemos y limpiamos el motivo para no duplicar.
        String motivo = nz(s.motivo);

        // Intento extraer de motivo si vino inyectado (Historial)
        Extracted extrasFromMotivo = extractMontoYAprobador(motivo);

        // Si el modelo ya trae monto, éste manda sobre el detectado por motivo
        Double montoFinal = (s.monto != null && s.monto > 0) ? s.monto : extrasFromMotivo.monto;
        String aprobadorFinal = extrasFromMotivo.aprobador; // SolicitudUI no trae aprobador; si lo agregas, úsalo

        // Limpia el texto del motivo para evitar ver " · Monto: ... · Aprobó: ..." repetido
        String motivoLimpio = cleanMotivo(motivo);

        // Mostrar motivo limpio
        h.tvMotivo.setText(nz(motivoLimpio));

        // Bind a vistas opcionales si existen
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

        // Click
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(s);
        });
    }

    @Override
    public int getItemCount() {
        return (data == null) ? 0 : data.size();
    }

    // ====== ViewHolder con vistas opcionales ======
    static class VH extends RecyclerView.ViewHolder {
        TextView tvCodigo, tvFecha, tvTipo, tvPlaca, tvPlanta, tvMotivo;
        // Opcionales si tu layout los define:
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

            // Estas pueden NO existir en tu item. Si el id no está, quedarán en null (y el adapter lo maneja).
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

        // Busca "Monto:" y "Aprobó:" con separadores tipo " · "
        // Ejemplos válidos:
        //   "Motivo X · Monto: S/ 123.45 · Aprobó: Juan Pérez"
        //   "Monto: S/ 50 · Aprobó: JP"
        String lower = motivo.toLowerCase(Locale.ROOT);

        // Monto
        int idxMonto = indexOfIgnoreCase(motivo, "Monto:");
        if (idxMonto >= 0) {
            int nextSep = motivo.indexOf("·", idxMonto + 6);
            String montoChunk = (nextSep > idxMonto) ? motivo.substring(idxMonto, nextSep) : motivo.substring(idxMonto);
            // intento parsear el número dentro
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
        // Elimina "Monto:", "S/", espacios y comas de millares
        String s = chunk.replace("Monto:", "")
                .replace("S/", "")
                .replace("s/", "")
                .replace("S/.", "")
                .replace("s/.", "")
                .replace(" ", "")
                .replace(",", "")
                .trim();
        if (s.isEmpty()) return null;
        // Ahora s debería ser el número
        try {
            // También corta si hay “·” colgando
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
        // borra " · Monto: S/ xxx.xx"
        out = out.replaceAll("\\s*·\\s*Monto:\\s*S/?\\.?\\s*[0-9.,]+", "");
        // borra " · Aprobó: Nombre"
        out = out.replaceAll("\\s*·\\s*Aprobó:\\s*[^·]+", "");
        return out.trim();
    }

    /** Colorea el chip según estado (simple; ajusta colores si quieres). */
    private static void tintEstado(Chip ch, String estado) {
        if (ch == null) return;
        String e = (estado == null ? "" : estado.trim().toLowerCase(Locale.ROOT));
        // Usa colores de Material; puedes afinar con tus recursos.
        if (e.startsWith("aproba")) {
            ch.setChipBackgroundColorResource(R.color.fn_accent_success);   // ejemplo
        } else if (e.startsWith("rechaza")) {
            ch.setChipBackgroundColorResource(R.color.gg_error);    // ejemplo
        } else if (e.startsWith("procesa") || e.startsWith("ingresa")) {
            ch.setChipBackgroundColorResource(R.color.fn_accent_success); // ejemplo
        } else if (e.startsWith("pend") || e.startsWith("envia")) {
            ch.setChipBackgroundColorResource(R.color.bottom_nav_active); // ejemplo
        } else {
            ch.setChipBackgroundColorResource(R.color.fn_accent_success);   // default
        }
    }

    // Contenedor para datos extraídos
    private static class Extracted {
        Double monto = null;
        String aprobador = null;
    }
}
