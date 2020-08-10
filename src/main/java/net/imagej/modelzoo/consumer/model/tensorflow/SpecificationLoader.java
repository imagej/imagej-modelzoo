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

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.model.ImageNode;
import net.imagej.modelzoo.consumer.model.InputImageNode;
import net.imagej.modelzoo.consumer.model.ModelZooAxis;
import net.imagej.modelzoo.consumer.model.OutputImageNode;
import net.imagej.modelzoo.consumer.tiling.Tiling;
import net.imagej.modelzoo.specification.DefaultInputNodeSpecification;
import net.imagej.modelzoo.specification.DefaultModelSpecification;
import net.imagej.modelzoo.specification.DefaultOutputNodeSpecification;
import net.imagej.modelzoo.specification.InputNodeSpecification;
import net.imagej.modelzoo.specification.ModelSpecification;
import net.imagej.modelzoo.specification.NodeSpecification;
import net.imagej.modelzoo.specification.OutputNodeSpecification;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.log.LogService;
import org.tensorflow.framework.SignatureDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SpecificationLoader {

	private final LogService log;
	private final SignatureDef sig;
	private final ModelSpecification spec;

	SpecificationLoader(LogService log, SignatureDef sig, ModelSpecification spec) {
		this.log = log;
		this.sig = sig;
		this.spec = spec;
	}

	public static ModelSpecification guessSpecification(LogService log, SignatureDef sig) {
		ModelSpecification specification = new DefaultModelSpecification();
		sig.getInputsMap().forEach((name, tensorInfo) -> {
			InputNodeSpecification inputSpec = new DefaultInputNodeSpecification();
			inputSpec.setName(tensorInfo.getName().substring(0, tensorInfo.getName().lastIndexOf(":")));
			List<Integer> shapeMin  = new ArrayList<>();
			List<Integer> shapeStep = new ArrayList<>();
			List<Integer> halo = new ArrayList<>();
			inputSpec.setAxes(guessAxes(tensorInfo.getTensorShape().getDimCount()));
			for (int i = 0; i < tensorInfo.getTensorShape().getDimCount(); i++) {
				long size = tensorInfo.getTensorShape().getDim(i).getSize();
				if(size < 0) {
					// variable size
					shapeMin.add(0);
					shapeStep.add(1);
				} else {
					// fixed size
					shapeMin.add((int) size);
					shapeStep.add(0);
				}
				halo.add(0);
			}
			inputSpec.setShapeMin(shapeMin);
			inputSpec.setShapeStep(shapeStep);
			inputSpec.setHalo(halo);
			specification.addInputNode(inputSpec);
		});
		sig.getOutputsMap().forEach((name, tensorInfo) -> {
			OutputNodeSpecification outputSpec = new DefaultOutputNodeSpecification();
			outputSpec.setName(tensorInfo.getName().substring(0, tensorInfo.getName().lastIndexOf(":")));
			outputSpec.setAxes(guessAxes(tensorInfo.getTensorShape().getDimCount()));
			int dimCount = tensorInfo.getTensorShape().getDimCount();
			outputSpec.setShapeOffset(Collections.nCopies(dimCount, 0));
			outputSpec.setShapeScale(Collections.nCopies(dimCount, 1));
			if(sig.getInputsMap().size() == 1 && sig.getOutputsMap().size() == 1) {
				outputSpec.setShapeReferenceInput(specification.getInputs().get(0).getName());
			}
			specification.addOutputNode(outputSpec);
		});
		return specification;
	}

	private static String guessAxes(int dimCount) {
		if(dimCount == 4) return "BXYC";
		if(dimCount == 5) return "BXYZC";
		return "BXYZC".substring(0, dimCount);
	}

	List<InputImageNode<?>> processInputs() {
		List<InputImageNode<?>> res = new ArrayList<>();
		try {
			List<InputNodeSpecification> inputs = spec.getInputs();
			if (sig.getInputsCount() != inputs.size()) {
				log.error("Model signature (" + sig.getInputsCount() +
						" inputs) does not match model description signature (" +
						inputs.size() + " inputs).");
				return res;
			}
			for (InputNodeSpecification input : inputs) {
				res.add(buildInputNode(input));
			}
		} catch (ClassCastException e) {
			log.error("Could not process model inputs");
		}
		return res;
	}

	private InputImageNode<?> buildInputNode(InputNodeSpecification data) {
		InputImageNode<?> node = new InputImageNode<>();
		node.setName(data.getName());
		setInputNodeShape(data, node);
		setNodeDataType(data, node);
		return node;
	}

	private OutputImageNode buildOutputNode(OutputNodeSpecification data, List<InputImageNode<?>> inputNodes) {
		OutputImageNode<?, ?> node = new OutputImageNode<>();
		node.setName(data.getName());
		setOutputNodeShape(data, node, inputNodes);
		setNodeDataType(data, node);
		return node;
	}

	private void setNodeDataType(NodeSpecification input, ImageNode node) {
		String dataType = input.getDataType();
		if (dataType != null && dataType.equals("float32")) {
			node.setDataType(new FloatType());
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
			Tiling.TilingAction tilingAction = Tiling.TilingAction.NO_TILING;
			if (axisName.equals("b")) {
				tilingAction = Tiling.TilingAction.TILE_WITHOUT_PADDING;
			} else {
				if (stepVal > 0) {
					tilingAction = Tiling.TilingAction.TILE_WITH_PADDING;
				}
			}
			ModelZooAxis axis = new ModelZooAxis(axisType);
			axis.setMin(minVal);
			axis.setStep(stepVal);
			axis.setTiling(tilingAction);
			axis.setHalo(halo.get(i));
			node.addAxis(axis);
		}
	}

	private <TO extends RealType<TO> & NativeType<TO>, TI extends RealType<TI> & NativeType<TI>> void setOutputNodeShape(OutputNodeSpecification data, OutputImageNode<TO, TI> node, List<InputImageNode<?>> inputNodes) {
		String axes = data.getAxes();
		String reference = data.getReferenceInputName();
		List<? extends Number> scale = data.getShapeScale();
		List<Integer> offset = data.getShapeOffset();
		node.setReference((InputImageNode<TI>) getInput(inputNodes, reference));
		node.clearAxes();
		for (int i = 0; i < axes.length(); i++) {
			AxisType axisType = getAxisType(axes.substring(i, i + 1));
			ModelZooAxis axis = new ModelZooAxis(axisType);
			axis.setScale(scale.get(i).doubleValue());
			axis.setOffset(offset.get(i));
			node.addAxis(axis);
		}
	}

	private InputImageNode<?> getInput(List<InputImageNode<?>> inputNodes, String name) {
		for (InputImageNode<?> inputNode : inputNodes) {
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

	List<OutputImageNode<?, ?>> processOutputs(List<InputImageNode<?>> inputNodes) {
		List<OutputImageNode<?, ?>> res = new ArrayList<>();
		try {
			List<OutputNodeSpecification> outputs = spec.getOutputs();
			if (sig.getInputsCount() != outputs.size()) {
				log.error("Model signature (" + sig.getOutputsCount() +
						" outputs) does not match model description signature (" +
						outputs.size() + " outputs).");
				return res;
			}
			for (OutputNodeSpecification output : outputs) {
				res.add(buildOutputNode(output, inputNodes));
			}
		} catch (ClassCastException e) {
			log.error("Could not process model outputs");
			e.printStackTrace();
		}
		return res;
	}

	void processPrediction() {
		//TODO
		// load prediction (Map)
		// load prediction > preprocess (ArrayList)
		// load prediction > preprocess > 0 (Map)
		// load prediction > preprocess > 0 > spec -> net.imagej.modelzoo.transform.normalize.PercentileNormalizer
		// load prediction > preprocess > 0 > kwargs (Map)
		// load prediction > preprocess > 0 > kwargs > data (List with input reference index?)
		// load prediction > preprocess > 0 > kwargs > min -> {Double@6905} 0.3
		// load prediction > preprocess > 0 > kwargs > max -> {Double@6907} 0.98
		// load prediction > weights (Map)
		// load prediction > weights > source -> ./saved_model.pb
		// load prediction > weights > hash -> ?
	}

}
