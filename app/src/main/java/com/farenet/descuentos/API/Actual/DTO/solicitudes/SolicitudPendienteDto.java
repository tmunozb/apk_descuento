package com.farenet.descuentos.API.Actual.DTO.solicitudes;

public class SolicitudPendienteDto {
    public String id;
    public String codigo;
    public String tipo;     // "DESCUENTO" o "CORTESIA"
    public String estado;   // ENVIADA/OBSERVADA/...
    public String planta_key, planta_nombre;
    public String placa, motivo, creado_en, solicitado_por_username;

    public String concepto_key, concepto_abreviatura;
    public String tipo_pago_key, tipo_pago_nombre;
    public Double monto;           // viene de valor
    public String tipo_desc;       // AUTORIZADO | CARTA | CAMPAÑA
    public String campania_nombre; // opcional
}
