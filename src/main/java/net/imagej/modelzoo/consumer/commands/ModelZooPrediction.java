/*-
 * #%L
 * ImageJ ModelZoo Consumer
 * %%
 * Copyright (C) 2019 MPI-CBG
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

import net.imagej.Dataset;
import net.imagej.modelzoo.consumer.network.DefaultInputMapper;
import net.imagej.modelzoo.consumer.network.DefaultInputValidator;
import net.imagej.modelzoo.consumer.network.InputMapper;
import net.imagej.modelzoo.consumer.network.InputValidator;
import net.imagej.modelzoo.consumer.network.model.Model;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

@Plugin(type = Command.class)
public class ModelZooPrediction implements Command {

	@Parameter(initializer = "input")
	private Dataset input;

	@Parameter(label = "Import model (.zip)", required = false)
	private File modelFile;

	@Parameter(label = "Import model (.zip) from URL", required = false)
	private String modelUrl;

	@Parameter(type = ItemIO.OUTPUT)
	private Dataset output;


	@Parameter
	private LogService log;

	@Parameter
	private CommandService commandService;

	@Parameter
	private UIService uiService;

	private final InputValidator inputValidator = new DefaultInputValidator();
	private final InputMapper inputMapper = new DefaultInputMapper();

	protected boolean initialized = false;

	public void run() {

		final long startTime = System.currentTimeMillis();

		if (noInputData()) return;

		try {

			CommandModule loaderModule = commandService.run(PredictionLoader.class, false,
					"modelFile", modelFile,
					"modelUrl", modelUrl).get();

			Model model = (Model) loaderModule.getOutput("model");

			if(!model.isInitialized()) {
				return;
			}

			boolean validInput = inputValidationAndMapping(model);

			if(!validInput) return;

			CommandModule preprocessorModule = commandService.run(PredictionPreprocessing.class, false,
					"input", input,
					"model", model).get();

			List<RandomAccessibleInterval> processedInput = (List<RandomAccessibleInterval>) preprocessorModule.getOutput("output");

			CommandModule executorModule = commandService.run(PredictionExecutor.class, false,
					"input", processedInput,
					"model", model).get();

			List<RandomAccessibleInterval<FloatType>> output = (List<RandomAccessibleInterval<FloatType>>) executorModule.getOutput("output");

			CommandModule postprocessorModule = commandService.run(PredictionPostprocessing.class, false,
					"input", output,
					"model", model).get();

			this.output = (Dataset) postprocessorModule.getOutput("output");

		} catch(CancellationException e) {
			log.warn("ModelZoo prediction canceled.");
			return;
		} catch(OutOfMemoryError e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		log.info("ModelZoo prediction exit (took " + (System.currentTimeMillis() - startTime) + " milliseconds)");

	}

	private boolean inputValidationAndMapping(Model model) {

		inputMapper.run(getInput(), model);
		try {
			inputValidator.run(getInput(), model);
		}
		catch(IncompatibleTypeException e) {
			log.error(e.getMessage());
			return false;
		}

		return true;
	}

	private boolean noInputData() {
		boolean noInput = getInput() == null;
		if(noInput) {
			if(isHeadless()) {
				log.error("Please open an image first");
			}else {
				showError("Please open an image first");
			}
		}
		return noInput;
	}

	private static void showError(final String errorMsg) {
		JOptionPane.showMessageDialog(null, errorMsg, "Error",
			JOptionPane.ERROR_MESSAGE);
	}

	private Dataset getInput() {
		return input;
	}

	private boolean isHeadless() {
		return uiService.isHeadless();
	}

}
