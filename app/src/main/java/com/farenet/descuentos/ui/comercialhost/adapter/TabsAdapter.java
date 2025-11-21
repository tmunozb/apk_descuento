package com.farenet.descuentos.ui.comercialhost.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.farenet.descuentos.ui.home.HomeFragment;
import com.farenet.descuentos.ui.bolsa.BolsaFragment;
import com.farenet.descuentos.ui.solicitudes.historial.HistorialSolicitudesFragment;
import com.farenet.descuentos.ui.solicitudes.pendientes.PendientesFragment;

public class TabsAdapter extends FragmentStateAdapter {

    public TabsAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new HomeFragment();
            case 1: return new BolsaFragment();
            case 2: return new PendientesFragment();
            default: return new HistorialSolicitudesFragment();
        }
    }

    @Override
    public int getItemCount() { return 4; }
}
