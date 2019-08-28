package com.farenet.descuentos.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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
import com.farenet.descuentos.domain.Conceptoinspeccion;
import com.farenet.descuentos.domain.Descuento;
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.domain.TipoPagoDescuento;
import com.farenet.descuentos.repository.DescuentoRepository;
import com.farenet.descuentos.sql.QueryRealm;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Author by Alexis Pumayalla on 28/08/19.
 * Email apumayallag@gmail.com
 * Phone 961778965
 */
public class FragmentDescuento extends Fragment {

    private Spinner spPlanta;
    private Spinner spConcepto;
    private Spinner spTipoPago;
    private EditText txtPlaca;
    private EditText txtMonto;
    private Button btnGuardar;
    private List<Planta> plantas;
    private List<Conceptoinspeccion> conceptoinspeccions;
    private List<TipoPagoDescuento> tipoPagoDescuentos;
    private SpinerAdapter<Planta> spPlantaAdapter;
    private SpinerAdapter<Conceptoinspeccion> spConceptoAdapter;
    private SpinerAdapter<TipoPagoDescuento> spTipoPagoAdapter;

    private DescuentoRepository descuentoRepository;
    private Descuento descuento = new Descuento();

    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.descuento_fragment, container, false);
        spPlanta = (Spinner) view.findViewById(R.id.sp_planta_desc);
        spConcepto = (Spinner) view.findViewById(R.id.sp_concepto_desc);
        spTipoPago = (Spinner) view.findViewById(R.id.sp_tipopago_desc);
        txtPlaca = (EditText) view.findViewById(R.id.txtPlaca_desc);
        txtMonto = (EditText) view.findViewById(R.id.txtMonto_desc);
        btnGuardar = (Button) view.findViewById(R.id.btnGuardar_desc);


        plantas = QueryRealm.getAllPlantas();
        spPlantaAdapter = new SpinerAdapter<Planta>(getContext(), plantas);
        spPlanta.setAdapter(spPlantaAdapter);

        conceptoinspeccions = QueryRealm.getAllConcepto();
        spConceptoAdapter = new SpinerAdapter<Conceptoinspeccion>(getContext(), conceptoinspeccions);
        spConcepto.setAdapter(spConceptoAdapter);

        tipoPagoDescuentos = QueryRealm.getAllTipoPagos();
        spTipoPagoAdapter = new SpinerAdapter<TipoPagoDescuento>(getContext(), tipoPagoDescuentos);
        spTipoPago.setAdapter(spTipoPagoAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        sharedPreferences = this.getActivity().getSharedPreferences(Constante.TOKEN, Context.MODE_PRIVATE);

        descuentoRepository = Constante.getDescuentoRepository();

        event();
    }


    private void event() {

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!validarCampos()) {
                    return;
                }
                Planta planta = (Planta) spPlanta.getSelectedItem();
                Conceptoinspeccion conceptoinspeccion = (Conceptoinspeccion) spConcepto.getSelectedItem();
                TipoPagoDescuento tipoPagoDescuento = (TipoPagoDescuento) spTipoPago.getSelectedItem();
                descuento.setConceptoinspeccion(conceptoinspeccion.getKey());
                descuento.setTipoPagoDescuento(tipoPagoDescuento.getKey());
                descuento.setPlanta(planta.getKey());
                descuento.setPlaca(txtPlaca.getText().toString().toUpperCase());
                descuento.setMonto(Double.valueOf(txtMonto.getText().toString()));
                Call<String> call = descuentoRepository.saveDescuento(descuento, sharedPreferences.getString("token", null));
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        Toast.makeText(getContext(), "Se agrego el descuento", Toast.LENGTH_LONG).show();
                        String msg = getResources().getString(R.string.msjwhtsp) + " " + descuento.getPlaca();
                        limpiar();
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        String uri = "whatsapp://send?text=" + msg;
                        intent.setData(Uri.parse(uri));
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        t.printStackTrace();
                        Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

            }
        });
    }

    private boolean validarCampos() {
        boolean pasa = false;
        if (txtPlaca.getText() != null && !txtPlaca.getText().toString().isEmpty()) {
            pasa = true;
        } else {
            txtPlaca.setError("Ingrese placa");
        }

        if (txtMonto.getText() != null && !txtMonto.getText().toString().isEmpty()) {
            pasa = true;
        } else {
            txtMonto.setError("Ingrese Monto");
        }
        return pasa;
    }

    private void limpiar() {
        spPlanta.setSelection(0);
        spTipoPago.setSelection(0);
        spConcepto.setSelection(0);
        txtPlaca.getText().clear();
        txtMonto.getText().clear();
    }
}
