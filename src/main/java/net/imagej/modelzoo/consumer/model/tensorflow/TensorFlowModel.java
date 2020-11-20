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

package net.imagej.modelzoo.consumer.model.tensorflow;

import com.google.protobuf.InvalidProtocolBufferException;
import io.scif.MissingLibraryException;
import net.imagej.DatasetService;
import net.imagej.modelzoo.consumer.model.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.DefaultModelZooModel;
import net.imagej.modelzoo.consumer.model.DefaultSpecificationLoader;
import net.imagej.modelzoo.consumer.model.InputImageNode;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.ModelZooNode;
import net.imagej.modelzoo.consumer.model.OutputImageNode;
import net.imagej.modelzoo.specification.DefaultModelSpecification;
import net.imagej.modelzoo.specification.ModelSpecification;
import net.imagej.tensorflow.CachedModelBundle;
import net.imagej.tensorflow.TensorFlowService;
import net.imagej.tensorflow.ui.TensorFlowLibraryManagementCommand;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.io.location.FileLocation;
import org.scijava.io.location.Location;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.tensorflow.Tensor;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type= ModelZooModel.class, name = "tensorflow")
public class TensorFlowModel extends DefaultModelZooModel {
	@Parameter
	private TensorFlowService tensorFlowService;

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private CommandService commandService;

	@Parameter
	private LogService log;

	@Parameter
	private Context context;

	private CachedModelBundle model;
	private SignatureDef sig;
	private boolean tensorFlowLoaded = false;
	// Same as
	// tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY
	// in Python. Perhaps this should be an exported constant in TensorFlow's Java
	// API.
	private static final String MODEL_TAG = "serve";
	private static final String DEFAULT_SERVING_SIGNATURE_DEF_KEY =
			"serving_default";

	public TensorFlowModel() {
	}

	public TensorFlowModel(Context context) {
		context.inject(this);
	}

	public ModelSpecification guessSpecification(final String source, final String modelName) throws IOException {
		return guessSpecification(new FileLocation(source), modelName);
	}

	public ModelSpecification guessSpecification(final Location source, final String modelName) throws IOException {
		loadModelFile(source, modelName);
		// Extract names from the model signature.
		// The strings "input", "probabilities" and "patches" are meant to be
		// in sync with the model exporter (export_saved_model()) in Python.
		loadSignature();
		ModelSpecification specification = TensorFlowUtils.guessSpecification(log, sig);
		specification.setName(modelName);
		return specification;
	}

	@Override
	public void loadLibrary() throws MissingLibraryException {
		tensorFlowService.loadLibrary();
		if (tensorFlowService.getStatus().isLoaded()) {
			log.info(tensorFlowService.getStatus().getInfo());
			tensorFlowLoaded = true;
		} else {
			tensorFlowLoaded = false;
			JOptionPane.showMessageDialog(null,
					"<html>Could not load TensorFlow.<br/>Opening the TensorFlow Library Management tool.</html>",
					"Loading TensorFlow failed",
					JOptionPane.ERROR_MESSAGE);
			commandService.run(TensorFlowLibraryManagementCommand.class, true);
			throw new MissingLibraryException("Could not load TensorFlow. Check previous errors and warnings for details.");
		}
	}

	@Override
	public void loadModel(final Location source, final String modelName) throws IOException, MissingLibraryException {
		if (!tensorFlowLoaded) throw new MissingLibraryException("TensorFlow not loaded");
		log.info("Loading TensorFlow model " + modelName + " from source file " + source.getURI());
		loadModelFile(source, modelName);
		// Extract names from the model signature.
		// The strings "input", "probabilities" and "patches" are meant to be
		// in sync with the model exporter (export_saved_model()) in Python.
		loadSignature();
		loadModelSettings(source, modelName);
	}

	private boolean loadModelSettings(Location source, String modelName) {
		try {
			return loadModelSettingsFromYaml(tensorFlowService.loadFile(source, modelName, "model.yaml"));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private void loadSignature() throws InvalidProtocolBufferException {
		sig = MetaGraphDef.parseFrom(model.model().metaGraphDef()).getSignatureDefOrThrow(
				DEFAULT_SERVING_SIGNATURE_DEF_KEY);
		log.info("Model inputs: " + sig.getInputsMap().toString().replace("\n", " ").replace("\t", " "));
		log.info("Model outputs: " + sig.getOutputsMap().toString().replace("\n", " ").replace("\t", " "));
	}

	private void loadModelFile(Location source, String modelName) throws IOException {
		if (model != null) {
			model.close();
		}
		model = tensorFlowService.loadCachedModel(source, modelName, MODEL_TAG);
//			model.model().graph().operations().forEachRemaining(op -> {
//				for (int i = 0; i < op.numOutputs(); i++) {
//					Output<Object> opOutput = op.output(i);
//					String name = opOutput.op().name();
//					System.out.println(name);
//				}
//			});
	}

	private boolean loadModelSettingsFromYaml(File yamlFile) throws IOException {
		log.info("load settings from yaml file " + yamlFile);
		if (!yamlFile.exists()) {
			log.warn("Could not load settings from YAML " + yamlFile + ": file does not exist.");
			return false;
		}
		ModelSpecification specification = new DefaultModelSpecification();
		if (!specification.read(yamlFile)) {
			log.error("Model seems to be incompatible.");
			return false;
		}
		inputNodes.clear();
		if(!verifySpecification(specification)) return false;
		DefaultSpecificationLoader loader = new DefaultSpecificationLoader(context, specification, this);
		loader.process();
		return true;
	}

	private boolean verifySpecification(ModelSpecification specification) {
		if (sig.getInputsCount() != specification.getInputs().size()) {
			log.error("Model signature (" + sig.getInputsCount() +
					" inputs) does not match model description signature (" +
					specification.getInputs().size() + " inputs).");
			return false;
		}
		if (sig.getInputsCount() != specification.getOutputs().size()) {
			log.error("Model signature (" + sig.getOutputsCount() +
					" outputs) does not match model description signature (" +
					specification.getOutputs().size() + " outputs).");
			return false;
		}
		return true;
	}

	@Override
	public void predict() throws IllegalArgumentException, OutOfMemoryError {
		List<Tensor<?>> inputTensors = getInputTensors();
		List<String> outputNames = getOutputNames();
		List<Tensor<?>> outputTensors = TensorFlowRunner.executeGraph(
				model.model(),
				inputTensors,
				getInputNames(),
				outputNames);

		setOutputTensors(outputTensors);
		inputTensors.forEach(Tensor::close);
		outputTensors.forEach(Tensor::close);
	}

	private List<Tensor<?>> getInputTensors() {
		List<Tensor<?>> res = new ArrayList<>();
		for (ModelZooNode<?> _node : getInputNodes()) {
			//TODO currently, we assume all inputs are images
			InputImageNode node = (InputImageNode) _node;
			final Tensor<?> tensor = TensorFlowConverter.imageToTensor(node.getData().getData(), node.getMappingIndices());
			if (tensor == null) {
				log.error("Cannot convert to tensor: " + node.getData());
			}
			res.add(tensor);
		}
		return res;
	}

	private List<String> getInputNames() {
		return getInputNodes().stream().map(ModelZooNode::getName).collect(Collectors.toList());
	}

	private List<String> getOutputNames() {
		return getOutputNodes().stream().map(ModelZooNode::getName).collect(Collectors.toList());
	}

	private <TO extends RealType<TO> & NativeType<TO>, TI extends RealType<TI> & NativeType<TI>> void setOutputTensors(List<Tensor<?>> tensors) {
		for (int i = 0; i < tensors.size(); i++) {
			Tensor tensor = tensors.get(i);
			OutputImageNode node = (OutputImageNode) getOutputNodes().get(i);
//			System.out.println(Arrays.toString(node.getMappingIndices()));
			RandomAccessibleInterval<TO> output = TensorFlowConverter.tensorToImage(tensor, node.getMappingIndices());
			node.setData(new DefaultImageDataReference(output, node.getData().getDataType()));
		}
	}

	@Override
	public boolean libraryLoaded() {
		return tensorFlowLoaded;
	}

	@Override
	public boolean isInitialized() {
		return model != null;
	}

	@Override
	public void dispose() {
		super.dispose();
		tensorFlowLoaded = false;
		sig = null;
		model = null;
	}

}
