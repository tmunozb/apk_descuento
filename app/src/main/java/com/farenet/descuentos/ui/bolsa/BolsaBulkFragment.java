package com.farenet.descuentos.ui.bolsa;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.R;
import com.farenet.descuentos.API.Actual.DTO.bolsa.BolsaBulkReq;
import com.farenet.descuentos.API.Actual.DTO.bolsa.BolsaBulkRsp;
import com.farenet.descuentos.API.Actual.DTO.maestros.AccesoPlantaDto;
import com.farenet.descuentos.Core.Network.NewApiClient;
import com.farenet.descuentos.Core.Storage.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BolsaBulkFragment extends Fragment {

    private static final String[] ESTADOS = new String[]{"ACTIVO", "CERRADO"};

    private EditText etPeriodo;
    private AutoCompleteTextView actEstado;
    private RecyclerView rv;
    private Button btnMarcar, btnDeseleccionar, btnGuardar;
    private ProgressBar progress;

    private BolsaBulkAdapter adapter;
    private SessionManager session;
    private Call<BolsaBulkRsp> bulkCall;

    public static BolsaBulkFragment newInstance() {
        return new BolsaBulkFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bolsa_bulk, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        session = new SessionManager(requireContext());

        MaterialToolbar tb = view.findViewById(R.id.toolbar);
        if (tb != null) {
            tb.setNavigationOnClickListener(v ->
                    requireActivity().getOnBackPressedDispatcher().onBackPressed()
            );
        }

        etPeriodo       = view.findViewById(R.id.et_periodo);
        actEstado       = view.findViewById(R.id.act_estado);
        rv              = view.findViewById(R.id.rv_plantas);
        btnMarcar       = view.findViewById(R.id.btn_marcar_todas);
        btnDeseleccionar= view.findViewById(R.id.btn_deseleccionar);
        btnGuardar      = view.findViewById(R.id.btn_guardar);
        progress        = view.findViewById(R.id.progress);

        actEstado.setAdapter(new android.widget.ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, ESTADOS));
        actEstado.setText("ACTIVO", false);

        etPeriodo.setText(yyyymmNow());

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BolsaBulkAdapter();
        rv.setAdapter(adapter);

        cargarPlantas();

        btnMarcar.setOnClickListener(v -> adapter.marcarTodas(true));
        btnDeseleccionar.setOnClickListener(v -> adapter.marcarTodas(false));
        btnGuardar.setOnClickListener(v -> guardar());
    }

    private void cargarPlantas() {
        List<AccesoPlantaDto> accesos = session.getAccesos();
        if (accesos == null) accesos = new ArrayList<>();
        adapter.setData(accesos);
    }

    private String yyyymmNow() {
        return new SimpleDateFormat("yyyyMM", Locale.getDefault()).format(new Date());
    }

    private void guardar() {
        String periodo = safe(etPeriodo.getText() != null ? etPeriodo.getText().toString() : "");
        String estado  = safe(actEstado.getText() != null ? actEstado.getText().toString() : "");

        if (periodo.length() != 6 || !TextUtils.isDigitsOnly(periodo)) {
            Toast.makeText(requireContext(), "Periodo inválido (formato YYYYMM)", Toast.LENGTH_LONG).show();
            return;
        }
        if (!"ACTIVO".equalsIgnoreCase(estado) && !"CERRADO".equalsIgnoreCase(estado)) {
            Toast.makeText(requireContext(), "Estado inválido", Toast.LENGTH_LONG).show();
            return;
        }

        List<BolsaBulkAdapter.Row> seleccionados = adapter.getSeleccionadosConMonto();
        if (seleccionados.isEmpty()) {
            Toast.makeText(requireContext(), "No hay plantas seleccionadas con monto", Toast.LENGTH_LONG).show();
            return;
        }

        BolsaBulkReq req = new BolsaBulkReq();
        req.periodo = periodo;
        req.estado  = estado.toUpperCase(Locale.ROOT);
        for (BolsaBulkAdapter.Row r : seleccionados) {
            req.items.add(new BolsaBulkReq.Item(r.plantaKey, r.montoTope));
        }

        showLoading(true);
        if (bulkCall != null) bulkCall.cancel();
        bulkCall = NewApiClient.bolsa().bulkUpsert(req);
        bulkCall.enqueue(new Callback<BolsaBulkRsp>() {
            @Override public void onResponse(Call<BolsaBulkRsp> call, Response<BolsaBulkRsp> response) {
                showLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(),
                            "No se pudo guardar (" + response.code() + ")", Toast.LENGTH_LONG).show();
                    return;
                }
                BolsaBulkRsp rsp = response.body();
                Toast.makeText(requireContext(),
                        "OK: " + rsp.count + " bolsas procesadas para " + rsp.periodo,
                        Toast.LENGTH_LONG).show();
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
            @Override public void onFailure(Call<BolsaBulkRsp> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(requireContext(), "Error de red al guardar", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        if (progress != null) progress.setVisibility(show ? View.VISIBLE : View.GONE);
        if (btnGuardar != null) btnGuardar.setEnabled(!show);
        if (btnMarcar != null) btnMarcar.setEnabled(!show);
        if (btnDeseleccionar != null) btnDeseleccionar.setEnabled(!show);
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bulkCall != null) bulkCall.cancel();
        rv.setAdapter(null); // evitar leaks
    }
}
