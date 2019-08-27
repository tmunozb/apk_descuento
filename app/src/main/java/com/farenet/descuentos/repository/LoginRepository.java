package com.farenet.descuentos.repository;

import com.farenet.descuentos.domain.Usuario;

import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface LoginRepository {

    @POST("/loginmaestro")
    Call<Usuario> getUsuario(@Header("uname") String user, @Header("pw") String pass);

}
