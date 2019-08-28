package com.farenet.descuentos.repository;

import com.farenet.descuentos.domain.Usuario;

import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Author by Alexis Pumayalla on 28/08/19.
 * Email apumayallag@gmail.com
 * Phone 961778965
 */
public interface LoginRepository {

    @POST("/loginmaestro")
    Call<Usuario> getUsuario(@Header("uname") String user, @Header("pw") String pass);

}
