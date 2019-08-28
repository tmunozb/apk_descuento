package com.farenet.descuentos.repository;

import com.farenet.descuentos.domain.Conceptoinspeccion;
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.domain.TipoPagoDescuento;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

/**
 * Author by Alexis Pumayalla on 28/08/19.
 * Email apumayallag@gmail.com
 * Phone 961778965
 */
public interface MaestroRepository {

    @GET("/maestro/plantas")
    Call<List<Planta>> getPlantas(@Header("Token") String token);

    @GET("/maestro/conceptoinspecciones")
    Call<List<Conceptoinspeccion>> getConceptoinspeccion(@Header("Token") String token);

    @GET("/maestro/tipopagodescuentos")
    Call<List<TipoPagoDescuento>> getTipoPagoDescuento(@Header("Token") String token);
}
