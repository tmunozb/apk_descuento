package com.farenet.descuentos.models.rsp;

public class AccionSolicitudRsp {
    public String id;
    public String codigo;
    public String estado;      // APROBADA o RECHAZADA
    public String aprobada_en; // puede venir null si rechazada
    public String rechazada_en;// puede venir null si aprobada
}
