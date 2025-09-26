// network/newapi/NewApiClient.java
package com.farenet.descuentos.network.newapi;

import com.farenet.descuentos.BuildConfig;
import com.farenet.descuentos.models.newapi.TipoPagoDto;
import com.farenet.descuentos.models.newapi.ConceptosResponse;

import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NewApiClient {
    private static NewApiService instance;

    public static NewApiService get() {
        if (instance == null) {
            OkHttpClient ok = new OkHttpClient.Builder().build();
            Retrofit r = new Retrofit.Builder()
                    .baseUrl(BuildConfig.NEW_API_BASE_URL) // debe terminar en '/'
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(ok)
                    .build();
            instance = r.create(NewApiService.class);
        }
        return instance;
    }

    // -------- helpers opcionales ----------
    public static Call<List<TipoPagoDto>> tiposPago() {
        return get().obtenerTiposPago(null);
    }
    public static Call<ConceptosResponse> conceptosPorPlanta(String plantaKey) {
        return get().conceptosPorPlanta(plantaKey);
    }
}
