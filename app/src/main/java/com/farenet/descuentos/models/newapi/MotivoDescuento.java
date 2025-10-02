package com.farenet.descuentos.models.newapi;

public class MotivoDescuento {
    @com.google.gson.annotations.SerializedName("id")
    private String id;

    @com.google.gson.annotations.SerializedName("nombre")
    private String nombre;

    @com.google.gson.annotations.SerializedName("activo")
    private boolean activo;

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public boolean isActivo() { return activo; }

    public void setId(String id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override public String toString() { return nombre; }
}
