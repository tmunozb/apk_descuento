package com.farenet.descuentos.ui.solicitudes.pendientes;

import android.content.Intent;
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

import com.farenet.descuentos.API.Actual.DTO.solicitudes.AutorizarSolicitudReq;
import com.farenet.descuentos.Core.config.Constante;
import com.farenet.descuentos.R;
import com.farenet.descuentos.domain.model.Descuento;
import com.farenet.descuentos.API.Actual.DTO.maestros.AccesoPlantaDto;
import com.farenet.descuentos.API.Actual.DTO.auth.UsuarioPerfil;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.AprobarSolicitudReq;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.RechazarSolicitudReq;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.AccionSolicitudRsp;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudPendienteDto;
import com.farenet.descuentos.ui.solicitudes.model.SolicitudUI;
import com.farenet.descuentos.Core.Network.NewApiClient;
import com.farenet.descuentos.API.Antigua.Service.DescuentoRepository;
import com.farenet.descuentos.Core.Storage.SessionManager;
import com.farenet.descuentos.ui.solicitudes.pendientes.adapter.PendienteSolicitudAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
    // Elimina la constante ESTADOS actual y agrega esto:
    private String[] estadosAdapterArray() {
        // Comercial SOLO ve “Pendiente (Autorización)”
        if (isSoloComercial()) {
            return new String[]{"Pendiente (Autorización)"};
        }

        List<String> estados = new ArrayList<>();
        estados.add("Pendiente"); // ENVIADA/OBSERVADA
        if (canVerPendienteAut()) {
            estados.add("Pendiente (Autorización)");
        }
        estados.add("Aprobada");
        estados.add("Rechazada");
        return estados.toArray(new String[0]);
    }


    private DescuentoRepository descuentoRepository;
    private SharedPreferences sharedPreferences;

    // Flags de rol (se calculan al iniciar)
    private boolean isSistemas = false;
    private boolean isOperaciones = false;
    private boolean isComercial = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitudes_pendientes);

        session = new SessionManager(getApplicationContext());
        computeRoleFlags();

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
                android.R.layout.simple_list_item_1, estadosAdapterArray()));

// Estado por defecto
        if (canVerTodasPlantas()) {
            actPlanta.setText("Todas", false);
        } else {
            // Si no hay “Todas”, selecciona la primera planta disponible si existe
            if (!plantasNombres.isEmpty()) {
                actPlanta.setText(plantasNombres.get(0), false);
            }
        }
        actTipo.setText("Todos", false);
        if (isSoloComercial()) {
            actEstado.setText("Pendiente (Autorización)", false);
        } else {
            actEstado.setText("Pendiente", false);
        }

        actPlanta.setOnItemClickListener((p, v, pos, id) -> filtrar());
        actTipo.setOnItemClickListener((p, v, pos, id) -> filtrar());
        actEstado.setOnItemClickListener((p, v, pos, id) -> filtrar());

        descuentoRepository = Constante.getDescuentoRepository();
        sharedPreferences   = getSharedPreferences(Constante.TOKEN, MODE_PRIVATE);

        // Bottom navigation
        BottomNavigationView bottom = findViewById(R.id.bottomNav);
        if (bottom != null) {
            bottom.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.tab_home) return true; // ya estás aquí
                if (id == R.id.tab_solicitudes) {
                    startActivity(new Intent(this, com.farenet.descuentos.ui.solicitudes.pendientes.SolicitudesPendientesActivity.class));
                    return true;
                }
                if (id == R.id.tab_reportes) {
                    // abrir reportes
                    return true;
                }
                return false;
            });
            bottom.setSelectedItemId(R.id.tab_home);
        }

        cargarPendientes();
    }

    private void cargarPlantasDesdeSesion() {
        plantasNombres.clear();
        plantaNombreToKey.clear();

        if (canVerTodasPlantas()) {
            plantasNombres.add("Todas");
        }

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

    private String perfilUpper(UsuarioPerfil up) {
        if (up == null || up.perfilId == null) return "";
        return up.perfilId.trim().toUpperCase();
    }

    private void computeRoleFlags() {
        UsuarioPerfil up = session.getPerfil();
        String p = perfilUpper(up);

        // Alias reconocidos (ajusta a tus valores reales)
        String[] sis = {"SIS", "SISTEMAS"};
        String[] ope = {"OPE", "OPERACION", "OPERACIONES"};
        String[] com = {"COM", "COMERCIAL", "VENTAS"};

        isSistemas    = matchesAny(p, sis);
        isOperaciones = matchesAny(p, ope);
        isComercial   = matchesAny(p, com);
    }

    private boolean matchesAny(String value, String[] options) {
        if (value == null) return false;
        for (String opt : options) {
            if (value.equalsIgnoreCase(opt)) return true;
        }
        return false;
    }

    // Permisos derivados

    private boolean isSoloComercial() {
        return isComercial && !isSistemas;
    }


    private boolean canVerTodasPlantas() {
        return isSistemas || isOperaciones || isComercial;
    }

    private boolean canVerPendienteAut() {
        return isSistemas || isComercial;
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
                    // ⛔ Para COMERCIAL: solo incorporar PENDIENTE_AUT
                    if (isSoloComercial() && (d.estado == null || !d.estado.equalsIgnoreCase("PENDIENTE_AUT"))) {
                        continue;
                    }
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
        if (estadoUi == null) return false;
        String e = estadoUi.trim().toLowerCase();
        return "pendiente".equals(e) || "pendiente (observada)".equals(e);
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
                new AutorizarSolicitudReq(userFinal, null, nomFinal, modoFinal)
        );

        // ===== Variante B: si tu req NO tiene constructor con args, usa setters
        // com.farenet.descuentos.API.Actual.DTO.solicitudes.AutorizarSolicitudReq body = new com.farenet.descuentos.API.Actual.DTO.solicitudes.AutorizarSolicitudReq();
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
