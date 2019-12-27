package com.farenet.descuentos.domain;

/**
 * Author by Alexis Pumayalla on 28/08/19.
 * Email apumayallag@gmail.com
 * Phone 961778965
 */
public class Descuento {

    private String conceptoinspeccion;
    private String planta;
    private String tipoPagoDescuento;
    private String placa;
    private double monto;
    private String motivo;
    private String autoriza;

    public Descuento() {
    }

    public String getConceptoinspeccion() {
        return conceptoinspeccion;
    }

    public void setConceptoinspeccion(String conceptoinspeccion) {
        this.conceptoinspeccion = conceptoinspeccion;
    }

    public String getPlanta() {
        return planta;
    }

    public void setPlanta(String planta) {
        this.planta = planta;
    }

    public String getTipoPagoDescuento() {
        return tipoPagoDescuento;
    }

    public void setTipoPagoDescuento(String tipoPagoDescuento) {
        this.tipoPagoDescuento = tipoPagoDescuento;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getAutoriza() {
        return autoriza;
    }

    public void setAutoriza(String autoriza) {
        this.autoriza = autoriza;
    }
}
