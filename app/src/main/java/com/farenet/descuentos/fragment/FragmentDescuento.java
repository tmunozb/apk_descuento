package com.farenet.descuentos.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.farenet.descuentos.R;
import com.farenet.descuentos.adapter.SpinerAdapter;
import com.farenet.descuentos.domain.Conceptoinspeccion;
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.domain.TipoPagoDescuento;
import com.farenet.descuentos.sql.QueryRealm;

import java.util.List;

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
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        plantas = QueryRealm.getAllPlantas();
        spPlantaAdapter = new SpinerAdapter<Planta>(getContext(), plantas);
        spPlanta.setAdapter(spPlantaAdapter);

        conceptoinspeccions = QueryRealm.getAllConcepto();
        spConceptoAdapter = new SpinerAdapter<Conceptoinspeccion>(getContext(), conceptoinspeccions);
        spConcepto.setAdapter(spConceptoAdapter);

        tipoPagoDescuentos = QueryRealm.getAllTipoPagos();
        spTipoPagoAdapter = new SpinerAdapter<TipoPagoDescuento>(getContext(), tipoPagoDescuentos);
        spTipoPago.setAdapter(spTipoPagoAdapter);




        event();
    }



    private void event() {

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }
}
