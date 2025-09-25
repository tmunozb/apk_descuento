package com.farenet.descuentos.models.ui;

public class SolicitudUI {
    public String codigo;
    public String tipo;     // Descuento / Cortesía
    public String placa;
    public String planta;
    public String motivo;
    public String estado;   // Pendiente / Aprobada / Rechazada
    public String fecha;    // ej. 2025-09-25

    // Constructor existente (compatibilidad)
    public SolicitudUI(String codigo, String tipo, String placa, String planta, String motivo, String estado) {
        this(codigo, tipo, placa, planta, motivo, estado, "");
    }

    // Nuevo constructor con fecha
    public SolicitudUI(String codigo, String tipo, String placa, String planta, String motivo, String estado, String fecha) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.placa = placa;
        this.planta = planta;
        this.motivo = motivo;
        this.estado = estado;
        this.fecha = fecha;
    }
}
