package com.farenet.descuentos.API.Actual.DTO.bolsa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BolsaBulkReq implements Serializable {
    public String periodo;   // "YYYYMM"
    public String estado;    // "ACTIVO" | "CERRADO"
    public List<Item> items = new ArrayList<>();

    public static class Item implements Serializable {
        public String planta_key;
        public Double monto_tope;

        public Item() {}
        public Item(String plantaKey, Double monto) {
            this.planta_key = plantaKey;
            this.monto_tope = monto;
        }
    }
}
