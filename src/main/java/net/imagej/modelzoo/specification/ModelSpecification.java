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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * This is the ImageJ version of the model zoo configuration specification
 * https://github.com/bioimage-io/configuration
 */
public interface ModelSpecification {

	String getTestInput();

	void setTestInput(String testInput);

	String getTestOutput();

	void setTestOutput(String testOutput);

	boolean readFromZIP(File zippedModel);

	boolean readFromDirectory(File directory) throws IOException;

	boolean read(String modelSpecificationFile) throws IOException;

	boolean read(Path specPath) throws IOException;

	boolean read(File modelSpecificationFile) throws IOException;

	boolean read(InputStream in);

	void write(String targetDirectory) throws IOException;

	void write(File targetDirectory) throws IOException;

	void write(Path specPath) throws IOException;

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

	void setGitRepo(String repo);

	List<InputNodeSpecification> getInputs();

	List<OutputNodeSpecification> getOutputs();

	String getFormatVersion();

	String getLanguage();

	String getFramework();

	String getPredictionWeightsSource();

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

	String getModelFileName();

	String getGitRepo();

	Map<String, Object> getAttachments();

	void setFramework(String framework);

	void setFormatVersion(String formatVersion);
}
