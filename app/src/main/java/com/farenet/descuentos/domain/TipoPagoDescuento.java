package com.farenet.descuentos.domain;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class TipoPagoDescuento extends RealmObject {

    @PrimaryKey
    private String key;
    private String nombre;

    public TipoPagoDescuento() {
    }

    public TipoPagoDescuento(String key, String nombre) {
        this.key = key;
        this.nombre = nombre;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return getNombre();
    }
}
