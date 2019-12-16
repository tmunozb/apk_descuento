package com.farenet.descuentos.repository;

import com.farenet.descuentos.domain.Cortesia;
import com.farenet.descuentos.domain.Descuento;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Author by Alexis Pumayalla on 28/08/19.
 * Email apumayallag@gmail.com
 * Phone 961778965
 */
public interface DescuentoRepository {

    @POST("/mobileapi/guardar/descuento")
    Call<String> saveDescuento(@Body Descuento descuento, @Header("Token") String token);

    @POST("/mobileapi/guardar/cortesia")
    Call<String> saveCortesia(@Body Cortesia cortesia, @Header("Token") String token);

    @POST("/mobileapi/guardar/cartas")
    Call<String> saveCarta(@Body Descuento descuento, @Header("Token") String token);

}
