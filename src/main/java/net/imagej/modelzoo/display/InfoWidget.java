/*-
 * #%L
 * This is the bioimage.io modelzoo library for ImageJ.
 * %%
 * Copyright (C) 2019 - 2021 Center for Systems Biology Dresden
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
