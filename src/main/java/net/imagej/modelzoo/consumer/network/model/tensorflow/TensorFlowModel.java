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

package net.imagej.modelzoo.consumer.network.model.tensorflow;

import com.google.protobuf.InvalidProtocolBufferException;
import net.imagej.DatasetService;
import net.imagej.modelzoo.consumer.network.model.DefaultModel;
import net.imagej.modelzoo.consumer.network.model.InputImageNode;
import net.imagej.modelzoo.consumer.network.model.OutputImageNode;
import net.imagej.modelzoo.specification.ModelSpecification;
import net.imagej.tensorflow.CachedModelBundle;
import net.imagej.tensorflow.TensorFlowService;
import net.imagej.tensorflow.ui.TensorFlowLibraryManagementCommand;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.CommandService;
import org.scijava.io.location.Location;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.tensorflow.Output;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TensorFlowModel extends DefaultModel
{
	@Parameter
	private TensorFlowService tensorFlowService;

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private CommandService commandService;

	@Parameter
	private LogService log;

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

	public TensorFlowModel()
	{
	}

	@Override
	public void loadLibrary() {
		tensorFlowService.loadLibrary();
		if (tensorFlowService.getStatus().isLoaded()) {
			log.info(tensorFlowService.getStatus().getInfo());
			tensorFlowLoaded = true;
		} else {
			tensorFlowLoaded = false;
			log.error("Could not load TensorFlow. Check previous errors and warnings for details.");
			JOptionPane.showMessageDialog(null,
					"<html>Could not load TensorFlow.<br/>Opening the TensorFlow Library Management tool.</html>",
					"Loading TensorFlow failed",
					JOptionPane.ERROR_MESSAGE);
			commandService.run(TensorFlowLibraryManagementCommand.class, true);
		}
	}

	@Override
	protected boolean loadModel(final Location source, final String modelName) {
		if(!tensorFlowLoaded) return false;
		log.info("Loading TensorFlow model " + modelName + " from source file " + source.getURI());
		if (!loadModelFile(source, modelName)) return false;
		// Extract names from the model signature.
		// The strings "input", "probabilities" and "patches" are meant to be
		// in sync with the model exporter (export_saved_model()) in Python.
		if(!loadSignature()) return false;
		return loadModelSettings(source, modelName);
	}

	private boolean loadModelSettings(Location source, String modelName) {
		try {
			loadModelSettingsFromYaml(tensorFlowService.loadFile(source, modelName, "model.yaml"));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private boolean loadSignature() {
		try {
			sig = MetaGraphDef.parseFrom(model.model().metaGraphDef()).getSignatureDefOrThrow(
				DEFAULT_SERVING_SIGNATURE_DEF_KEY);
			System.out.println("Model inputs: " + sig.getInputsMap());
			System.out.println("Model outputs: " + sig.getOutputsMap());
		}
		catch (final InvalidProtocolBufferException e) {
			e.printStackTrace();
			return false;
		}
		return sig != null;
	}

	private boolean loadModelFile(Location source, String modelName) {
		try {
			if (model != null) {
				model.close();
			}
			model = tensorFlowService.loadCachedModel(source, modelName, MODEL_TAG);
			model.model().graph().operations().forEachRemaining( op -> {
				for ( int i = 0; i < op.numOutputs(); i++ ) {
					Output< Object > opOutput = op.output( i );
					String name = opOutput.op().name();
					System.out.println( name );
				}
			} );
		}
		catch (TensorFlowException | IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void loadModelSettingsFromYaml(File yamlFile) throws IOException {
		if(!yamlFile.exists()) return;
		ModelSpecification specification = new ModelSpecification();
		boolean success = specification.read(yamlFile);
		if(!success) {
			log.error("Model seems to be incompatible.");
			return;
		}
		inputNodes.clear();
		SpecificationLoader loader = new SpecificationLoader(log, sig, specification);
		inputNodes.addAll(loader.processInputs());
		outputNodes.clear();
		outputNodes.addAll(loader.processOutputs(inputNodes));
		loader.processPrediction();
	}

	// TODO this is the tensorflow runner
	@Override
	public void predict() throws IllegalArgumentException, OutOfMemoryError {
		List<Tensor> inputTensors = getInputTensors();
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

	private List<Tensor> getInputTensors() {
		List<Tensor> res = new ArrayList<>();
		for (InputImageNode node : getInputNodes()) {
			final Tensor tensor = TensorFlowConverter.toTensor(node.getData(), node.getMappingIndices());
			if(tensor == null) {
				System.out.println("[ERROR] Cannot convert to tensor: " + node.getData());
			}
			res.add(tensor);
		}
		return res;
	}

	private List<String> getInputNames() {
		return getInputNodes().stream().map(InputImageNode::getName).collect(Collectors.toList());
	}

	private List<String> getOutputNames() {
		return getOutputNodes().stream().map(OutputImageNode::getName).collect(Collectors.toList());
	}

	private <TO extends RealType<TO>, TI extends RealType<TI>> void setOutputTensors(List<Tensor<?>> tensors) {
		for (int i = 0; i < tensors.size(); i++) {
			Tensor tensor = tensors.get(i);
			OutputImageNode<TO, TI> node = (OutputImageNode<TO, TI>) getOutputNodes().get(i);
//			System.out.println(Arrays.toString(node.getMappingIndices()));
			RandomAccessibleInterval<TO> output = TensorFlowConverter.fromTensor(tensor, node.getMappingIndices());
			node.setData(output);
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
	public void clear() {
		super.clear();
		sig = null;
		model = null;
	}

	@Override
	public void dispose() {
		super.dispose();
		tensorFlowLoaded = false;
		clear();
	}

}
