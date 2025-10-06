package com.farenet.descuentos.API.Actual.DTO.maestros;

import com.google.gson.annotations.SerializedName;

public class PlantaDto {

    // Si el JSON viene como {"key":"SM","nombre":"SAN MIGUEL"}
    @SerializedName("key")
    public String key;

    @SerializedName("nombre")
    public String nombre;

    // (Opcional) Constructor vacío por si lo necesitas
    public PlantaDto() {}
}
