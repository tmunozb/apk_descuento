package com.farenet.descuentos.Core.config;

import com.farenet.descuentos.Core.Network.RetrofitMaestro;
import com.farenet.descuentos.API.Antigua.Service.DescuentoRepository;
import com.farenet.descuentos.API.Antigua.Service.LoginRepository;
import com.farenet.descuentos.API.Antigua.Service.MaestroRepository;


public class Constante {

    public static final String API_URL = "https://api.farenet.net";

    //key sharedprefences
    public static final String TOKEN = "TOKEN";

    public static final boolean USE_NEW_LOGIN = true; // pon false si quieres usar el legacy

    public static LoginRepository getLoginRespository(){
        return RetrofitMaestro.getMaestros(API_URL).create(LoginRepository.class);
    }

    public static MaestroRepository getMaestroRespository(){
        return RetrofitMaestro.getMaestros(API_URL).create(MaestroRepository.class);
    }

    public static DescuentoRepository getDescuentoRepository(){
        return RetrofitMaestro.getMaestros(API_URL).create(DescuentoRepository.class);
    }

}
