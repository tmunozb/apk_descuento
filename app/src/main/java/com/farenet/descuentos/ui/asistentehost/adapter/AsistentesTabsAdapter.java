package com.farenet.descuentos.ui.asistentehost.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.farenet.descuentos.ui.bolsa.BolsaFragment;
import com.farenet.descuentos.ui.home.AsistentesHomeFragment;
import com.farenet.descuentos.ui.solicitudes.pendientes.AsistentesPendientesFragment;



public class AsistentesTabsAdapter extends FragmentStateAdapter {

    public AsistentesTabsAdapter(@NonNull androidx.fragment.app.FragmentActivity fa) { super(fa); }
    @NonNull @Override public Fragment createFragment(int position) {
        if (position == 0) return new AsistentesHomeFragment();
        else return new AsistentesPendientesFragment();
    }
    @Override public int getItemCount() { return 2; }
}
