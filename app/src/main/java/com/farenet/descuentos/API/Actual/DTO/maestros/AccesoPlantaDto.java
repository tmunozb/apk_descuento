package com.farenet.descuentos.API.Actual.DTO.maestros;

import com.google.gson.annotations.SerializedName;

public class AccesoPlantaDto {

    // Tu endpoint /obtener_accesos_usuario retorna:
    // [{"key": "...", "planta": "...", "usuario": "..."}]
    @SerializedName("key")
    public String key;       // key de planta

    @SerializedName("planta")
    public String planta;    // nombre de la planta

    @SerializedName("usuario")
    public String usuario;   // username asociado

    public AccesoPlantaDto() {}
}
