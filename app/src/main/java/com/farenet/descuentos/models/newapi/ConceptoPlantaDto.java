// com.farenet.descuentos.models.newapi.ConceptoPlantaDto
package com.farenet.descuentos.models.newapi;

import com.google.gson.annotations.SerializedName;

public class ConceptoPlantaDto {
    @SerializedName("planta_key")    public String plantaKey;
    @SerializedName("planta_nombre") public String plantaNombre;
    @SerializedName("concepto_key")  public String conceptoKey;
    @SerializedName("abreviatura")   public String abreviatura;
    @SerializedName("valor")         public Double valor; // puede venir entero -> usa Double
}
