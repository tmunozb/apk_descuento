// models/req/AdjuntoCrearReq.java
package com.farenet.descuentos.API.Actual.DTO.adjuntos;

import java.util.List;

public class AdjuntoCrearReq {
    public String tipo;                 // "FOTO" | "DOCUMENTO" | "OTRO"
    public String nombre_original;
    public String mime_type;
    public Long   size_bytes;
    public Integer width_px;
    public Integer height_px;
    public Boolean exif_removido;

    public String b64_input;            // OBLIGATORIO (contenido)
    public String b64_thumb_input;      // opcional (miniatura)

    public Long   subido_por_id;
    public String subido_por_username;
    public String subido_por_nombre;

    public String device_model;
    public String device_os;
    public String ip_origen;
    public Double lat;
    public Double lon;
    public List<String> labels_json;    // ej. Arrays.asList("placa","vehiculo")
    public String capturado_en;         // ISO-8601 opcional
}
