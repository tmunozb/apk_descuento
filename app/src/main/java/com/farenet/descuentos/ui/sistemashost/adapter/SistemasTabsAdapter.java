package com.farenet.descuentos.ui.sistemashost.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.farenet.descuentos.ui.bolsa.BolsaBulkFragment;
import com.farenet.descuentos.ui.bolsa.BolsaFragment;
import com.farenet.descuentos.ui.home.OperacionesHomeFragment;
import com.farenet.descuentos.ui.home.SistemasHomeFragment;
import com.farenet.descuentos.ui.solicitudes.cortesia.CortesiaFragment;
import com.farenet.descuentos.ui.solicitudes.crear.FragmentCortesia;
import com.farenet.descuentos.ui.solicitudes.crear.FragmentDescuento;
import com.farenet.descuentos.ui.solicitudes.historial.HistorialSolicitudesFragment;
import com.farenet.descuentos.ui.solicitudes.pendientes.OperacionesPendientesFragment;
import com.farenet.descuentos.ui.solicitudes.cortesia.CortesiaActivity;
import com.farenet.descuentos.ui.solicitudes.pendientes.PendientesFragment;
public class SistemasTabsAdapter extends FragmentStateAdapter {

    public SistemasTabsAdapter(@NonNull androidx.fragment.app.FragmentActivity fa) { super(fa); }
    @NonNull @Override public Fragment createFragment(int position) {
        if (position == 0)
            return new SistemasHomeFragment();
        else if (position==1)
            return new FragmentDescuento();
        else if (position==2)
            return new BolsaFragment();
        else if (position==3)
            return new HistorialSolicitudesFragment();
        else if (position==4)
            return new FragmentCortesia();
        else return new BolsaBulkFragment();
    }
    @Override public int getItemCount() { return 6; }
}
