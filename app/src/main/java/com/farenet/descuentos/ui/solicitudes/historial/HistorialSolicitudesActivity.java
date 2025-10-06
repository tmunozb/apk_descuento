package com.farenet.descuentos.ui.solicitudes.historial;

import android.os.Bundle;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.solicitudes.model.SolicitudUI;
import com.farenet.descuentos.ui.solicitudes.comunes.adapter.SolicitudSimpleAdapter;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class HistorialSolicitudesActivity extends AppCompatActivity {

    private AutoCompleteTextView actTipo, actEstado;
    private TextView tvEmpty;
    private RecyclerView rv;
    private SolicitudSimpleAdapter adapter;

    private final List<SolicitudUI> all  = new ArrayList<>();
    private final List<SolicitudUI> data = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_solicitudes);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        actTipo   = findViewById(R.id.act_tipo);
        actEstado = findViewById(R.id.act_estado);
        tvEmpty   = findViewById(R.id.tv_empty);

        rv = findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SolicitudSimpleAdapter(data, s -> {
            // TODO: abrir detalle histórico si aplica
        });
        rv.setAdapter(adapter);

        // Adapters de filtros (historial suele mostrar Aprobadas/Rechazadas por defecto)
        actTipo.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new String[]{"Todos", "Descuento", "Cortesía"}));
        actEstado.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new String[]{"Aprobada", "Rechazada", "Todos"}));

        actTipo.setText("Todos", false);
        actEstado.setText("Aprobada", false); // por defecto mostrar aprobadas

        // Datos demo (reemplazar por API de historial)
        all.add(new SolicitudUI("DESC-0088","Descuento","AAA111","Surco","Campaña","Aprobada","2025-08-12"));
        all.add(new SolicitudUI("CORT-0066","Cortesía","BBB222","Ate","Evento","Rechazada","2025-08-10"));
        all.add(new SolicitudUI("DESC-0042","Descuento","CCC333","Arequipa","Regularización","Aprobada","2025-07-29"));

        filtrar();

        actTipo.setOnItemClickListener((p, v, i, id) -> filtrar());
        actEstado.setOnItemClickListener((p, v, i, id) -> filtrar());
    }

    private void filtrar() {
        String tipo   = value(actTipo.getText());
        String estado = value(actEstado.getText());

        data.clear();
        for (SolicitudUI s : all) {
            if (!("Todos".equals(tipo) || s.tipo.equalsIgnoreCase(tipo))) continue;

            // Si "Aprobada" o "Rechazada", filtramos exacto; si "Todos", entra cualquiera.
            if (!("Todos".equals(estado) || s.estado.equalsIgnoreCase(estado))) continue;

            // Historial — no mostramos Pendiente a menos que elija "Todos"
            if (!"Todos".equals(estado) && "Pendiente".equalsIgnoreCase(s.estado)) continue;

            data.add(s);
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(data.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private String value(CharSequence cs) { return cs == null ? "" : cs.toString().trim(); }
}
