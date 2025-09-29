package com.farenet.descuentos.network.newapi;

import com.farenet.descuentos.BuildConfig;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NewApiClient {
    private static NewApiService instance;

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
                        //     .header("Token", token)
                        //     .build());
                        return chain.proceed(chain.request());
                    })
                    .addInterceptor(log)
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            // Asegúrate que NEW_API_BASE_URL termine con '/'
            Retrofit r = new Retrofit.Builder()
                    .baseUrl(BuildConfig.NEW_API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(ok)
                    .build();

            instance = r.create(NewApiService.class);
        }
        return instance;
    }


}
