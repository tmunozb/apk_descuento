package com.farenet.descuentos.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.farenet.descuentos.R;
import com.farenet.descuentos.adapter.SpinerAdapter;
import com.farenet.descuentos.config.Constante;
import com.farenet.descuentos.domain.Autorizadores;
import com.farenet.descuentos.domain.Cortesia;
import com.farenet.descuentos.domain.MotivoCortesia;
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.network.newapi.NewApiClient;
import com.farenet.descuentos.network.newapi.NewApiService;
import com.farenet.descuentos.repository.DescuentoRepository;
import com.farenet.descuentos.repository.MaestroRepository;
import com.farenet.descuentos.sql.QueryRealm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentCortesia extends Fragment {

    private Spinner spPlanta, spAutoriza;
    private EditText txtPlaca, txtMotivo;
    private Button btnGuardar;

    private SpinerAdapter<Planta> spPlantaAdapter;
    private SpinerAdapter<Autorizadores> spAutorizaAdapter;

    private DescuentoRepository descuentoRepository;
    private MaestroRepository maestroRepository;
    private NewApiService newApi; // Para motivos
    private SharedPreferences sharedPreferences;

    // Cache de motivos desde la API
    private final List<MotivoCortesia> motivos = new ArrayList<>();
    private String motivoSeleccionadoId; // por si luego decides enviar el ID

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cortesia_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spPlanta   = view.findViewById(R.id.sp_planta_cort);
        spAutoriza = view.findViewById(R.id.sp_autoriza_cort);
        txtPlaca   = view.findViewById(R.id.txtPlaca_cort);
        // OJO: en tu XML el id es txtMotivo_cort
        txtMotivo  = view.findViewById(R.id.actvMotivo_cort);
        btnGuardar = view.findViewById(R.id.btnGuardar_cort);

        descuentoRepository = Constante.getDescuentoRepository();
        maestroRepository   = Constante.getMaestroRespository();
        newApi              = NewApiClient.get(); // baseUrl viene de BuildConfig.NEW_API_BASE_URL
        sharedPreferences   = requireActivity().getSharedPreferences(Constante.TOKEN, Context.MODE_PRIVATE);

        // Cargar copias (unmanaged) desde Realm
        List<Planta> plantas = safe(QueryRealm.copyAllPlantas());
        List<Autorizadores> autores = safe(QueryRealm.copyAllAutorizadores());

        spPlantaAdapter   = new SpinerAdapter<>(requireContext(), plantas);
        spAutorizaAdapter = new SpinerAdapter<>(requireContext(), autores);

        spPlanta.setAdapter(spPlantaAdapter);
        spAutoriza.setAdapter(spAutorizaAdapter);

        // Guardar
        btnGuardar.setOnClickListener(v -> onGuardarClicked());

        // Selector de motivo via diálogo
        txtMotivo.setFocusable(false);
        txtMotivo.setOnClickListener(v -> mostrarSelectorMotivo());

        // Si los spinners están vacíos, refresca desde API
        ensureMaestrosDesdeApiSiHaceFalta();

        // Cargar motivos de cortesía desde NewApiService
        cargarMotivosCortesia();
    }

    private <T> List<T> safe(List<T> list) { return list != null ? list : Collections.emptyList(); }

    // ----------------- Motivos desde NewApiService -----------------
    private void cargarMotivosCortesia() {
        // Si tu endpoint no requiere token, no retornes aunque falte:
        String token = sharedPreferences.getString("token", null);
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(requireContext(), "Sesión no válida. Inicie sesión.", Toast.LENGTH_LONG).show();
            // Si el endpoint es público, puedes NO retornar.
            // return;
        }

        if (newApi == null) {
            Toast.makeText(requireContext(), "Servicio no disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        newApi.getMotivosCortesia(true).enqueue(new Callback<List<MotivoCortesia>>() {
            @Override
            public void onResponse(Call<List<MotivoCortesia>> call, Response<List<MotivoCortesia>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    motivos.clear();
                    motivos.addAll(response.body());
                    if (!motivos.isEmpty()) {
                        // Prefijar el primero (opcional)
                        txtMotivo.setText(motivos.get(0).getNombre());
                        motivoSeleccionadoId = motivos.get(0).getId();
                    }
                } else {
                    handleHttpError("Motivos cortesía", response.code(), response.message());
                }
            }

            @Override
            public void onFailure(Call<List<MotivoCortesia>> call, Throwable t) {
                Toast.makeText(requireContext(), "Error motivos: " + safeMsg(t), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarSelectorMotivo() {
        if (motivos.isEmpty()) {
            Toast.makeText(requireContext(), "No hay motivos disponibles", Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence[] items = new CharSequence[motivos.size()];
        int preseleccion = -1;
        String actual = txtMotivo.getText() != null ? txtMotivo.getText().toString() : "";
        for (int i = 0; i < motivos.size(); i++) {
            items[i] = motivos.get(i).getNombre();
            if (!TextUtils.isEmpty(actual) && actual.equals(motivos.get(i).getNombre())) {
                preseleccion = i;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Selecciona un motivo")
                .setSingleChoiceItems(items, preseleccion, (dialog, which) -> {
                    MotivoCortesia sel = motivos.get(which);
                    txtMotivo.setText(sel.getNombre());
                    motivoSeleccionadoId = sel.getId();
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    // ----------------- FIN motivos -----------------

    private void onGuardarClicked() {
        if (!validarCampos()) return;

        String token = sharedPreferences.getString("token", null);
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(requireContext(), "Sesión no válida. Inicie sesión nuevamente.", Toast.LENGTH_LONG).show();
            return;
        }

        Planta planta = (Planta) spPlanta.getSelectedItem();
        Autorizadores aut = (Autorizadores) spAutoriza.getSelectedItem();

        if (planta == null) {
            Toast.makeText(requireContext(), "Seleccione una planta", Toast.LENGTH_SHORT).show();
            return;
        }
        if (aut == null) {
            Toast.makeText(requireContext(), "Seleccione quién autoriza", Toast.LENGTH_SHORT).show();
            return;
        }

        String placa    = txtPlaca.getText().toString().trim().toUpperCase();
        String motivo   = txtMotivo.getText().toString().trim(); // texto del motivo seleccionado
        String autoriza = aut.getNombre() != null ? aut.getNombre() : aut.toString();

        Cortesia cortesia = new Cortesia();
        cortesia.setPlaca(placa);
        cortesia.setPlanta(planta.getKey());
        cortesia.setAutoriza(autoriza);
        cortesia.setMotivo(motivo);
        // Si luego quieres enviar el ID: ya lo tienes en motivoSeleccionadoId

        btnGuardar.setEnabled(false);
        Call<String> call = descuentoRepository.saveCortesia(cortesia, token);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                btnGuardar.setEnabled(true);
                if (response.isSuccessful() && response.code() == 200) {
                    Toast.makeText(requireContext(), "Se agregó la cortesía", Toast.LENGTH_LONG).show();
                    abrirWhatsappYLimpiar(placa);
                } else {
                    if (response.code() == 401 || response.code() == 403) {
                        Toast.makeText(requireContext(), "Sesión expirada. Inicie sesión.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), "Error al registrar (" + response.code() + ")", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                btnGuardar.setEnabled(true);
                Toast.makeText(requireContext(), "Error de red: " + (t.getMessage() != null ? t.getMessage() : ""), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validarCampos() {
        boolean ok = true;

        String placa = txtPlaca.getText() != null ? txtPlaca.getText().toString().trim() : "";
        if (placa.isEmpty()) { txtPlaca.setError("Ingrese placa"); ok = false; }

        String motivo = txtMotivo.getText() != null ? txtMotivo.getText().toString().trim() : "";
        if (motivo.isEmpty()) { txtMotivo.setError("Seleccione motivo"); ok = false; }

        if (spPlanta.getAdapter() == null || spPlanta.getAdapter().getCount() == 0) {
            Toast.makeText(requireContext(), "No hay plantas disponibles", Toast.LENGTH_SHORT).show();
            ok = false;
        }
        if (spAutoriza.getAdapter() == null || spAutoriza.getAdapter().getCount() == 0) {
            Toast.makeText(requireContext(), "No hay autorizadores disponibles", Toast.LENGTH_SHORT).show();
            ok = false;
        }

        return ok;
    }

    private void abrirWhatsappYLimpiar(String placa) {
        String msg = getString(R.string.msjwhtsp_corte, placa);

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, msg);

        boolean launched = tryStartActivityWithPackage(send, "com.whatsapp");
        if (!launched) launched = tryStartActivityWithPackage(send, "com.whatsapp.w4b");

        if (!launched) {
            String url = "https://wa.me/?text=" + Uri.encode(msg);
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(requireContext(), "No se encontró WhatsApp ni navegador disponible", Toast.LENGTH_SHORT).show();
            }
        }

        limpiar();
    }

    private boolean tryStartActivityWithPackage(Intent base, String packageName) {
        try {
            Intent i = new Intent(base);
            i.setPackage(packageName);
            if (i.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(i);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void limpiar() {
        if (spPlanta.getAdapter() != null && spPlanta.getAdapter().getCount() > 0) spPlanta.setSelection(0);
        if (spAutoriza.getAdapter() != null && spAutoriza.getAdapter().getCount() > 0) spAutoriza.setSelection(0);
        txtPlaca.setText("");
        txtMotivo.setText("");
        motivoSeleccionadoId = null;
    }

    /** Si los spinners están vacíos, baja maestros desde API, guarda en Realm y refresca adapters. */
    private void ensureMaestrosDesdeApiSiHaceFalta() {
        boolean needPlantas       = spPlantaAdapter.getCount() == 0;
        boolean needAutorizadores = spAutorizaAdapter.getCount() == 0;

        if (!(needPlantas || needAutorizadores)) return;

        String token = sharedPreferences.getString("token", null);
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(requireContext(), "Sesión no válida. Inicie sesión.", Toast.LENGTH_LONG).show();
            return;
        }

        btnGuardar.setEnabled(false);
        final AtomicInteger pending = new AtomicInteger(0);

        Runnable refreshUiIfDone = () -> {
            if (pending.decrementAndGet() == 0) {
                spPlantaAdapter.setItems(QueryRealm.copyAllPlantas());
                spAutorizaAdapter.setItems(QueryRealm.copyAllAutorizadores());
                btnGuardar.setEnabled(true);
            }
        };

        if (needPlantas) {
            pending.incrementAndGet();
            maestroRepository.getPlantas(token).enqueue(new Callback<List<Planta>>() {
                @Override public void onResponse(Call<List<Planta>> call, Response<List<Planta>> rsp) {
                    if (rsp.isSuccessful() && rsp.body() != null) {
                        QueryRealm.savePlantaAsync(rsp.body(), new QueryRealm.TxCallback() {
                            @Override public void onSuccess() { refreshUiIfDone.run(); }
                            @Override public void onError(Throwable error) {
                                Toast.makeText(requireContext(), "Guardar plantas: " + safeMsg(error), Toast.LENGTH_SHORT).show();
                                refreshUiIfDone.run();
                            }
                        });
                    } else {
                        handleHttpError("Plantas", rsp.code(), rsp.message());
                        refreshUiIfDone.run();
                    }
                }
                @Override public void onFailure(Call<List<Planta>> call, Throwable t) {
                    Toast.makeText(requireContext(), "Error plantas: " + safeMsg(t), Toast.LENGTH_SHORT).show();
                    refreshUiIfDone.run();
                }
            });
        }

        if (needAutorizadores) {
            pending.incrementAndGet();
            maestroRepository.getAutorizadores(token).enqueue(new Callback<List<Autorizadores>>() {
                @Override public void onResponse(Call<List<Autorizadores>> call, Response<List<Autorizadores>> rsp) {
                    if (rsp.isSuccessful() && rsp.body() != null) {
                        QueryRealm.saveAutorizadoresAsync(rsp.body(), new QueryRealm.TxCallback() {
                            @Override public void onSuccess() { refreshUiIfDone.run(); }
                            @Override public void onError(Throwable error) {
                                Toast.makeText(requireContext(), "Guardar autorizadores: " + safeMsg(error), Toast.LENGTH_SHORT).show();
                                refreshUiIfDone.run();
                            }
                        });
                    } else {
                        handleHttpError("Autorizadores", rsp.code(), rsp.message());
                        refreshUiIfDone.run();
                    }
                }
                @Override public void onFailure(Call<List<Autorizadores>> call, Throwable t) {
                    Toast.makeText(requireContext(), "Error autorizadores: " + safeMsg(t), Toast.LENGTH_SHORT).show();
                    refreshUiIfDone.run();
                }
            });
        }

        if (pending.get() == 0) btnGuardar.setEnabled(true);
    }

    private void handleHttpError(String tag, int code, String msg) {
        if (code == 401 || code == 403) {
            Toast.makeText(requireContext(), tag + ": sesión expirada. Inicie sesión.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(requireContext(), tag + " HTTP " + code + " - " + msg, Toast.LENGTH_SHORT).show();
        }
    }

    private String safeMsg(Throwable t) {
        return t != null && t.getMessage() != null ? t.getMessage() : "desconocido";
    }
}
