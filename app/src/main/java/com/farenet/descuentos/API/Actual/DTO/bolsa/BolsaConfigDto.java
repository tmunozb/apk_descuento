package com.farenet.descuentos.API.Actual.DTO.bolsa;

import java.io.Serializable;

public class BolsaConfigDto implements Serializable {
    public Long id;
    public String planta_key;
    public String periodo_yyyymm;
    public Double monto_tope;
    public Double monto_usado;
    public String estado;
    public Integer version;
    public Double saldo;
    public String created_at;
    public String updated_at;
}
