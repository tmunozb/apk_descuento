package com.farenet.descuentos.API.Actual.DTO.auth;

import com.google.gson.annotations.SerializedName;

public class UsuarioResumenDto {
    @SerializedName("username") public String username;
    @SerializedName("persona_nrodocumentoidentidad") public String dni;
    @SerializedName("estado") public Boolean estado;        // true/false o 1/0 según BD
    @SerializedName("nombres") public String nombres;
    @SerializedName("apellidos") public String apellidos;
    @SerializedName("perfil_id") public String perfilId;
}
