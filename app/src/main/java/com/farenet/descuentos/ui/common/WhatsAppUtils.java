package com.farenet.descuentos.ui.common;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public final class WhatsAppUtils {

    private WhatsAppUtils() {}

    /** Construye el label del monto según el tipo de pago. */
    public static String buildMontoLabel(String tipoPagoNameOrKey) {
        String src = safe(tipoPagoNameOrKey).toLowerCase();
        if (src.contains("flat")) return "💰 Monto a pagar:";
        if (src.contains("porcentaje") || src.contains("%")) return "💰 % de descuento:";
        if (src.contains("monto")) return "💰 Monto de descuento:";
        return "💰 Monto:";
    }

    /** Resuelve un nombre legible de concepto priorizando abreviatura, luego nombre y por último key. */
    public static String resolveConceptoDisplay(String abreviatura, String nombre, String key) {
        if (!empty(abreviatura)) return abreviatura.trim();
        if (!empty(nombre))      return nombre.trim();
        if (!empty(key))         return key.trim();
        return "Concepto";
    }

    /** Arma el texto “Descuento registrado” con los campos disponibles (SIN fecha ni código). */
    public static String buildMensajeRegistrado(
            String placa,
            String plantaNombre,
            String conceptoDisplay,      // Debe venir ya resuelto (abreviatura->nombre->key)
            String motivo,
            String tipoPagoNameOrKey,    // usado para el label
            Double monto,
            String tipoDescuento,        // AUTORIZADO / CARTA / CAMPAÑA
            String campaniaNombre,       // opcional (solo campaña)
            String autorizadoPor         // opcional
    ) {
        String montoLabel = buildMontoLabel(tipoPagoNameOrKey);
        DecimalFormat df = new DecimalFormat("#,##0.00");

        List<String> lines = new ArrayList<>();
        lines.add("✅ Descuento Registrado");
        lines.add("");
        if (!empty(plantaNombre))   lines.add("📍 Planta: " + plantaNombre);
        if (!empty(placa))          lines.add("🚗 Placa: " + placa);
        if (!empty(conceptoDisplay)) lines.add("🧾 Concepto: " + conceptoDisplay);

        String tipo = safe(tipoDescuento);
        if ("CAMPANA".equalsIgnoreCase(tipo)) tipo = "CAMPAÑA";
        if (!empty(tipo)) {
            String extra = (!empty(campaniaNombre) ? " (" + campaniaNombre + ")" : "");
            lines.add("🎯 Tipo: " + tipo + extra);
        }

        if (!empty(motivo))        lines.add("✍️ Motivo: " + motivo);
        if (!empty(autorizadoPor)) lines.add("👤 Autorizado por: " + autorizadoPor);
        if (monto != null)         lines.add(montoLabel + " " + df.format(monto));

        return join(lines, "\n");
    }

    /** Arma el texto “Descuento solicitado” con los mismos campos del “registrado”, solo cambia la cabecera. */
    public static String buildMensajeSolicitado(
            String placa,
            String plantaNombre,
            String conceptoDisplay,
            String motivo,
            String tipoPagoNameOrKey,
            Double monto,
            String tipoDescuento,
            String campaniaNombre,
            String solicitadoPor
    ) {
        String montoLabel = buildMontoLabel(tipoPagoNameOrKey);
        DecimalFormat df = new DecimalFormat("#,##0.00");

        List<String> lines = new ArrayList<>();
        lines.add("🟡 Descuento solicitado 🟡");
        lines.add("");
        if (!empty(plantaNombre))   lines.add("📍 Planta: " + plantaNombre);
        if (!empty(placa))          lines.add("🚗 Placa: " + placa);
        if (!empty(conceptoDisplay)) lines.add("📋 Concepto: " + conceptoDisplay);

        String tipo = safe(tipoDescuento);
        if ("CAMPANA".equalsIgnoreCase(tipo)) tipo = "CAMPAÑA";
        if (!empty(tipo)) {
            String extra = (!empty(campaniaNombre) ? " (" + campaniaNombre + ")" : "");
            lines.add("📄 Tipo: " + tipo + extra);
        }

        if (!empty(motivo))         lines.add("💬 Motivo: " + motivo);
        if (monto != null)          lines.add(montoLabel + " " + df.format(monto));
        if (!empty(solicitadoPor))  lines.add("👤 Solicitado por: " + solicitadoPor);

        return join(lines, "\n");
    }

    /** Envía el mensaje por WhatsApp (estándar o business). Si no hay app, abre wa.me */
    public static void sendToWhatsApp(Context ctx, String message) {
        if (ctx == null || TextUtils.isEmpty(message)) return;

        Intent base = new Intent(Intent.ACTION_SEND);
        base.setType("text/plain");
        base.putExtra(Intent.EXTRA_TEXT, message);

        if (tryStartWithPackage(ctx, base, "com.whatsapp")) return;
        if (tryStartWithPackage(ctx, base, "com.whatsapp.w4b")) return;

        // fallback web
        String url = "https://wa.me/?text=" + Uri.encode(message);
        try {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(ctx, "No se encontró WhatsApp ni navegador", Toast.LENGTH_SHORT).show();
        }
    }

    // ===== Conveniencias =====

    /** Overload recomendado: pásame abreviatura, nombre y key del concepto, y yo resuelvo el display. */
    public static void sendRegisteredDiscountMessage(
            Context ctx,
            String placa,
            String plantaNombre,
            String conceptoAbreviatura,  // getAbreviatura()
            String conceptoNombre,       // getNombre()
            String conceptoKey,          // getKey()
            String motivo,
            String tipoPagoNameOrKey,
            Double monto,
            String tipoDescuento,
            String campaniaNombre,
            String autorizadoPor
    ) {
        String conceptoDisplay = resolveConceptoDisplay(conceptoAbreviatura, conceptoNombre, conceptoKey);
        String msg = buildMensajeRegistrado(
                placa,
                plantaNombre,
                conceptoDisplay,
                motivo,
                tipoPagoNameOrKey,
                monto,
                tipoDescuento,
                campaniaNombre,
                autorizadoPor
        );
        sendToWhatsApp(ctx, msg);
    }

    /** Overload corto: si ya resolviste el display del concepto fuera (ideal: abreviatura). */
    public static void sendRegisteredDiscountMessage(
            Context ctx,
            String placa,
            String plantaNombre,
            String conceptoDisplay,      // ya resuelto (ideal: getAbreviatura())
            String motivo,
            String tipoPagoNameOrKey,
            Double monto,
            String tipoDescuento,
            String campaniaNombre,
            String autorizadoPor
    ) {
        String msg = buildMensajeRegistrado(
                placa,
                plantaNombre,
                conceptoDisplay,
                motivo,
                tipoPagoNameOrKey,
                monto,
                tipoDescuento,
                campaniaNombre,
                autorizadoPor
        );
        sendToWhatsApp(ctx, msg);
    }

    /** SOLO cabecera (por compatibilidad con llamadas antiguas). */
    public static void sendRequestedDiscountMessage(Context ctx) {
        sendToWhatsApp(ctx, "🟡 DESCUENTO SOLICITADO 🟡");
    }

    /** Versión completa (cabecera “Solicitado” + todos los datos). */
    public static void sendRequestedDiscountMessage(
            Context ctx,
            String placa,
            String plantaNombre,
            String conceptoDisplay,
            String motivo,
            String tipoPagoNameOrKey,
            Double monto,
            String tipoDescuento,
            String campaniaNombre,
            String solicitadoPor
    ) {
        String msg = buildMensajeSolicitado(
                placa,
                plantaNombre,
                conceptoDisplay,
                motivo,
                tipoPagoNameOrKey,
                monto,
                tipoDescuento,
                campaniaNombre,
                solicitadoPor
        );
        sendToWhatsApp(ctx, msg);
    }

    // ===== helpers =====
    private static boolean tryStartWithPackage(Context ctx, Intent base, String pkg) {
        try {
            Intent i = new Intent(base);
            i.setPackage(pkg);
            if (i.resolveActivity(ctx.getPackageManager()) != null) {
                ctx.startActivity(i);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static boolean empty(String s) { return s == null || s.trim().isEmpty(); }
    private static String safe(String s) { return s == null ? "" : s.trim(); }
    private static String join(List<String> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) sb.append(sep);
        }
        return sb.toString();
    }
}
