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
import androidx.fragment.app.Fragment;

import com.farenet.descuentos.R;
import com.farenet.descuentos.adapter.SpinerAdapter;
import com.farenet.descuentos.config.Constante;
import com.farenet.descuentos.domain.Autorizadores;
import com.farenet.descuentos.domain.Cortesia;
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.repository.DescuentoRepository;
import com.farenet.descuentos.sql.QueryRealm;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

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
    private SharedPreferences sharedPreferences;

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
        txtMotivo  = view.findViewById(R.id.txtMotivo_cort);
        btnGuardar = view.findViewById(R.id.btnGuardar_cort);

        descuentoRepository = Constante.getDescuentoRepository();
        sharedPreferences = requireActivity().getSharedPreferences(Constante.TOKEN, Context.MODE_PRIVATE);

        // Cargar COPIAS (unmanaged) desde Realm (no deja instancias abiertas)
        List<Planta> plantas = safe(QueryRealm.copyAllPlantas());
        List<Autorizadores> autores = safe(QueryRealm.copyAllAutorizadores());

        spPlantaAdapter   = new SpinerAdapter<>(requireContext(), plantas);
        spAutorizaAdapter = new SpinerAdapter<>(requireContext(), autores);

        spPlanta.setAdapter(spPlantaAdapter);
        spAutoriza.setAdapter(spAutorizaAdapter);

        btnGuardar.setOnClickListener(v -> onGuardarClicked());
    }

    private <T> List<T> safe(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }

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

        String placa  = txtPlaca.getText().toString().trim().toUpperCase();
        String motivo = txtMotivo.getText().toString().trim();
        String autoriza = aut.getNombre() != null ? aut.getNombre() : aut.toString();

        Cortesia cortesia = new Cortesia();
        cortesia.setPlaca(placa);
        cortesia.setPlanta(planta.getKey());
        cortesia.setAutoriza(autoriza);
        cortesia.setMotivo(motivo);

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
                    Toast.makeText(requireContext(), "Error al registrar", Toast.LENGTH_LONG).show();
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
        if (placa.isEmpty()) {
            txtPlaca.setError("Ingrese placa");
            ok = false;
        }

        String motivo = txtMotivo.getText() != null ? txtMotivo.getText().toString().trim() : "";
        if (motivo.isEmpty()) {
            txtMotivo.setError("Ingrese motivo");
            ok = false;
        }

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
        String msg = getString(R.string.msjwhtsp_corte) + " " + placa;
        String encoded = URLEncoder.encode(msg, StandardCharsets.UTF_8);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://send?text=" + encoded));

        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=" + encoded));
            if (web.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(web);
            } else {
                Toast.makeText(requireContext(), "No se encontró WhatsApp", Toast.LENGTH_SHORT).show();
            }
        }

        limpiar();
    }

    private void limpiar() {
        spPlanta.setSelection(0);
        spAutoriza.setSelection(0);
        txtPlaca.setText("");
        txtMotivo.setText("");
    }
}
