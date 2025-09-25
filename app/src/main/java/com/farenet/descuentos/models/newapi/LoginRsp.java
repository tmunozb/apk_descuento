// com/farenet/descuentos/models/newapi/LoginRsp.java
package com.farenet.descuentos.models.newapi;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class LoginRsp {
    @SerializedName("message")   public String message;
    @SerializedName("username")  public String username;
    @SerializedName("error")     public String error;

    @SerializedName("perfil_id") public String perfilId;
    @SerializedName("estado")    public Boolean estado;

    @SerializedName("dni")       public String nroDocumento;
    @SerializedName("nombres")   public String nombres;
    @SerializedName("apellidos") public String apellidos;

    // ✅ Reutiliza tu DTO existente
    @SerializedName("accesos")   public List<AccesoPlantaDto> accesos;

    public boolean isOk() {
        if (error != null && !error.isEmpty()) return false;
        if (estado != null) return estado;
        return message != null && message.toLowerCase().contains("exitos");
    }
}
