package net.imagej.modelzoo.specification.io;

import net.imagej.modelzoo.consumer.model.tensorflow.TensorFlowSavedModelBundleSpecification;
import net.imagej.modelzoo.specification.CitationSpecification;
import net.imagej.modelzoo.specification.DefaultCitationSpecification;
import net.imagej.modelzoo.specification.DefaultInputNodeSpecification;
import net.imagej.modelzoo.specification.DefaultModelSpecification;
import net.imagej.modelzoo.specification.DefaultOutputNodeSpecification;
import net.imagej.modelzoo.specification.InputNodeSpecification;
import net.imagej.modelzoo.specification.ModelSpecification;
import net.imagej.modelzoo.specification.NodeSpecification;
import net.imagej.modelzoo.specification.OutputNodeSpecification;
import net.imagej.modelzoo.specification.TransformationSpecification;
import net.imagej.modelzoo.specification.WeightsSpecification;
import net.imagej.modelzoo.specification.transformation.ImageTransformation;
import net.imagej.modelzoo.specification.transformation.ScaleLinearTransformation;
import net.imagej.modelzoo.specification.transformation.ZeroMeanUnitVarianceTransformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SpecificationReaderWriterV3 {

	private final static String idName = "name";
	private final static String idDescription = "description";
	private final static String idCite = "cite";
	private final static String idAuthors = "authors";
	private final static String idDocumentation = "documentation";
	private final static String idTags = "tags";
	private final static String idLicense = "license";
	private final static String idFormatVersion = "format_version";
	private final static String idLanguage = "language";
	private final static String idTimestamp = "timestamp";
	private final static String idFramework = "framework";
	private final static String idSource = "source";
	private final static String idGitRepo = "git_repo";
	private final static String idAttachments = "attachments";
	private final static String idTestInputs = "test_inputs";
	private final static String idTestOutputs = "test_outputs";
	private final static String idSampleInputs = "sample_inputs";
	private final static String idSampleOutputs = "sample_outputs";
	private final static String idInputs = "inputs";
	private final static String idOutputs = "outputs";
	private final static String idWeights = "weights";
	private final static String idWeightsSource = "source";
	private final static String idWeightsHash = "sha256";
	private final static String idWeightsTag = "tag";
	private final static String idConfig = "config";
	private final static String idConfigFiji = "fiji";
	private final static String idTraining = "training";
	private final static String idTrainingSource = "source";
	private final static String idTrainingKwargs = "kwargs";

	private final static String idNodeName = "name";
	private final static String idNodeAxes = "axes";
	private final static String idNodeDataType = "data_type";
	private final static String idNodeDataRange = "data_range";
	private final static String idNodeShape = "shape";
	private final static String idNodeHalo = "halo";

	private final static String idNodeShapeMin = "min";
	private final static String idNodeShapeStep = "step";
	private final static String idNodePreprocessing = "preprocessing";
	private final static String idNodeShapeReferenceInput = "reference_input";
	private final static String idNodeShapeScale = "scale";
	private final static String idNodeShapeOffset = "offset";
	private final static String idNodePostprocessing = "postprocessing";


	private final static String idCiteText = "text";
	private final static String idCiteDoi = "doi";

	private final static String idTransformationName = "name";
	private final static String idTransformationKwargs = "kwargs";
	private static final String idTransformationMode = "mode";
	private static String idTransformationModeFixed = "fixed";
	private static String idTransformationModePerDataset = "per_dataset";
	private static String idTransformationModePerSample = "per_sample";

	private static final String idTransformationScaleLinear = "scale_linear";
	private static final String idTransformationScaleLinearGain = "gain";
	private static final String idTransformationScaleLinearOffset = "offset";

	private static final String idTransformationZeroMean = "zero_mean_unit_variance";
	private static final String idTransformationZeroMeanMean = "mean";
	private static final String idTransformationZeroMeanStd = "std";
	private static final String idWeightsTensorFlowSavedModelBundle = "tensorflow_saved_model_bundle";

	public static ModelSpecification read(DefaultModelSpecification specification, Map<String, Object> obj) {
		readMeta(specification, obj);
		readInputsOutputs(specification, obj);
		readConfig(specification, obj);
		readWeights(specification, obj);
		return specification;
	}

	private static void readMeta(DefaultModelSpecification specification, Map<String, Object> obj) {
		specification.setName((String) obj.get(idName));
		specification.setDescription((String) obj.get(idDescription));
		specification.setTimestamp((String) obj.get(idTimestamp));
		if(obj.get(idCite) != null && List.class.isAssignableFrom(obj.get(idCite).getClass())) {
			List<Map> citations = (List<Map>) obj.get(idCite);
			for (Map citation : citations) {
				specification.addCitation(readCitation(citation));
			}
		}
		Object authors = obj.get(idAuthors);
		if (authors != null) {
			if (List.class.isAssignableFrom(authors.getClass())) {
				specification.setAuthors(((List<String>) authors));
			} else if (String.class.isAssignableFrom(authors.getClass())) {
				specification.setAuthors(Arrays.asList((String) authors));
			}
		}
		Object attachments = obj.get(idAttachments);
		if (attachments != null) {
			if (Map.class.isAssignableFrom(attachments.getClass())) {
				((Map<?, ?>) attachments).forEach((s, s2) -> {
					specification.getAttachments().put(s.toString(), s2);
				});
			}
		}
		specification.setDocumentation((String) obj.get(idDocumentation));
		specification.setTags((List<String>) obj.get(idTags));
		specification.setLicense((String) obj.get(idLicense));
		specification.setFormatVersion((String) obj.get(idFormatVersion));
		specification.setLanguage((String) obj.get(idLanguage));
		specification.setFramework((String) obj.get(idFramework));
		specification.setSource((String) parseSource(obj));
		specification.setGitRepo((String) obj.get(idGitRepo));
		specification.setTestInputs((List<String>) obj.get(idTestInputs));
		specification.setTestOutputs((List<String>) obj.get(idTestOutputs));
		specification.setSampleInputs((List<String>) obj.get(idSampleInputs));
		specification.setSampleOutputs((List<String>) obj.get(idSampleOutputs));
	}

	private static Object parseSource(Map<String, Object> obj) {
		Object source = obj.get(idSource);
		if(source != null && source.equals("n2v")) {
			return null;
		}
		return source;
	}

	private static void readInputsOutputs(DefaultModelSpecification specification, Map<String, Object> obj) {
		List<Map> inputs = (List<Map>) obj.get(idInputs);
		for (Map input : inputs) {
			specification.addInputNode(readInputNode(input));
		}
		List<Map> outputs = (List<Map>) obj.get(idOutputs);
		for (Map output : outputs) {
			specification.addOutputNode(readOutputNode(output));
		}
	}

	private static void readConfig(DefaultModelSpecification specification, Map<String, Object> obj) {
		Map<String, Object> config = (Map<String, Object>) obj.get(idConfig);
		if(config == null) return;
		Map<String, Object> fijiConfig = (Map<String, Object>) config.get(idConfigFiji);
		if(fijiConfig == null) return;
		Map<String, Object> training = (Map<String, Object>) fijiConfig.get(idTraining);
		if(training == null) return;
		specification.setTrainingSource((String) training.get(idTrainingSource));
		specification.setTrainingKwargs((Map<String, Object>) training.get(idTrainingKwargs));
	}

	private static void readWeights(ModelSpecification specification, Map<String, Object> obj) {
		Map<String, Object> weights = (Map<String, Object>) obj.get(idWeights);
		if(weights == null) return;
		weights.forEach((name, object) -> readWeightsEntry(specification, name, (Map<String, Object>) object));
	}

	private static void readWeightsEntry(ModelSpecification specification, String name, Map<String, Object> data) {
		if(name.equals(idWeightsTensorFlowSavedModelBundle)) {
			TensorFlowSavedModelBundleSpecification weightsSpec = new TensorFlowSavedModelBundleSpecification();
			if(data != null) {
				weightsSpec.setTag((String) data.get(idWeightsTag));
				weightsSpec.setSha256((String) data.get(idWeightsHash));
				weightsSpec.setSource((String) data.get(idWeightsSource));
			}
			specification.getWeights().add(weightsSpec);
		}
	}

	public static Map<String, Object> write(DefaultModelSpecification specification) {
		Map<String, Object> data = new LinkedHashMap<>();
		writeMeta(specification, data);
		writeInputsOutputs(specification, data);
		writeWeights(specification, data);
		writeConfig(specification, data);
		return data;
	}


	private static void writeWeights(DefaultModelSpecification specification, Map<String, Object> data) {
		Map<String, Object> weights = new LinkedHashMap<>();
		for (WeightsSpecification weight : specification.getWeights()) {
			Map<String, Object> weightData = new HashMap<>();
			weightData.put(idWeightsSource, weight.getSource());
			weightData.put(idWeightsHash, weight.getSha256());
			if(weight instanceof TensorFlowSavedModelBundleSpecification) {
				weightData.put(idWeightsTag, ((TensorFlowSavedModelBundleSpecification) weight).getTag());
			}
			weights.put(getWeightsName(weight), weightData);
		}
		data.put(idWeights, weights);
	}

	private static String getWeightsName(WeightsSpecification weight) {
		if(weight instanceof TensorFlowSavedModelBundleSpecification) return idWeightsTensorFlowSavedModelBundle;
		return null;
	}

	private static List<Map<String, Object>> buildTransformationList(List<TransformationSpecification> transformations) {
		List<Map<String, Object>> res = new ArrayList<>();
		for (TransformationSpecification transformation : transformations) {
			res.add(writeTransformation(transformation));
		}
		return res;
	}

	private static void writeConfig(ModelSpecification specification, Map<String, Object> data) {
		Map<String, Object> training = new LinkedHashMap<>();
		training.put(idTrainingSource, specification.getTrainingSource());
		if (specification.getTrainingKwargs() != null) {
			training.put(idTrainingKwargs, specification.getTrainingKwargs());
		}
		Map<String, Object> fijiConfig = new LinkedHashMap<>();
		fijiConfig.put(idTraining, training);
		Map<String, Object> config = new LinkedHashMap<>();
		config.put(idConfigFiji, fijiConfig);
		data.put(idConfig, config);
	}

	private static void writeInputsOutputs(ModelSpecification specification, Map<String, Object> data) {
		data.put(idInputs, buildInputList(specification));
		data.put(idOutputs, buildOutputList(specification));
	}

	private static void writeMeta(ModelSpecification specification, Map<String, Object> data) {
		data.put(idFormatVersion, specification.getFormatVersion());
		data.put(idName, specification.getName());
		data.put(idTimestamp, specification.getTimestamp());
		data.put(idDescription, specification.getDescription());
		data.put(idAuthors, specification.getAuthors());
		data.put(idCite, buildCitationList(specification));
		data.put(idDocumentation, specification.getDocumentation());
		data.put(idTags, specification.getTags());
		data.put(idLicense, specification.getLicense());
		data.put(idLanguage, specification.getLanguage());
		data.put(idFramework, specification.getFramework());
		data.put(idSource, specification.getSource());
		data.put(idGitRepo, specification.getGitRepo());
		data.put(idAttachments, specification.getAttachments());
		data.put(idTestInputs, specification.getTestInputs());
		data.put(idTestOutputs, specification.getTestOutputs());
		data.put(idSampleInputs, specification.getSampleInputs());
		data.put(idSampleOutputs, specification.getSampleOutputs());
	}

	private static List<Map<String, Object>> buildInputList(ModelSpecification specification) {
		List<Map<String, Object>> inputs = new ArrayList<>();
		for (InputNodeSpecification input : specification.getInputs()) {
			inputs.add(writeInputNode(input));
		}
		return inputs;
	}

	private static List<Map<String, Object>> buildOutputList(ModelSpecification specification) {
		List<Map<String, Object>> outputs = new ArrayList<>();
		for (OutputNodeSpecification output : specification.getOutputs()) {
			outputs.add(writeOutputNode(output));
		}
		return outputs;
	}

	private static List<Map<String, Object>> buildCitationList(ModelSpecification specification) {
		List<Map<String, Object>> cite = new ArrayList<>();
		for (CitationSpecification citation : specification.getCitations()) {
			cite.add(writeCitation(citation));
		}
		return cite;
	}

	private static InputNodeSpecification readInputNode(Map data) {
		InputNodeSpecification node = new DefaultInputNodeSpecification();
		readNode(node, data);
		Map<String, Object> shapeData = (Map<String, Object>) data.get(idNodeShape);
		node.setShapeMin((List<Integer>) shapeData.get(idNodeShapeMin));
		node.setShapeStep((List<Integer>) shapeData.get(idNodeShapeStep));
		Object preprocessings = data.get(idNodePreprocessing);
		ArrayList<TransformationSpecification> preprocessing = null;
		if(preprocessings != null && List.class.isAssignableFrom(preprocessings.getClass())) {
			preprocessing = new ArrayList<>();
			for (Map processing : ((List<Map>) preprocessings)) {
				preprocessing.add(readTransformation(processing));
			}
		}
		node.setPreprocessing(preprocessing);
		return node;
	}

	private static TransformationSpecification readTransformation(Map data) {
		Map<String, Object> kwargs = (Map<String, Object>) data.get(idTransformationKwargs);
		switch ((String)data.get(idTransformationName)) {
			case idTransformationScaleLinear:
				ScaleLinearTransformation scaleLinear = new ScaleLinearTransformation();
				scaleLinear.setMode(toMode(kwargs.get(idTransformationMode)));
				scaleLinear.setGain(toNumber(kwargs.get(idTransformationScaleLinearGain)));
				scaleLinear.setOffset(toNumber(kwargs.get(idTransformationScaleLinearOffset)));
				return scaleLinear;
			case idTransformationZeroMean:
				ZeroMeanUnitVarianceTransformation zeroMean = new ZeroMeanUnitVarianceTransformation();
				zeroMean.setMode(toMode(kwargs.get(idTransformationMode)));
				zeroMean.setMean(toNumber(kwargs.get(idTransformationZeroMeanMean)));
				zeroMean.setStd(toNumber(kwargs.get(idTransformationZeroMeanStd)));
				return zeroMean;
		}
		return null;
	}

	private static ImageTransformation.Mode toMode(Object obj) {
		if(obj == null) return null;
		String mode = (String) obj;
		if(mode.equals(idTransformationModeFixed)) {
			return ImageTransformation.Mode.FIXED;
		}
		if(mode.equals(idTransformationModePerDataset)) {
			return ImageTransformation.Mode.PER_DATASET;
		}
		if(mode.equals(idTransformationModePerSample)) {
			return ImageTransformation.Mode.PER_SAMPLE;
		}
		return null;
	}

	private static Number toNumber(Object obj) {
		if(Number.class.isAssignableFrom(obj.getClass())) {
			return (Number) obj;
		}
		if(List.class.isAssignableFrom(obj.getClass())) {
			return toNumber(((List) obj).get(0));
		}
		throw new ClassCastException("Cannot convert " + obj + " to number.");
	}

	private static OutputNodeSpecification readOutputNode(Map data) {
		OutputNodeSpecification node = new DefaultOutputNodeSpecification();
		readNode(node, data);
		Map<String, Object> shapeData = (Map<String, Object>) data.get(idNodeShape);
		node.setShapeReferenceInput((String) shapeData.get(idNodeShapeReferenceInput));
		node.setShapeScale((List<Number>) shapeData.get(idNodeShapeScale));
		node.setShapeOffset((List<Integer>) shapeData.get(idNodeShapeOffset));
		Object postprocessings = data.get(idNodePostprocessing);
		ArrayList<TransformationSpecification> postprocessing = null;
		if(postprocessings != null && List.class.isAssignableFrom(postprocessings.getClass())) {
			postprocessing = new ArrayList<>();
			for (Map processing : ((List<Map>) postprocessings)) {
				postprocessing.add(readTransformation(processing));
			}
		}
		node.setPostprocessing(postprocessing);
		return node;
	}

	private static void readNode(NodeSpecification node, Map data) {
		node.setName((String) data.get(idNodeName));
		node.setAxes((String) data.get(idNodeAxes));
		node.setDataType((String) data.get(idNodeDataType));
		node.setDataRange((List<?>) data.get(idNodeDataRange));
		node.setHalo((List<Integer>) data.get(idNodeHalo));
	}

	private static Map<String, Object> writeNode(NodeSpecification node) {
		Map<String, Object> res = new LinkedHashMap<>();
		res.put(idNodeName, node.getName());
		if (node.getAxes() != null) res.put(idNodeAxes, node.getAxes());
		if (node.getDataType() != null) res.put(idNodeDataType, node.getDataType());
		if (node.getDataRange() != null) res.put(idNodeDataRange, node.getDataRange());
		if (node.getHalo() != null) res.put(idNodeHalo, node.getHalo());
		return res;
	}

	private static Map<String, Object> writeInputNode(InputNodeSpecification node) {
		Map<String, Object> res = writeNode(node);
		Map<String, Object> shape = new HashMap<>();
		if (node.getShapeMin() != null) shape.put(idNodeShapeMin, node.getShapeMin());
		if (node.getShapeStep() != null) shape.put(idNodeShapeStep, node.getShapeStep());
		res.put(idNodeShape, shape);
		if(node.getPreprocessing() != null) {
			res.put(idNodePreprocessing, buildTransformationList(node.getPreprocessing()));
		}
		return res;
	}

	private static Map<String, Object> writeOutputNode(OutputNodeSpecification node) {
		Map<String, Object> res = writeNode(node);
		Map<String, Object> shape = new HashMap<>();
		shape.put(idNodeShapeReferenceInput, node.getReferenceInputName());
		shape.put(idNodeShapeScale, node.getShapeScale());
		shape.put(idNodeShapeOffset, node.getShapeOffset());
		res.put(idNodeShape, shape);
		if(node.getPostprocessing() != null) {
			res.put(idNodePostprocessing, buildTransformationList(node.getPostprocessing()));
		}
		return res;
	}

	private static Map<String, Object> writeTransformation(TransformationSpecification transformation) {
		Map<String, Object> res = new LinkedHashMap<>();
		Map<String, Object> kwargs = new LinkedHashMap<>();
		if(transformation instanceof ScaleLinearTransformation) {
			res.put(idTransformationName, idTransformationScaleLinear);
			ScaleLinearTransformation scaleLinear = (ScaleLinearTransformation) transformation;
			kwargs.put(idTransformationMode, writeMode(scaleLinear.getMode()));
			kwargs.put(idTransformationScaleLinearGain, Collections.singletonList(scaleLinear.getGain()));
			kwargs.put(idTransformationScaleLinearOffset, Collections.singletonList(scaleLinear.getOffset()));
		} else if(transformation instanceof ZeroMeanUnitVarianceTransformation) {
			res.put(idTransformationName, idTransformationZeroMean);
			ZeroMeanUnitVarianceTransformation zeroMean = (ZeroMeanUnitVarianceTransformation) transformation;
			kwargs.put(idTransformationMode, writeMode(zeroMean.getMode()));
			kwargs.put(idTransformationZeroMeanMean, Collections.singletonList(zeroMean.getMean()));
			kwargs.put(idTransformationZeroMeanStd, Collections.singletonList(zeroMean.getStd()));
		}
		res.put(idTransformationKwargs, kwargs);
		return res;
	}

	private static String writeMode(ImageTransformation.Mode mode) {
		if(mode == null) return null;
		if(mode.equals(ImageTransformation.Mode.FIXED)) return idTransformationModeFixed;
		if(mode.equals(ImageTransformation.Mode.PER_DATASET)) return idTransformationModePerDataset;
		if(mode.equals(ImageTransformation.Mode.PER_SAMPLE)) return idTransformationModePerSample;
		return null;
	}

	private static Map<String, Object> writeCitation(CitationSpecification citation) {
		Map<String, Object> res = new LinkedHashMap<>();
		res.put(idCiteText, citation.getCitationText());
		res.put(idCiteDoi, citation.getDoiText());
		return res;
	}

	private static CitationSpecification readCitation(Map data) {
		CitationSpecification citation = new DefaultCitationSpecification();
		citation.setCitationText((String) data.get(idCiteText));
		citation.setDOIText((String) data.get(idCiteDoi));
		return citation;
	}

	public static boolean canRead(Map<String, Object> obj) {
		String version = (String) obj.get(idFormatVersion);
		return Objects.equals(version, "0.3.0");
	}
}
