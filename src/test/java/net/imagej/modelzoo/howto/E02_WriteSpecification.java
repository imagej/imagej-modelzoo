package net.imagej.modelzoo.howto;

import net.imagej.modelzoo.specification.CitationSpecification;
import net.imagej.modelzoo.specification.InputNodeSpecification;
import net.imagej.modelzoo.specification.ModelSpecification;
import net.imagej.modelzoo.specification.OutputNodeSpecification;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

public class E02_WriteSpecification {

	@Test
	public void run() throws IOException {

		// create specification
		ModelSpecification specification = new ModelSpecification();

		// set specification values

		// meta data
		specification.setTags(Arrays.asList("segmentation", "imagej"));
		specification.setAuthors(Arrays.asList("Maya", "Selma"));
		specification.setLicense("bsd");
		specification.setName("OurAwesomeModel");
		CitationSpecification citation = new CitationSpecification();
		citation.setCitationText("Krull, A. and Buchholz, T. and Jug, F. Noise2void - learning denoising from single noisy images.\n" +
				"Proceedings of the IEEE Conference on Computer Vision and Pattern Recognition (2019)");
		citation.setDOIText("arXiv:1811.10980");
		specification.addCitation(citation);

		// input nodes
		InputNodeSpecification input = new InputNodeSpecification();
		input.setName("input");
		input.setDataType("float");
		input.setAxes("XYZC");
		input.setHalo(Arrays.asList(32, 32, 32, 1));
		input.setDataRange(Arrays.asList("-inf", "inf"));
		input.setShapeMin(Arrays.asList(16, 16, 16, 1));
		input.setShapeStep(Arrays.asList(4, 4, 4, 0));
		specification.addInputNode(input);

		// output nodes
		OutputNodeSpecification output = new OutputNodeSpecification();
		output.setName("output");
		output.setAxes("XYZC");
		output.setShapeReferenceInput(input.getName());
		output.setShapeScale(Arrays.asList(1., 1., 1., 1.));
		output.setShapeOffset(Arrays.asList(1, 1, 1, 3));
		specification.addOutputNode(output);

		// create temporary directory to save model specification to
		File destination = Files.createTempDirectory("modelzoo-spec").toFile();

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
