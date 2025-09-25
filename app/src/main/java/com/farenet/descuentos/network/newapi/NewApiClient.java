package com.farenet.descuentos.network.newapi;

import com.farenet.descuentos.BuildConfig;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class NewApiClient {
    private static NewApiService instance;

    public static NewApiService get() {
        if (instance == null) {
            OkHttpClient ok = new OkHttpClient.Builder().build();

            // Asegúrate que en build.gradle tu base tenga slash final (ya lo pusiste)
            Retrofit r = new Retrofit.Builder()
                    .baseUrl(BuildConfig.NEW_API_BASE_URL) // ej: http://209.45.83.185:5004/
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(ok)
                    .build();
            instance = r.create(NewApiService.class);
        }
        return instance;
    }
}
