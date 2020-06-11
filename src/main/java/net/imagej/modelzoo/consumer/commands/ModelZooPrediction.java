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
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CancellationException;

public class ModelZooPrediction {

	private File modelFile;
	private String modelUrl;
	private RandomAccessibleInterval output;

	@Parameter
	private LogService log;

	@Parameter
	private Context context;
	private PredictionInputHarvesting inputHarvesting;

	public ModelZooPrediction() {
		inputHarvesting = new PredictionInputHarvesting();
	}

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

	public void setInput(String name, Object value) {
		inputHarvesting.addInput(name, value);
	}

	public File getModelFile() {
		return modelFile;
	}

	public void setModelFile(File modelFile) {
		this.modelFile = modelFile;
	}

	public String getModelUrl() {
		return modelUrl;
	}

	public void setModelUrl(String modelUrl) {
		this.modelUrl = modelUrl;
	}

	public RandomAccessibleInterval getOutput() {
		return output;
	}

	public void setOutput(RandomAccessibleInterval output) {
		this.output = output;
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
		context.inject(inputHarvesting);
		inputHarvesting.setModel(model);
		inputHarvesting.run();
		return inputHarvesting.getSuccess();
	}

	public void setHarvestInputs(boolean harvestInputs) {
		inputHarvesting.setAskUser(harvestInputs);
	}

	public static void main(String...args) throws IOException, URISyntaxException {
		ImageJ ij = new ImageJ();
		ij.launch();

		Path img = Paths.get(ModelZooPrediction.class.getClassLoader()
				.getResource("denoise2D/input.tif").toURI());

		Img input = (Img) ij.io().open(img.toAbsolutePath().toString());

		ij.ui().show(input);

		File model = new File(ModelZooPrediction.class.getClassLoader()
				.getResource("denoise2D/model.zip").toURI());

		ModelZooPrediction prediction = new ModelZooPrediction();
		ij.context().inject(prediction);
		prediction.setInput("input", input);
		prediction.setModelFile(model);
		prediction.run();
		RandomAccessibleInterval output = prediction.getOutput();

		ij.ui().show(output);
	}
}
