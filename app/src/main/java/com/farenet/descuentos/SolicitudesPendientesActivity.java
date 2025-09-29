package com.farenet.descuentos;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farenet.descuentos.models.newapi.AccesoPlantaDto;
import com.farenet.descuentos.models.newapi.UsuarioPerfil;
import com.farenet.descuentos.models.req.AprobarSolicitudReq;
import com.farenet.descuentos.models.req.RechazarSolicitudReq;
import com.farenet.descuentos.models.rsp.AccionSolicitudRsp;
import com.farenet.descuentos.models.rsp.SolicitudPendienteDto;
import com.farenet.descuentos.models.ui.SolicitudUI;
import com.farenet.descuentos.network.newapi.NewApiClient;
import com.farenet.descuentos.repository.SessionManager;
import com.farenet.descuentos.ui.PendienteSolicitudAdapter;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SolicitudesPendientesActivity extends AppCompatActivity
        implements PendienteSolicitudAdapter.Actions {

    private AutoCompleteTextView actPlanta, actTipo, actEstado;
    private RecyclerView rv;
    private View progress;

    private PendienteSolicitudAdapter adapter;
    private final List<SolicitudUI> data = new ArrayList<>();
    private final List<SolicitudUI> all  = new ArrayList<>();

    private SessionManager session;
    private final List<String> plantasNombres = new ArrayList<>();
    private final Map<String, String> plantaNombreToKey = new LinkedHashMap<>();

    private Call<List<SolicitudPendienteDto>> listarCall;
    private Call<AccionSolicitudRsp> aprobarCall;
    private Call<AccionSolicitudRsp> rechazarCall;

    // opciones de filtros
    private static final String[] TIPOS = new String[]{"Todos", "Descuento", "Cortesía"};
    private static final String[] ESTADOS = new String[]{"Pendiente"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitudes_pendientes);

        session = new SessionManager(getApplicationContext());

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        actPlanta = findViewById(R.id.act_planta);
        actTipo   = findViewById(R.id.act_tipo);
        actEstado = findViewById(R.id.act_estado);
        rv        = findViewById(R.id.rv_pendientes);
        progress  = findViewById(android.R.id.progress); // opcional: agrega un ProgressBar en el layout si quieres

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendienteSolicitudAdapter(data, this);
        rv.setAdapter(adapter);

        // Plantas desde sesión (usuario logueado)
        cargarPlantasDesdeSesion();
        actPlanta.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                plantasAdapterArray()));
        actTipo.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, TIPOS));
        actEstado.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, ESTADOS));

        // defaults
        actPlanta.setText("Todas", false);
        actTipo.setText("Todos", false);
        actEstado.setText("Pendiente", false);

        // listeners
        actPlanta.setOnItemClickListener((p, v, pos, id) -> filtrar());
        actTipo.setOnItemClickListener((p, v, pos, id) -> filtrar());
        actEstado.setOnItemClickListener((p, v, pos, id) -> filtrar());

        // Cargar de API
        cargarPendientes();
    }

    private void cargarPlantasDesdeSesion() {
        plantasNombres.clear();
        plantaNombreToKey.clear();
        plantasNombres.add("Todas");

        List<AccesoPlantaDto> accesos = session.getAccesos();
        if (accesos == null) return;
        for (AccesoPlantaDto a : accesos) {
            if (a == null) continue;
            String nombre = safe(a.planta);
            String key    = safe(a.key);
            if (TextUtils.isEmpty(nombre)) continue;
            if (!plantaNombreToKey.containsKey(nombre)) {
                plantaNombreToKey.put(nombre, key);
                plantasNombres.add(nombre);
            }
        }
    }

    private String[] plantasAdapterArray() {
        return plantasNombres.toArray(new String[0]);
    }

    private void cargarPendientes() {
        showLoading(true);
        if (listarCall != null) listarCall.cancel();
        listarCall = NewApiClient.get().listarPendientes();
        listarCall.enqueue(new Callback<List<SolicitudPendienteDto>>() {
            @Override
            public void onResponse(Call<List<SolicitudPendienteDto>> call, Response<List<SolicitudPendienteDto>> response) {
                showLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(SolicitudesPendientesActivity.this, "No se pudo cargar pendientes", Toast.LENGTH_LONG).show();
                    return;
                }
                all.clear();
                for (SolicitudPendienteDto d : response.body()) {
                    // tipo backend en mayúsculas → UI amigable
                    String tipoNice = "DESCUENTO".equalsIgnoreCase(d.tipo) ? "Descuento"
                            : "CORTESIA".equalsIgnoreCase(d.tipo) ? "Cortesía" : safe(d.tipo);
                    String estadoNice = mapEstadoUI(d.estado);
                    String fechaNice  = niceDate(d.creado_en);
                    all.add(new SolicitudUI(
                            d.codigo,
                            tipoNice,
                            safe(d.placa),
                            safe(d.planta_nombre),
                            safe(d.motivo),
                            estadoNice,
                            d.id, // guardamos id para aprobar/rechazar
                            fechaNice
                    ));
                }
                filtrar();
            }

            private String niceDate(String iso) {
                if (iso == null || iso.isEmpty()) return "";
                try {
                    String s = iso.replace('T',' ');
                    if (s.length() >= 16) return s.substring(0,16);
                    return s;
                } catch (Exception e) {
                    return iso;
                }
            }

            @Override
            public void onFailure(Call<List<SolicitudPendienteDto>> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(SolicitudesPendientesActivity.this, "Error al cargar pendientes", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void filtrar() {
        String planta = safe(actPlanta.getText() != null ? actPlanta.getText().toString() : "");
        String tipo   = safe(actTipo.getText()   != null ? actTipo.getText().toString()   : "");
        String estado = safe(actEstado.getText() != null ? actEstado.getText().toString() : "");

        data.clear();
        for (SolicitudUI s : all) {
            if (!("Todas".equalsIgnoreCase(planta) || s.planta.equalsIgnoreCase(planta))) continue;
            if (!("Todos".equalsIgnoreCase(tipo) || s.tipo.equalsIgnoreCase(tipo))) continue;

            // Mapeo de “Pendiente” UI a estados backend ENVIADA/OBSERVADA (ya mapeado en UI a "Pendiente")
            if (!TextUtils.isEmpty(estado) && !"Todos".equalsIgnoreCase(estado)) {
                if (!s.estado.equalsIgnoreCase(estado)) continue;
            }
            data.add(s);
        }
        adapter.notifyDataSetChanged();
    }

    private String mapEstadoUI(String backendEstado) {
        if (backendEstado == null) return "Pendiente";
        switch (backendEstado.toUpperCase()) {
            case "APROBADA": return "Aprobada";
            case "RECHAZADA": return "Rechazada";
            case "ENVIADA":
            case "OBSERVADA":
            default: return "Pendiente";
        }
    }

    @Override
    public void onAprobar(SolicitudUI s) {
        new AlertDialog.Builder(this)
                .setTitle("Aprobar solicitud")
                .setMessage("¿Aprobar " + s.codigo + "?")
                .setPositiveButton("Aprobar", (d, w) -> aprobar(s))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void aprobar(SolicitudUI s) {
        showLoading(true);

        // Datos del aprobador desde sesión
        UsuarioPerfil up = session.getPerfil();
        String aprobUser = (up != null && !TextUtils.isEmpty(up.username))
                ? up.username
                : session.getUsername(); // si tienes este helper, si no, deja null/"" y backend valida

        String aprobNom = null;
        if (up != null) {
            String nom = safe(up.nombres);
            String ape = safe(up.apellidos);
            String full = (nom + " " + ape).trim();
            if (!TextUtils.isEmpty(full)) aprobNom = full;
        }
        if (TextUtils.isEmpty(aprobNom)) aprobNom = aprobUser;

        // No tenemos 'id' en UsuarioPerfil → lo enviamos null
        Integer aprobId = null;

        AprobarSolicitudReq req = new AprobarSolicitudReq(aprobUser, aprobId, aprobNom);

        if (aprobarCall != null) aprobarCall.cancel();
        aprobarCall = NewApiClient.get().aprobarSolicitud(s.id, req);
        aprobarCall.enqueue(new Callback<AccionSolicitudRsp>() {
            @Override
            public void onResponse(Call<AccionSolicitudRsp> call, Response<AccionSolicitudRsp> response) {
                showLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(SolicitudesPendientesActivity.this, "No se pudo aprobar", Toast.LENGTH_LONG).show();
                    return;
                }
                s.estado = "Aprobada";
                adapter.notifyDataSetChanged();
                Toast.makeText(SolicitudesPendientesActivity.this, "Aprobada", Toast.LENGTH_SHORT).show();
                cargarPendientes();
            }

            @Override
            public void onFailure(Call<AccionSolicitudRsp> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(SolicitudesPendientesActivity.this, "Error al aprobar", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRechazar(SolicitudUI s) {
        // simple prompt de motivo (rápido); puedes reemplazar por un diálogo con input
        new AlertDialog.Builder(this)
                .setTitle("Rechazar solicitud")
                .setMessage("¿Rechazar " + s.codigo + "?\nMotivo: se registrará como 'No corresponde'.")
                .setPositiveButton("Rechazar", (d, w) -> rechazar(s, "No corresponde"))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void rechazar(SolicitudUI s, String motivo) {
        showLoading(true);

        RechazarSolicitudReq req = new RechazarSolicitudReq(motivo);
        if (rechazarCall != null) rechazarCall.cancel();
        rechazarCall = NewApiClient.get().rechazarSolicitud(s.id, req);
        rechazarCall.enqueue(new Callback<AccionSolicitudRsp>() {
            @Override
            public void onResponse(Call<AccionSolicitudRsp> call, Response<AccionSolicitudRsp> response) {
                showLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(SolicitudesPendientesActivity.this, "No se pudo rechazar", Toast.LENGTH_LONG).show();
                    return;
                }
                s.estado = "Rechazada";
                adapter.notifyDataSetChanged();
                Toast.makeText(SolicitudesPendientesActivity.this, "Rechazada", Toast.LENGTH_SHORT).show();
                cargarPendientes();
            }

            @Override
            public void onFailure(Call<AccionSolicitudRsp> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(SolicitudesPendientesActivity.this, "Error al rechazar", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        if (progress != null) progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listarCall != null) listarCall.cancel();
        if (aprobarCall != null) aprobarCall.cancel();
        if (rechazarCall != null) rechazarCall.cancel();
    }
}
