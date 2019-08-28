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
import com.farenet.descuentos.domain.Cortesia;
import com.farenet.descuentos.domain.Descuento;
import com.farenet.descuentos.domain.Planta;
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
public class FragmentCortesia extends Fragment {

    private Spinner spPlanta;
    private EditText txtPlaca;
    private Button btnGuardar;

    private List<Planta> plantas;
    private SpinerAdapter<Planta> spPlantaAdapter;

    private DescuentoRepository descuentoRepository;
    private Cortesia cortesia = new Cortesia();

    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cortesia_fragment, container, false);
        spPlanta = (Spinner) view.findViewById(R.id.sp_planta_cort);
        txtPlaca = (EditText) view.findViewById(R.id.txtPlaca_cort);
        btnGuardar = (Button) view.findViewById(R.id.btnGuardar_cort);

        descuentoRepository = Constante.getDescuentoRepository();
        sharedPreferences = this.getActivity().getSharedPreferences(Constante.TOKEN, Context.MODE_PRIVATE);
        plantas = QueryRealm.getAllPlantas();
        spPlantaAdapter = new SpinerAdapter<Planta>(getContext(), plantas);
        spPlanta.setAdapter(spPlantaAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
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
                final Cortesia cortesia = new Cortesia();
                cortesia.setPlaca(txtPlaca.getText().toString().toUpperCase());
                cortesia.setPlanta(planta.getKey());
                Call<String> call = descuentoRepository.saveCortesia(cortesia, sharedPreferences.getString("token", null));
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        Toast.makeText(getContext(), "Se agrego el descuento", Toast.LENGTH_LONG).show();
                        String msg = getResources().getString(R.string.msjwhtsp_corte) + " " + cortesia.getPlaca();
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
        if (txtPlaca.getText() != null && !txtPlaca.getText().toString().isEmpty()) {
            return true;
        } else {
            txtPlaca.setError("Ingrese placa");
            return false;
        }
    }

    private void limpiar() {
        txtPlaca.getText().clear();
    }
}
