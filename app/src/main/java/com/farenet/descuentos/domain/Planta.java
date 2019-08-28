package com.farenet.descuentos.domain;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Author by Alexis Pumayalla on 28/08/19.
 * Email apumayallag@gmail.com
 * Phone 961778965
 */
public class Planta extends RealmObject {

    @PrimaryKey
    private String key;

    private String nombre;

    public Planta() {
    }

    public Planta(String key, String nombre) {
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
