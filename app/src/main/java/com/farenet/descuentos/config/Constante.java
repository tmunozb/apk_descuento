package com.farenet.descuentos.config;

import com.farenet.descuentos.repository.LoginRepository;
import com.farenet.descuentos.repository.MaestroRepository;

public class Constante {

    public static final String API_URL = "https://api.farenet.net";

    //key shpf
    public static final String TOKEN = "TOKEN";

    public static final String user = "apumayalla";
    public static final String password = "123qwe";

    public static LoginRepository getLoginRespository(){
        return RetrofitMaestro.getMaestros(API_URL).create(LoginRepository.class);
    }

    public static MaestroRepository getMaestroRespository(){
        return RetrofitMaestro.getMaestros(API_URL).create(MaestroRepository.class);
    }

}
