package com.farenet.descuentos.network.newapi;

import com.farenet.descuentos.domain.MotivoCortesia;
import com.farenet.descuentos.models.newapi.*;
import com.farenet.descuentos.models.req.*;
import com.farenet.descuentos.models.rsp.AccionSolicitudRsp;
import com.farenet.descuentos.models.rsp.SolicitudPendienteDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NewApiService {

    // POST /login_perfiles
    @POST("login_perfiles")
    Call<LoginRsp> login(@Body LoginReq req);

    // GET /buscar_usuarios?filtro=...
    @GET("buscar_usuarios")
    Call<List<UsuarioResumenDto>> buscarUsuarios(@Query("filtro") String filtro);

    // GET /login_perfiles?username=...
    @GET("login_perfiles")
    Call<LoginRsp> loginPerfilesGet(@Query("username") String username);

    // GET /obtener_accesos_usuario?usuario=...
    @GET("obtener_accesos_usuario")
    Call<List<AccesoPlantaDto>> obtenerAccesos(@Query("usuario") String usuario);

    // Fallback por si backend usa ?user=
    @GET("obtener_accesos_usuario")
    Call<List<AccesoPlantaDto>> obtenerAccesosPorUser(@Query("user") String user);

    // GET /conceptos_planta?planta_key=...
    @GET("conceptos_planta")
    Call<ConceptosResponse> conceptosPorPlanta(@Query("planta_key") String plantaKeyCsv);

    // GET /obtener_tipo_pago_descuento
    @GET("obtener_tipo_pago_descuento")
    Call<List<TipoPagoDto>> obtenerTiposPago(@Query("filtro") String filtro);

    // POST /solicitudes
    @POST("solicitudes")
    Call<SolicitudCrearRsp> crearSolicitud(@Body SolicitudCrearReq req);

    // POST /solicitudes/{id}/adjuntos
    @POST("solicitudes/{id}/adjuntos")
    Call<AdjuntoCrearRsp> subirAdjunto(
            @Path("id") String solicitudId,
            @Body AdjuntoCrearReq req
    );

    // Listar pendientes (ENVIADA/OBSERVADA)
    @GET("/solicitudes/pendientes")
    Call<List<SolicitudPendienteDto>> listarPendientes();

    // Aprobar
    @POST("/solicitudes/{id}/aprobar")
    Call<AccionSolicitudRsp> aprobarSolicitud(
            @Path("id") String id,
            @Body AprobarSolicitudReq req
    );

    // Rechazar
    @POST("/solicitudes/{id}/rechazar")
    Call<AccionSolicitudRsp> rechazarSolicitud(
            @Path("id") String id,
            @Body RechazarSolicitudReq req
    );

    // GET /motivos_cortesia?activo=true
    @GET("motivos_cortesia")
    Call<List<MotivoCortesia>> getMotivosCortesia(@Query("activo") Boolean activo);

}
