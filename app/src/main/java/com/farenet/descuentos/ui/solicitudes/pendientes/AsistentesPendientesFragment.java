package com.farenet.descuentos.ui.solicitudes.pendientes;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.farenet.descuentos.API.Actual.DTO.auth.UsuarioPerfil;
import com.farenet.descuentos.API.Actual.DTO.maestros.AccesoPlantaDto;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.AccionSolicitudRsp;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.AprobarSolicitudReq;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.AutorizarSolicitudReq;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.RechazarSolicitudReq;
import com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudPendienteDto;
import com.farenet.descuentos.API.Antigua.Service.DescuentoRepository;
import com.farenet.descuentos.API.Antigua.Service.MaestroRepository;
import com.farenet.descuentos.Core.Network.NewApiClient;
import com.farenet.descuentos.Core.Storage.SessionManager;
import com.farenet.descuentos.Core.config.Constante;
import com.farenet.descuentos.R;
import com.farenet.descuentos.data.local.realm.dao.QueryRealm;
import com.farenet.descuentos.data.local.realm.entity.Conceptoinspeccion;
import com.farenet.descuentos.data.local.realm.entity.TipoPagoDescuento;
import com.farenet.descuentos.domain.model.Descuento;
import com.farenet.descuentos.ui.common.WhatsAppUtils;
import com.farenet.descuentos.ui.solicitudes.model.SolicitudUI;
import com.farenet.descuentos.ui.solicitudes.pendientes.adapter.PendienteSolicitudAdapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AsistentesPendientesFragment extends Fragment implements PendienteSolicitudAdapter.Actions {

    private AutoCompleteTextView actPlanta, actTipo, actEstado;
    private RecyclerView rv;
    private SwipeRefreshLayout srl;
    private TextView emptyView;
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

    private DescuentoRepository descuentoRepository;
    private MaestroRepository maestroRepository;
    private SharedPreferences sharedPreferences;

    // Flags
    private boolean isSistemas = false;
    private boolean isOperaciones = false;
    private boolean isComercial = false;
    private boolean isAsistente = false;
    private boolean isMecanico = false;

    // Mapas display para concepto y tipo de pago
    private final java.util.Map<String,String> conceptoNombreByKey = new java.util.HashMap<>();
    private final java.util.Map<String,String> tipoPagoNombreByKey = new java.util.HashMap<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_solicitudes_pendientes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        session = new SessionManager(requireContext());
        computeRoleFlags();

        actPlanta = view.findViewById(R.id.act_planta);
        actTipo   = view.findViewById(R.id.act_tipo);
        actEstado = view.findViewById(R.id.act_estado);
        rv        = view.findViewById(R.id.rv_pendientes);
        srl       = view.findViewById(R.id.srl);
        emptyView = view.findViewById(R.id.empty_view);
        progress  = view.findViewById(android.R.id.progress);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PendienteSolicitudAdapter(data, this);
        rv.setAdapter(adapter);

        // Pull-to-refresh
        if (srl != null) {
            srl.setOnRefreshListener(this::cargarPendientes);
        }

        cargarPlantasDesdeSesion();
        actPlanta.setAdapter(new android.widget.ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1,
                plantasAdapterArray()));
        actTipo.setAdapter(new android.widget.ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, TIPOS));
        actEstado.setAdapter(new android.widget.ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, estadosAdapterArray()));

        if (canVerTodasPlantas()) {
            actPlanta.setText("Todas", false);
        } else if (!plantasNombres.isEmpty()) {
            actPlanta.setText(plantasNombres.get(0), false);
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
        maestroRepository   = Constante.getMaestroRespository();
        sharedPreferences   = requireContext().getSharedPreferences(Constante.TOKEN, android.content.Context.MODE_PRIVATE);

        // Construye mapas display desde Realm y, si están vacíos, refresca maestros del API
        buildDisplayMaps();
        if (conceptoNombreByKey.isEmpty() || tipoPagoNombreByKey.isEmpty()) {
            refreshMaestrosIfNeeded();
        }

        // Estado inicial (oculto lista y vacío; muestro loader)
        showLoading(true);
        cargarPendientes();
    }

    // ===== Roles / permisos =====
    private String[] estadosAdapterArray() {
        if (isSoloComercial()) {
            return new String[]{"Pendiente (Autorización)"};
        }
        List<String> estados = new ArrayList<>();
        estados.add("Pendiente");
        if (canVerPendienteAut()) estados.add("Pendiente (Autorización)");
        estados.add("Aprobada");
        estados.add("Rechazada");
        return estados.toArray(new String[0]);
    }

    private void computeRoleFlags() {
        UsuarioPerfil up = session.getPerfil();
        String p = perfilUpper(up);

        String[] sis = {"SIS", "SISTEMAS"};
        String[] ope = {"OPE", "OPERACION", "OPERACIONES"};
        String[] com = {"COM", "COMERCIAL", "VENTAS"};
        String[] asi = {"ASI", "ASISTENTE", "ASISTENTES"};
        String[] mec = {"MEC", "MECANICO", "MECANICOS"};

        isSistemas    = matchesAny(p, sis);
        isOperaciones = matchesAny(p, ope);
        isComercial   = matchesAny(p, com);
        isAsistente   = matchesAny(p, asi);
        isMecanico    = matchesAny(p, mec);
    }

    private String perfilUpper(UsuarioPerfil up) {
        if (up == null || up.perfilId == null) return "";
        return up.perfilId.trim().toUpperCase();
    }

    private boolean matchesAny(String value, String[] options) {
        if (value == null) return false;
        for (String opt : options) if (value.equalsIgnoreCase(opt)) return true;
        return false;
    }

    private boolean isSoloComercial() { return isComercial && !isSistemas; }
    private boolean isSoloAsistente() { return isAsistente && !isMecanico; }
    private boolean canVerTodasPlantas() { return isSistemas || isOperaciones || isComercial; }
    private boolean canVerPendienteAut() { return isSistemas || isComercial; }

    // ===== Datos / filtros =====
    private void cargarPlantasDesdeSesion() {
        plantasNombres.clear();
        plantaNombreToKey.clear();

        if (canVerTodasPlantas()) plantasNombres.add("Todas");

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
        if (listarCall != null) listarCall.cancel();

        // Mientras carga: oculto lista y empty
        showLoading(true);

        listarCall = NewApiClient.get().listarPendientes();
        listarCall.enqueue(new Callback<List<SolicitudPendienteDto>>() {
            @Override
            public void onResponse(Call<List<SolicitudPendienteDto>> call, Response<List<SolicitudPendienteDto>> response) {
                showLoading(false);
                if (srl != null && srl.isRefreshing()) srl.setRefreshing(false);

                if (!response.isSuccessful() || response.body() == null) {
                    showEmpty("No se pudo cargar pendientes.");
                    return;
                }

                all.clear();
                for (SolicitudPendienteDto d : response.body()) {
                    if (isSoloComercial() && (d.estado == null || !d.estado.equalsIgnoreCase("PENDIENTE_AUT"))) {
                        continue;
                    }
                    String tipoNice = "DESCUENTO".equalsIgnoreCase(d.tipo) ? "Descuento"
                            : "CORTESIA".equalsIgnoreCase(d.tipo) ? "Cortesía" : safe(d.tipo);
                    String estadoNice = mapEstadoUI(d.estado);
                    String fechaNice  = niceDate(d.creado_en);

                    SolicitudUI ui = new SolicitudUI(
                            d.codigo,
                            tipoNice,
                            s(d.placa),
                            s(d.planta_nombre),
                            s(d.motivo),
                            estadoNice,
                            s(d.id),
                            fechaNice,
                            s(d.planta_key),
                            s(d.concepto_key),
                            s(d.tipo_pago_key),
                            d.monto != null ? d.monto : 0d,
                            s(d.tipo_desc),
                            s(d.campania_nombre)
                    );
                    all.add(ui);
                }
                filtrar(); // internamente actualiza empty state
            }

            @Override
            public void onFailure(Call<List<SolicitudPendienteDto>> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                if (srl != null && srl.isRefreshing()) srl.setRefreshing(false);
                showEmpty("Error al cargar pendientes.");
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

        // Empty state según filtros
        if (data.isEmpty()) {
            showEmpty("No hay descuentos por aprobar.");
        } else {
            showList();
        }
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

    // ===== Acciones =====
    @Override
    public void onAprobar(SolicitudUI s) {
        if ("Pendiente (Autorización)".equalsIgnoreCase(s.estado)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Autorización final")
                    .setMessage("La solicitud está pendiente de autorización.\n¿Cómo deseas proceder?")
                    .setPositiveButton("Autorizar sin bolsa", (d, w) -> autorizar(s, "AUTORIZADO"))
                    .setNeutralButton("Intentar con bolsa", (d, w) -> autorizar(s, "BOLSA"))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(requireContext())
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

        final String nomFinal  = nom;
        final String userFinal = user;

        Call<AccionSolicitudRsp> call = NewApiClient.get().autorizarSolicitud(
                s.id, new AutorizarSolicitudReq(userFinal, null, nomFinal, modo)
        );

        call.enqueue(new Callback<AccionSolicitudRsp>() {
            @Override public void onResponse(Call<AccionSolicitudRsp> call, Response<AccionSolicitudRsp> rsp) {
                showLoading(false);
                if (!rsp.isSuccessful() || rsp.body()==null) {
                    Toast.makeText(requireContext(), "No se pudo autorizar", Toast.LENGTH_LONG).show();
                    return;
                }
                String est = rsp.body().estado != null ? rsp.body().estado : "ENVIADA";
                s.estado = mapEstadoUI(est);
                adapter.notifyDataSetChanged();

                if ("APROBADA".equalsIgnoreCase(est)) {
                    Toast.makeText(requireContext(), "Aprobada", Toast.LENGTH_SHORT).show();
                    generarDescuentoSiCorresponde(s, nomFinal, rsp.body());
                } else {
                    Toast.makeText(requireContext(), "Sigue pendiente de autorización", Toast.LENGTH_LONG).show();

                    // También enviamos "Descuento solicitado"
                    String placa = s.placa != null ? s.placa : "";
                    String plantaNombre = s.planta != null ? s.planta : "";

                    String conceptoDisplay;
                    if (!TextUtils.isEmpty(s.conceptoKey)) {
                        conceptoDisplay = conceptoNombreByKey.get(s.conceptoKey);
                        if (TextUtils.isEmpty(conceptoDisplay)) conceptoDisplay = s.conceptoKey;
                    } else {
                        conceptoDisplay = "Concepto";
                    }

                    String tipoPagoNameOrKey;
                    if (!TextUtils.isEmpty(s.tipoPagoKey)) {
                        tipoPagoNameOrKey = tipoPagoNombreByKey.get(s.tipoPagoKey);
                        if (TextUtils.isEmpty(tipoPagoNameOrKey)) tipoPagoNameOrKey = s.tipoPagoKey;
                    } else {
                        tipoPagoNameOrKey = "Tipo de pago";
                    }

                    Double monto = (s.monto != null ? s.monto : 0d);
                    String tipoDescuento = !TextUtils.isEmpty(s.tipoDesc) ? s.tipoDesc : "AUTORIZADO";
                    String campaniaNombre = "CAMPAÑA".equalsIgnoreCase(tipoDescuento) ? s.campaniaNombre : null;
                    String solicitadoPor = nomFinal;

                    WhatsAppUtils.sendRequestedDiscountMessage(
                            requireContext(),
                            placa,
                            plantaNombre,
                            conceptoDisplay,
                            s.motivo,
                            tipoPagoNameOrKey,
                            monto,
                            tipoDescuento,
                            campaniaNombre,
                            solicitadoPor
                    );
                }
                cargarPendientes();
            }
            @Override public void onFailure(Call<AccionSolicitudRsp> call, Throwable t) {
                showLoading(false);
                Toast.makeText(requireContext(), "Error al autorizar", Toast.LENGTH_LONG).show();
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

        AprobarSolicitudReq req = new AprobarSolicitudReq(aprobUser, null, aprobNomFinal);

        if (aprobarCall != null) aprobarCall.cancel();
        aprobarCall = NewApiClient.get().aprobarSolicitud(s.id, req);
        aprobarCall.enqueue(new Callback<AccionSolicitudRsp>() {
            @Override
            public void onResponse(Call<AccionSolicitudRsp> call, Response<AccionSolicitudRsp> response) {
                showLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "No se pudo aprobar", Toast.LENGTH_LONG).show();
                    return;
                }
                AccionSolicitudRsp rsp = response.body();

                String nuevoEstado = mapEstadoUI(rsp.estado != null ? rsp.estado : "ENVIADA");
                s.estado = nuevoEstado;
                adapter.notifyDataSetChanged();

                if ("Aprobada".equalsIgnoreCase(nuevoEstado)) {
                    Toast.makeText(requireContext(), "Aprobada", Toast.LENGTH_SHORT).show();
                } else if ("Pendiente (Autorización)".equalsIgnoreCase(nuevoEstado)) {
                    Toast.makeText(requireContext(), "Sin saldo: Queda pendiente de autorización", Toast.LENGTH_LONG).show();

                    // --- WhatsApp: DESCUENTO SOLICITADO (con todos los datos) ---
                    String placa = s.placa != null ? s.placa : "";
                    String plantaNombre = s.planta != null ? s.planta : "";

                    String conceptoDisplay;
                    if (!TextUtils.isEmpty(s.conceptoKey)) {
                        conceptoDisplay = conceptoNombreByKey.get(s.conceptoKey);
                        if (TextUtils.isEmpty(conceptoDisplay)) conceptoDisplay = s.conceptoKey;
                    } else {
                        conceptoDisplay = "Concepto";
                    }

                    String tipoPagoNameOrKey;
                    if (!TextUtils.isEmpty(s.tipoPagoKey)) {
                        tipoPagoNameOrKey = tipoPagoNombreByKey.get(s.tipoPagoKey);
                        if (TextUtils.isEmpty(tipoPagoNameOrKey)) tipoPagoNameOrKey = s.tipoPagoKey;
                    } else {
                        tipoPagoNameOrKey = "Tipo de pago";
                    }

                    Double monto = (s.monto != null ? s.monto : 0d);
                    String tipoDescuento = !TextUtils.isEmpty(s.tipoDesc) ? s.tipoDesc : "AUTORIZADO";
                    String campaniaNombre = "CAMPAÑA".equalsIgnoreCase(tipoDescuento) ? s.campaniaNombre : null;
                    String solicitadoPor = aprobNomFinal;

                    WhatsAppUtils.sendRequestedDiscountMessage(
                            requireContext(),
                            placa,
                            plantaNombre,
                            conceptoDisplay,
                            s.motivo,
                            tipoPagoNameOrKey,
                            monto,
                            tipoDescuento,
                            campaniaNombre,
                            solicitadoPor
                    );
                    // --- fin WhatsApp ---

                } else {
                    Toast.makeText(requireContext(), "Aprobación realizada", Toast.LENGTH_SHORT).show();
                }

                generarDescuentoSiCorresponde(s, aprobNomFinal, rsp);
                cargarPendientes();
            }

            @Override
            public void onFailure(Call<AccionSolicitudRsp> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(requireContext(), "Error al aprobar", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRechazar(SolicitudUI s) {
        new AlertDialog.Builder(requireContext())
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
                    Toast.makeText(requireContext(), "No se pudo rechazar", Toast.LENGTH_LONG).show();
                    return;
                }
                s.estado = "Rechazada";
                adapter.notifyDataSetChanged();
                Toast.makeText(requireContext(), "Rechazada", Toast.LENGTH_SHORT).show();
                cargarPendientes();
            }

            @Override
            public void onFailure(Call<AccionSolicitudRsp> call, Throwable t) {
                if (call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(requireContext(), "Error al rechazar", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void generarDescuentoSiCorresponde(SolicitudUI s, String aprobNom, AccionSolicitudRsp rsp) {
        String estadoBk = rsp != null ? rsp.estado : null;
        boolean aprobadaOk = "APROBADA".equalsIgnoreCase(estadoBk);
        if (!aprobadaOk) return;
        generarDescuentoDesdeSolicitud(s, aprobNom);
    }

    private void generarDescuentoDesdeSolicitud(SolicitudUI s, String aprobNom) {
        if (!s.isCompletaParaDescuento()) {
            Toast.makeText(requireContext(), "Solicitud incompleta para generar descuento", Toast.LENGTH_LONG).show();
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
            Toast.makeText(requireContext(), "Sesión no válida para registrar descuento", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(requireContext(),
                            "Descuento registrado correctamente", Toast.LENGTH_SHORT).show();

                    // ===== Enviar WhatsApp con “Descuento registrado” (sin código/fecha) =====
                    String placa = s.placa != null ? s.placa : "";
                    String plantaNombre = s.planta != null ? s.planta : "";

                    // Concepto legible: abreviatura/nombre desde maestros; si no hay, usa la key
                    String conceptoDisplay;
                    if (!TextUtils.isEmpty(s.conceptoKey)) {
                        conceptoDisplay = conceptoNombreByKey.get(s.conceptoKey);
                        if (TextUtils.isEmpty(conceptoDisplay)) conceptoDisplay = s.conceptoKey;
                    } else {
                        conceptoDisplay = "Concepto";
                    }

                    // Tipo de pago para decidir el label del monto
                    String tipoPagoNameOrKey;
                    if (!TextUtils.isEmpty(s.tipoPagoKey)) {
                        tipoPagoNameOrKey = tipoPagoNombreByKey.get(s.tipoPagoKey);
                        if (TextUtils.isEmpty(tipoPagoNameOrKey)) tipoPagoNameOrKey = s.tipoPagoKey;
                    } else {
                        tipoPagoNameOrKey = "Tipo de pago";
                    }

                    Double monto = (s.monto != null ? s.monto : 0d);

                    String tipoDescuento = !TextUtils.isEmpty(s.tipoDesc) ? s.tipoDesc : "AUTORIZADO";
                    String campaniaNombre = "CAMPAÑA".equalsIgnoreCase(tipoDescuento) ? s.campaniaNombre : null;

                    String autorizadoPor = aprobNom;

                    WhatsAppUtils.sendRegisteredDiscountMessage(
                            requireContext(),
                            placa,
                            plantaNombre,
                            conceptoDisplay,
                            s.motivo,
                            tipoPagoNameOrKey,
                            monto,
                            tipoDescuento,
                            campaniaNombre,
                            autorizadoPor
                    );
                    // ===== FIN =====

                } else {
                    Toast.makeText(requireContext(),
                            "Aprobada, pero falló el registro de descuento (" + rsp.code() + ")",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<String> c, Throwable t) {
                Toast.makeText(requireContext(),
                        "Aprobada, error registrando descuento: " +
                                (t.getMessage() != null ? t.getMessage() : ""),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ===== Carga de nombres legibles (conceptos / tipos de pago) =====
    private void buildDisplayMaps() {
        conceptoNombreByKey.clear();
        tipoPagoNombreByKey.clear();

        // Conceptos
        List<Conceptoinspeccion> conceptosLoc = QueryRealm.copyAllConceptos();
        if (conceptosLoc != null) {
            for (Conceptoinspeccion c : conceptosLoc) {
                if (c == null) continue;
                String key = c.getKey();
                String ab  = c.getAbreviatura();
                String nm  = null;
                try { nm = c.getAbreviatura(); } catch (Exception ignored) {}  // <-- FIX: nombre real
                String display = !TextUtils.isEmpty(ab) ? ab
                        : (!TextUtils.isEmpty(nm) ? nm
                        : (!TextUtils.isEmpty(key) ? key : "Concepto"));
                if (!TextUtils.isEmpty(key)) {
                    conceptoNombreByKey.put(key, display);
                }
            }
        }

        // Tipos de pago
        List<TipoPagoDescuento> tiposLoc = QueryRealm.copyAllTipoPagos();
        if (tiposLoc != null) {
            for (TipoPagoDescuento t : tiposLoc) {
                if (t == null) continue;
                String key = t.getKey();
                String nm  = null;
                try { nm = t.getNombre(); } catch (Exception ignored) {}
                String display = !TextUtils.isEmpty(nm)
                        ? (nm + (TextUtils.isEmpty(key) ? "" : (" " + key)))
                        : (!TextUtils.isEmpty(key) ? key : "Tipo de pago");
                if (!TextUtils.isEmpty(key)) {
                    tipoPagoNombreByKey.put(key, display);
                }
            }
        }
    }

    private void refreshMaestrosIfNeeded() {
        String token = (sharedPreferences != null) ? sharedPreferences.getString("token", null) : null;
        if (TextUtils.isEmpty(token)) return;

        // Conceptos
        maestroRepository.getConceptoinspeccion(token).enqueue(new retrofit2.Callback<List<Conceptoinspeccion>>() {
            @Override public void onResponse(retrofit2.Call<List<Conceptoinspeccion>> call,
                                             retrofit2.Response<List<Conceptoinspeccion>> rsp) {
                if (rsp.isSuccessful() && rsp.body() != null) {
                    QueryRealm.saveConceptosAsync(rsp.body(), new QueryRealm.TxCallback() {
                        @Override public void onSuccess() { buildDisplayMaps(); }
                        @Override public void onError(Throwable error) { /* noop */ }
                    });
                }
            }
            @Override public void onFailure(retrofit2.Call<List<Conceptoinspeccion>> call, Throwable t) { /* noop */ }
        });

        // Tipos de pago
        maestroRepository.getTipoPagoDescuento(token).enqueue(new retrofit2.Callback<List<TipoPagoDescuento>>() {
            @Override public void onResponse(retrofit2.Call<List<TipoPagoDescuento>> call,
                                             retrofit2.Response<List<TipoPagoDescuento>> rsp) {
                if (rsp.isSuccessful() && rsp.body() != null) {
                    QueryRealm.saveTipoPagoAsync(rsp.body(), new QueryRealm.TxCallback() {
                        @Override public void onSuccess() { buildDisplayMaps(); }
                        @Override public void onError(Throwable error) { /* noop */ }
                    });
                }
            }
            @Override public void onFailure(retrofit2.Call<List<TipoPagoDescuento>> call, Throwable t) { /* noop */ }
        });
    }

    // ===== UI helpers (loader / empty) =====
    private void showLoading(boolean show) {
        if (progress != null) progress.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            if (srl != null) srl.setVisibility(View.GONE);
            if (rv  != null) rv.setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
        }
    }

    private void showEmpty(String message) {
        if (emptyView != null) {
            emptyView.setText(TextUtils.isEmpty(message) ? "No hay descuentos por aprobar." : message);
            emptyView.setVisibility(View.VISIBLE);
        }
        if (srl != null) srl.setVisibility(View.GONE);
        if (rv  != null) rv.setVisibility(View.GONE);
    }

    private void showList() {
        if (srl != null) srl.setVisibility(View.VISIBLE);
        if (rv  != null) rv.setVisibility(View.VISIBLE);
        if (emptyView != null) emptyView.setVisibility(View.GONE);
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

    private String safe(String s) { return s == null ? "" : s.trim(); }
    private String s(Object o) { return o == null ? "" : String.valueOf(o).trim(); }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listarCall != null) listarCall.cancel();
        if (aprobarCall != null) aprobarCall.cancel();
        if (rechazarCall != null) rechazarCall.cancel();
    }
}
