package com.farenet.descuentos.API.Actual.Service;

import com.farenet.descuentos.API.Actual.DTO.adjuntos.AdjuntoCrearReq;
import com.farenet.descuentos.API.Actual.DTO.auth.LoginReq;
import com.farenet.descuentos.API.Actual.DTO.auth.LoginRsp;
import com.farenet.descuentos.API.Actual.DTO.auth.UsuarioResumenDto;
import com.farenet.descuentos.API.Actual.DTO.maestros.AccesoPlantaDto;
import com.farenet.descuentos.API.Actual.DTO.maestros.ConceptosResponse;
import com.farenet.descuentos.API.Actual.DTO.maestros.MotivoDescuento;
import com.farenet.descuentos.API.Actual.DTO.maestros.TipoPagoDto;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.AdjuntoCrearRsp;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.AprobarSolicitudReq;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.AutorizarSolicitudReq;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.RechazarSolicitudReq;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudCrearReq;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudCrearRsp;
import com.farenet.descuentos.data.local.realm.entity.MotivoCortesia;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.AccionSolicitudRsp;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudPendienteDto;

import java.util.List;
import java.util.Map;

import com.farenet.descuentos.API.Actual.DTO.bolsa.BolsaConfigDto;
import com.farenet.descuentos.API.Actual.DTO.bolsa.BolsaAuditoriaDto;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
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

    // GET /motivos_descuento?activo=true
    @GET("motivos_descuento")
    Call<List<MotivoDescuento>> getMotivosDescuento(@Query("activo") boolean activo);

    @POST("/solicitudes/{id}/autorizar")
    Call<AccionSolicitudRsp> autorizarSolicitud(
            @Path("id") String id,
            @Body AutorizarSolicitudReq body
    );

    @GET("/bolsa/config/list")
    Call<List<BolsaConfigDto>> bolsaListConfigs(
            @Query("periodo") String periodo,
            @Query("planta_key") String plantaKey,
            @Query("estado") String estado,        // opcional
            @Query("limit") Integer limit          // opcional
    );

    @POST("/bolsa/config")
    Call<BolsaConfigDto> bolsaUpsertConfig(@Body Map<String, Object> body);

    @PATCH("/bolsa/config/estado")
    Call<Map<String, Object>> bolsaSetEstado(@Body Map<String, Object> body);

    @GET("/bolsa/auditoria")
    Call<List<BolsaAuditoriaDto>> bolsaAuditoria(
            @Query("planta_key") String plantaKey,
            @Query("periodo") String periodo,
            @Query("limit") Integer limit
    );

    @GET("solicitudes/ultimas-aprobadas")
    Call<List<SolicitudDto>> ultimasAprobadas(@Query("user") String user, @Query("limit") Integer limit);

    @GET("solicitudes")
    Call<List<SolicitudDto>> listarSolicitudes(
            @Query("user") String user,
            @Query("estado") String estado,
            @Query("tipo") String tipo,
            @Query("q") String q,
            @Query("limit") Integer limit,
            @Query("offset") Integer offset,
            @Query("orden") String orden
    );


}
