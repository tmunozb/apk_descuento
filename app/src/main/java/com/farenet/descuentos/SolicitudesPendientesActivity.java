package com.farenet.descuentos;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.AutoCompleteTextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.models.ui.SolicitudUI;
import com.farenet.descuentos.ui.PendienteSolicitudAdapter;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class SolicitudesPendientesActivity extends AppCompatActivity
        implements PendienteSolicitudAdapter.Actions {

    private AutoCompleteTextView actPlanta, actTipo, actEstado;
    private RecyclerView rv;
    private PendienteSolicitudAdapter adapter;
    private List<SolicitudUI> data = new ArrayList<>();
    private List<SolicitudUI> all  = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitudes_pendientes);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        actPlanta = findViewById(R.id.act_planta);
        actTipo   = findViewById(R.id.act_tipo);
        actEstado = findViewById(R.id.act_estado);

        rv = findViewById(R.id.rv_pendientes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendienteSolicitudAdapter(data, this);
        rv.setAdapter(adapter);

        // Mocks de filtros
        actPlanta.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new String[]{"Todas", "Surco", "Ate", "Arequipa"}));
        actTipo.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new String[]{"Todos", "Descuento", "Cortesía"}));
        actEstado.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new String[]{"Pendiente", "Aprobada", "Rechazada"}));

        actPlanta.setText("Todas", false);
        actTipo.setText("Todos", false);
        actEstado.setText("Pendiente", false);

        // Carga demo (luego reemplazas por API)
        all.add(new SolicitudUI("DESC-0001","Descuento","ABC123","Surco","Regularización","Pendiente"));
        all.add(new SolicitudUI("CORT-0002","Cortesía","DEF456","Ate","Cortesía campaña","Pendiente"));
        all.add(new SolicitudUI("DESC-0003","Descuento","XYZ999","Arequipa","Error de caja","Pendiente"));

        filtrar();

        // listeners
        actPlanta.setOnItemClickListener((p, v, pos, id) -> filtrar());
        actTipo.setOnItemClickListener((p, v, pos, id) -> filtrar());
        actEstado.setOnItemClickListener((p, v, pos, id) -> filtrar());
    }

    private void filtrar() {
        String planta = safe(actPlanta.getText());
        String tipo   = safe(actTipo.getText());
        String estado = safe(actEstado.getText());

        data.clear();
        for (SolicitudUI s : all) {
            if (!("Todas".equals(planta) || s.planta.equalsIgnoreCase(planta))) continue;
            if (!("Todos".equals(tipo) || s.tipo.equalsIgnoreCase(tipo))) continue;
            if (!TextUtils.isEmpty(estado) && !s.estado.equalsIgnoreCase(estado)) continue;
            data.add(s);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onAprobar(SolicitudUI s) {
        new AlertDialog.Builder(this)
                .setTitle("Aprobar solicitud")
                .setMessage("¿Aprobar " + s.codigo + "?")
                .setPositiveButton("Aprobar", (d, w) -> {
                    // Aquí llamas a tu API de aprobación.
                    s.estado = "Aprobada";
                    filtrar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onRechazar(SolicitudUI s) {
        new AlertDialog.Builder(this)
                .setTitle("Rechazar solicitud")
                .setMessage("¿Rechazar " + s.codigo + "?")
                .setPositiveButton("Rechazar", (d, w) -> {
                    // Aquí llamas a tu API de rechazo.
                    s.estado = "Rechazada";
                    filtrar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String safe(CharSequence cs) { return cs == null ? "" : cs.toString().trim(); }
}
