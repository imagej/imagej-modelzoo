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

import net.imagej.modelzoo.specification.io.SpecificationReaderV1;
import net.imagej.modelzoo.specification.io.SpecificationReaderV2;
import net.imagej.modelzoo.specification.io.SpecificationReaderWriterV3;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

/**
 * This is the ImageJ version of the model zoo configuration specification
 * https://github.com/bioimage-io/configuration
 */
public class DefaultModelSpecification implements ModelSpecification {

	private final static String defaultTestInput = "testinput.tif";
	private final static String defaultTestOutput = "testoutput.tif";
	final static String dependenciesFileName = "dependencies.yaml";

	final static String modelZooSpecificationVersion = "0.3.0";
	private String modelFileName = "model.yaml";

	private String formatVersion = modelZooSpecificationVersion;
	private String language = "java";
	private String framework = "tensorflow";
	private String testInput = defaultTestInput;
	private String testOutput = defaultTestOutput;

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
	private final List<WeightsSpecification> weights = new ArrayList<>();
	private String trainingSource;
	private String gitRepo;
	private final Map<String, Object> attachments = new HashMap<>();

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
		return read(obj);
	}

	private boolean read(Map<String, Object> obj) {
		if(SpecificationReaderWriterV3.canRead(obj)) {
			SpecificationReaderWriterV3.read(this, obj);
			return true;
		}
		if(SpecificationReaderV2.canRead(obj)) {
			SpecificationReaderV2.read(this, obj);
			return true;
		}
		if(SpecificationReaderV1.canRead(obj)) {
			SpecificationReaderV1.read(this, obj);
			return true;
		}
		return false;
	}

	@Override
	public void write(String targetDirectory) throws IOException {
		write(new File(targetDirectory));
	}

	@Override
	public void write(File targetDirectory) throws IOException {
		writeDependenciesFile(targetDirectory);
		// when (re)writing the specification, use the most recent specification version
		setFormatVersion(modelZooSpecificationVersion);
		Map<String, Object> data = toMap();
		Yaml yaml = new Yaml();
		try (FileWriter writer = new FileWriter(new File(targetDirectory, modelFileName))) {
			yaml.dump(data, writer);
		}
	}

	public Map<String, Object> toMap() {
		return SpecificationReaderWriterV3.write(this);
	}

	@Override
	public void write(Path modelSpecificationPath) throws IOException {
		setFormatVersion(modelZooSpecificationVersion);
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
	public List<WeightsSpecification> getWeights() {
		return weights;
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
	public String getModelFileName() {
		return modelFileName;
	}

	@Override
	public String getGitRepo() {
		return gitRepo;
	}

	@Override
	public Map<String, Object> getAttachments() {
		return attachments;
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

	public void setLanguage(String language) {
		this.language = language;
	}

	public void setModelFileName(String modelFileName) {
		this.modelFileName = modelFileName;
	}

	private static InputStream extractFile(File zipFile, String fileName) throws IOException {
		ZipFile zf = new ZipFile(zipFile);
		return zf.getInputStream(zf.getEntry(fileName));
	}

	public void read(ModelSpecification spec) {
		setName(spec.getName());
		setDocumentation(spec.getDocumentation());
		setDescription(spec.getDescription());
		setAuthors(spec.getAuthors());
		setTestInput(spec.getTestInput());
		setTestOutput(spec.getTestOutput());
		setFramework(spec.getFramework());
		setLanguage(spec.getLanguage());
		setLicense(spec.getLicense());
		setSource(spec.getSource());
		setTags(spec.getTags());
		setTrainingKwargs(spec.getTrainingKwargs());
		setTrainingSource(spec.getTrainingSource());
		setGitRepo(spec.getGitRepo());
		setModelFileName(spec.getModelFileName());
		getInputs().clear();
		getInputs().addAll(spec.getInputs());
		getOutputs().clear();
		getOutputs().addAll(spec.getOutputs());
		getAttachments().clear();
		getAttachments().putAll(spec.getAttachments());
		getWeights().clear();
		getWeights().addAll(spec.getWeights());
	}
}
