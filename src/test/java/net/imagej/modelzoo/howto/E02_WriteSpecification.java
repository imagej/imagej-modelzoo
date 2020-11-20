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
package net.imagej.modelzoo.howto;

import net.imagej.modelzoo.specification.CitationSpecification;
import net.imagej.modelzoo.specification.DefaultCitationSpecification;
import net.imagej.modelzoo.specification.DefaultInputNodeSpecification;
import net.imagej.modelzoo.specification.DefaultModelSpecification;
import net.imagej.modelzoo.specification.DefaultOutputNodeSpecification;
import net.imagej.modelzoo.specification.InputNodeSpecification;
import net.imagej.modelzoo.specification.ModelSpecification;
import net.imagej.modelzoo.specification.OutputNodeSpecification;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

public class E02_WriteSpecification {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void run() throws IOException {

		// create specification
		ModelSpecification specification = new DefaultModelSpecification();

		// set specification values

		// meta data
		specification.setTags(Arrays.asList("segmentation", "imagej"));
		specification.setAuthors(Arrays.asList("Maya", "Selma"));
		specification.setLicense("bsd");
		specification.setName("OurAwesomeModel");
		CitationSpecification citation = new DefaultCitationSpecification();
		citation.setCitationText("Krull, A. and Buchholz, T. and Jug, F. Noise2void - learning denoising from single noisy images.\n" +
				"Proceedings of the IEEE Conference on Computer Vision and Pattern Recognition (2019)");
		citation.setDOIText("arXiv:1811.10980");
		specification.addCitation(citation);

		// input nodes
		InputNodeSpecification input = new DefaultInputNodeSpecification();
		input.setName("input");
		input.setDataType("float");
		input.setAxes("XYZC");
		input.setHalo(Arrays.asList(32, 32, 32, 1));
		input.setDataRange(Arrays.asList("-inf", "inf"));
		input.setShapeMin(Arrays.asList(16, 16, 16, 1));
		input.setShapeStep(Arrays.asList(4, 4, 4, 0));
		specification.addInputNode(input);

		// output nodes
		OutputNodeSpecification output = new DefaultOutputNodeSpecification();
		output.setName("output");
		output.setAxes("XYZC");
		output.setShapeReferenceInput(input.getName());
		output.setShapeScale(Arrays.asList(1., 1., 1., 1.));
		output.setShapeOffset(Arrays.asList(1, 1, 1, 3));
		specification.addOutputNode(output);

		// create temporary directory to save model specification to
		folder.create();
		File destination = folder.getRoot();

		// save model specification
		specification.write(destination);

		// print file content to console
		File modelSpecFile = new File(destination, specification.getModelFileName());
		System.out.println(FileUtils.readFileToString(modelSpecFile, StandardCharsets.UTF_8));

	}

	public static void main(String... args) throws IOException {
		new E02_WriteSpecification().run();
	}
}
