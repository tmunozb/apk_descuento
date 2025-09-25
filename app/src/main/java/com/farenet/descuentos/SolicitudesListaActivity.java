package com.farenet.descuentos;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.models.ui.SolicitudUI;
import com.farenet.descuentos.ui.SolicitudSimpleAdapter;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class SolicitudesListaActivity extends AppCompatActivity {

    private AutoCompleteTextView actTipo, actEstado;
    private TextView etBuscar, tvEmpty;
    private RecyclerView rv;
    private SolicitudSimpleAdapter adapter;

    private final List<SolicitudUI> all  = new ArrayList<>();
    private final List<SolicitudUI> data = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitudes_lista);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        actTipo   = findViewById(R.id.act_tipo);
        actEstado = findViewById(R.id.act_estado);
        etBuscar  = findViewById(R.id.et_buscar);
        tvEmpty   = findViewById(R.id.tv_empty);

        rv = findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SolicitudSimpleAdapter(data, s -> {
            // TODO: abrir detalle si lo deseas
        });
        rv.setAdapter(adapter);

        // Adapters de filtros (mock)
        actTipo.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new String[]{"Todos", "Descuento", "Cortesía"}));
        actEstado.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new String[]{"Todos", "Pendiente", "Aprobada", "Rechazada"}));

        actTipo.setText("Todos", false);
        actEstado.setText("Todos", false);

        // Datos demo (reemplazar por API de "mis solicitudes")
        all.add(new SolicitudUI("DESC-0101","Descuento","ABC123","Surco","Regularización","Pendiente","2025-09-20"));
        all.add(new SolicitudUI("CORT-0102","Cortesía","DEF456","Ate","Campaña","Aprobada","2025-09-18"));
        all.add(new SolicitudUI("DESC-0103","Descuento","XYZ999","Arequipa","Error carga","Rechazada","2025-09-15"));

        filtrar();

        // Listeners
        actTipo.setOnItemClickListener((p, v, i, id) -> filtrar());
        actEstado.setOnItemClickListener((p, v, i, id) -> filtrar());
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { filtrar(); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void filtrar() {
        String tipo   = value(actTipo.getText());
        String estado = value(actEstado.getText());
        String q      = value(etBuscar.getText()).toLowerCase();

        data.clear();
        for (SolicitudUI s : all) {
            if (!("Todos".equals(tipo)   || s.tipo.equalsIgnoreCase(tipo))) continue;
            if (!("Todos".equals(estado) || s.estado.equalsIgnoreCase(estado))) continue;
            if (!q.isEmpty()) {
                String comp = (s.codigo + " " + s.placa).toLowerCase();
                if (!comp.contains(q)) continue;
            }
            data.add(s);
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(data.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private String value(CharSequence cs) { return cs == null ? "" : cs.toString().trim(); }
}
