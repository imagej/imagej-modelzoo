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

package net.imagej.modelzoo.consumer;

import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.consumer.model.ImageDataReference;
import net.imagej.modelzoo.consumer.model.ImageNode;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.ModelZooNode;
import net.imagej.modelzoo.consumer.model.NodeProcessor;
import net.imagej.modelzoo.consumer.model.NodeProcessorException;
import net.imagej.modelzoo.consumer.preprocessing.InputMappingHandler;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
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
	private boolean tilingEnabled = true;

	private Path cacheDir = null;

	private Map<String, Object> outputs;

	private boolean contextInjected = false;

	public DefaultModelZooPrediction() {
		inputHandling = new InputMappingHandler();
	}

	public DefaultModelZooPrediction(Context context) {
		this();
		context.inject(this);
	}

	@Override
	public void run() throws OutOfMemoryError, Exception {

		ModelZooModel model = loadModel(modelArchive);
		if (!validateModel(model)) return;
		try {
			preprocessing(model);
			executePrediction(model);
			postprocessing(model);
			log.info("Prediction done.");
		} finally {
			model.dispose();
		}
	}

	protected boolean validateModel(ModelZooModel model) {
		if (model == null || !model.isInitialized()) {
			log.error("Model does not exist or cannot be loaded. Exiting.");
			if(model != null) model.dispose();
			return false;
		}
		if(!inputValidationAndMapping(model)) {
			log.error("Model and input data do not match. Exiting.");
			model.dispose();
			return false;
		}
		return true;
	}

	protected ModelZooModel loadModel(ModelZooArchive modelArchive) throws Exception {
		return modelArchive.createModelInstance();
	}

	@Override
	public void setInput(String name, Object value, String axes) {
		inputHandling.addInput(name, value, axes);
	}

	protected void preprocessing(ModelZooModel model) throws NodeProcessorException {
		for (ModelZooNode<?> inputNode : model.getInputNodes()) {
			for (NodeProcessor processor : inputNode.getProcessors()) {
				processor.run();
			}
		}
	}

	protected void postprocessing(ModelZooModel model) throws NodeProcessorException {
		for (ModelZooNode<?> outputNode : model.getOutputNodes()) {
			for (NodeProcessor processor : outputNode.getProcessors()) {
				processor.run();
			}
		}
		outputs = new HashMap<>();
		model.getOutputNodes().forEach(node -> {
			if(ImageNode.class.isAssignableFrom(node.getClass())) {
				outputs.put(node.getName(), ((ImageNode)node).getData().getData());
			} else {
				outputs.put(node.getName(), node.getData());
			}
		});
	}

	@Override
	public Map<String, Object> getOutputs() {
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

	@Override
	public void setCacheDir(Path cacheDir) {
		this.cacheDir = cacheDir;
	}

	protected void executePrediction(ModelZooModel model) throws OutOfMemoryError {
		TiledPredictionExecutor executor = new TiledPredictionExecutor(model, context);
		executor.setTilingEnabled(tilingEnabled);
		executor.setNumberOfTiles(nTiles);
		executor.setBatchSize(batchSize);
		executor.setCacheDir(cacheDir);
		boolean isOutOfMemory = true;
		boolean canHandleOutOfMemory = true;

		while (isOutOfMemory) {
			try {
				executor.run();
				isOutOfMemory = false;
			} catch (final OutOfMemoryError e) {
				log.debug(e);
				canHandleOutOfMemory = executor.increaseTiling();
				setNumberOfTiles(executor.getNumberOfTiles());
				setBatchSize(executor.getBatchSize());
				if(!canHandleOutOfMemory) throw new OutOfMemoryError();
			} finally {
				executor.dispose();
			}
		}
	}

	protected boolean inputValidationAndMapping(ModelZooModel model) {
		if(!contextInjected) {
			contextInjected = true;
			context.inject(inputHandling);
		}
		inputHandling.setModel(model);
		return inputHandling.getSuccess();
	}

	Context context() {
		return context;
	}
}
