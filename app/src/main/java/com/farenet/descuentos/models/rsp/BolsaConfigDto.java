package com.farenet.descuentos.models.rsp;

public class BolsaConfigDto {
    public Long id;
    public String planta_key;
    public String periodo_yyyymm;
    public Double monto_tope;
    public Double monto_usado;
    public String estado;     // ACTIVO | CERRADO
    public Integer version;
    public Double saldo;      // viene calculado en el API list/get
    public String created_at; // opcional
    public String updated_at; // opcional
}
