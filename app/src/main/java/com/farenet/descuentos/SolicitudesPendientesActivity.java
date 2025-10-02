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

import com.farenet.descuentos.config.Constante;
import com.farenet.descuentos.domain.Descuento;
import com.farenet.descuentos.models.newapi.AccesoPlantaDto;
import com.farenet.descuentos.models.newapi.UsuarioPerfil;
import com.farenet.descuentos.models.req.AprobarSolicitudReq;
import com.farenet.descuentos.models.req.RechazarSolicitudReq;
import com.farenet.descuentos.models.rsp.AccionSolicitudRsp;
import com.farenet.descuentos.models.rsp.SolicitudPendienteDto;
import com.farenet.descuentos.models.ui.SolicitudUI;
import com.farenet.descuentos.network.newapi.NewApiClient;
import com.farenet.descuentos.repository.DescuentoRepository;
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
import android.content.SharedPreferences;

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

    private static final String[] TIPOS = new String[]{"Todos", "Descuento", "Cortesía"};
    private static final String[] ESTADOS = new String[]{"Pendiente", "Aprobada", "Rechazada"};

    private DescuentoRepository descuentoRepository;
    private SharedPreferences sharedPreferences;

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
        progress  = findViewById(android.R.id.progress);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendienteSolicitudAdapter(data, this);
        rv.setAdapter(adapter);

        cargarPlantasDesdeSesion();
        actPlanta.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                plantasAdapterArray()));
        actTipo.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, TIPOS));
        actEstado.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, ESTADOS));

        actPlanta.setText("Todas", false);
        actTipo.setText("Todos", false);
        actEstado.setText("Pendiente", false);

        actPlanta.setOnItemClickListener((p, v, pos, id) -> filtrar());
        actTipo.setOnItemClickListener((p, v, pos, id) -> filtrar());
        actEstado.setOnItemClickListener((p, v, pos, id) -> filtrar());

        descuentoRepository = Constante.getDescuentoRepository();
        sharedPreferences   = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);

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
                    String tipoNice = "DESCUENTO".equalsIgnoreCase(d.tipo) ? "Descuento"
                            : "CORTESIA".equalsIgnoreCase(d.tipo) ? "Cortesía" : safe(d.tipo);
                    String estadoNice = mapEstadoUI(d.estado);
                    String fechaNice  = niceDate(d.creado_en);

                    String idStr          = s(d.id);
                    String plantaKeyStr   = s(d.planta_key);
                    String conceptoKeyStr = s(d.concepto_key);
                    String tipoPagoKeyStr = s(d.tipo_pago_key);
                    Double montoVal       = d.monto != null ? d.monto : 0d;
                    String tipoDescVal    = s(d.tipo_desc);
                    String campaniaVal    = s(d.campania_nombre);

                    SolicitudUI ui = new SolicitudUI(
                            d.codigo,
                            tipoNice,
                            s(d.placa),
                            s(d.planta_nombre),
                            s(d.motivo),
                            estadoNice,
                            idStr,
                            fechaNice,
                            plantaKeyStr,
                            conceptoKeyStr,
                            tipoPagoKeyStr,
                            montoVal,
                            tipoDescVal,
                            campaniaVal
                    );

                    all.add(ui);
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

    private void generarDescuentoSiCorresponde(SolicitudUI s, String aprobNom, AccionSolicitudRsp rsp) {
        String estadoBk = rsp != null ? rsp.estado : null;
        boolean aprobadaOk = "APROBADA".equalsIgnoreCase(estadoBk);
        if (!aprobadaOk) return; // sólo cuando queda aprobada final
        generarDescuentoDesdeSolicitud(s, aprobNom);
    }

    private void generarDescuentoDesdeSolicitud(SolicitudUI s, String aprobNom) {
        if (!s.isCompletaParaDescuento()) {
            Toast.makeText(this, "Solicitud incompleta para generar descuento", Toast.LENGTH_LONG).show();
            return;
        }

        Descuento d = new Descuento();
        d.setConceptoinspeccion(s.conceptoKey);
        d.setPlanta(s.plantaKey);
        d.setTipoPagoDescuento(s.tipoPagoKey);
        d.setPlaca(s.placa);
        d.setMonto(s.monto != null ? s.monto : 0d);
        d.setMotivo(!TextUtils.isEmpty(s.motivo) ? s.motivo : "Aprobado desde solicitudes");
        d.setAutoriza(!TextUtils.isEmpty(aprobNom) ? aprobNom : "APROBADOR");

        if ("CAMPAÑA".equalsIgnoreCase(s.tipoDesc) && !TextUtils.isEmpty(s.campaniaNombre)) {
            d.setNomDescuento(s.campaniaNombre);
        }

        String token = sharedPreferences != null ? sharedPreferences.getString("token", null) : null;
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(this, "Sesión no válida para registrar descuento", Toast.LENGTH_LONG).show();
            return;
        }

        retrofit2.Call<String> call;
        if ("AUTORIZADO".equalsIgnoreCase(s.tipoDesc)) {
            call = descuentoRepository.saveDescuento(d, token);
        } else if ("CARTA".equalsIgnoreCase(s.tipoDesc)) {
            call = descuentoRepository.saveCarta(d, token);
        } else if ("CAMPAÑA".equalsIgnoreCase(s.tipoDesc)) {
            call = descuentoRepository.saveCampana(d, token);
        } else {
            call = descuentoRepository.saveDescuento(d, token);
        }

        call.enqueue(new retrofit2.Callback<String>() {
            @Override
            public void onResponse(retrofit2.Call<String> c, retrofit2.Response<String> rsp) {
                if (rsp.isSuccessful()) {
                    Toast.makeText(SolicitudesPendientesActivity.this,
                            "Descuento registrado correctamente", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SolicitudesPendientesActivity.this,
                            "Aprobada, pero falló el registro de descuento (" + rsp.code() + ")",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<String> c, Throwable t) {
                Toast.makeText(SolicitudesPendientesActivity.this,
                        "Aprobada, error registrando descuento: " +
                                (t.getMessage()!=null?t.getMessage():""),
                        Toast.LENGTH_LONG).show();
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

            if (!TextUtils.isEmpty(estado) && !"Todos".equalsIgnoreCase(estado)) {
                if ("Pendiente".equalsIgnoreCase(estado)) {
                    if (!isPendienteGrupo(s.estado)) continue;
                } else if (!s.estado.equalsIgnoreCase(estado)) {
                    continue;
                }
            }
            data.add(s);
        }
        adapter.notifyDataSetChanged();
    }

    private boolean isPendienteGrupo(String estadoUi) {
        if (estadoUi == null) return true;
        String e = estadoUi.toLowerCase();
        return e.startsWith("pendiente");
    }

    private String mapEstadoUI(String backendEstado) {
        if (backendEstado == null) return "Pendiente";
        switch (backendEstado.toUpperCase()) {
            case "APROBADA": return "Aprobada";
            case "RECHAZADA": return "Rechazada";
            case "PENDIENTE_AUT": return "Pendiente (Autorización)";
            case "OBSERVADA": return "Pendiente (Observada)";
            case "ENVIADA":
            default: return "Pendiente";
        }
    }

    @Override
    public void onAprobar(SolicitudUI s) {

        if ("Pendiente (Autorización)".equalsIgnoreCase(s.estado)) {
            // Segunda aprobación
            new AlertDialog.Builder(this)
                    .setTitle("Autorización final")
                    .setMessage("La solicitud está pendiente de autorización.\n¿Cómo deseas proceder?")
                    .setPositiveButton("Autorizar sin bolsa", (d, w) -> autorizar(s, "AUTORIZADO"))
                    .setNeutralButton("Intentar con bolsa", (d, w) -> autorizar(s, "BOLSA"))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Aprobar solicitud")
                .setMessage("¿Aprobar " + s.codigo + "?")
                .setPositiveButton("Aprobar", (d, w) -> aprobar(s))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void autorizar(SolicitudUI s, String modo) {
        showLoading(true);

        UsuarioPerfil up = session.getPerfil();
        String user = (up != null && !TextUtils.isEmpty(up.username)) ? up.username : session.getUsername();
        String nom  = null;
        if (up != null) {
            String full = (safe(up.nombres) + " " + safe(up.apellidos)).trim();
            if (!TextUtils.isEmpty(full)) nom = full;
        }
        if (TextUtils.isEmpty(nom)) nom = user;

        // ⬇️ Variables finales para usarlas dentro del Callback
        final String nomFinal  = nom;
        final String userFinal = user;
        final String modoFinal = modo;

        // ===== Variante A: tu req tiene constructor (user, id, nom, modo)
        retrofit2.Call<AccionSolicitudRsp> call = NewApiClient.get().autorizarSolicitud(
                s.id,
                new com.farenet.descuentos.models.req.AutorizarSolicitudReq(userFinal, null, nomFinal, modoFinal)
        );

        // ===== Variante B: si tu req NO tiene constructor con args, usa setters
        // com.farenet.descuentos.models.req.AutorizarSolicitudReq body = new com.farenet.descuentos.models.req.AutorizarSolicitudReq();
        // body.setAprobado_por_username(userFinal);
        // body.setAprobado_por_id(null);
        // body.setAprobado_por_nombre(nomFinal);
        // body.setModo(modoFinal);
        // retrofit2.Call<AccionSolicitudRsp> call = NewApiClient.get().autorizarSolicitud(s.id, body);

        call.enqueue(new Callback<AccionSolicitudRsp>() {
            @Override public void onResponse(Call<AccionSolicitudRsp> call, Response<AccionSolicitudRsp> rsp) {
                showLoading(false);
                if (!rsp.isSuccessful() || rsp.body()==null) {
                    Toast.makeText(SolicitudesPendientesActivity.this, "No se pudo autorizar", Toast.LENGTH_LONG).show();
                    return;
                }
                String est = rsp.body().estado != null ? rsp.body().estado : "ENVIADA";
                s.estado = mapEstadoUI(est);
                adapter.notifyDataSetChanged();

                if ("APROBADA".equalsIgnoreCase(est)) {
                    Toast.makeText(SolicitudesPendientesActivity.this, "Aprobada", Toast.LENGTH_SHORT).show();
                    // ✅ ahora sí existe nomFinal y es final
                    generarDescuentoSiCorresponde(s, nomFinal, rsp.body());
                } else {
                    Toast.makeText(SolicitudesPendientesActivity.this, "Sigue pendiente de autorización", Toast.LENGTH_LONG).show();
                }
                cargarPendientes();
            }
            @Override public void onFailure(Call<AccionSolicitudRsp> call, Throwable t) {
                showLoading(false);
                Toast.makeText(SolicitudesPendientesActivity.this, "Error al autorizar", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void aprobar(SolicitudUI s) {
        showLoading(true);

        UsuarioPerfil up = session.getPerfil();
        String aprobUser = (up != null && !TextUtils.isEmpty(up.username)) ? up.username : session.getUsername();

        String aprobNom = null;
        if (up != null) {
            String nom = safe(up.nombres);
            String ape = safe(up.apellidos);
            String full = (nom + " " + ape).trim();
            if (!TextUtils.isEmpty(full)) aprobNom = full;
        }
        if (TextUtils.isEmpty(aprobNom)) aprobNom = aprobUser;

        final String aprobNomFinal = aprobNom;

        // ===== Variante A: tu req tiene constructor (user, id, nombre)
        AprobarSolicitudReq req = new AprobarSolicitudReq(aprobUser, null, aprobNomFinal);

        // ===== Variante B: si NO tiene ese constructor, usa setters
        // AprobarSolicitudReq req = new AprobarSolicitudReq();
        // req.setAprobado_por_username(aprobUser);
        // req.setAprobado_por_id(null);
        // req.setAprobado_por_nombre(aprobNomFinal);

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
                AccionSolicitudRsp rsp = response.body();

                String nuevoEstado = mapEstadoUI(rsp.estado != null ? rsp.estado : "ENVIADA");
                s.estado = nuevoEstado;
                adapter.notifyDataSetChanged();

                if ("Aprobada".equalsIgnoreCase(nuevoEstado)) {
                    Toast.makeText(SolicitudesPendientesActivity.this, "Aprobada", Toast.LENGTH_SHORT).show();
                } else if ("Pendiente (Autorización)".equalsIgnoreCase(nuevoEstado)) {
                    Toast.makeText(SolicitudesPendientesActivity.this, "Sin saldo: queda pendiente de autorización", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SolicitudesPendientesActivity.this, "Aprobación realizada", Toast.LENGTH_SHORT).show();
                }

                generarDescuentoSiCorresponde(s, aprobNomFinal, rsp);
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

    private String s(Object o) { return o == null ? "" : String.valueOf(o).trim(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listarCall != null) listarCall.cancel();
        if (aprobarCall != null) aprobarCall.cancel();
        if (rechazarCall != null) rechazarCall.cancel();
    }
}
