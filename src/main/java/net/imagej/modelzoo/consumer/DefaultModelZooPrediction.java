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

import io.scif.MissingLibraryException;
import net.imagej.ImageJ;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.postprocessing.PredictionPostprocessing;
import net.imagej.modelzoo.consumer.preprocessing.InputMappingHandler;
import net.imagej.modelzoo.consumer.preprocessing.PredictionPreprocessing;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class DefaultModelZooPrediction implements ModelZooPrediction {

	private ModelZooArchive modelArchive;

	@Parameter
	private LogService log;

	@Parameter
	private Context context;

	@Parameter
	private ModelZooService modelZooService;

	private final InputMappingHandler inputHandling;
	private int nTiles = 8;
	private int batchSize = 10;
	boolean tilingEnabled = true;
	private Map<String, RandomAccessibleInterval<?>> outputs;

	public DefaultModelZooPrediction(Context context) {
		inputHandling = new InputMappingHandler();
		context.inject(this);
	}

	@Override
	public void run() throws OutOfMemoryError, FileNotFoundException, MissingLibraryException {

		ModelZooModel model = loadModel(modelArchive);

		if (model == null || !model.isInitialized() || !inputValidationAndMapping(model)) {
			log.error("Model does not exist or cannot be loaded. Exiting.");
			if(model != null) model.dispose();
			return;
		}
		try {
			preprocessing(model);
			executePrediction(model);
			postprocessing(model);
		} finally {
			model.dispose();
		}
	}

	private ModelZooModel loadModel(ModelZooArchive modelArchive) throws FileNotFoundException, MissingLibraryException {
		return modelArchive.createModelInstance();
	}

	@Override
	public void setInput(String name, RandomAccessibleInterval<?> value, String axes) {
		inputHandling.addInput(name, value, axes);
	}

	private void preprocessing(ModelZooModel model) {
		PredictionPreprocessing preprocessing = new PredictionPreprocessing(context);
		preprocessing.setModel(model);
		preprocessing.run();
	}

	private void postprocessing(ModelZooModel model) {
		PredictionPostprocessing postprocessing = new PredictionPostprocessing(context);
		postprocessing.setModel(model);
		postprocessing.run();
		this.outputs = postprocessing.getOutputs();
	}

	@Override
	public Map<String, RandomAccessibleInterval<?>> getOutputs() {
		return outputs;
	}

	@Override
	public void setTilingEnabled(boolean enabled) {
		this.tilingEnabled = tilingEnabled;
	}

	@Override
	public void setNumberOfTiles(int nTiles) {
		this.nTiles = nTiles;
	}

	@Override
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	@Override
	public ModelZooArchive getTrainedModel() {
		return modelArchive;
	}

	@Override
	public void setTrainedModel(ModelZooArchive trainedModel) {
		this.modelArchive = trainedModel;
	}

	public void setTrainedModel(String trainedModel) throws IOException {
		setTrainedModel(modelZooService.open(trainedModel));
	}

	private void executePrediction(ModelZooModel model) throws OutOfMemoryError {
		TiledPredictionExecutor executor = new TiledPredictionExecutor(model, context);
		executor.setTilingEnabled(tilingEnabled);
		executor.setNumberOfTiles(nTiles);
		executor.setBatchSize(batchSize);
		boolean isOutOfMemory = true;
		boolean canHandleOutOfMemory = true;

		while (isOutOfMemory) {
			try {
				executor.run();
				isOutOfMemory = false;
			} catch (final OutOfMemoryError e) {
				canHandleOutOfMemory = executor.increaseTiling();
				setNumberOfTiles(executor.getNumberOfTiles());
				setBatchSize(executor.getBatchSize());
				if(!canHandleOutOfMemory) throw new OutOfMemoryError();
			}
		}
	}

	private boolean inputValidationAndMapping(ModelZooModel model) {
		context.inject(inputHandling);
		inputHandling.setModel(model);
		return inputHandling.getSuccess();
	}
}
