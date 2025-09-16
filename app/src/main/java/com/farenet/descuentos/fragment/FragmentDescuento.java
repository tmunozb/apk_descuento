package com.farenet.descuentos.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter; // <-- NUEVO
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.farenet.descuentos.R;
import com.farenet.descuentos.adapter.SpinerAdapter;
import com.farenet.descuentos.config.Constante;
import com.farenet.descuentos.domain.Autorizadores;
import com.farenet.descuentos.domain.Conceptoinspeccion;
import com.farenet.descuentos.domain.Descuento;
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.domain.TipoPagoDescuento;
import com.farenet.descuentos.repository.DescuentoRepository;
import com.farenet.descuentos.repository.MaestroRepository;
import com.farenet.descuentos.sql.QueryRealm;
import com.google.android.material.textfield.MaterialAutoCompleteTextView; // <-- NUEVO

import java.util.ArrayList;
import java.util.Arrays; // <-- NUEVO
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentDescuento extends Fragment {

    private Spinner spPlanta, spConcepto, spTipoPago, spTipoCampana, spTipoDescuento, spAutoriza;
    private EditText txtPlaca, txtMonto, txtMotivo; // Mantengo EditText para no romper nada
    private Button btnGuardar, btnLogout;

    private SpinerAdapter<Planta> spPlantaAdapter;
    private SpinerAdapter<Conceptoinspeccion> spConceptoAdapter;
    private SpinerAdapter<TipoPagoDescuento> spTipoPagoAdapter;
    private SpinerAdapter<Autorizadores> spAutorizadoresAdapter;
    private SpinerAdapter<String> spTipoCampanaAdapter;
    private SpinerAdapter<String> spTipoDescuentoAdapter;

    private DescuentoRepository descuentoRepository;
    private MaestroRepository maestroRepository;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.descuento_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spPlanta        = view.findViewById(R.id.sp_planta_desc);
        spConcepto      = view.findViewById(R.id.sp_concepto_desc);
        spTipoPago      = view.findViewById(R.id.sp_tipopago_desc);
        txtPlaca        = view.findViewById(R.id.txtPlaca_desc);
        txtMonto        = view.findViewById(R.id.txtMonto_desc);
        txtMotivo       = view.findViewById(R.id.txtMotivo_desc); // puede ser EditText o MaterialAutoCompleteTextView
        btnGuardar      = view.findViewById(R.id.btnGuardar_desc);
        btnLogout       = view.findViewById(R.id.btnLogout_desc);
        spTipoCampana   = view.findViewById(R.id.sp_tipocampana);
        spTipoDescuento = view.findViewById(R.id.sp_tipodescuento);
        spAutoriza      = view.findViewById(R.id.sp_autoriza_desc);

        sharedPreferences   = requireActivity().getSharedPreferences(Constante.TOKEN, Context.MODE_PRIVATE);
        descuentoRepository = Constante.getDescuentoRepository();
        maestroRepository   = Constante.getMaestroRespository();

        // 1) Cargar local
        List<Planta> plantas = safe(QueryRealm.copyAllPlantas());
        List<Autorizadores> autorizadores = safe(QueryRealm.copyAllAutorizadores());
        List<Conceptoinspeccion> conceptos = safe(QueryRealm.copyAllConceptos());
        List<TipoPagoDescuento> tiposPago = safe(QueryRealm.copyAllTipoPagos());

        // 2) Adapters
        spPlantaAdapter        = new SpinerAdapter<>(requireContext(), plantas);
        spAutorizadoresAdapter = new SpinerAdapter<>(requireContext(), autorizadores);
        spConceptoAdapter      = new SpinerAdapter<>(requireContext(), conceptos);
        spTipoPagoAdapter      = new SpinerAdapter<>(requireContext(), tiposPago);

        spPlanta.setAdapter(spPlantaAdapter);
        spAutoriza.setAdapter(spAutorizadoresAdapter);
        spConcepto.setAdapter(spConceptoAdapter);
        spTipoPago.setAdapter(spTipoPagoAdapter);

        // 3) Tipo descuento (estático)
        List<String> tipoDescuento = new ArrayList<>();
        tipoDescuento.add("AUTORIZADO");
        tipoDescuento.add("CARTA");
        tipoDescuento.add("CAMPAÑA");

        spTipoDescuentoAdapter = new SpinerAdapter<>(requireContext(), tipoDescuento);
        spTipoDescuento.setAdapter(spTipoDescuentoAdapter);

        // 4) Campañas (estático)
        List<String> campañas = new ArrayList<>();
        campañas.add("COMPETENCIA");
        campañas.add("RECUPERADOS");
        campañas.add("REZAGADOS");
        campañas.add("CUPONIDAD");
        campañas.add("PAGO WEB");

        spTipoCampanaAdapter = new SpinerAdapter<>(requireContext(), campañas);
        spTipoCampana.setAdapter(spTipoCampanaAdapter);
        toggleCampanaVisibility();

        spTipoDescuento.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                toggleCampanaVisibility();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 4.1) MOTIVO como combo editable (si el layout usa MaterialAutoCompleteTextView)
        if (txtMotivo instanceof MaterialAutoCompleteTextView) {
            MaterialAutoCompleteTextView mac = (MaterialAutoCompleteTextView) txtMotivo;

            String[] motivos = getResources().getStringArray(R.array.motivos_descuento);
            ArrayAdapter<String> motivoAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    Arrays.asList(motivos)
            );
            mac.setAdapter(motivoAdapter);

            mac.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) mac.showDropDown(); });
            mac.setOnItemClickListener((parent, v, pos, id) -> {
                String sel = (String) parent.getItemAtPosition(pos);
                if ("Otro (especificar)".equalsIgnoreCase(sel)) {
                    mac.setText("");
                    mac.requestFocus();
                }
            });
        }

        btnGuardar.setOnClickListener(v -> onGuardarClicked());
        btnLogout.setOnClickListener(v -> confirmarCerrarSesion());

        // 5) Re-sync si local vacío
        ensureMaestrosDesdeApiSiHaceFalta();
    }

    private <T> List<T> safe(List<T> list) { return list != null ? list : Collections.emptyList(); }

    private void toggleCampanaVisibility() {
        String tipo = (spTipoDescuento.getSelectedItem() != null)
                ? spTipoDescuento.getSelectedItem().toString()
                : "";
        spTipoCampana.setVisibility("CAMPAÑA".equalsIgnoreCase(tipo) ? View.VISIBLE : View.GONE);
    }

    /** Muestra diálogo y, si acepta, borra token + Realm y navega a Login. */
    private void confirmarCerrarSesion() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Desea cerrar sesión y borrar el caché local?")
                .setPositiveButton("Sí", (d, w) -> cerrarSesionYBorrarCache())
                .setNegativeButton("No", null)
                .show();
    }

    private void cerrarSesionYBorrarCache() {
        btnGuardar.setEnabled(false);
        btnLogout.setEnabled(false);

        sharedPreferences.edit().clear().apply();

        QueryRealm.wipeAllAsync(new QueryRealm.TxCallback() {
            @Override public void onSuccess() {
                spPlantaAdapter.setItems(Collections.emptyList());
                spAutorizadoresAdapter.setItems(Collections.emptyList());
                spConceptoAdapter.setItems(Collections.emptyList());
                spTipoPagoAdapter.setItems(Collections.emptyList());
                limpiar();

                try {
                    Class<?> loginClass = Class.forName("com.farenet.descuentos.LoginActivity");
                    Intent i = new Intent(requireContext(), loginClass);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    requireActivity().finish();
                } catch (ClassNotFoundException e) {
                    Toast.makeText(requireContext(), "No se encontró LoginActivity", Toast.LENGTH_LONG).show();
                    btnGuardar.setEnabled(true);
                    btnLogout.setEnabled(true);
                }
            }

            @Override public void onError(Throwable error) {
                Toast.makeText(requireContext(), "Error limpiando caché: " +
                                (error != null && error.getMessage()!=null ? error.getMessage() : "desconocido"),
                        Toast.LENGTH_LONG).show();
                btnGuardar.setEnabled(true);
                btnLogout.setEnabled(true);
            }
        });
    }

    private void ensureMaestrosDesdeApiSiHaceFalta() {
        boolean needPlantas       = spPlantaAdapter.getCount() == 0;
        boolean needAutorizadores = spAutorizadoresAdapter.getCount() == 0;
        boolean needConceptos     = spConceptoAdapter.getCount() == 0;
        boolean needTipoPago      = spTipoPagoAdapter.getCount() == 0;

        if (!(needPlantas || needAutorizadores || needConceptos || needTipoPago)) return;

        String token = sharedPreferences.getString("token", null);
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(requireContext(), "Sesión no válida. Inicie sesión.", Toast.LENGTH_LONG).show();
            return;
        }

        btnGuardar.setEnabled(false);
        final AtomicInteger pending = new AtomicInteger(0);

        Runnable refreshUiIfDone = () -> {
            if (pending.decrementAndGet() == 0) {
                spPlantaAdapter.setItems(QueryRealm.copyAllPlantas());
                spAutorizadoresAdapter.setItems(QueryRealm.copyAllAutorizadores());
                spConceptoAdapter.setItems(QueryRealm.copyAllConceptos());
                spTipoPagoAdapter.setItems(QueryRealm.copyAllTipoPagos());
                btnGuardar.setEnabled(true);
            }
        };

        if (needPlantas) {
            pending.incrementAndGet();
            maestroRepository.getPlantas(token).enqueue(new Callback<List<Planta>>() {
                @Override public void onResponse(Call<List<Planta>> call, Response<List<Planta>> rsp) {
                    if (rsp.isSuccessful() && rsp.body() != null) {
                        QueryRealm.savePlantaAsync(rsp.body(), new QueryRealm.TxCallback() {
                            @Override public void onSuccess() { refreshUiIfDone.run(); }
                            @Override public void onError(Throwable error) {
                                Toast.makeText(requireContext(), "Guardar plantas: " + safeMsg(error), Toast.LENGTH_SHORT).show();
                                refreshUiIfDone.run();
                            }
                        });
                    } else {
                        handleHttpError("Plantas", rsp.code(), rsp.message());
                        refreshUiIfDone.run();
                    }
                }
                @Override public void onFailure(Call<List<Planta>> call, Throwable t) {
                    Toast.makeText(requireContext(), "Error plantas: " + safeMsg(t), Toast.LENGTH_SHORT).show();
                    refreshUiIfDone.run();
                }
            });
        }

        if (needAutorizadores) {
            pending.incrementAndGet();
            maestroRepository.getAutorizadores(token).enqueue(new Callback<List<Autorizadores>>() {
                @Override public void onResponse(Call<List<Autorizadores>> call, Response<List<Autorizadores>> rsp) {
                    if (rsp.isSuccessful() && rsp.body() != null) {
                        QueryRealm.saveAutorizadoresAsync(rsp.body(), new QueryRealm.TxCallback() {
                            @Override public void onSuccess() { refreshUiIfDone.run(); }
                            @Override public void onError(Throwable error) {
                                Toast.makeText(requireContext(), "Guardar autorizadores: " + safeMsg(error), Toast.LENGTH_SHORT).show();
                                refreshUiIfDone.run();
                            }
                        });
                    } else {
                        handleHttpError("Autorizadores", rsp.code(), rsp.message());
                        refreshUiIfDone.run();
                    }
                }
                @Override public void onFailure(Call<List<Autorizadores>> call, Throwable t) {
                    Toast.makeText(requireContext(), "Error autorizadores: " + safeMsg(t), Toast.LENGTH_SHORT).show();
                    refreshUiIfDone.run();
                }
            });
        }

        if (needConceptos) {
            pending.incrementAndGet();
            maestroRepository.getConceptoinspeccion(token).enqueue(new Callback<List<Conceptoinspeccion>>() {
                @Override public void onResponse(Call<List<Conceptoinspeccion>> call, Response<List<Conceptoinspeccion>> rsp) {
                    if (rsp.isSuccessful() && rsp.body() != null) {
                        QueryRealm.saveConceptosAsync(rsp.body(), new QueryRealm.TxCallback() {
                            @Override public void onSuccess() { refreshUiIfDone.run(); }
                            @Override public void onError(Throwable error) {
                                Toast.makeText(requireContext(), "Guardar conceptos: " + safeMsg(error), Toast.LENGTH_SHORT).show();
                                refreshUiIfDone.run();
                            }
                        });
                    } else {
                        handleHttpError("Conceptos", rsp.code(), rsp.message());
                        refreshUiIfDone.run();
                    }
                }
                @Override public void onFailure(Call<List<Conceptoinspeccion>> call, Throwable t) {
                    Toast.makeText(requireContext(), "Error conceptos: " + safeMsg(t), Toast.LENGTH_SHORT).show();
                    refreshUiIfDone.run();
                }
            });
        }

        if (needTipoPago) {
            pending.incrementAndGet();
            maestroRepository.getTipoPagoDescuento(token).enqueue(new Callback<List<TipoPagoDescuento>>() {
                @Override public void onResponse(Call<List<TipoPagoDescuento>> call, Response<List<TipoPagoDescuento>> rsp) {
                    if (rsp.isSuccessful() && rsp.body() != null) {
                        QueryRealm.saveTipoPagoAsync(rsp.body(), new QueryRealm.TxCallback() {
                            @Override public void onSuccess() { refreshUiIfDone.run(); }
                            @Override public void onError(Throwable error) {
                                Toast.makeText(requireContext(), "Guardar tipo pago: " + safeMsg(error), Toast.LENGTH_SHORT).show();
                                refreshUiIfDone.run();
                            }
                        });
                    } else {
                        handleHttpError("Tipos de pago", rsp.code(), rsp.message());
                        refreshUiIfDone.run();
                    }
                }
                @Override public void onFailure(Call<List<TipoPagoDescuento>> call, Throwable t) {
                    Toast.makeText(requireContext(), "Error tipo pago: " + safeMsg(t), Toast.LENGTH_SHORT).show();
                    refreshUiIfDone.run();
                }
            });
        }

        if (pending.get() == 0) btnGuardar.setEnabled(true);
    }

    private void handleHttpError(String tag, int code, String msg) {
        if (code == 401 || code == 403) {
            Toast.makeText(requireContext(), tag + ": sesión expirada. Inicie sesión.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(requireContext(), tag + " HTTP " + code + " - " + msg, Toast.LENGTH_SHORT).show();
        }
    }

    private String safeMsg(Throwable t) {
        return t != null && t.getMessage() != null ? t.getMessage() : "desconocido";
    }

    private void onGuardarClicked() {
        if (!validarCampos()) return;

        String token = sharedPreferences.getString("token", null);
        if (TextUtils.isEmpty(token)) {
            Toast.makeText(requireContext(), "Sesión no válida. Inicie sesión nuevamente.", Toast.LENGTH_LONG).show();
            return;
        }

        Planta planta = (Planta) spPlanta.getSelectedItem();
        Conceptoinspeccion concepto = (Conceptoinspeccion) spConcepto.getSelectedItem();
        TipoPagoDescuento tipoPago = (TipoPagoDescuento) spTipoPago.getSelectedItem();
        Autorizadores aut = (Autorizadores) spAutoriza.getSelectedItem();

        if (planta == null || concepto == null || tipoPago == null || aut == null) {
            Toast.makeText(requireContext(), "Complete los datos de maestros.", Toast.LENGTH_SHORT).show();
            return;
        }

        String placa = txtPlaca.getText().toString().trim().toUpperCase();
        Double monto = parseMonto(txtMonto.getText().toString().trim());
        if (monto == null) {
            txtMonto.setError("Monto inválido");
            return;
        }

        String motivo = txtMotivo.getText().toString().trim();
        String autoriza = aut.getNombre() != null ? aut.getNombre() : aut.toString();

        String tipoSelec = spTipoDescuento.getSelectedItem() != null
                ? spTipoDescuento.getSelectedItem().toString()
                : "";

        Descuento descuento = new Descuento();
        descuento.setConceptoinspeccion(concepto.getKey());
        descuento.setTipoPagoDescuento(tipoPago.getKey());
        descuento.setPlanta(planta.getKey());
        descuento.setPlaca(placa);
        descuento.setMonto(monto);
        descuento.setAutoriza(autoriza);
        descuento.setMotivo(motivo);

        if ("CAMPAÑA".equalsIgnoreCase(tipoSelec) && spTipoCampana.getSelectedItem() != null) {
            descuento.setNomDescuento(spTipoCampana.getSelectedItem().toString());
        }

        Call<String> call;
        if ("AUTORIZADO".equalsIgnoreCase(tipoSelec)) {
            call = descuentoRepository.saveDescuento(descuento, token);
        } else if ("CARTA".equalsIgnoreCase(tipoSelec)) {
            call = descuentoRepository.saveCarta(descuento, token);
        } else if ("CAMPAÑA".equalsIgnoreCase(tipoSelec)) {
            call = descuentoRepository.saveCampana(descuento, token);
        } else {
            Toast.makeText(requireContext(), "Tipo de descuento inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGuardar.setEnabled(false);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> c, Response<String> response) {
                btnGuardar.setEnabled(true);
                if (response.isSuccessful() && response.code() == 200) {
                    Toast.makeText(requireContext(), "Se agregó el descuento", Toast.LENGTH_LONG).show();

                    // WhatsApp con placa + planta + concepto + motivo + etiqueta monto + valor
                    String plantaNombre   = planta.getNombre() != null ? planta.getNombre() : String.valueOf(planta);
                    String conceptoNombre = (concepto.getAbreviatura() != null && !concepto.getAbreviatura().isEmpty())
                            ? concepto.getAbreviatura() : String.valueOf(concepto);

                    // === NUEVO: etiqueta dinámica según TipoPagoDescuento ===
                    String pagoNameOrKey = (tipoPago.getNombre() != null ? tipoPago.getNombre() : "") +
                            " " +
                            (tipoPago.getKey() != null ? tipoPago.getKey() : "");
                    String montoLabel;
                    String lower = pagoNameOrKey.toLowerCase();
                    if (lower.contains("flat")) {
                        montoLabel = "💰 Monto a pagar:";
                    } else if (lower.contains("porcentaje") || lower.contains("%")) {
                        montoLabel = "💰 % de descuento:";
                    } else if (lower.contains("monto")) {
                        montoLabel = "💰 Monto de descuento:";
                    } else {
                        montoLabel = "💰 Monto:"; // fallback
                    }

                    abrirWhatsappYLimpiar(placa, plantaNombre, tipoSelec, conceptoNombre, motivo, montoLabel, monto);

                } else {
                    if (response.code() == 401 || response.code() == 403) {
                        Toast.makeText(requireContext(), "Sesión expirada. Inicie sesión.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), "Error al registrar (" + response.code() + ")", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<String> c, Throwable t) {
                btnGuardar.setEnabled(true);
                Toast.makeText(requireContext(),
                        "Error de red: " + (t.getMessage() != null ? t.getMessage() : ""),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Nullable
    private Double parseMonto(@NonNull String montoStr) {
        try {
            return Double.parseDouble(montoStr.replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean validarCampos() {
        boolean ok = true;

        String placa = txtPlaca.getText() != null ? txtPlaca.getText().toString().trim() : "";
        if (placa.isEmpty()) { txtPlaca.setError("Ingrese placa"); ok = false; }

        String montoStr = txtMonto.getText() != null ? txtMonto.getText().toString().trim() : "";
        if (montoStr.isEmpty()) { txtMonto.setError("Ingrese monto"); ok = false; }
        else if (parseMonto(montoStr) == null) { txtMonto.setError("Monto inválido"); ok = false; }

        String motivo = txtMotivo.getText() != null ? txtMotivo.getText().toString().trim() : "";
        if (motivo.isEmpty()) { txtMotivo.setError("Ingrese motivo"); ok = false; }

        if (spPlanta.getAdapter() == null || spPlanta.getAdapter().getCount() == 0) {
            Toast.makeText(requireContext(), "No hay plantas disponibles", Toast.LENGTH_SHORT).show();
            ok = false;
        }
        if (spAutoriza.getAdapter() == null || spAutoriza.getAdapter().getCount() == 0) {
            Toast.makeText(requireContext(), "No hay autorizadores disponibles", Toast.LENGTH_SHORT).show();
            ok = false;
        }
        if (spConcepto.getAdapter() == null || spConcepto.getAdapter().getCount() == 0) {
            Toast.makeText(requireContext(), "No hay conceptos disponibles", Toast.LENGTH_SHORT).show();
            ok = false;
        }
        if (spTipoPago.getAdapter() == null || spTipoPago.getAdapter().getCount() == 0) {
            Toast.makeText(requireContext(), "No hay tipos de pago disponibles", Toast.LENGTH_SHORT).show();
            ok = false;
        }

        String tipoSelec = spTipoDescuento.getSelectedItem() != null
                ? spTipoDescuento.getSelectedItem().toString() : "";
        if ("CAMPAÑA".equalsIgnoreCase(tipoSelec)) {
            if (spTipoCampana.getAdapter() == null || spTipoCampana.getAdapter().getCount() == 0) {
                Toast.makeText(requireContext(), "No hay campañas disponibles", Toast.LENGTH_SHORT).show();
                ok = false;
            }
        }

        return ok;
    }

    /** Arma el texto según el tipo de descuento y abre WhatsApp; luego limpia el formulario. */
    private void abrirWhatsappYLimpiar(String placa,
                                       String plantaNombre,
                                       String tipoSelec,
                                       String conceptoNombre,
                                       String motivo,
                                       String montoLabel,   // <-- NUEVO
                                       double monto) {      // <-- NUEVO
        String msg;
        if ("CARTA".equalsIgnoreCase(tipoSelec)) {
            msg = getString(R.string.msjwhtspcarta,  placa, plantaNombre, conceptoNombre, motivo, montoLabel, monto);
        } else if ("CAMPAÑA".equalsIgnoreCase(tipoSelec)) {
            msg = getString(R.string.msjwhtsp_campana, placa, plantaNombre, conceptoNombre, motivo, montoLabel, monto);
        } else {
            msg = getString(R.string.msjwhtsp,       placa, plantaNombre, conceptoNombre, motivo, montoLabel, monto); // AUTORIZADO
        }

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, msg);

        boolean launched = tryStartActivityWithPackage(send, "com.whatsapp");
        if (!launched) launched = tryStartActivityWithPackage(send, "com.whatsapp.w4b");

        if (!launched) {
            String url = "https://wa.me/?text=" + Uri.encode(msg);
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(requireContext(), "No se encontró WhatsApp ni navegador disponible", Toast.LENGTH_SHORT).show();
            }
        }

        limpiar();
    }

    /** Intenta lanzar un ACTION_SEND con un paquete específico. Devuelve true si se lanzó. */
    private boolean tryStartActivityWithPackage(Intent base, String packageName) {
        try {
            Intent i = new Intent(base);
            i.setPackage(packageName);
            if (i.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(i);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void limpiar() {
        if (spPlanta.getAdapter() != null && spPlanta.getAdapter().getCount() > 0) spPlanta.setSelection(0);
        if (spTipoPago.getAdapter() != null && spTipoPago.getAdapter().getCount() > 0) spTipoPago.setSelection(0);
        if (spConcepto.getAdapter() != null && spConcepto.getAdapter().getCount() > 0) spConcepto.setSelection(0);
        if (spAutoriza.getAdapter() != null && spAutoriza.getAdapter().getCount() > 0) spAutoriza.setSelection(0);
        if (spTipoDescuento.getAdapter() != null && spTipoDescuento.getAdapter().getCount() > 0) spTipoDescuento.setSelection(0);
        if (spTipoCampana.getAdapter() != null && spTipoCampana.getAdapter().getCount() > 0) spTipoCampana.setSelection(0);
        txtPlaca.setText("");
        txtMonto.setText("");
        txtMotivo.setText("");
    }
}
