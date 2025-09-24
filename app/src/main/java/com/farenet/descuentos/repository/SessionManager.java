package com.farenet.descuentos.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.farenet.descuentos.config.Constante;
import com.farenet.descuentos.models.newapi.UsuarioPerfil;
import com.google.gson.Gson;

public class SessionManager {
    private final SharedPreferences sp;
    private final Gson gson = new Gson();

    public SessionManager(Context ctx) {
        sp = ctx.getSharedPreferences(Constante.TOKEN, Context.MODE_PRIVATE);
    }

    public void saveUsername(String user) {
        sp.edit().putString("user", user).apply();
    }

    public String getUsername() {
        return sp.getString("user", null);
    }

    public void savePerfil(UsuarioPerfil perfil) {
        sp.edit().putString("perfil_json", gson.toJson(perfil)).apply();
    }

    public UsuarioPerfil getPerfil() {
        String json = sp.getString("perfil_json", null);
        if (json == null) return null;
        return gson.fromJson(json, UsuarioPerfil.class);
    }

    public void clear() {
        sp.edit().clear().apply();
    }
}
