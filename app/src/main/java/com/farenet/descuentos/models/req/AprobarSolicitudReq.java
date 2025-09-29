package com.farenet.descuentos.models.req;

public class AprobarSolicitudReq {
    public String aprobado_por_username;
    public Integer aprobado_por_id;     // puede ser null
    public String aprobado_por_nombre;  // opcional

    public AprobarSolicitudReq(String user, Integer id, String nombre) {
        this.aprobado_por_username = user;
        this.aprobado_por_id = id;
        this.aprobado_por_nombre = nombre;
    }
}
