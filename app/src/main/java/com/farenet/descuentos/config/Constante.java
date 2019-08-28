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

    public static final String API_URL = "http://34.209.251.245:8080";

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

    public static DescuentoRepository getDescuentoRepository(){
        return RetrofitMaestro.getMaestros(API_URL).create(DescuentoRepository.class);
    }

}
