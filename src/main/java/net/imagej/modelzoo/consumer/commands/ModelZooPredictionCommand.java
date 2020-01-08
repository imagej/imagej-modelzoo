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

import net.imagej.ImageJ;
import net.imagej.modelzoo.consumer.commands.postprocessing.PredictionPostprocessing;
import net.imagej.modelzoo.consumer.commands.preprocessing.PredictionInputHarvesting;
import net.imagej.modelzoo.consumer.commands.preprocessing.PredictionPreprocessing;
import net.imagej.modelzoo.consumer.network.model.Model;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

@Plugin(type = Command.class)
public class ModelZooPredictionCommand implements Command {

	@Parameter
	private Img input;

	@Parameter(label = "Import model (.zip)", required = false)
	private File modelFile;

	@Parameter(label = "Import model (.zip) from URL", required = false)
	private String modelUrl;

	@Parameter(type = ItemIO.OUTPUT)
	private RandomAccessibleInterval output;

	@Parameter
	private LogService log;

	@Parameter
	private Context context;

	public void run() {

		final long startTime = System.currentTimeMillis();

		try {

			Model model = loadModel();

			if(!model.isInitialized()) {
				return;
			}

			if(!inputValidationAndMapping(model)) return;

			preprocessing(model);
			executePrediction(model);
			postprocessing(model);

		} catch(CancellationException e) {
			log.warn("ModelZoo prediction canceled.");
		} catch(OutOfMemoryError e) {
			e.printStackTrace();
		}
		log.info("ModelZoo prediction exit (took " + (System.currentTimeMillis() - startTime) + " milliseconds)");

	}

	private Model loadModel() {
		PredictionLoader loader = new PredictionLoader();
		context.inject(loader);
		loader.setModelFromFile(modelFile);
		loader.setModelFromURL(modelUrl);
		loader.run();
		return loader.getModel();
	}

	private void preprocessing(Model model) {
		PredictionPreprocessing preprocessing = new PredictionPreprocessing();
		context.inject(preprocessing);
		preprocessing.setModel(model);
		preprocessing.run();
	}

	private void executePrediction(Model model) {
		PredictionExecutor executor = new PredictionExecutor();
		context.inject(executor);
		executor.setModel(model);
		executor.run();
	}

	private void postprocessing(Model model) {
		PredictionPostprocessing postprocessing = new PredictionPostprocessing();
		context.inject(postprocessing);
		postprocessing.setModel(model);
		postprocessing.run();
		Map<String, Object> outputs = postprocessing.getOutputs();
		this.output = (RandomAccessibleInterval) outputs.values().iterator().next();
	}

	private boolean inputValidationAndMapping(Model model) {
		PredictionInputHarvesting inputHarvesting = new PredictionInputHarvesting();
		context.inject(inputHarvesting);
		inputHarvesting.setModel(model);
		inputHarvesting.addInput("input", input);
		inputHarvesting.run();
		return inputHarvesting.getSuccess();
	}

	public static void main(String...args) throws IOException, URISyntaxException, ExecutionException, InterruptedException {
		ImageJ ij = new ImageJ();
		ij.launch();

		Path img = Paths.get(ModelZooPredictionCommand.class.getClassLoader()
				.getResource("denoise2D/input.tif").toURI());

		Img input = (Img) ij.io().open(img.toAbsolutePath().toString());

		ij.ui().show(input);

		Path model = Paths.get(ModelZooPredictionCommand.class.getClassLoader()
				.getResource("denoise2D/model.zip").toURI());

		CommandModule module = ij.command().run(ModelZooPredictionCommand.class,
				false,
				"input", input,
				"modelFile", model.toFile()).get();

		RandomAccessibleInterval output = (RandomAccessibleInterval) module.getOutput("output");

		ij.ui().show(output);
	}

}
