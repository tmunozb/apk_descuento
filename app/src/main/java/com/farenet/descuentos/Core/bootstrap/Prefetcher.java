package com.farenet.descuentos.Core.bootstrap;

import android.util.Log;

import com.farenet.descuentos.data.local.realm.entity.MotivoCortesia;
import com.farenet.descuentos.API.Actual.DTO.maestros.MotivoDescuento;
import com.farenet.descuentos.API.Actual.Service.NewApiService;
import com.farenet.descuentos.API.Antigua.Service.MaestroRepository;
import com.farenet.descuentos.data.local.realm.dao.QueryRealm;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class Prefetcher {

    private static final String TAG = "Prefetcher";

    private Prefetcher() { }

    /**
     * Lanza en paralelo requests que guardan en Realm usando tus repositorios
     * y dejan todo listo para el primer render.
     */
    public static void warmUpAfterLogin(
            String token,
            MaestroRepository maestroRepository,
            NewApiService newApi // puede ser null si aún no lo usas
    ) {
        if (token == null || token.trim().isEmpty()) {
            Log.w(TAG, "warmUpAfterLogin(): token vacío, se salta prefetch");
            return;
        }
        if (maestroRepository == null) {
            Log.w(TAG, "warmUpAfterLogin(): maestroRepository nulo");
            return;
        }

        // ====== Maestros “legacy” que ya usas con Realm ======
        maestroRepository.getPlantas(token).enqueue(noopList(
                "Plantas",
                list -> QueryRealm.savePlantaAsync(list, new QueryRealm.TxCallback() {
                    @Override public void onSuccess() { Log.d(TAG, "Plantas guardadas"); }
                    @Override public void onError(Throwable error) { Log.w(TAG, "Plantas error", error); }
                })
        ));

        maestroRepository.getAutorizadores(token).enqueue(noopList(
                "Autorizadores",
                list -> QueryRealm.saveAutorizadoresAsync(list, new QueryRealm.TxCallback() {
                    @Override public void onSuccess() { Log.d(TAG, "Autorizadores guardados"); }
                    @Override public void onError(Throwable error) { Log.w(TAG, "Autorizadores error", error); }
                })
        ));

        maestroRepository.getConceptoinspeccion(token).enqueue(noopList(
                "Conceptos",
                list -> QueryRealm.saveConceptosAsync(list, new QueryRealm.TxCallback() {
                    @Override public void onSuccess() { Log.d(TAG, "Conceptos guardados"); }
                    @Override public void onError(Throwable error) { Log.w(TAG, "Conceptos error", error); }
                })
        ));

        maestroRepository.getTipoPagoDescuento(token).enqueue(noopList(
                "TipoPago",
                list -> QueryRealm.saveTipoPagoAsync(list, new QueryRealm.TxCallback() {
                    @Override public void onSuccess() { Log.d(TAG, "TipoPago guardado"); }
                    @Override public void onError(Throwable error) { Log.w(TAG, "TipoPago error", error); }
                })
        ));

        // ====== Endpoints nuevos (opcional) ======
        if (newApi != null) {
            // Motivos de Cortesía
            newApi.getMotivosCortesia(true).enqueue(new Callback<List<MotivoCortesia>>() {
                @Override public void onResponse(Call<List<MotivoCortesia>> call, Response<List<MotivoCortesia>> rsp) {
                    Log.d(TAG, "MotivosCortesia prefetch: " + rsp.code());
                }
                @Override public void onFailure(Call<List<MotivoCortesia>> call, Throwable t) {
                    Log.w(TAG, "MotivosCortesia prefetch error", t);
                }
            });

            // Motivos de Descuento (asegúrate de tener este endpoint en NewApiService)
            try {
                newApi.getMotivosDescuento(true).enqueue(new Callback<List<MotivoDescuento>>() {
                    @Override public void onResponse(Call<List<MotivoDescuento>> call, Response<List<MotivoDescuento>> rsp) {
                        Log.d(TAG, "MotivosDescuento prefetch: " + rsp.code());
                    }
                    @Override public void onFailure(Call<List<MotivoDescuento>> call, Throwable t) {
                        Log.w(TAG, "MotivosDescuento prefetch error", t);
                    }
                });
            } catch (Throwable ignore) {
                // Por si aún no has creado getMotivosDescuento en el servicio
            }
        }
    }

    // Utilidad genérica para listas que guardas en Realm vía repositorio
    private static <T> Callback<List<T>> noopList(String tag, Saver<List<T>> saver) {
        return new Callback<List<T>>() {
            @Override public void onResponse(Call<List<T>> call, Response<List<T>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try { saver.save(response.body()); } catch (Throwable t) { Log.w(TAG, tag + " save error", t); }
                } else {
                    Log.d(TAG, tag + " prefetch HTTP " + response.code());
                }
            }
            @Override public void onFailure(Call<List<T>> call, Throwable t) {
                Log.w(TAG, tag + " prefetch error", t);
            }
        };
    }

    private interface Saver<T> { void save(T data); }
}
