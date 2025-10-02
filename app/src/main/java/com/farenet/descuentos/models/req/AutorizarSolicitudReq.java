package com.farenet.descuentos.models.req;

public class AutorizarSolicitudReq {
    public String autorizado_por_username;
    public Integer autorizado_por_id;      // opcional
    public String autorizado_por_nombre;
    public String modo;                    // "AUTORIZADO" | "BOLSA"

    public AutorizarSolicitudReq() {}

    public AutorizarSolicitudReq(String username, Integer id, String nombre, String modo) {
        this.autorizado_por_username = username;
        this.autorizado_por_id = id;
        this.autorizado_por_nombre = nombre;
        this.modo = modo;
    }
}
