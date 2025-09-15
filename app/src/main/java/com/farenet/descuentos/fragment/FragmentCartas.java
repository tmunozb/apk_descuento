package com.farenet.descuentos.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.farenet.descuentos.R;
import com.farenet.descuentos.adapter.SpinerAdapter;
import com.farenet.descuentos.config.Constante;
import com.farenet.descuentos.domain.Conceptoinspeccion;
import com.farenet.descuentos.domain.Descuento;
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.domain.TipoPagoDescuento;
import com.farenet.descuentos.repository.DescuentoRepository;
import com.farenet.descuentos.sql.QueryRealm;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentCartas extends Fragment {

    private Spinner spPlanta;
    private Spinner spConcepto;
    private Spinner spTipoPago;
    private EditText txtPlaca;
    private EditText txtMonto;
    private Button btnGuardar;

    private SpinerAdapter<Planta> spPlantaAdapter;
    private SpinerAdapter<Conceptoinspeccion> spConceptoAdapter;
    private SpinerAdapter<TipoPagoDescuento> spTipoPagoAdapter;

    private DescuentoRepository descuentoRepository;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cartas_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spPlanta = view.findViewById(R.id.sp_planta_desc);
        spConcepto = view.findViewById(R.id.sp_concepto_desc);
        spTipoPago = view.findViewById(R.id.sp_tipopago_desc);
        txtPlaca = view.findViewById(R.id.txtPlaca_desc);
        txtMonto = view.findViewById(R.id.txtMonto_desc);
        btnGuardar = view.findViewById(R.id.btnGuardar_desc);

        sharedPreferences = requireActivity().getSharedPreferences(Constante.TOKEN, Context.MODE_PRIVATE);
        descuentoRepository = Constante.getDescuentoRepository();

        // Cargar datos desde Realm como COPIAS (unmanaged) – no mantiene Realm abierto
        List<Planta> plantas = safe(QueryRealm.copyAllPlantas());
        List<Conceptoinspeccion> conceptos = safe(QueryRealm.copyAllConceptos());
        List<TipoPagoDescuento> tiposPago = safe(QueryRealm.copyAllTipoPagos());

        spPlantaAdapter = new SpinerAdapter<>(requireContext(), plantas);
        spConceptoAdapter = new SpinerAdapter<>(requireContext(), conceptos);
        spTipoPagoAdapter = new SpinerAdapter<>(requireContext(), tiposPago);

        spPlanta.setAdapter(spPlantaAdapter);
        spConcepto.setAdapter(spConceptoAdapter);
        spTipoPago.setAdapter(spTipoPagoAdapter);

        btnGuardar.setOnClickListener(v -> onGuardarClicked());
    }

    private <T> List<T> safe(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }

    private void onGuardarClicked() {
        if (!validarCampos()) return;

        Planta planta = (Planta) spPlanta.getSelectedItem();
        Conceptoinspeccion concepto = (Conceptoinspeccion) spConcepto.getSelectedItem();
        TipoPagoDescuento tipoPago = (TipoPagoDescuento) spTipoPago.getSelectedItem();

        if (planta == null || concepto == null || tipoPago == null) {
            Toast.makeText(requireContext(), "Faltan datos de maestros", Toast.LENGTH_SHORT).show();
            return;
        }

        String placa = txtPlaca.getText().toString().trim().toUpperCase();
        Double monto = parseMonto(txtMonto.getText().toString().trim());
        if (monto == null) {
            txtMonto.setError("Monto inválido");
            return;
        }

        String token = sharedPreferences.getString("token", null);
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(requireContext(), "Sesión no válida. Inicie sesión nuevamente.", Toast.LENGTH_LONG).show();
            return;
        }

        Descuento descuento = new Descuento();
        descuento.setConceptoinspeccion(concepto.getKey());
        descuento.setTipoPagoDescuento(tipoPago.getKey());
        descuento.setPlanta(planta.getKey());
        descuento.setPlaca(placa);
        descuento.setMonto(monto);

        Call<String> call = descuentoRepository.saveCarta(descuento, token);
        btnGuardar.setEnabled(false);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                btnGuardar.setEnabled(true);
                if (response.isSuccessful() && response.code() == 200) {
                    Toast.makeText(requireContext(), "Se agregó el descuento", Toast.LENGTH_LONG).show();
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

        String montoStr = txtMonto.getText() != null ? txtMonto.getText().toString().trim() : "";
        if (montoStr.isEmpty()) {
            txtMonto.setError("Ingrese monto");
            ok = false;
        } else if (parseMonto(montoStr) == null) {
            txtMonto.setError("Monto inválido");
            ok = false;
        }

        return ok;
    }

    @Nullable
    private Double parseMonto(@NonNull String montoStr) {
        try {
            // Si usas coma decimal en Perú, podrías hacer: montoStr = montoStr.replace(',', '.');
            double v = Double.parseDouble(montoStr.replace(',', '.'));
            return v >= 0 ? v : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void abrirWhatsappYLimpiar(String placa) {
        String msg = getString(R.string.msjwhtspcarta) + " " + placa;
        String encoded = URLEncoder.encode(msg, StandardCharsets.UTF_8);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("whatsapp://send?text=" + encoded));

        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // fallback web
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
        spTipoPago.setSelection(0);
        spConcepto.setSelection(0);
        txtPlaca.setText("");
        txtMonto.setText("");
    }
}
