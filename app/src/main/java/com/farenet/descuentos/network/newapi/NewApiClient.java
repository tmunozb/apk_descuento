package com.farenet.descuentos.network.newapi;


import com.farenet.descuentos.BuildConfig;
import com.farenet.descuentos.config.RetrofitMaestro;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NewApiClient {
    private static NewApiService instance;
    private static BolsaApi bolsaApi;

    public static NewApiService get() {
        if (instance == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient ok = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        // Si más adelante usas Token, colócalo aquí:
                        // String token = ...;
                        // return chain.proceed(chain.request().newBuilder()
                        //     .header("Authorization", "Bearer " + token)
                        //     .build());
                        return chain.proceed(chain.request());
                    })
                    .addInterceptor(log)
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            // Asegúrate que NEW_API_BASE_URL termina con '/'
            Retrofit r = new Retrofit.Builder()
                    .baseUrl(BuildConfig.NEW_API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(ok)
                    .build();

            // Usa el MISMO Retrofit y cliente para ambos servicios
            instance = r.create(NewApiService.class);
            if (bolsaApi == null) {
                bolsaApi = r.create(BolsaApi.class);
            }
        }
        return instance;
    }

    // Si prefieres que Bolsa use el singleton de RetrofitMaestro, descomenta esto:
    // private static void ensureBolsaWithMaestro() {
    //     if (bolsaApi == null) {
    //         Retrofit maestro = RetrofitMaestro.getMaestros(BuildConfig.NEW_API_BASE_URL);
    //         bolsaApi = maestro.create(BolsaApi.class);
    //     }
    // }

    /** Acceso al API de bolsas */
    public static BolsaApi bolsa() {
        // ensureBolsaWithMaestro(); // (opcional) si quieres usar RetrofitMaestro
        if (bolsaApi == null) {
            // Si aún no se llamó get(), crea un Retrofit simple para bolsa
            Retrofit maestro = RetrofitMaestro.getMaestros(BuildConfig.NEW_API_BASE_URL);
            bolsaApi = maestro.create(BolsaApi.class);
        }
        return bolsaApi;
    }
}
