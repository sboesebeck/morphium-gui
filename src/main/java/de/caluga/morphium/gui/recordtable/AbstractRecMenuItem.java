package de.caluga.morphium.gui.recordtable;

import javax.swing.*;

/**
 * User: Stephan Bösebeck
 * Date: 31.03.12
 * Time: 16:06
 * <p/>
 * TODO: Add documentation here
 */
public abstract class AbstractRecMenuItem extends JMenuItem {
    public AbstractRecMenuItem(String label) {
        super(label);
    }

    public AbstractRecMenuItem() {
        super();
    }

    public abstract boolean isEnabled(Object selectedRecord);
}
