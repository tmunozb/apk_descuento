package com.farenet.descuentos.API.Antigua.Service;

import com.farenet.descuentos.domain.model.Usuario;

import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface LoginRepository {

    @POST("/loginmaestro")
    Call<Usuario> getUsuario(@Header("uname") String user, @Header("pw") String pass);

}
