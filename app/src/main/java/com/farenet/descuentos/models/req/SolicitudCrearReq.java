package com.farenet.descuentos.models.req;

public class SolicitudCrearReq {
    public String tipoSolicitud;   // "Descuento" | "Cortesía"
    public String planta;          // Nombre visible (opcional si backend no lo usa)
    public String plantaKey;       // <-- CLAVE REAL (de SessionManager.getAccesos())
    public String concepto;      // texto visible (abreviatura)
    public String conceptoKey;   // 👈 NUEVO: clave real del concepto
    public Double conceptoValor;
    public String tipoCampania;    // "N/A" si no aplica
    public String placa;
    public double monto;
    public String motivo;
    public String autoriza;

    // Si tu backend prefiere IDs numéricos, agrega aquí esos campos.
}
