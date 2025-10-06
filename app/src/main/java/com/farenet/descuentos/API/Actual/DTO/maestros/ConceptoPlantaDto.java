// com.farenet.descuentos.API.Actual.DTO.maestros.ConceptoPlantaDto
package com.farenet.descuentos.API.Actual.DTO.maestros;

import com.google.gson.annotations.SerializedName;

public class ConceptoPlantaDto {
    @SerializedName("planta_key")    public String plantaKey;
    @SerializedName("planta_nombre") public String plantaNombre;
    @SerializedName("concepto_key")  public String conceptoKey;
    @SerializedName("abreviatura")   public String abreviatura;
    @SerializedName("valor")         public Double valor; // puede venir entero -> usa Double
}
