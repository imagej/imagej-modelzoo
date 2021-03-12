/*-
 * #%L
 * This is the bioimage.io modelzoo library for ImageJ.
 * %%
 * Copyright (C) 2019 - 2021 Center for Systems Biology Dresden
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

import io.bioimage.specification.InputNodeSpecification;
import io.bioimage.specification.ModelSpecification;
import io.bioimage.specification.NodeSpecification;
import io.bioimage.specification.OutputNodeSpecification;
import io.bioimage.specification.TransformationSpecification;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageNode;
import net.imagej.modelzoo.consumer.model.node.InputImageNode;
import net.imagej.modelzoo.consumer.model.node.ModelZooAxis;
import net.imagej.modelzoo.consumer.model.node.ModelZooNode;
import net.imagej.modelzoo.consumer.model.node.OutputImageNode;
import net.imagej.modelzoo.consumer.model.node.processor.ImageNodeProcessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodePostprocessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodePreprocessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessor;
import net.imagej.modelzoo.consumer.postprocessing.ResizePostprocessor;
import net.imagej.modelzoo.consumer.preprocessing.InputImageConverterProcessor;
import net.imagej.modelzoo.consumer.preprocessing.ResizePreprocessor;
import net.imagej.modelzoo.consumer.tiling.TilingAction;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import java.util.ArrayList;
import java.util.List;

public class DefaultSpecificationLoader {

	@Parameter
	private LogService log;

	@Parameter
	private PluginService pluginService;

	private final ModelSpecification spec;
	private final List<PluginInfo<NodePreprocessor>> preprocessors;
	private final List<PluginInfo<NodePostprocessor>> postprocessors;
	private final ModelZooModel model;

	public DefaultSpecificationLoader(Context context, ModelSpecification spec, ModelZooModel model) {
		context.inject(this);
		preprocessors = pluginService.getPluginsOfType(NodePreprocessor.class);
		postprocessors = pluginService.getPluginsOfType(NodePostprocessor.class);
		this.spec = spec;
		this.model = model;
	}

	public List<InputImageNode> processInputs() {
		List<InputImageNode> res = new ArrayList<>();
		try {
			List<InputNodeSpecification> inputs = spec.getInputs();
			for (InputNodeSpecification input : inputs) {
				res.add(buildInputNode(input));
			}
		} catch (ClassCastException e) {
			log.error("Could not process model inputs");
		}
		return res;
	}

	private InputImageNode buildInputNode(InputNodeSpecification data) {
		InputImageNode node = new InputImageNode();
		node.setName(data.getName());
		setInputNodeShape(data, node);
		assignProcessors(node, data.getPreprocessing(), this.preprocessors);
		node.getProcessors().add(new ResizePreprocessor(node, log));
		node.getProcessors().add(new InputImageConverterProcessor(node, getNodeDataType(data)));
		return node;
	}

	private ModelZooNode buildOutputNode(OutputNodeSpecification data) {
		OutputImageNode node = new OutputImageNode();
		node.setName(data.getName());
		setOutputNodeShape(data, node);
		node.setData(new DefaultImageDataReference(null, getNodeDataType(data)));
		assignDefaultImagePostprocessors(node);
		assignProcessors(node, data.getPostprocessing(), this.postprocessors);
		return node;
	}

	private void assignDefaultImagePostprocessors(OutputImageNode node) {
		node.getProcessors().add(new ResizePostprocessor(node));
	}

	private RealType<?> getNodeDataType(NodeSpecification input) {
		String dataType = input.getDataType();
		if (dataType != null && dataType.equals("float32")) {
			return new FloatType();
		}
		return null;
	}

	private <T extends NodeProcessor> void assignProcessors(ImageNode node, List<TransformationSpecification> transformations, List<PluginInfo<T>> availableProcessors) {
		if(transformations == null) return;
		for (TransformationSpecification transformation : transformations) {
			String name = transformation.getName();
			for (PluginInfo<T> info : availableProcessors) {
				if(info.getName().equals(name)) {
					NodeProcessor processor = pluginService.createInstance(info);
					if(ImageNodeProcessor.class.isAssignableFrom(processor.getClass())) {
						((ImageNodeProcessor)processor).setup(node, model);
					}
					processor.readSpecification(transformation);
					node.getProcessors().add(processor);
					break;
				}
			}
		}
	}

	private void setInputNodeShape(InputNodeSpecification data, InputImageNode node) {
		String axes = data.getAxes();
		List<Integer> min = data.getShapeMin();
		List<Integer> step = data.getShapeStep();
		List<Integer> halo = data.getHalo();
		node.clearAxes();
		for (int i = 0; i < axes.length(); i++) {
			int minVal = min.get(i);
			Integer stepVal = step.get(i);
			String axisName = axes.substring(i, i + 1).toLowerCase();
			AxisType axisType = getAxisType(axisName);
			TilingAction tilingAction = TilingAction.NO_TILING;
			if (axisName.equals("b")) {
				tilingAction = TilingAction.TILE_WITHOUT_PADDING;
			} else {
				if (stepVal > 0) {
					tilingAction = TilingAction.TILE_WITH_PADDING;
				}
			}
			ModelZooAxis axis = new ModelZooAxis(axisType);
			axis.setMin(minVal);
			axis.setStep(stepVal);
			axis.setTiling(tilingAction);
			if(halo != null) axis.setHalo(halo.get(i));
			node.addAxis(axis);
		}
	}

	private void setOutputNodeShape(OutputNodeSpecification data, OutputImageNode node) {
		String axes = data.getAxes();
		List<? extends Number> scale = data.getShapeScale();
		List<Integer> offset = data.getShapeOffset();
		List<Integer> halo = data.getHalo();
		String reference = data.getReferenceInputName();
		ModelZooNode input = getInput(reference);
		node.setReference(input);
		node.clearAxes();
		for (int i = 0; i < axes.length(); i++) {
			AxisType axisType = getAxisType(axes.substring(i, i + 1));
			ModelZooAxis axis = new ModelZooAxis(axisType);
			axis.setScale(scale.get(i).doubleValue());
			axis.setOffset(offset.get(i));
			if(halo != null && input != null) {
				// this is here in case the halo is not defined in the input, but in the output node
				InputImageNode inputImageNode = (InputImageNode) input;
				ModelZooAxis inputAxis = inputImageNode.getAxes().get(i);
				if(inputAxis != null && inputAxis.getHalo() == null) {
					inputAxis.setHalo((int) (data.getHalo().get(i) / axis.getScale()));
				}
			}
			node.addAxis(axis);
		}
	}

	private ModelZooNode getInput(String name) {
		for (ModelZooNode<?> inputNode : model.getInputNodes()) {
			if (inputNode.getName().equals(name)) return inputNode;
		}
		return null;
	}

	private AxisType getAxisType(String axis) {
		if (axis.toLowerCase().equals("x")) return Axes.X;
		if (axis.toLowerCase().equals("y")) return Axes.Y;
		if (axis.toLowerCase().equals("z")) return Axes.Z;
		if (axis.toLowerCase().equals("c")) return Axes.CHANNEL;
		if (axis.toLowerCase().equals("b")) return Axes.TIME;
		return Axes.unknown();
	}

	public List<ModelZooNode<?>> processOutputs() {
		List<ModelZooNode<?>> res = new ArrayList<>();
		try {
			List<OutputNodeSpecification> outputs = spec.getOutputs();
			for (OutputNodeSpecification output : outputs) {
				res.add(buildOutputNode(output));
			}
		} catch (ClassCastException e) {
			log.error("Could not process model outputs");
			e.printStackTrace();
		}
		return res;
	}

	public void process() {
		model.getInputNodes().clear();
		model.getInputNodes().addAll(processInputs());
		model.getOutputNodes().clear();
		model.getOutputNodes().addAll(processOutputs());
	}
}
