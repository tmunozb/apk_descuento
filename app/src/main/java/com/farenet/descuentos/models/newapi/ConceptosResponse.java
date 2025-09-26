// com.farenet.descuentos.models.newapi.ConceptosResponse
package com.farenet.descuentos.models.newapi;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ConceptosResponse {
    @SerializedName("items") public List<ConceptoPlantaDto> items;
    @SerializedName("count") public int count;
}
