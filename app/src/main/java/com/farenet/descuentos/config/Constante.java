package com.farenet.descuentos.config;

import com.farenet.descuentos.repository.DescuentoRepository;
import com.farenet.descuentos.repository.LoginRepository;
import com.farenet.descuentos.repository.MaestroRepository;

/**
 * Author by Alexis Pumayalla on 28/08/19.
 * Email apumayallag@gmail.com
 * Phone 961778965
 */
public class Constante {

    public static final String API_URL = "https://api.farenet.net";

    //key shpf
    public static final String TOKEN = "TOKEN";

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
