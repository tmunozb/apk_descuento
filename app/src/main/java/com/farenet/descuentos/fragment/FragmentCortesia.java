package com.farenet.descuentos.fragment;

import android.content.Intent;
import android.net.Uri;
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
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.sql.QueryRealm;

import java.util.List;

public class FragmentCortesia extends Fragment {

    private Spinner spPlanta;
    private EditText txtPlaca;
    private Button btnGuardar;

    private List<Planta> plantas;
    private SpinerAdapter<Planta> spPlantaAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cortesia_fragment, container, false);
        spPlanta = (Spinner) view.findViewById(R.id.sp_planta_cort);
        txtPlaca = (EditText) view.findViewById(R.id.txtPlaca_cort);
        btnGuardar = (Button) view.findViewById(R.id.btnGuardar_cort);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        plantas = QueryRealm.getAllPlantas();
        spPlantaAdapter = new SpinerAdapter<Planta>(getContext(), plantas);
        spPlanta.setAdapter(spPlantaAdapter);


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
