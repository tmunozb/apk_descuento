package com.farenet.descuentos.API.Actual.DTO.solicitudes;

public class RechazarSolicitudReq {
    public String rechazo_motivo;

    public RechazarSolicitudReq(String motivo) {
        this.rechazo_motivo = motivo;
    }
}
