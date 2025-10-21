package com.farenet.descuentos.ui.solicitudes.model;

import androidx.annotation.Nullable;

import com.farenet.descuentos.data.local.realm.dao.QueryRealm;
import com.farenet.descuentos.data.local.realm.entity.Conceptoinspeccion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo de item para la lista de solicitudes pendientes/aprobadas/rechazadas.
 * Ahora resuelve automáticamente el nombre del concepto según su key usando los maestros locales.
 */
public class SolicitudUI {
    // Visibles en la tarjeta
    public String codigo;
    public String tipo;     // "Descuento" | "Cortesía" (amigable UI)
    public String placa;
    public String planta;   // nombre de planta
    public String motivo;
    public String estado;   // "Pendiente" | "Aprobada" | "Rechazada" (amigable UI)
    public String fecha;    // fecha amigable para UI (ej. "2025-09-27 03:30")

    // Para acciones
    public String id;               // id/uuid de la solicitud (para aprobar/rechazar)

    // ===== Campos necesarios para generar el descuento con el repo antiguo =====
    public String plantaKey;        // key de planta (para Descuento.planta)
    public String conceptoKey;      // key de concepto (para Descuento.conceptoinspeccion)
    public String tipoPagoKey;      // key de tipo pago (para Descuento.tipoPagoDescuento)
    public Double monto;            // importe del descuento (usa Double para evitar NPE en parse)
    public String tipoDesc;         // "AUTORIZADO" | "CARTA" | "CAMPAÑA"
    public String campaniaNombre;   // opcional, solo si tipoDesc == "CAMPAÑA"

    // ===== Campo amigable para mostrar en UI =====
    /** Nombre/abreviatura del concepto a mostrar en la lista (abreviatura -> nombre -> key). */
    public String conceptoDisplay;

    // Cache estático de conceptos para evitar recargar en cada instancia
    private static Map<String, String> conceptosCache;

    // ===== Constructores de compatibilidad (no toques llamadas existentes) =====
    public SolicitudUI(String codigo, String tipo, String placa, String planta, String motivo, String estado) {
        this(codigo, tipo, placa, planta, motivo, estado, null, null);
    }

    public SolicitudUI(String codigo, String tipo, String placa, String planta, String motivo, String estado, String id) {
        this(codigo, tipo, placa, planta, motivo, estado, id, null);
    }

    public SolicitudUI(String codigo, String tipo, String placa, String planta, String motivo,
                       String estado, String id, String fecha) {
        this.codigo = nz(codigo);
        this.tipo   = nz(tipo);
        this.placa  = nz(placa);
        this.planta = nz(planta);
        this.motivo = nz(motivo);
        this.estado = nz(estado);
        this.id     = id;
        this.fecha  = fecha;
        this.conceptoDisplay = "Concepto";
    }

    // ===== Constructor completo =====
    public SolicitudUI(String codigo, String tipo, String placa, String planta, String motivo,
                       String estado, String id, String fecha,
                       @Nullable String plantaKey, @Nullable String conceptoKey, @Nullable String tipoPagoKey,
                       @Nullable Double monto, @Nullable String tipoDesc, @Nullable String campaniaNombre) {

        this(codigo, tipo, placa, planta, motivo, estado, id, fecha);
        this.plantaKey      = emptyToNull(plantaKey);
        this.conceptoKey    = emptyToNull(conceptoKey);
        this.tipoPagoKey    = emptyToNull(tipoPagoKey);
        this.monto          = (monto != null ? monto : 0d);
        this.tipoDesc       = defaultTipoDesc(tipoDesc);
        this.campaniaNombre = emptyToNull(campaniaNombre);

        // Resolver el nombre del concepto automáticamente
        this.conceptoDisplay = resolveConceptoDisplay(this.conceptoKey);
    }

    // ===== Métodos principales =====

    /** True si tenemos lo mínimo para generar descuento vía repo legacy. */
    public boolean isCompletaParaDescuento() {
        return notEmpty(plantaKey) && notEmpty(conceptoKey) && notEmpty(tipoPagoKey) && notEmpty(placa);
    }

    /**
     * Busca el nombre/abreviatura del concepto desde los maestros locales.
     * Si no se encuentra, devuelve el key como fallback.
     */
    private static String resolveConceptoDisplay(@Nullable String conceptoKey) {
        if (conceptoKey == null || conceptoKey.trim().isEmpty()) return "Concepto";

        // Inicializa cache si es necesario
        if (conceptosCache == null) {
            conceptosCache = new HashMap<>();
            try {
                List<Conceptoinspeccion> lista = QueryRealm.copyAllConceptos();
                if (lista != null) {
                    for (Conceptoinspeccion c : lista) {
                        if (c == null) continue;
                        String key = safe(c.getKey());
                        if (key.isEmpty()) continue;
                        String ab = safe(c.getAbreviatura());
                        String nm = safe(c.getKey());
                        String display = !ab.isEmpty() ? ab : (!nm.isEmpty() ? nm : key);
                        conceptosCache.put(key, display);
                    }
                }
            } catch (Exception ignored) { }
        }

        String display = conceptosCache.get(conceptoKey);
        if (display == null || display.trim().isEmpty()) return conceptoKey;
        return display;
    }

    // ===== Helpers internos =====
    private static String nz(String s) { return s == null ? "" : s; }

    private static boolean notEmpty(@Nullable String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static @Nullable String emptyToNull(@Nullable String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private static String defaultTipoDesc(@Nullable String in) {
        if (in == null) return "AUTORIZADO";
        String v = in.trim();
        if (v.isEmpty()) return "AUTORIZADO";
        return v.toUpperCase();
    }

    private static String safe(@Nullable String s) {
        return (s == null) ? "" : s.trim();
    }

    @Override
    public String toString() {
        return "SolicitudUI{" +
                "codigo='" + codigo + '\'' +
                ", tipo='" + tipo + '\'' +
                ", placa='" + placa + '\'' +
                ", planta='" + planta + '\'' +
                ", motivo='" + motivo + '\'' +
                ", estado='" + estado + '\'' +
                ", fecha='" + fecha + '\'' +
                ", id='" + id + '\'' +
                ", plantaKey='" + plantaKey + '\'' +
                ", conceptoKey='" + conceptoKey + '\'' +
                ", tipoPagoKey='" + tipoPagoKey + '\'' +
                ", monto=" + monto +
                ", tipoDesc='" + tipoDesc + '\'' +
                ", campaniaNombre='" + campaniaNombre + '\'' +
                ", conceptoDisplay='" + conceptoDisplay + '\'' +
                '}';
    }
}
