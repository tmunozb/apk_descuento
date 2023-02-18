package com.farenet.descuentos.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import com.farenet.descuentos.domain.Conceptoinspeccion;
import com.farenet.descuentos.domain.Descuento;
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.domain.TipoPagoDescuento;
import com.farenet.descuentos.repository.DescuentoRepository;
import com.farenet.descuentos.sql.QueryRealm;

import java.util.ArrayList;
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
    private Spinner spTipoCampana;
    private Spinner spTipoDescuento;
    private Spinner spAutoriza;
    private EditText txtPlaca;
    private EditText txtMonto;
    private EditText txtMotivo;
    private Button btnGuardar;
    private List<Planta> plantas;
    private List<Conceptoinspeccion> conceptoinspeccions;
    private List<TipoPagoDescuento> tipoPagoDescuentos;
    private List<Autorizadores> autorizadores;
    private SpinerAdapter<Planta> spPlantaAdapter;
    private SpinerAdapter<Conceptoinspeccion> spConceptoAdapter;
    private SpinerAdapter<TipoPagoDescuento> spTipoPagoAdapter;
    private SpinerAdapter<Autorizadores> spAutorizadoresAdapter;
    private SpinerAdapter<String> spTipoCampanaAdapter;
    private SpinerAdapter<String> spTipoDescuentoAdapter;

    private DescuentoRepository descuentoRepository;
    private Descuento descuento = new Descuento();

    private SharedPreferences sharedPreferences;
    private String tipoDescSelect;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.descuento_fragment, container, false);
        spPlanta = (Spinner) view.findViewById(R.id.sp_planta_desc);
        spConcepto = (Spinner) view.findViewById(R.id.sp_concepto_desc);
        spTipoPago = (Spinner) view.findViewById(R.id.sp_tipopago_desc);
        txtPlaca = (EditText) view.findViewById(R.id.txtPlaca_desc);
        txtMonto = (EditText) view.findViewById(R.id.txtMonto_desc);
        txtMotivo = (EditText) view.findViewById(R.id.txtMotivo_desc) ;
        btnGuardar = (Button) view.findViewById(R.id.btnGuardar_desc);
        spTipoCampana = (Spinner) view.findViewById(R.id.sp_tipocampaña);
        spTipoDescuento = (Spinner) view.findViewById(R.id.sp_tipodescuento);
        spAutoriza = (Spinner) view.findViewById(R.id.sp_autoriza_desc);


        plantas = QueryRealm.getAllPlantas();
        spPlantaAdapter = new SpinerAdapter<Planta>(getContext(), plantas);
        spPlanta.setAdapter(spPlantaAdapter);

        autorizadores = QueryRealm.getAllAutorizadores();
        spAutorizadoresAdapter = new SpinerAdapter<Autorizadores>(getContext(), autorizadores);
        spAutoriza.setAdapter(spAutorizadoresAdapter);

        List<String> tipoDescuento = new ArrayList<>();
        tipoDescuento.add("AUTORIZADO");
        tipoDescuento.add("CARTA");
        tipoDescuento.add("CAMPAÑA");
        spTipoDescuentoAdapter = new SpinerAdapter<>(getContext(), tipoDescuento);
        spTipoDescuento.setAdapter(spTipoDescuentoAdapter);

        List<String> campañas = new ArrayList<>();
        campañas.add("COMPETENCIA");
        campañas.add("RECUPERADOS");
        campañas.add("REZAGADOS");
        campañas.add("CUPONIDAD");

        spTipoCampanaAdapter = new SpinerAdapter<>(getContext(), campañas);
        spTipoCampana.setAdapter(spTipoCampanaAdapter);

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

        spTipoDescuento.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                tipoDescSelect = adapterView.getItemAtPosition(i).toString();
                if ("CAMPAÑA".equalsIgnoreCase(tipoDescSelect)) {
                    spTipoCampana.setVisibility(View.VISIBLE);
                } else {
                    spTipoCampana.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

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
                descuento.setAutoriza(spAutoriza.getSelectedItem().toString());
                descuento.setMotivo(txtMotivo.getText().toString());
                if ("CAMPAÑA".equalsIgnoreCase(tipoDescSelect)) {
                    descuento.setNomDescuento(spTipoCampana.getSelectedItem().toString());
                }
                Call<String> call = null;
                if ("AUTORIZADO".equalsIgnoreCase(tipoDescSelect)) {
                    call = descuentoRepository.saveDescuento(descuento, sharedPreferences.getString("token", null));
                }else if ("CARTA".equalsIgnoreCase(tipoDescSelect)) {
                    call = descuentoRepository.saveCarta(descuento, sharedPreferences.getString("token", null));
                }else if ("CAMPAÑA".equalsIgnoreCase(tipoDescSelect)) {
                    call = descuentoRepository.saveCampana(descuento, sharedPreferences.getString("token", null));
                }

                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful() && response.code() == 200) {
                            Toast.makeText(getContext(), "Se agrego el descuento", Toast.LENGTH_LONG).show();
                            String msg = getResources().getString(R.string.msjwhtsp) + " " + descuento.getPlaca();
                            limpiar();
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            String uri = "whatsapp://send?text=" + msg;
                            intent.setData(Uri.parse(uri));
                            startActivity(intent);
                        } else {
                            Toast.makeText(getContext(), "Error al registrar", Toast.LENGTH_LONG).show();
                        }
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
            pasa = false;
            txtPlaca.setError("Ingrese placa");
        }

        if (txtMonto.getText() != null && !txtMonto.getText().toString().isEmpty()) {
            pasa = true;
        } else {
            pasa = false;
            txtMonto.setError("Ingrese Monto");
        }

        if (txtMotivo.getText() != null && !txtMotivo.getText().toString().isEmpty()) {
            pasa = true;
        } else {
            pasa = false;
            txtMotivo.setError("Ingrese Motivo");
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
