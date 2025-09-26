package com.farenet.descuentos.models.req;

import com.google.gson.annotations.SerializedName;

public class SolicitudCrearReq {

    // ===== Paso 1 =====
    @SerializedName("tipo_solicitud")
    public String tipoSolicitud;     // "Descuento" | "Cortesía"

    @SerializedName("planta")
    public String planta;            // Nombre visible de planta

    @SerializedName("planta_key")
    public String plantaKey;         // Clave real de planta

    // Concepto seleccionado (desde /conceptos_por_planta)
    @SerializedName("concepto_label")
    public String concepto;          // Etiqueta visible (p.e. "TAXI (S/ 75)")

    @SerializedName("concepto_key")
    public String conceptoKey;       // p.e. "3"

    @SerializedName("concepto_valor")
    public Double conceptoValor;     // p.e. 75.0

    // Tipo de pago (desde /obtener_tipo_pago_descuento)
    @SerializedName("tipo_pago_key")
    public String tipoPagoKey;       // "POR" | "MON" | "FLA"

    @SerializedName("tipo_pago")
    public String tipoPago;          // "Porcentaje" | "Monto" | "Flat"

    // Tipo de descuento (combo fijo)
    @SerializedName("tipo_descuento")
    public String tipoDescuento;     // "Autorizado" | "Carta" | "Campaña"

    // ===== Paso 2 =====
    @SerializedName("placa")
    public String placa;

    @SerializedName("monto")
    public Double monto;

    @SerializedName("motivo")
    public String motivo;

    @SerializedName("autoriza")
    public String autoriza;
}
