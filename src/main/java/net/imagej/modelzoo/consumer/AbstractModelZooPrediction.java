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
import net.imagej.modelzoo.consumer.model.node.ImageNode;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.prediction.PredictionInput;
import net.imagej.modelzoo.consumer.model.prediction.PredictionOutput;
import net.imagej.modelzoo.consumer.model.node.ModelZooNode;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessorException;
import net.imagej.modelzoo.consumer.preprocessing.InputMappingHandler;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractModelZooPrediction<I extends PredictionInput, O extends PredictionOutput> implements ModelZooPrediction<I, O> {

	@Parameter
	private LogService log;

	@Parameter
	private Context context;

	@Parameter
	private ModelZooService modelZooService;

	private ModelZooPredictionOptions options = ModelZooPredictionOptions.options();
	private final InputMappingHandler inputHandling;
	private ModelZooArchive modelArchive;

	private boolean contextInjected = false;
	private O output;

	private Map<String, Object> outputs;
	private I input;

	public AbstractModelZooPrediction() {
		inputHandling = new InputMappingHandler();
	}

	public AbstractModelZooPrediction(Context context) {
		this();
		context.inject(this);
	}

	@Override
	public void setOptions(ModelZooPredictionOptions options) {
		this.options = options;
	}

	@Override
	public void run() throws OutOfMemoryError, Exception {

		input.attachToInputHandler(inputHandling);
		ModelZooModel model = loadModel(modelArchive);
		if (!validateModel(model)) return;
		try {
			preprocessing(model);
			executePrediction(model);
			postprocessing(model);
			this.output = createOutput(model);
			log.info("Prediction done.");
		} finally {
			model.dispose();
		}
	}

	@Override
	public void setInput(I input) {
		this.input = input;
	}

	@Override
	public I getInput() {
		return input;
	}

	@Override
	public O getOutput() {
		return output;
	}

	protected abstract O createOutput(ModelZooModel model);

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
	public ModelZooArchive getTrainedModel() {
		return modelArchive;
	}

	@Override
	public void setTrainedModel(ModelZooArchive trainedModel) {
		this.modelArchive = trainedModel;
	}

	public void setTrainedModel(String trainedModel) throws IOException {
		setTrainedModel(modelZooService.io().open(trainedModel));
	}

	protected void executePrediction(ModelZooModel model) throws OutOfMemoryError {
		TiledPredictionExecutor executor = new TiledPredictionExecutor(model, context);
		executor.setTilingEnabled(options.values.tilingEnabled());
		executor.setNumberOfTiles(options.values.numberOfTiles());
		executor.setBatchSize(options.values.batchSize());
		executor.setCacheDir(options.values.cacheDirectory());
		boolean isOutOfMemory = true;
		boolean canHandleOutOfMemory = true;

		while (isOutOfMemory) {
			try {
				executor.run();
				isOutOfMemory = false;
			} catch (final OutOfMemoryError e) {
				log.debug(e);
				canHandleOutOfMemory = executor.increaseTiling();
				options.numberOfTiles(executor.getNumberOfTiles());
				options.batchSize(executor.getBatchSize());
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

	@Override
	public Map<String, Object> getOutputs() {
		return outputs;
	}

	Context context() {
		return context;
	}
}
