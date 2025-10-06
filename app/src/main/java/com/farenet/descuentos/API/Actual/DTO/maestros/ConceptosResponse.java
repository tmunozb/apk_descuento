// com.farenet.descuentos.API.Actual.DTO.maestros.ConceptosResponse
package com.farenet.descuentos.API.Actual.DTO.maestros;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ConceptosResponse {
    @SerializedName("items") public List<ConceptoPlantaDto> items;
    @SerializedName("count") public int count;
}
