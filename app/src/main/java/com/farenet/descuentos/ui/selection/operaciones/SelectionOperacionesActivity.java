// ui/selection/operaciones/SelectionOperacionesActivity.java
package com.farenet.descuentos.ui.selection.operaciones;

import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.selection.base.BaseSelectionActivity;

public class SelectionOperacionesActivity extends BaseSelectionActivity {
    @Override protected Config provideConfig() {
        Config c = new Config();
        c.menuRes = R.menu.menu_selection_operaciones;
        c.vPend = c.vCort = true;
        c.vHist = true; // si corresponde
        c.vFeed = true;

        c.canPend = true;
        c.canCort = true;
        c.canHist = true;
        return c;
    }
}
