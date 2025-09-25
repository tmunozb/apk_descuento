// SessionManager.java
package com.farenet.descuentos.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.farenet.descuentos.config.Constante;
import com.farenet.descuentos.models.newapi.LoginRsp;
import com.farenet.descuentos.models.newapi.UsuarioPerfil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

public class SessionManager {
    private final SharedPreferences sp;
    private final Gson gson = new Gson();

    private static final String K_USER = "user";
    private static final String K_PERFIL = "perfil_json";
    private static final String K_ACCESOS = "accesos_json";
    private static final String K_LOGIN_RAW = "login_raw_json"; // opcional: para debug


    public SessionManager(Context ctx) {
        Context app = (ctx != null) ? ctx.getApplicationContext() : null;
        if (app == null) throw new IllegalStateException("Context nulo al crear SessionManager");
        sp = app.getSharedPreferences(Constante.TOKEN, Context.MODE_PRIVATE);
    }


    // ===== username básico =====
    public void saveUsername(String user) { sp.edit().putString(K_USER, user).apply(); }
    public String getUsername() { return sp.getString(K_USER, null); }

    // ===== perfil =====
    public void savePerfil(UsuarioPerfil perfil) {
        sp.edit().putString(K_PERFIL, gson.toJson(perfil)).apply();
    }
    public UsuarioPerfil getPerfil() {
        String json = sp.getString(K_PERFIL, null);
        return json == null ? null : gson.fromJson(json, UsuarioPerfil.class);
    }

    // ===== accesos (lista) =====
    public void saveAccesos(List<LoginRsp.PlantaAcceso> accesos) {
        // De-dup por (key, planta) manteniendo orden
        if (accesos == null) accesos = Collections.emptyList();
        LinkedHashMap<String, LoginRsp.PlantaAcceso> map = new LinkedHashMap<>();
        for (LoginRsp.PlantaAcceso a : accesos) {
            if (a == null) continue;
            String k = (a.key == null ? "" : a.key.trim()) + "|" + (a.planta == null ? "" : a.planta.trim());
            map.put(k, a);
        }
        List<LoginRsp.PlantaAcceso> dedup = new ArrayList<>(map.values());
        sp.edit().putString(K_ACCESOS, gson.toJson(dedup)).apply();
    }

    public List<LoginRsp.PlantaAcceso> getAccesos() {
        String json = sp.getString(K_ACCESOS, null);
        if (json == null) return Collections.emptyList();
        Type t = new TypeToken<List<LoginRsp.PlantaAcceso>>(){}.getType();
        List<LoginRsp.PlantaAcceso> list = gson.fromJson(json, t);
        return (list == null) ? Collections.emptyList() : list;
    }

    // ===== helpers de conveniencia =====
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
        return (p == null) ? (getUsername() != null ? getUsername() : "")
                : p.getNombreCompleto();
    }



    public boolean isActivo() {
        UsuarioPerfil p = getPerfil();
        return p != null && p.isActivo();
    }

    // ===== debug opcional: guardar respuesta cruda del login nuevo =====
    public void saveLoginRaw(LoginRsp rsp) {
        sp.edit().putString(K_LOGIN_RAW, gson.toJson(rsp)).apply();
    }

    // En SessionManager
    public String debugSnapshot() {
        UsuarioPerfil p = getPerfil();
        List<com.farenet.descuentos.models.newapi.LoginRsp.PlantaAcceso> acc = getAccesos();
        return "SessionSnapshot{"
                + "user=" + getUsername()
                + ", perfilId=" + (p != null ? p.perfilId : "null")
                + ", activo=" + (p != null && p.isActivo())
                + ", nombre=" + (p != null ? p.getNombreCompleto() : "null")
                + ", accesos=" + (acc != null ? acc.size() : 0)
                + "}";
    }


    public void clear() { sp.edit().clear().apply(); }
}
