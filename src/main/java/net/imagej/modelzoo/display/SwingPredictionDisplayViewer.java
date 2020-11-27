/*-
 * #%L
 * This is the bioimage.io modelzoo library for ImageJ.
 * %%
 * Copyright (C) 2019 - 2020 Center for Systems Biology Dresden
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

import io.bioimage.specification.ModelSpecification;
import net.imagej.modelzoo.consumer.ModelZooPrediction;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.scijava.plugin.Plugin;
import org.scijava.ui.viewer.DisplayViewer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.Component;
import java.awt.Dimension;

@Plugin(type = DisplayViewer.class)
public class SwingPredictionDisplayViewer extends EasySwingDisplayViewer<ModelZooPrediction> {

	public SwingPredictionDisplayViewer() {
		super(ModelZooPrediction.class);
	}

	@Override
	protected boolean canView(ModelZooPrediction value) {
		return true;
	}

	@Override
	protected void redoLayout() {

	}

	@Override
	protected void setLabel(String s) {

	}

	@Override
	protected void redraw() {
		getWindow().pack();
	}

	@Override
	protected JPanel createDisplayPanel(ModelZooPrediction prediction) {
		JPanel panel = new JPanel(new MigLayout());
		panel.add(createMetaDataPanel(prediction), "spanx");
		panel.add(createProgressBar(prediction), "newline, growx, pushx");
		panel.add(createActionBar(prediction));
		prediction.addCallbackOnCompleted(getWindow()::close);
		return panel;
	}

	private Component createMetaDataPanel(ModelZooPrediction prediction) {
		JPanel panel = new JPanel();
		ModelSpecification specification = prediction.getTrainedModel().getSpecification();
		panel.add(new JLabel("Predicting model " + specification.getName() + ".."));
		return panel;
	}

	//
	private Component createActionBar(ModelZooPrediction prediction) {
		JPanel panel = new JPanel();
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> new Thread(() -> {
			cancel.setEnabled(false);
			prediction.cancel();
			getWindow().close();
		}).start());
		panel.add(cancel);
		return panel;
	}

	private JProgressBar createProgressBar(ModelZooPrediction value) {
		JProgressBar progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		progressBar.setMinimumSize(new Dimension(70, 25));
		return progressBar;
	}
}
