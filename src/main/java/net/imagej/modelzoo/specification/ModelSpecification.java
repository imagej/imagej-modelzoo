package net.imagej.modelzoo.specification;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

/**
 * This is the ImageJ version of the model zoo configuration specification
 * https://github.com/bioimage-io/configuration
 */
public class ModelSpecification {

	private final static String idName = "name";
	private final static String idDescription = "description";
	private final static String idCite = "cite";
	private final static String idAuthors = "authors";
	private final static String idDocumentation = "documentation";
	private final static String idTags = "tags";
	private final static String idLicense = "license";
	private final static String idFormatVersion = "format_version";
	private final static String idLanguage = "language";
	private final static String idFramework = "framework";
	private final static String idSource = "source";
	private final static String idInputs = "inputs";
	private final static String idOutputs = "outputs";
	private final static String idPrediction = "prediction";
	private final static String idPredictionPreprocess = "preprocess";
	private final static String idPredictionWeights = "weights";
	private final static String idPredictionWeightsSource = "source";
	private final static String idPredictionWeightsHash = "hash";
	private final static String idPredictionPostprocess = "postprocess";
	private final static String idPredictionDependencies = "dependencies";
	private final static String idTraining = "training";
	private final static String idTrainingSource = "source";
	private final static String idTrainingKwargs = "kwargs";

	final static String modelFileName = "model.yaml";
	final static String dependenciesFileName = "dependencies.yaml";
	final static String modelZooSpecificationVersion = "0.1.0";

	private String formatVersion = modelZooSpecificationVersion;
	private String language = "java";
	private String framework = "tensorflow";
	private String predictionWeightsSource = "./variables/variables";
	private String predictionDependencies = "./" + dependenciesFileName;
	private Map<String, Object> trainingKwargs;
	private String name;
	private String description;
	private List<CitationSpecification> citations = new ArrayList<>();
	private List<String> authors;
	private String documentation;
	private List<String> tags;
	private String license;
	private String source;
	private List<InputNodeSpecification> inputNodes = new ArrayList<>();
	private List<OutputNodeSpecification> outputNodes = new ArrayList<>();
	private String trainingSource;
	private TransformationSpecification predictionPreprocessing;
	private TransformationSpecification predictionPostprocessing;

	public boolean readFromZIP(File zippedModel) {
		try {
			return read(extractFile(zippedModel, modelFileName));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean readFromDirectory(File directory) {
		return read(new File(directory, modelFileName));
	}


	public boolean read(File modelSpecificationFile) {
		try (InputStream stream = new FileInputStream(modelSpecificationFile)){
			return read(stream);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean read(InputStream stream) {
		Yaml yaml = new Yaml();
		Map<String, Object> obj = yaml.load(stream);
//		System.out.println(obj);
		if (obj == null) return false;
		readMeta(obj);
		readInputsOutputs(obj);
		readTraining(obj);
		readPrediction(obj);
		return true;
	}

	private void readMeta(Map<String, Object> obj) {
		setName((String) obj.get(idName));
		setDescription((String) obj.get(idDescription));
		List<Map> citations = (List<Map>) obj.get(idCite);
		for (Map citation : citations) {
			addCitation(new CitationSpecification().fromMap(citation));
		}
		Object authors = obj.get(idAuthors);
		if(authors != null) {
			if(List.class.isAssignableFrom(authors.getClass())) {
				setAuthors(((List<String>)authors));
			}
			else if(String.class.isAssignableFrom(authors.getClass())) {
				setAuthors(Arrays.asList((String)authors));
			}
		}
		setDocumentation((String) obj.get(idDocumentation));
		setTags((List<String>) obj.get(idTags));
		setLicense((String) obj.get(idLicense));
		setFormatVersion((String) obj.get(idFormatVersion));
		setLanguage((String)obj.get(idLanguage));
		setFramework((String)obj.get(idFramework));
		setSource((String)obj.get(idSource));
	}

	private void readInputsOutputs(Map<String, Object> obj) {
		List<Map> inputs = (List<Map>) obj.get(idInputs);
		for (Map input : inputs) {
			addInputNode(new InputNodeSpecification().fromMap(input));
		}
		List<Map> outputs = (List<Map>) obj.get(idOutputs);
		for (Map output : outputs) {
			addOutputNode(new OutputNodeSpecification().fromMap(output));
		}
	}

	private void readTraining(Map<String, Object> obj) {
		Map<String, Object> training = (Map<String, Object>) obj.get(idTraining);
		if(training != null) {
			setTrainingSource((String) training.get(idTrainingSource));
			setTrainingKwargs((Map<String, Object>) training.get(idTrainingKwargs));
		}
	}

	private void readPrediction(Map<String, Object> obj) {
		Map<String, Object> prediction = (Map<String, Object>) obj.get(idPrediction);
		if(prediction != null) {
			Map<String, Object> preprocess = (Map<String, Object>) prediction.get(idPredictionPreprocess);
			if(preprocess != null) {
				setPredictionPreprocessing(new TransformationSpecification().fromMap(preprocess));
			}
			Map<String, Object> postprocess = (Map<String, Object>) prediction.get(idPredictionPostprocess);
			if(postprocess != null) {
				setPredictionPostprocessing(new TransformationSpecification().fromMap(postprocess));
			}
			setPredictionWeightsSource((String) prediction.get(idPredictionWeightsSource));
			setPredictionDependencies((String) prediction.get(idPredictionDependencies));
		}
	}

	public void write(File targetDirectory) {
		writeDependenciesFile(targetDirectory);
		Map<String, Object> data = new LinkedHashMap<>();
		writeMeta(data);
		writeInputsOutputs(data);
		writeTraining(data);
		writePrediction(data);
		Yaml yaml = new Yaml();
		try(FileWriter writer = new FileWriter(new File(targetDirectory, modelFileName))) {
			yaml.dump(data, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writePrediction(Map<String, Object> data) {
		Map<String, Object> prediction = new LinkedHashMap<>();
		Map<String, Object> weights = new LinkedHashMap<>();
		weights.put(idPredictionWeightsSource, predictionWeightsSource);
		prediction.put(idPredictionWeights, weights);
		if(predictionPreprocessing != null) prediction.put(idPredictionPreprocess, predictionPreprocessing.asMap());
		if(predictionPostprocessing != null) prediction.put(idPredictionPostprocess, predictionPostprocessing.asMap());
		prediction.put(idPredictionDependencies, predictionDependencies);
		data.put(idPrediction, prediction);
	}

	private void writeTraining(Map<String, Object> data) {
		Map<String, Object> training = new LinkedHashMap<>();
		training.put(idTrainingSource, trainingSource);
		if(trainingKwargs != null) training.put(idTrainingKwargs, trainingKwargs);
		data.put(idTraining, training);
	}

	private void writeInputsOutputs(Map<String, Object> data) {
		data.put(idInputs, buildInputList());
		data.put(idOutputs, buildOutputList());
	}

	private void writeMeta(Map<String, Object> data) {
		data.put(idName, name);
		data.put(idDescription, description);
		data.put(idCite, buildCitationList());
		data.put(idAuthors, authors);
		data.put(idDocumentation, documentation);
		data.put(idTags, tags);
		data.put(idLicense, license);
		data.put(idFormatVersion, formatVersion);
		data.put(idLanguage, language);
		data.put(idFramework, framework);
		data.put(idSource, source);
	}

	private List<Map<String, Object>> buildInputList() {
		List<Map<String, Object>> inputs = new ArrayList<>();
		for (InputNodeSpecification input : this.inputNodes) {
			inputs.add(input.asMap());
		}
		return inputs;
	}

	private List<Map<String, Object>> buildOutputList() {
		List<Map<String, Object>> outputs = new ArrayList<>();
		for (OutputNodeSpecification output : this.outputNodes) {
			outputs.add(output.asMap());
		}
		return outputs;
	}

	private List<Map<String, Object>> buildCitationList() {
		List<Map<String, Object>> cite = new ArrayList<>();
		for (CitationSpecification citation : citations) {
			cite.add(citation.asMap());

		}
		return cite;
	}

	private static void writeDependenciesFile(File targetDirectory) {
		Map<String, Object> data = new LinkedHashMap<>();
		List<String> dependencies = new ArrayList<>();
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		for(URL url: ((URLClassLoader)cl).getURLs()){
			dependencies.add(url.getPath());
		}
		data.put("classPath", dependencies);
		Yaml yaml = new Yaml();
		FileWriter writer = null;
		try {
			writer = new FileWriter(new File(targetDirectory, dependenciesFileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
		yaml.dump(data, writer);
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addCitation(CitationSpecification citation) {
		citations.add(citation);
	}

	public void setAuthors(List<String> modelAuthors) {
		this.authors = modelAuthors;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	private void setFormatVersion(String version) {
		formatVersion = version;
	}

	private void setLanguage(String language) {
		this.language = language;
	}

	private void setFramework(String framework) {
		this.framework = framework;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void addInputNode(InputNodeSpecification inputNode) {
		inputNodes.add(inputNode);
	}

	public void addOutputNode(OutputNodeSpecification outputNode) {
		outputNodes.add(outputNode);
	}

	public void setTrainingSource(String trainingSource) {
		this.trainingSource = trainingSource;
	}

	public void setTrainingKwargs(Map<String, Object> trainingKwargs) {
		this.trainingKwargs = trainingKwargs;
	}

	public void setPredictionPreprocessing(TransformationSpecification preprocessing) {
		this.predictionPreprocessing = preprocessing;
	}

	public void setPredictionPostprocessing(TransformationSpecification postprocessing) {
		this.predictionPostprocessing = postprocessing;
	}

	private void setPredictionWeightsSource(String weightsSource) {
		this.predictionWeightsSource = weightsSource;
	}

	private void setPredictionDependencies(String predictionDependencies) {
		this.predictionDependencies = predictionDependencies;
	}

	private static InputStream extractFile(File zipFile, String fileName) throws IOException {
		ZipFile zf = new ZipFile(zipFile);
		return zf.getInputStream(zf.getEntry(fileName));
	}

	public List<InputNodeSpecification> getInputs() {
		return inputNodes;
	}

	public List<OutputNodeSpecification> getOutputs() {
		return outputNodes;
	}

	public String getFormatVersion() {
		return formatVersion;
	}

	public String getLanguage() {
		return language;
	}

	public String getFramework() {
		return framework;
	}

	public String getPredictionWeightsSource() {
		return predictionWeightsSource;
	}

	public String getPredictionDependencies() {
		return predictionDependencies;
	}

	public Map<String, Object> getTrainingKwargs() {
		return trainingKwargs;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public List<CitationSpecification> getCitations() {
		return citations;
	}

	public List<String> getAuthors() {
		return authors;
	}

	public String getDocumentation() {
		return documentation;
	}

	public List<String> getTags() {
		return tags;
	}

	public String getLicense() {
		return license;
	}

	public String getSource() {
		return source;
	}

	public String getTrainingSource() {
		return trainingSource;
	}

	public TransformationSpecification getPredictionPreprocessing() {
		return predictionPreprocessing;
	}

	public TransformationSpecification getPredictionPostprocessing() {
		return predictionPostprocessing;
	}
}
