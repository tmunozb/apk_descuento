package com.farenet.descuentos.models.ui;

import androidx.annotation.Nullable;

/** Modelo de item para la lista de solicitudes pendientes/aprobadas/rechazadas. */
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
        this.id     = id;      // puede ser null hasta aprobar/rechazar
        this.fecha  = fecha;   // opcional
    }

    // ===== Nuevo ctor completo (cuando mapeas desde el DTO del backend) =====
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
    }

    /** True si tenemos lo mínimo para generar descuento vía repo legacy. */
    public boolean isCompletaParaDescuento() {
        return notEmpty(plantaKey) && notEmpty(conceptoKey) && notEmpty(tipoPagoKey) && notEmpty(placa);
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
        // Normalizamos a mayúsculas para comparaciones en UI/lógica
        return v.toUpperCase();
    }

    @Override
    public String toString() {
        return "SolicitudUI{" +
                "codigo='" + codigo + '\'' +
                ", tipo='" + tipo + '\'' +
                ", placa='" + placa + '\'' +
                ", planta='" + planta + '\'' +
                ", estado='" + estado + '\'' +
                ", id='" + id + '\'' +
                ", plantaKey='" + plantaKey + '\'' +
                ", conceptoKey='" + conceptoKey + '\'' +
                ", tipoPagoKey='" + tipoPagoKey + '\'' +
                ", monto=" + monto +
                ", tipoDesc='" + tipoDesc + '\'' +
                ", campaniaNombre='" + campaniaNombre + '\'' +
                '}';
    }
}
