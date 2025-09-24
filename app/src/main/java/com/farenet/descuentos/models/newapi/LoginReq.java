package com.farenet.descuentos.models.newapi;

import com.google.gson.annotations.SerializedName;

public class LoginReq {

    @SerializedName("username")
    public String username;

    @SerializedName("password")
    public String password;

    public LoginReq(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public LoginReq() {} // requerido si alguna vez lo instancias sin args
}
