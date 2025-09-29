package com.farenet.descuentos.models.req;

public class RechazarSolicitudReq {
    public String rechazo_motivo;

    public RechazarSolicitudReq(String motivo) {
        this.rechazo_motivo = motivo;
    }
}
