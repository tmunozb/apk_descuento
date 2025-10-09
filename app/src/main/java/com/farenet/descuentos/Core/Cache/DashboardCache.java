package com.farenet.descuentos.Core.Cache;

import com.farenet.descuentos.API.Actual.DTO.solicitudes.SolicitudDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DashboardCache {
    private DashboardCache() {}

    private static int kpiPendAut = -1;
    private static int kpiAprobadas = -1;
    private static final List<SolicitudDto> ultimas = new ArrayList<>();

    public static synchronized void setKpiPendAut(int v) { kpiPendAut = v; }
    public static synchronized void setKpiAprobadas(int v) { kpiAprobadas = v; }

    public static synchronized int getKpiPendAut() { return kpiPendAut; }
    public static synchronized int getKpiAprobadas() { return kpiAprobadas; }

    public static synchronized void setUltimas(List<SolicitudDto> list) {
        ultimas.clear();
        if (list != null) ultimas.addAll(list);
    }
    public static synchronized List<SolicitudDto> getUltimas() {
        return Collections.unmodifiableList(new ArrayList<>(ultimas));
    }

    public static synchronized boolean hasAny() {
        return kpiPendAut >= 0 || kpiAprobadas >= 0 || !ultimas.isEmpty();
    }

    public static synchronized void clear() {
        kpiPendAut = -1;
        kpiAprobadas = -1;
        ultimas.clear();
    }
}
