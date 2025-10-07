// ui/selection/asistente/SelectionAsistenteActivity.java
package com.farenet.descuentos.ui.selection.asistente;

import com.farenet.descuentos.R;
import com.farenet.descuentos.ui.selection.base.BaseSelectionActivity;

public class SelectionAsistenteActivity extends BaseSelectionActivity {
    @Override protected Config provideConfig() {
        Config c = new Config();
        c.menuRes = R.menu.menu_selection;
        c.vPend = true;       // si revisa pendientes
        c.vFeed = true;

        c.canPend = true;
        return c;
    }
}
