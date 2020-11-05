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
package net.imagej.modelzoo.specification;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
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
public class DefaultModelSpecification implements ModelSpecification {

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
	private final static String idGitRepo = "git_repo";
	private final static String idTestInput = "test_input";
	private final static String idTestOutput = "test_output";
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
	private final static String defaultTestInput = "testinput.tif";
	private final static String defaultTestOutput = "testoutput.tif";
	final static String dependenciesFileName = "dependencies.yaml";

	final static String modelZooSpecificationVersion = "0.2.1-csbdeep";
	private String modelFileName = "model.yaml";

	private String formatVersion = modelZooSpecificationVersion;
	private String language = "java";
	private String framework = "tensorflow";
	private String testInput = defaultTestInput;
	private String testOutput = defaultTestOutput;
	private String predictionWeightsSource = "./variables/variables";

	private String predictionDependencies = "./" + dependenciesFileName;
	private Map<String, Object> trainingKwargs;
	private String name;
	private String description;
	private final List<CitationSpecification> citations = new ArrayList<>();
	private List<String> authors;
	private String documentation;
	private List<String> tags;
	private String license;
	private String source;
	private final List<InputNodeSpecification> inputNodes = new ArrayList<>();
	private final List<OutputNodeSpecification> outputNodes = new ArrayList<>();
	private String trainingSource;
	private final List<TransformationSpecification> predictionPreprocessing = new ArrayList<>();
	private final List<TransformationSpecification> predictionPostprocessing = new ArrayList<>();
	private String gitRepo;

	@Override
	public boolean readFromZIP(File zippedModel) {
		try {
			return read(extractFile(zippedModel, modelFileName));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean readFromDirectory(File directory) throws IOException {
		return read(new File(directory, modelFileName));
	}

	@Override
	public boolean read(String modelSpecificationFile) throws IOException {
		return read(new File(modelSpecificationFile));
	}

	@Override
	public boolean read(File modelSpecificationFile) throws IOException {
		try (InputStream stream = new FileInputStream(modelSpecificationFile)) {
			return read(stream);
		}
	}

	@Override
	public boolean read(Path modelSpecificationPath) throws IOException {
		try (InputStream stream = Files.newInputStream(modelSpecificationPath)) {
			return read(stream);
		}
	}

	@Override
	public boolean read(InputStream stream) {
		Yaml yaml = new Yaml();
		Map<String, Object> obj = yaml.load(stream);
//		System.out.println(obj);
		if (obj == null) return false;
		readFromMap(obj);
		return true;
	}

	private void readFromMap(Map<String, Object> obj) {
		readMeta(obj);
		readInputsOutputs(obj);
		readTraining(obj);
		readPrediction(obj);
	}

	@Override
	public void write(String targetDirectory) throws IOException {
		write(new File(targetDirectory));
	}

	@Override
	public void write(File targetDirectory) throws IOException {
		writeDependenciesFile(targetDirectory);
		Map<String, Object> data = toMap();
		// when (re)writing the specification, use the most recent specification version
		data.put(idFormatVersion, modelZooSpecificationVersion);
		Yaml yaml = new Yaml();
		try (FileWriter writer = new FileWriter(new File(targetDirectory, modelFileName))) {
			yaml.dump(data, writer);
		}
	}

	@Override
	public void write(Path modelSpecificationPath) throws IOException {
		Map<String, Object> data = toMap();
		Yaml yaml = new Yaml();
		try {
			Files.delete(modelSpecificationPath);
		} catch(IOException ignored) {}
		try (Writer writer = Files.newBufferedWriter(modelSpecificationPath)) {
			yaml.dump(data, writer);
		}
	}

	@Override
	public void addPredictionPreprocessing(TransformationSpecification transformationSpecification) {
		predictionPreprocessing.add(transformationSpecification);
	}

	@Override
	public void addPredictionPostprocessing(TransformationSpecification transformationSpecification) {
		predictionPostprocessing.add(transformationSpecification);
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void addCitation(CitationSpecification citation) {
		citations.add(citation);
	}

	@Override
	public void setAuthors(List<String> modelAuthors) {
		this.authors = modelAuthors;
	}

	@Override
	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}

	@Override
	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	@Override
	public void setLicense(String license) {
		this.license = license;
	}

	@Override
	public void setSource(String source) {
		this.source = source;
	}

	@Override
	public void addInputNode(InputNodeSpecification inputNode) {
		inputNodes.add(inputNode);
	}

	@Override
	public void addOutputNode(OutputNodeSpecification outputNode) {
		outputNodes.add(outputNode);
	}

	@Override
	public void setTrainingSource(String trainingSource) {
		this.trainingSource = trainingSource;
	}

	@Override
	public void setTrainingKwargs(Map<String, Object> trainingKwargs) {
		this.trainingKwargs = trainingKwargs;
	}

	@Override
	public void setGitRepo(String repo) {
		this.gitRepo = repo;
	}

	@Override
	public List<InputNodeSpecification> getInputs() {
		return inputNodes;
	}

	@Override
	public List<OutputNodeSpecification> getOutputs() {
		return outputNodes;
	}

	@Override
	public String getFormatVersion() {
		return formatVersion;
	}

	@Override
	public String getLanguage() {
		return language;
	}

	@Override
	public String getFramework() {
		return framework;
	}

	@Override
	public String getPredictionWeightsSource() {
		return predictionWeightsSource;
	}

	@Override
	public String getPredictionDependencies() {
		return predictionDependencies;
	}

	@Override
	public Map<String, Object> getTrainingKwargs() {
		return trainingKwargs;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public List<CitationSpecification> getCitations() {
		return citations;
	}

	@Override
	public List<String> getAuthors() {
		return authors;
	}

	@Override
	public String getDocumentation() {
		return documentation;
	}

	@Override
	public List<String> getTags() {
		return tags;
	}

	@Override
	public String getLicense() {
		return license;
	}

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public String getTrainingSource() {
		return trainingSource;
	}

	@Override
	public List<TransformationSpecification> getPredictionPreprocessing() {
		return predictionPreprocessing;
	}

	@Override
	public List<TransformationSpecification> getPredictionPostprocessing() {
		return predictionPostprocessing;
	}

	@Override
	public String getModelFileName() {
		return modelFileName;
	}

	@Override
	public String getGitRepo() {
		return gitRepo;
	}

	@Override
	public String getTestInput() {
		if(testInput == null) testInput = defaultTestInput;
		return testInput;
	}

	@Override
	public void setTestInput(String testInput) {
		this.testInput = testInput;
	}

	@Override
	public String getTestOutput() {
		if(testOutput == null) testOutput = defaultTestOutput;
		return testOutput;
	}

	@Override
	public void setTestOutput(String testOutput) {
		this.testOutput = testOutput;
	}

	@Override
	public void setFramework(String framework) {
		this.framework = framework;
	}

	@Override
	public void setFormatVersion(String version) {
		formatVersion = version;
	}

	private void readMeta(Map<String, Object> obj) {
		setName((String) obj.get(idName));
		setDescription((String) obj.get(idDescription));
		if(obj.get(idCite) != null && List.class.isAssignableFrom(obj.get(idCite).getClass())) {
			List<Map> citations = (List<Map>) obj.get(idCite);
			for (Map citation : citations) {
				addCitation(new DefaultCitationSpecification().fromMap(citation));
			}
		}
		Object authors = obj.get(idAuthors);
		if (authors != null) {
			if (List.class.isAssignableFrom(authors.getClass())) {
				setAuthors(((List<String>) authors));
			} else if (String.class.isAssignableFrom(authors.getClass())) {
				setAuthors(Arrays.asList((String) authors));
			}
		}
		setDocumentation((String) obj.get(idDocumentation));
		setTags((List<String>) obj.get(idTags));
		setLicense((String) obj.get(idLicense));
		setFormatVersion((String) obj.get(idFormatVersion));
		setLanguage((String) obj.get(idLanguage));
		setFramework((String) obj.get(idFramework));
		setSource((String) obj.get(idSource));
		setGitRepo((String) obj.get(idGitRepo));
		setTestInput((String) obj.get(idTestInput));
		setTestOutput((String) obj.get(idTestOutput));
	}

	private void readInputsOutputs(Map<String, Object> obj) {
		List<Map> inputs = (List<Map>) obj.get(idInputs);
		for (Map input : inputs) {
			addInputNode(new DefaultInputNodeSpecification().fromMap(input));
		}
		List<Map> outputs = (List<Map>) obj.get(idOutputs);
		for (Map output : outputs) {
			addOutputNode(new DefaultOutputNodeSpecification().fromMap(output));
		}
	}

	private void readTraining(Map<String, Object> obj) {
		Map<String, Object> training = (Map<String, Object>) obj.get(idTraining);
		if (training != null) {
			setTrainingSource((String) training.get(idTrainingSource));
			setTrainingKwargs((Map<String, Object>) training.get(idTrainingKwargs));
		}
	}

	private void readPrediction(Map<String, Object> obj) {
		Map<String, Object> prediction = (Map<String, Object>) obj.get(idPrediction);
		if (prediction != null) {
			Object oPreprocess = prediction.get(idPredictionPreprocess);
			if(oPreprocess != null) {
				if(Map.class.isAssignableFrom(oPreprocess.getClass())) {
					addPredictionPreprocessing(new DefaultTransformationSpecification().fromMap((Map<String, Object>) oPreprocess));
				} else {
					if(List.class.isAssignableFrom(oPreprocess.getClass())) {
						List<Map<String, Object>> preprocess = (List<Map<String, Object>>) oPreprocess;
						for (Map<String, Object> transformation : preprocess) {
							addPredictionPreprocessing(new DefaultTransformationSpecification().fromMap(transformation));
						}
					}
				}
			}
			Object oPostprocess = prediction.get(idPredictionPostprocess);
			if(oPostprocess != null) {
				if(Map.class.isAssignableFrom(oPostprocess.getClass())) {
					addPredictionPostprocessing(new DefaultTransformationSpecification().fromMap((Map<String, Object>) oPostprocess));
				} else {
					if(List.class.isAssignableFrom(oPostprocess.getClass())) {
						List<Map<String, Object>> postprocess = (List<Map<String, Object>>) oPostprocess;
						for (Map<String, Object> transformation : postprocess) {
							addPredictionPostprocessing(new DefaultTransformationSpecification().fromMap(transformation));
						}
					}
				}
			}
			setPredictionWeightsSource((String) prediction.get(idPredictionWeightsSource));
			setPredictionDependencies((String) prediction.get(idPredictionDependencies));
		}
	}

	private void writePrediction(Map<String, Object> data) {
		Map<String, Object> prediction = new LinkedHashMap<>();
		Map<String, Object> weights = new LinkedHashMap<>();
		weights.put(idPredictionWeightsSource, predictionWeightsSource);
		prediction.put(idPredictionWeights, weights);
		if (predictionPreprocessing != null)
			prediction.put(idPredictionPreprocess, buildTransformationList(predictionPreprocessing));
		if (predictionPostprocessing != null)
			prediction.put(idPredictionPostprocess, buildTransformationList(predictionPostprocessing));
		prediction.put(idPredictionDependencies, predictionDependencies);
		data.put(idPrediction, prediction);
	}

	private List<Map<String, Object>> buildTransformationList(List<TransformationSpecification> transformations) {
		List<Map<String, Object>> res = new ArrayList<>();
		for (TransformationSpecification transformation : transformations) {
			res.add(transformation.asMap());
		}
		return res;
	}

	private void writeTraining(Map<String, Object> data) {
		Map<String, Object> training = new LinkedHashMap<>();
		training.put(idTrainingSource, trainingSource);
		if (trainingKwargs != null) training.put(idTrainingKwargs, trainingKwargs);
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
		data.put(idGitRepo, gitRepo);
		data.put(idTestInput, testInput);
		data.put(idTestOutput, testOutput);
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
		for (URL url : ((URLClassLoader) cl).getURLs()) {
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

	private void setLanguage(String language) {
		this.language = language;
	}

	private void setPredictionWeightsSource(String weightsSource) {
		this.predictionWeightsSource = weightsSource;
	}

	private void setPredictionDependencies(String predictionDependencies) {
		this.predictionDependencies = predictionDependencies;
	}

	public void setModelFileName(String modelFileName) {
		this.modelFileName = modelFileName;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> data = new LinkedHashMap<>();
		writeMeta(data);
		writeInputsOutputs(data);
		writeTraining(data);
		writePrediction(data);
		return data;
	}

	private static InputStream extractFile(File zipFile, String fileName) throws IOException {
		ZipFile zf = new ZipFile(zipFile);
		return zf.getInputStream(zf.getEntry(fileName));
	}

	@Override
	public String toString() {
		return toMap().toString();
	}
}
