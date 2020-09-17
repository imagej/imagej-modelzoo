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

package net.imagej.modelzoo.consumer.commands;

import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.consumer.SanityCheck;
import net.imagej.modelzoo.display.InfoWidget;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.module.Module;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static net.imagej.modelzoo.consumer.commands.DefaultModelZooSanityCheckFromFileCommand.descriptionText;

@Plugin(type = Command.class)
public class DefaultModelZooSanityCheckFromImageCommand extends DynamicCommand {

	@Parameter(label = "Import model (.zip) from file")
	private File modelFile;

	@Parameter
	private SingleImagePredictionCommand predictionCommand;

	@Parameter(label="<html><h1>Sanity check</h1>", description = descriptionText, required = false, style = InfoWidget.STYLE)
	private String description = "";

	@Parameter(label = "Prediction input image", persist = false)
	private Dataset input;

	@Parameter(label = "Expected result image", persist = false)
	private Dataset inputGroundTruth;

	@Parameter
	private LogService log;

	@Parameter
	private StatusService status;

	@Parameter
	private UIService uiService;

	@Parameter
	private OpService opService;

	@Parameter
	private ModelZooService modelZooService;

	private static final int numBins = 20;

	public void run() {

		final long startTime = System.currentTimeMillis();
		log("ModelZoo sanity check start");

		Module predictionModule = (Module) predictionCommand;
		getInputs().forEach(predictionModule::setInput);
			try {
				predictionModule.setInput("input", input);
				predictionModule.getInputs().forEach(this::setInput);
				context().service(ModuleService.class).run((Module)predictionCommand, true).get();
				predictionModule.getInputs().forEach(this::setInput);
				Dataset output = (Dataset) predictionModule.getOutput("output");
				uiService.show("result after prediction", output);
				ModelZooArchive model = modelZooService.open(modelFile);
				SanityCheck.compare(inputGroundTruth, input, output, model.getTestInput(), model.getTestOutput(), opService);
			} catch (ExecutionException | InterruptedException | IOException e) {
				e.printStackTrace();
			}

		log("ModelZoo sanity check exit (took " + (System.currentTimeMillis() - startTime) + " milliseconds)");

	}

	private void log(String msg) {
		log.info(msg);
		status.showStatus(msg);
	}
}
