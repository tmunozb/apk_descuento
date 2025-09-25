package com.farenet.descuentos.models.newapi;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class LoginRsp {
    @SerializedName("message") public String message;
    @SerializedName("username") public String username;
    @SerializedName("error")    public String error;

    @SerializedName("perfil_id") public String perfilId;
    @SerializedName("estado")    public Boolean estado;

    // El backend devuelve "dni"
    @SerializedName("dni") public String nroDocumento;

    @SerializedName("nombres")   public String nombres;
    @SerializedName("apellidos") public String apellidos;

    // Accesos del usuario para mapear a Plantas
    @SerializedName("accesos") public List<PlantaAcceso> accesos;

    public boolean isOk() {
        return (error == null || error.isEmpty());
    }

    // LoginRsp.java
    public static class PlantaAcceso implements java.io.Serializable {
        @SerializedName("key")    public String key;     // id de planta (string en tu API)
        @SerializedName("planta") public String planta;  // nombre
    }

}
