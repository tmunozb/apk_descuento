package com.farenet.descuentos.API.Actual.DTO.solicitudes;

import com.google.gson.annotations.SerializedName;

public class SolicitudDto {
    @SerializedName("id")                 public String id;
    @SerializedName("codigo")             public String codigo;

    @SerializedName("tipo")               public String tipo;
    @SerializedName("estado")             public String estado;

    @SerializedName("placa")              public String placa;
    @SerializedName("planta_nombre")      public String plantaNombre;
    @SerializedName("motivo")             public String motivo;

    @SerializedName("creado_en")          public String creadoEn;     // ISO8601
    @SerializedName("aprobada_en")        public String aprobadaEn;   // ISO8601 (opcional)

    // ===== Campos de monto (opcionales, según cómo responda tu API) =====
    @SerializedName("monto")              public Double monto;
    @SerializedName("monto_aprobado")     public Double montoAprobado;
    @SerializedName("monto_solicitado")   public Double montoSolicitado;

    @SerializedName("concepto_key")          public String conceptoKey;
    @SerializedName("concepto_abreviatura") public String conceptoAbreviatura;

    @SerializedName("concepto_nombre")      public String conceptoNombre;
    @SerializedName("importe")            public Double importe;
    @SerializedName("total")              public Double total;         // por si acaso

    // En SolicitudDto
    @SerializedName("aprobado_por_nombre")     public String aprobadoPorNombre;

}
