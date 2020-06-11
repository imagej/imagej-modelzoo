package net.imagej.modelzoo.consumer.network.model.tensorflow;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.network.model.DefaultModelZooAxis;
import net.imagej.modelzoo.consumer.network.model.InputNode;
import net.imagej.modelzoo.consumer.network.model.ModelZooAxis;
import net.imagej.modelzoo.consumer.network.model.ModelZooNode;
import net.imagej.modelzoo.consumer.network.model.OutputNode;
import net.imagej.modelzoo.consumer.tiling.Tiling;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.log.LogService;
import org.tensorflow.framework.SignatureDef;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YamlReader {

	private final LogService log;
	private final SignatureDef sig;
	private final Map<String, Object> data;

	YamlReader(LogService log, SignatureDef sig, File yamlFile) throws FileNotFoundException {
		this.log = log;
		this.sig = sig;
		Yaml yaml = new Yaml();
		this.data = yaml.load(new FileInputStream(yamlFile));
	}

	List<InputNode> processInputs() {
		List<InputNode> res = new ArrayList<>();
		try {
			List<Map<String, Object>> inputs = (List) data.get("inputs");
			if (sig.getInputsCount() != inputs.size()) {
				log.error("Model signature (" + sig.getInputsCount() +
						" inputs) does not match model description signature (" +
						inputs.size() + " inputs).");
				return res;
			}
			for (Map<String, Object> input : inputs) {
				res.add(buildInputNode(input));
			}
		} catch(ClassCastException e) {
			log.error("Could not process model inputs");
		}
		return res;
	}

	private InputNode buildInputNode(Map<String, Object> data) {
		InputNode node = new InputNode();
		setNodeName(data, node);
		setInputNodeShape(data, node);
		setNodeDataType(data, node);
		return node;
	}

	private OutputNode buildOutputNode(Map<String, Object> data, List<InputNode> inputNodes) {
		OutputNode node = new OutputNode();
		setNodeName(data, node);
		setOutputNodeShape(data, node, inputNodes);
		setNodeDataType(data, node);
		return node;
	}

	private void setNodeDataType(Map<String, Object> input, ModelZooNode node) {
		String dataType = (String) input.get("data_type");
		if(dataType.equals("float32")) {
			node.setDataType(new FloatType());
		}
	}

	private void setInputNodeShape(Map<String, Object> data, InputNode node) {
		String axes = (String) data.get("axes");
		Map<String, Object> shape = (Map) data.get("shape");
		List min = (List) shape.get("min");
		List step = (List) shape.get("step");
		node.clearAxes();
		for (int i = 0; i < axes.length(); i++) {
			int minVal = (int) min.get(i);
			Object stepVal = step.get(i);
			String axisName = axes.substring(i, i + 1);
			AxisType axisType = getAxisType(axisName);
			Tiling.TilingAction tilingAction = Tiling.TilingAction.NO_TILING;
			if(axisName.equals("b")) {
				tilingAction = Tiling.TilingAction.TILE_WITHOUT_PADDING;
			} else {
				if(stepVal != null && (int)stepVal > 0) {
					tilingAction = Tiling.TilingAction.TILE_WITH_PADDING;
				}
			}
			DefaultModelZooAxis axis = new DefaultModelZooAxis(axisType);
			axis.getAttributes().put("min", minVal);
			axis.getAttributes().put("step", stepVal);
			axis.getAttributes().put("tiling", tilingAction);
			node.addAxis(axis);
		}
	}

	private void setOutputNodeShape(Map<String, Object> data, OutputNode node, List<InputNode> inputNodes) {
		String axes = (String) data.get("axes");
		Map<String, Object> shape = (Map) data.get("shape");
		String reference = (String) shape.get("reference_input");
		List scale = (List) shape.get("scale");
		List offset = (List) shape.get("offset");
		List halo = (List) data.get("halo");
		node.setReference(getInput(inputNodes, reference));
		node.clearAxes();
		for (int i = 0; i < axes.length(); i++) {
			float scaleVal = ((Number)scale.get(i)).floatValue();
			int offsetVal = ((Number)offset.get(i)).intValue();
			int haloVal = ((Number)halo.get(i)).intValue();
			AxisType axisType = getAxisType(axes.substring(i, i + 1));
			ModelZooAxis axis = new DefaultModelZooAxis(axisType);
			axis.getAttributes().put("scale", scaleVal);
			axis.getAttributes().put("offset", offsetVal);
			axis.getAttributes().put("halo", haloVal);
			node.addAxis(axis);
		}
	}
	private InputNode getInput(List<InputNode> inputNodes, String name) {
		for (InputNode inputNode : inputNodes) {
			if(inputNode.getName().equals(name)) return inputNode;
		}
		return null;
	}

	private AxisType getAxisType(String axis) {
		if(axis.equals("x")) return Axes.X;
		if(axis.equals("y")) return Axes.Y;
		if(axis.equals("z")) return Axes.Z;
		if(axis.equals("c")) return Axes.CHANNEL;
		if(axis.equals("b")) return Axes.TIME;
		return Axes.unknown();
	}

	private void setNodeName(Map<String, Object> data, ModelZooNode node) {
		String name = (String) data.get("name");
		if(name != null) {
			node.setName(name);
		}
	}

	List<OutputNode> processOutputs(List<InputNode> inputNodes) {
		List<OutputNode> res = new ArrayList<>();
		try {
			List<Map<String, Object>> outputs = (List) data.get("outputs");
			if (sig.getInputsCount() != outputs.size()) {
				log.error("Model signature (" + sig.getOutputsCount() +
						" outputs) does not match model description signature (" +
						outputs.size() + " outputs).");
				return res;
			}
			for (Map<String, Object> output : outputs) {
				res.add(buildOutputNode(output, inputNodes));
			}
		} catch(ClassCastException e) {
			log.error("Could not process model outputs");
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

	boolean isJavaModel() {
		Object language = data.get("language");
		return language != null && language.equals("java");
	}

}
