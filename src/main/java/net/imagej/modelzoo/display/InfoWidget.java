package net.imagej.modelzoo.display;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;
import org.scijava.ui.AbstractUIInputWidget;
import org.scijava.ui.UserInterface;
import org.scijava.ui.swing.SwingUI;
import org.scijava.ui.swing.widget.SwingInputWidget;
import org.scijava.widget.InputWidget;
import org.scijava.widget.TextWidget;
import org.scijava.widget.WidgetModel;

import javax.swing.JLabel;
import javax.swing.JPanel;

@Plugin(type = InputWidget.class, priority = Priority.HIGH)
public class InfoWidget extends AbstractUIInputWidget<String, JPanel> implements TextWidget<JPanel> {

    public static final String STYLE = "infowidget";

    JPanel panel;

    // -- InputWidget methods --

    @Override
    public String getValue() {
        return "";
    }

    // -- WrapperPlugin methods --

    @Override
    public void set(final WidgetModel model) {
        super.set(model);
        panel = new JPanel();
        JLabel label = new JLabel(get().getItem().getDescription());
        panel.add(label);
        refreshWidget();
    }

    // -- Typed methods --

    @Override
    public boolean supports(final WidgetModel model) {
        return model.isStyle(STYLE);
    }

    // -- AbstractUIInputWidget methods ---

    @Override
    public void doRefresh() { /* No-op. */ }

    @Override
    protected UserInterface ui() {
        return ui(SwingUI.NAME);
    }

    @Override
    public JPanel getComponent() {
        return panel;
    }

    @Override
    public Class<JPanel> getComponentType() {
        return JPanel.class;
    }
}
