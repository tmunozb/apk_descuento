// SelectionComercialActivity.java
package com.farenet.descuentos.ui.selection.comercial;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.bolsa.BolsaEstadoActivity;
import com.farenet.descuentos.ui.selection.base.BaseSelectionActivity;
import com.farenet.descuentos.ui.solicitudes.crear.SolicitudCrearActivity;
import com.farenet.descuentos.ui.solicitudes.historial.HistorialSolicitudesActivity;
import com.farenet.descuentos.ui.solicitudes.pendientes.SolicitudesPendientesActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class SelectionComercialActivity extends BaseSelectionActivity {
    @Override protected Config provideConfig() {
        Config c = new Config();
        c.layoutRes = R.layout.activity_selection_comercial; // tu layout
        c.menuRes   = R.menu.menu_selection_comercial;       // overflow del Toolbar
        return c;
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Toolbar: manejar clicks del menú (logout, etc.)
        if (toolbar != null) {
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.nav_logout) {
                    // reutiliza el diálogo del base vía método público o implementa aquí
                    // versión rápida:
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Cerrar sesión")
                            .setMessage("¿Desea cerrar sesión y borrar el caché local?")
                            .setPositiveButton("Sí", (d,w) -> {
                                session.clear();
                                startActivity(new Intent(this, com.farenet.descuentos.ui.auth.LoginActivity.class)
                                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                | Intent.FLAG_ACTIVITY_NEW_TASK
                                                | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                finish();
                            })
                            .setNegativeButton("No", null)
                            .show();
                    return true;
                }
                return false;
            });
        }

        // Pinta el saludo
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        if (tvWelcome != null) {
            String nombre = session.getNombreVisible();
            tvWelcome.setText("¡Hola, " + (nombre == null ? "Comercial" : nombre) + "!");
        }

        // Configura los tiles (ids del layout comercial)
        MaterialCardView tileNueva = findViewById(R.id.bolsa_estado);
        if (tileNueva != null) {
            ((TextView) tileNueva.findViewById(R.id.tvTitle)).setText("Bolsa");
            ((TextView) tileNueva.findViewById(R.id.tvSubtitle)).setText("Descuento Autorizado");
            ((ImageView) tileNueva.findViewById(R.id.ivIcon)).setImageResource(R.drawable.ic_add_circle_24);
            tileNueva.setOnClickListener(v ->
                    startActivity(new Intent(this, BolsaEstadoActivity.class)));
        }

        MaterialCardView tilePend = findViewById(R.id.tilePendientesAut);
        if (tilePend != null) {
            ((TextView) tilePend.findViewById(R.id.tvTitle)).setText("Pendientes (Aut.)");
            ((TextView) tilePend.findViewById(R.id.tvSubtitle)).setText("Aprobar / Autorizar");
            ((ImageView) tilePend.findViewById(R.id.ivIcon)).setImageResource(R.drawable.ic_pending_actions_24);
            tilePend.setOnClickListener(v ->
                    startActivity(new Intent(this, SolicitudesPendientesActivity.class)));
        }

        MaterialCardView tileHist = findViewById(R.id.tileHistorial);
        if (tileHist != null) {
            ((TextView) tileHist.findViewById(R.id.tvTitle)).setText("Historial");
            ((TextView) tileHist.findViewById(R.id.tvSubtitle)).setText("Aprobadas / Rechazadas");
            ((ImageView) tileHist.findViewById(R.id.ivIcon)).setImageResource(R.drawable.ic_history_24);
            tileHist.setOnClickListener(v ->
                    startActivity(new Intent(this, HistorialSolicitudesActivity.class)));
        }

        MaterialCardView tileRep = findViewById(R.id.tileReportes);
        if (tileRep != null) {
            ((TextView) tileRep.findViewById(R.id.tvTitle)).setText("Reportes");
            ((TextView) tileRep.findViewById(R.id.tvSubtitle)).setText("KPIs / Exportar");
            ((ImageView) tileRep.findViewById(R.id.ivIcon)).setImageResource(R.drawable.ic_bar_chart);
            tileRep.setOnClickListener(v -> { /* abrir módulo reportes */ });
        }

        // Bottom navigation
        BottomNavigationView bottom = findViewById(R.id.bottomNav);
        if (bottom != null) {
            bottom.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.tab_home) return true; // ya estás aquí
                if (id == R.id.tab_solicitudes) {
                    startActivity(new Intent(this, com.farenet.descuentos.ui.solicitudes.lista.SolicitudesListaActivity.class));
                    return true;
                }
                if (id == R.id.tab_reportes) {
                    // abrir reportes
                    return true;
                }
                return false;
            });
            bottom.setSelectedItemId(R.id.tab_home);
        }
    }
}
