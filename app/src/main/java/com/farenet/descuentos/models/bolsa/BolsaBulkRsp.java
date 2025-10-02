package com.farenet.descuentos.models.bolsa;

import java.io.Serializable;
import java.util.List;

public class BolsaBulkRsp implements Serializable {
    public String periodo;
    public Integer count;
    public List<BolsaConfigDto> items;
}
