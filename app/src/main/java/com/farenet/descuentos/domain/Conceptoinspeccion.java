package com.farenet.descuentos.domain;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Conceptoinspeccion extends RealmObject {

    @PrimaryKey
    private String key;

    private String abreviatura;

    public Conceptoinspeccion() {
    }

    public Conceptoinspeccion(String key, String abreviatura) {
        this.key = key;
        this.abreviatura = abreviatura;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getAbreviatura() {
        return abreviatura;
    }

    public void setAbreviatura(String abreviatura) {
        this.abreviatura = abreviatura;
    }

    @Override
    public String toString() {
        return getAbreviatura();
    }
}
