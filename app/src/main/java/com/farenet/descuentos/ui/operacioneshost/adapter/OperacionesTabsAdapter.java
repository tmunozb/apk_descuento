package com.farenet.descuentos.ui.operacioneshost.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.farenet.descuentos.ui.bolsa.BolsaFragment;
import com.farenet.descuentos.ui.home.OperacionesHomeFragment;
import com.farenet.descuentos.ui.solicitudes.cortesia.CortesiaFragment;
import com.farenet.descuentos.ui.solicitudes.crear.FragmentCortesia;
import com.farenet.descuentos.ui.solicitudes.pendientes.OperacionesPendientesFragment;
import com.farenet.descuentos.ui.solicitudes.cortesia.CortesiaActivity;
import com.farenet.descuentos.ui.solicitudes.pendientes.PendientesFragment;

public class OperacionesTabsAdapter extends FragmentStateAdapter {
    public OperacionesTabsAdapter(@NonNull androidx.fragment.app.FragmentActivity fa) { super(fa); }
    @NonNull @Override public Fragment createFragment(int position) {
        if (position == 0)
            return new OperacionesHomeFragment();
        else if (position==1)
            return new BolsaFragment();
        else if (position==2)
            return new OperacionesPendientesFragment();
            else return new FragmentCortesia();
    }
    @Override public int getItemCount() { return 4; }
}
