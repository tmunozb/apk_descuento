// models/newapi/TipoPagoDto.java
package com.farenet.descuentos.API.Actual.DTO.maestros;

import com.google.gson.annotations.SerializedName;

public class TipoPagoDto {
    @SerializedName("key")    public String key;     // "POR" | "MON" | "FLA"
    @SerializedName("nombre") public String nombre;  // "Porcentaje" | "Monto" | "Flat"
}
