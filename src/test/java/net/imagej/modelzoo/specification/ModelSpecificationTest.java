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
package net.imagej.modelzoo.specification;

import io.bioimage.specification.io.SpecificationReader;
import io.bioimage.specification.io.SpecificationWriter;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ModelSpecificationTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	// example values
	private final static String trainingSource = "trainingSource";
	private final static Map<String, Object> trainingKwargs = Collections.singletonMap("bla", "blubb");

	@Test
	public void testExampleSpec() throws IOException {

		// create spec

		File dir = folder.getRoot();
		ImageJModelSpecification specification = new ImageJModelSpecification();
		setExampleValues(specification);

		// check values
		checkExampleValues(specification);

		// write spec
		SpecificationWriter.write(specification, dir);
		File modelFile = new File(dir.getAbsolutePath(), SpecificationWriter.getModelFileName());
		assertTrue(modelFile.exists());
		String content = FileUtils.readFileToString(modelFile, StandardCharsets.UTF_8);
		System.out.println(content);

		// read spec
		ImageJModelSpecification newSpec = new ImageJModelSpecification();
		assertTrue(SpecificationReader.readFromDirectory(dir, newSpec));

		// check values
		checkExampleValues(newSpec);

	}

	private void setExampleValues(ImageJModelSpecification specification) {
		// meta
		ImageJConfigSpecification config = new ImageJConfigSpecification();
		config.setTrainingSource(trainingSource);
		config.setTrainingKwargs(trainingKwargs);
		specification.setConfig(config);
	}

	private void checkExampleValues(ImageJModelSpecification specification) {
		ImageJConfigSpecification config = specification.getImageJConfig();
		assertEquals(trainingSource, config.getTrainingSource());
		assertEquals(trainingKwargs, config.getTrainingKwargs());
	}

}
