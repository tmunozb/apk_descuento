// SessionManager.java
package com.farenet.descuentos.Core.Storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.farenet.descuentos.Core.config.Constante;
import com.farenet.descuentos.API.Actual.DTO.maestros.AccesoPlantaDto;
import com.farenet.descuentos.API.Actual.DTO.auth.LoginRsp;
import com.farenet.descuentos.API.Actual.DTO.auth.UsuarioPerfil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

public class SessionManager {

    private final SharedPreferences sp;
    private final Gson gson = new Gson();

    private static final String K_USER       = "user";
    private static final String K_PERFIL     = "perfil_json";
    private static final String K_ACCESOS    = "accesos_json";
    private static final String K_LOGIN_RAW  = "login_raw_json"; // opcional: debug

    public SessionManager(Context ctx) {
        Context app = (ctx != null) ? ctx.getApplicationContext() : null;
        if (app == null) throw new IllegalStateException("Context nulo al crear SessionManager");
        sp = app.getSharedPreferences(Constante.TOKEN, Context.MODE_PRIVATE);
    }

    // ===== Username básico =====
    public void saveUsername(String user) {
        sp.edit().putString(K_USER, user).apply();
    }
    public String getUsername() {
        return sp.getString(K_USER, null);
    }

    // ===== Perfil =====
    public void savePerfil(UsuarioPerfil perfil) {
        sp.edit().putString(K_PERFIL, gson.toJson(perfil)).apply();
    }
    public UsuarioPerfil getPerfil() {
        String json = sp.getString(K_PERFIL, null);
        return json == null ? null : gson.fromJson(json, UsuarioPerfil.class);
    }

    // ===== Accesos (lista) =====
    /** Guarda accesos con de-dup por (key|planta) manteniendo orden de llegada. */
    public void saveAccesos(List<AccesoPlantaDto> accesos) {
        if (accesos == null) accesos = Collections.emptyList();
        LinkedHashMap<String, AccesoPlantaDto> map = new LinkedHashMap<>();
        for (AccesoPlantaDto a : accesos) {
            if (a == null) continue;
            String k = safe(a.key) + "|" + safe(a.planta);
            map.put(k, a);
        }
        List<AccesoPlantaDto> dedup = new ArrayList<>(map.values());
        sp.edit().putString(K_ACCESOS, gson.toJson(dedup)).apply();
    }

    /**
     * Lee accesos. Intenta parsear el formato actual y, si falla, migra desde un formato antiguo
     * equivalente (key/planta) usando una clase interna local.
     */
    public List<AccesoPlantaDto> getAccesos() {
        String json = sp.getString(K_ACCESOS, null);
        if (json == null) return Collections.emptyList();

        // Intento principal: formato actual (AccesoPlantaDto)
        try {
            Type tDto = new TypeToken<List<AccesoPlantaDto>>() {}.getType();
            List<AccesoPlantaDto> list = gson.fromJson(json, tDto);
            if (list != null) return list;
        } catch (Throwable ignore) { /* fallback abajo */ }

        // Fallback/migración: estructura simple con key/planta (ya no dependemos de clases antiguas)
        try {
            Type tOld = new TypeToken<List<OldPlantaAcceso>>() {}.getType();
            List<OldPlantaAcceso> old = gson.fromJson(json, tOld);
            if (old != null) {
                List<AccesoPlantaDto> migrated = new ArrayList<>();
                for (OldPlantaAcceso o : old) {
                    if (o == null) continue;
                    AccesoPlantaDto d = new AccesoPlantaDto();
                    d.key = o.key;
                    d.planta = o.planta;
                    // d.usuario queda null (no existía antes)
                    migrated.add(d);
                }
                // Persistir ya en el nuevo formato
                saveAccesos(migrated);
                return migrated;
            }
        } catch (Throwable ignore) { }

        return Collections.emptyList();
    }

    // ===== Helpers de conveniencia =====
    public boolean isPerfil(String... perfiles) {
        UsuarioPerfil p = getPerfil();
        if (p == null || p.perfilId == null) return false;
        String actual = p.perfilId.trim().toLowerCase(Locale.ROOT);
        for (String x : perfiles) {
            if (x != null && actual.equals(x.trim().toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    public String getNombreVisible() {
        UsuarioPerfil p = getPerfil();
        return (p == null)
                ? (getUsername() != null ? getUsername() : "")
                : p.getNombreCompleto();
    }

    public boolean isActivo() {
        UsuarioPerfil p = getPerfil();
        return p != null && p.isActivo();
    }

    // ===== Debug opcional =====
    public void saveLoginRaw(LoginRsp rsp) {
        sp.edit().putString(K_LOGIN_RAW, gson.toJson(rsp)).apply();
    }

    public String debugSnapshot() {
        UsuarioPerfil p = getPerfil();
        List<AccesoPlantaDto> acc = getAccesos();
        return "SessionSnapshot{"
                + "user=" + getUsername()
                + ", perfilId=" + (p != null ? p.perfilId : "null")
                + ", activo=" + (p != null && p.isActivo())
                + ", nombre=" + (p != null ? p.getNombreCompleto() : "null")
                + ", accesos=" + (acc != null ? acc.size() : 0)
                + "}";
    }

    // ===== Limpiar todo =====
    public void clear() {
        sp.edit().clear().apply();
    }

    // ===== Util =====
    private static String safe(String s) { return s == null ? "" : s.trim(); }

    /** Clase interna mínima para migrar JSON antiguo (key/planta). */
    private static class OldPlantaAcceso {
        String key;
        String planta;
    }
}
