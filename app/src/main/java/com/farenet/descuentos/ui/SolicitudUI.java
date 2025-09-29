package com.farenet.descuentos.models.ui;

public class SolicitudUI {
    public String codigo;
    public String tipo;
    public String placa;
    public String planta;
    public String motivo;
    public String estado;
    public String id;     // uuid de la solicitud (para aprobar/rechazar)
    public String fecha;  // <-- NUEVO: fecha amigable para UI

    public SolicitudUI(String codigo, String tipo, String placa, String planta, String motivo, String estado) {
        this(codigo, tipo, placa, planta, motivo, estado, null, null);
    }

    public SolicitudUI(String codigo, String tipo, String placa, String planta, String motivo, String estado, String id) {
        this(codigo, tipo, placa, planta, motivo, estado, id, null);
    }

    public SolicitudUI(String codigo, String tipo, String placa, String planta, String motivo,
                       String estado, String id, String fecha) {
        this.codigo = codigo;
        this.tipo   = tipo;
        this.placa  = placa;
        this.planta = planta;
        this.motivo = motivo;
        this.estado = estado;
        this.id     = id;
        this.fecha  = fecha;
    }
}
