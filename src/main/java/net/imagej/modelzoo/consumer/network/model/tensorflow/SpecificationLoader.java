package net.imagej.modelzoo.consumer.network.model.tensorflow;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.network.model.ImageNode;
import net.imagej.modelzoo.consumer.network.model.InputImageNode;
import net.imagej.modelzoo.consumer.network.model.ModelZooAxis;
import net.imagej.modelzoo.consumer.network.model.OutputImageNode;
import net.imagej.modelzoo.consumer.tiling.Tiling;
import net.imagej.modelzoo.specification.InputNodeSpecification;
import net.imagej.modelzoo.specification.ModelSpecification;
import net.imagej.modelzoo.specification.NodeSpecification;
import net.imagej.modelzoo.specification.OutputNodeSpecification;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.log.LogService;
import org.tensorflow.framework.SignatureDef;

import java.util.ArrayList;
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
		if (dataType.equals("float32")) {
			node.setDataType(new FloatType());
		}
	}

	private void setInputNodeShape(InputNodeSpecification data, InputImageNode node) {
		String axes = data.getAxes();
		List<Integer> min = data.getShapeMin();
		List<Integer> step = data.getShapeStep();
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
			node.addAxis(axis);
		}
	}

	private <TO extends RealType<TO>, TI extends RealType<TI>> void setOutputNodeShape(OutputNodeSpecification data, OutputImageNode<TO, TI> node, List<InputImageNode<?>> inputNodes) {
		String axes = data.getAxes();
		String reference = data.getReferenceInputName();
		List<? extends Number> scale = data.getShapeScale();
		List<Integer> offset = data.getShapeOffset();
		List<Integer> halo = data.getHalo();
		node.setReference((InputImageNode<TI>) getInput(inputNodes, reference));
		node.clearAxes();
		for (int i = 0; i < axes.length(); i++) {
			AxisType axisType = getAxisType(axes.substring(i, i + 1));
			ModelZooAxis axis = new ModelZooAxis(axisType);
			axis.setScale(scale.get(i).doubleValue());
			axis.setOffset(offset.get(i));
			axis.setHalo(halo.get(i));
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
		if (axis.equals("x")) return Axes.X;
		if (axis.equals("y")) return Axes.Y;
		if (axis.equals("z")) return Axes.Z;
		if (axis.equals("c")) return Axes.CHANNEL;
		if (axis.equals("b")) return Axes.TIME;
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
