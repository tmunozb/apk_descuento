// ui/selection/sistemas/SelectionSistemasActivity.java
package com.farenet.descuentos.ui.selection.sistemas;

import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.selection.base.BaseSelectionActivity;

public class SelectionSistemasActivity extends BaseSelectionActivity {
    @Override protected Config provideConfig() {
        Config c = new Config();
        c.menuRes = R.menu.menu_selection_sistemas; // crea uno específico o reutiliza el general
        c.vNueva = c.vMis = c.vPend = c.vHist = true;
        c.vDesc = c.vCort = c.vRep = c.vDatos = c.vFeed = true;
        c.vBolsaEstado = c.vBolsaBulk = true;

        c.canNueva = c.canMis = c.canPend = c.canHist = true;
        c.canDesc = c.canCort = true;
        c.canBolsaEstado = c.canBolsaBulk = true;
        return c;
    }
}
