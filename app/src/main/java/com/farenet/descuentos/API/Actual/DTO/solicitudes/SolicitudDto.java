
package com.farenet.descuentos.API.Actual.DTO.solicitudes;

import com.google.gson.annotations.SerializedName;

public class SolicitudDto {
    @SerializedName("id")               public String id;
    @SerializedName("codigo")           public String codigo;

    @SerializedName("tipo")             public String tipo;
    @SerializedName("estado")           public String estado;

    @SerializedName("placa")            public String placa;
    @SerializedName("planta_nombre")    public String plantaNombre;
    @SerializedName("motivo")           public String motivo;

    @SerializedName("creado_en")        public String creadoEn;    // ISO8601
    @SerializedName("aprobada_en")      public String aprobadaEn;  // opcional
}
