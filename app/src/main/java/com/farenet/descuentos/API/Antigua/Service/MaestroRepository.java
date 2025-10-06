package com.farenet.descuentos.API.Antigua.Service;

import com.farenet.descuentos.data.local.realm.entity.Autorizadores;
import com.farenet.descuentos.data.local.realm.entity.Conceptoinspeccion;
import com.farenet.descuentos.data.local.realm.entity.MotivoCortesia;
import com.farenet.descuentos.data.local.realm.entity.Planta;
import com.farenet.descuentos.data.local.realm.entity.TipoPagoDescuento;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface MaestroRepository {

    @GET("/maestro/plantas")
    Call<List<Planta>> getPlantas(@Header("Token") String token);

    @GET("/maestro/conceptoinspecciones")
    Call<List<Conceptoinspeccion>> getConceptoinspeccion(@Header("Token") String token);

    @GET("/maestro/tipopagodescuentos")
    Call<List<TipoPagoDescuento>> getTipoPagoDescuento(@Header("Token") String token);

    @GET("/maestro/autorizadores")
    Call<List<Autorizadores>> getAutorizadores(@Header("Token") String token);

    // En tu interfaz de MaestroRepository/ApiService:
    @GET("/motivos_cortesia")
    Call<List<MotivoCortesia>> getMotivosCortesia(@Query("activo") boolean activo);

}
