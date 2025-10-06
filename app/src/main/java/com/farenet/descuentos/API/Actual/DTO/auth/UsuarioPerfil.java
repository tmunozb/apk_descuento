package com.farenet.descuentos.API.Actual.DTO.auth;

import java.io.Serializable;

public class UsuarioPerfil implements Serializable {
    public String username;
    public String perfilId;
    public Boolean estado;           // true/false (activo)
    public String nroDocumento;      // DNI
    public String nombres;
    public String apellidos;

    // Ctor vacío (Gson/serialización)
    public UsuarioPerfil() {}

    // Ctor de conveniencia
    public UsuarioPerfil(String username, String perfilId, Boolean estado,
                         String nroDocumento, String nombres, String apellidos) {
        this.username = username;
        this.perfilId = perfilId;
        this.estado = estado;
        this.nroDocumento = nroDocumento;
        this.nombres = nombres;
        this.apellidos = apellidos;
    }

    public boolean isActivo() {
        return estado != null && estado;
    }

    public String getNombreCompleto() {
        String n = (nombres == null ? "" : nombres.trim());
        String a = (apellidos == null ? "" : apellidos.trim());
        String full = (n + " " + a).trim();
        return full.isEmpty() ? (username != null ? username : "") : full;
    }

    @Override
    public String toString() {
        return "UsuarioPerfil{" +
                "username='" + username + '\'' +
                ", perfilId='" + perfilId + '\'' +
                ", estado=" + estado +
                ", nroDocumento='" + nroDocumento + '\'' +
                ", nombres='" + nombres + '\'' +
                ", apellidos='" + apellidos + '\'' +
                '}';
    }
}
