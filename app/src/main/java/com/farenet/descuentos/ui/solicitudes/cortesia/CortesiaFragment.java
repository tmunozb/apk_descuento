package com.farenet.descuentos.ui.solicitudes.cortesia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.farenet.descuentos.R;

public class CortesiaFragment extends Fragment {

    // (opcional) Factory si necesitas pasar argumentos
    public static CortesiaFragment newInstance(/* String arg1, ... */) {
        CortesiaFragment f = new CortesiaFragment();
        Bundle b = new Bundle();
        // b.putString("key", arg1);
        f.setArguments(b);
        return f;
    }

    public CortesiaFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cortesia_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TODO: aquí mueve la lógica que tenías en el Activity:
        // - findViewById sobre 'view' (no sobre la Activity)
        // - listeners, viewmodels, llamadas a API, etc.
        //
        // Ejemplo:
        // TextView tv = view.findViewById(R.id.tvAlgo);
        // tv.setText("...");

        // Si usabas Toolbar propia del Activity, ahora usa la de tu host (requireActivity().findViewById(...))
        // o añade un toolbar dentro del layout del fragment si quieres uno específico.
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpia callbacks/listeners si aplica
    }
}
