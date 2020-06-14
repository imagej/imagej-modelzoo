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

package net.imagej.modelzoo.consumer;

import net.imagej.ImageJ;
import net.imagej.modelzoo.consumer.network.ModelExecutor;
import net.imagej.modelzoo.consumer.network.model.Model;
import net.imagej.modelzoo.consumer.postprocessing.PredictionPostprocessing;
import net.imagej.modelzoo.consumer.preprocessing.InputMappingHandler;
import net.imagej.modelzoo.consumer.preprocessing.PredictionPreprocessing;
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
	private Model model;

	@Parameter
	private LogService log;

	@Parameter
	private Context context;
	private InputMappingHandler inputHandling;
	private int nTiles = 8;
	private int batchSize = 10;

	public ModelZooPrediction(Context context) {
		inputHandling = new InputMappingHandler();
		context.inject(this);
	}

	public Map<String, Object> run() {

		final long startTime = System.currentTimeMillis();

		Map<String, Object> res = null;

		if(model == null) loadModel();

		try {
			if(!model.isInitialized()) {
				return res;
			}

			if(!inputValidationAndMapping(model)) return null;

			preprocessing(model);
			executePrediction(model);
			res = postprocessing(model);

		} catch(CancellationException e) {
			log.warn("ModelZoo prediction canceled.");
		} catch(OutOfMemoryError e) {
			e.printStackTrace();
		} finally {
			model.dispose();
		}
		log.info("ModelZoo prediction exit (took " + (System.currentTimeMillis() - startTime) + " milliseconds)");
		return res;
	}

	public void setInput(String name, RandomAccessibleInterval<?> value, String mapping) {
		inputHandling.addInput(name, value, mapping);
	}

	public File getModelFile() {
		return modelFile;
	}

	public void setModelFile(String modelFile) {
		if(modelFile != null && !modelFile.isEmpty()) setModelFile(new File(modelFile));
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

	public Model loadModel() {
		PredictionLoader loader = new PredictionLoader();
		context.inject(loader);
		loader.setModelFromFile(modelFile);
		loader.setModelFromURL(modelUrl);
		loader.run();
		this.model = loader.getModel();
		return model;
	}

	private void preprocessing(Model model) {
		PredictionPreprocessing preprocessing = new PredictionPreprocessing();
		context.inject(preprocessing);
		preprocessing.setModel(model);
		preprocessing.run();
	}

	private void executePrediction(Model model) {
		ModelExecutor executor = new ModelExecutor(model, context);
		executor.setNumberOfTiles(nTiles);
		executor.setBatchSize(batchSize);
		boolean isOutOfMemory = true;
		boolean canHandleOutOfMemory = true;

		while (isOutOfMemory && canHandleOutOfMemory) {
			try {
				executor.run();
				isOutOfMemory = false;
			}
			catch (final OutOfMemoryError e) {
				canHandleOutOfMemory = executor.increaseTiling();
			}
		}
	}

	private Map<String, Object> postprocessing(Model model) {
		PredictionPostprocessing postprocessing = new PredictionPostprocessing();
		context.inject(postprocessing);
		postprocessing.setModel(model);
		postprocessing.run();
		return postprocessing.getOutputs();
	}

	private boolean inputValidationAndMapping(Model model) {
		context.inject(inputHandling);
		inputHandling.setModel(model);
		return inputHandling.getSuccess();
	}

	public void setHarvestInputs(boolean harvestInputs) {
		inputHandling.setAskUser(harvestInputs);
	}

	public void setNumberOfTiles(int nTiles) {
		this.nTiles = nTiles;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
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

		ModelZooPrediction prediction = new ModelZooPrediction(ij.context());
		prediction.setInput("input", input, "XYZ");
		prediction.setModelFile(model);
		Map<String, Object> res = prediction.run();
		RandomAccessibleInterval output = (RandomAccessibleInterval) res.values().iterator().next();

		ij.ui().show(output);
	}
}
