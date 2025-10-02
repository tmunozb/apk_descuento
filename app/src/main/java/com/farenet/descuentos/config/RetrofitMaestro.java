package com.farenet.descuentos.config;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitMaestro {

    // Mantengo tu singleton actual
    private static Retrofit retrofit;

    /** Mantén este método tal cual lo usabas */
    public static Retrofit getMaestros(String url) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /** Alias opcional por si quieres un nombre más genérico sin romper compatibilidad */
    public static Retrofit getRetrofit(String url) {
        return getMaestros(url);
    }
}
