package com.farenet.descuentos.API.Actual.Service;

import com.farenet.descuentos.API.Actual.DTO.bolsa.BolsaBulkReq;
import com.farenet.descuentos.API.Actual.DTO.bolsa.BolsaBulkRsp;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface BolsaApi {
    @POST("/bolsa/config/bulk")
    Call<BolsaBulkRsp> bulkUpsert(@Body BolsaBulkReq body);
}
