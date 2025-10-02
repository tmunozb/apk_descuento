package com.farenet.descuentos.models.rsp;

public class BolsaAuditoriaDto {
    public Long id;
    public Long quota_id;
    public Long descuento_id;
    public String tipo_mov;       // RESERVA | CONSUMO | ROLLBACK
    public Double monto;
    public String actor_username;
    public String motivo;
    public String created_at;
}
