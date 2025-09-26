package com.farenet.descuentos.network.newapi;

import com.farenet.descuentos.models.newapi.*;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface NewApiService {

    // POST /login  {username, password}
    @POST("login_perfiles")
    Call<LoginRsp> login(@Body LoginReq req);

    // GET /buscar_usuarios?filtro=user  -> devuelve lista; tomaremos el que username==user
    @GET("/buscar_usuarios")
    Call<List<UsuarioResumenDto>> buscarUsuarios(@Query("filtro") String filtro);

    @GET("login_perfiles")
    Call<LoginRsp> loginPerfilesGet(@Query("username") String username);

    // GET /obtener_accesos_usuario?usuario=user
    @GET("/obtener_accesos_usuario")
    Call<List<AccesoPlantaDto>> obtenerAccesos(@Query("usuario") String usuario);

    // 🔁 Fallback por si el backend espera "user" en lugar de "usuario"
    @GET("/obtener_accesos_usuario")
    Call<List<AccesoPlantaDto>> obtenerAccesosPorUser(@Query("user") String user);

    @GET("conceptos_planta")
    Call<ConceptosResponse> conceptosPorPlanta(@Query("planta_key") String plantaKeyCsv);

    @GET("/obtener_tipo_pago_descuento")
    Call<List<TipoPagoDto>> obtenerTiposPago(@Query("filtro") String filtro);
}
