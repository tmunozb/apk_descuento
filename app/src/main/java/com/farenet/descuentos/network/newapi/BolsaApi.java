package com.farenet.descuentos.network.newapi;

import com.farenet.descuentos.models.bolsa.BolsaBulkReq;
import com.farenet.descuentos.models.bolsa.BolsaBulkRsp;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface BolsaApi {
    @POST("/bolsa/config/bulk")
    Call<BolsaBulkRsp> bulkUpsert(@Body BolsaBulkReq body);
}
