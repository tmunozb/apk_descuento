package com.farenet.descuentos.API.Antigua.Service;

import com.farenet.descuentos.domain.model.Cortesia;
import com.farenet.descuentos.domain.model.Descuento;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface DescuentoRepository {

    @POST("/mobileapi/guardar/descuento")
    Call<String> saveDescuento(@Body Descuento descuento, @Header("Token") String token);

    @POST("/mobileapi/guardar/campana")
    Call<String> saveCampana(@Body Descuento descuento, @Header("Token") String token);

    @POST("/mobileapi/guardar/cortesia")
    Call<String> saveCortesia(@Body Cortesia cortesia, @Header("Token") String token);

    @POST("/mobileapi/guardar/cartas")
    Call<String> saveCarta(@Body Descuento descuento, @Header("Token") String token);

}
