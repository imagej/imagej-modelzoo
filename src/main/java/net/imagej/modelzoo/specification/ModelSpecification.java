package net.imagej.modelzoo.specification;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This is the ImageJ version of the model zoo configuration specification
 * https://github.com/bioimage-io/configuration
 */
public interface ModelSpecification {

	boolean readFromZIP(File zippedModel);

	boolean readFromDirectory(File directory) throws IOException;

	void read(String modelSpecificationFile) throws IOException;

	boolean read(File modelSpecificationFile) throws IOException;

	void write(String targetDirectory) throws IOException;

	void write(File targetDirectory) throws IOException;

	void addPredictionPreprocessing(TransformationSpecification transformationSpecification);

	void addPredictionPostprocessing(TransformationSpecification transformationSpecification);

	void setDescription(String description);

	void setName(String name);

	void addCitation(CitationSpecification citation);

	void setAuthors(List<String> modelAuthors);

	void setDocumentation(String documentation);

	void setTags(List<String> tags);

	void setLicense(String license);

	void setSource(String source);

	void addInputNode(InputNodeSpecification inputNode);

	void addOutputNode(OutputNodeSpecification outputNode);

	void setTrainingSource(String trainingSource);

	void setTrainingKwargs(Map<String, Object> trainingKwargs);

	List<InputNodeSpecification> getInputs();

	List<OutputNodeSpecification> getOutputs();

	String getFormatVersion();

	String getLanguage();

	String getFramework();

	String getPredictionWeightsSource();

	String getPredictionDependencies();

	Map<String, Object> getTrainingKwargs();

	String getName();

	String getDescription();

	List<CitationSpecification> getCitations();

	List<String> getAuthors();

	String getDocumentation();

	List<String> getTags();

	String getLicense();

	String getSource();

	String getTrainingSource();

	List<TransformationSpecification> getPredictionPreprocessing();

	List<TransformationSpecification> getPredictionPostprocessing();

	String getModelFileName();
}
