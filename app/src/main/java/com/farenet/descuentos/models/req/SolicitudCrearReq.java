// models/req/SolicitudCrearReq.java
package com.farenet.descuentos.models.req;

public class SolicitudCrearReq {
    public String tipo;                 // "DESCUENTO" | "CORTESIA"
    public String planta_key;
    public String planta_nombre;
    public String placa;
    public String motivo;
    public String sustento_texto;

    // Solo descuento
    public String concepto_key;
    public String concepto_abreviatura;
    public String tipo_pago_key;
    public String tipo_pago_nombre;
    public String tipo_desc;            // "AUTORIZADO" | "CARTA" | "CAMPAÑA"
    public String campania_nombre;
    public Double monto;
    public String autorizado_nombre;

    // Trazabilidad
    public String solicitado_por_username;
    public Long   solicitado_por_id;
    public String solicitado_por_nombre;
    public String solicitado_por_perfil;
}
