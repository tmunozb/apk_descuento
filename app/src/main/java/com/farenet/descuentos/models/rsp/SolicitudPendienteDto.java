package com.farenet.descuentos.models.rsp;

public class SolicitudPendienteDto {
    public String id;
    public String codigo;
    public String tipo;              // DESCUENTO | CORTESIA
    public String estado;            // ENVIADA | OBSERVADA | APROBADA | RECHAZADA
    public String planta_key;
    public String planta_nombre;
    public String placa;
    public String motivo;
    public String creado_en;
    public String solicitado_por_username;
}
