package net.imagej.modelzoo.specification;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ModelSpecificationTest {


	// example values
	private final static Map<String, Object> trainingKwargs = Collections.singletonMap("arg1", 1);
	private final static String trainingSource = "trainingSource";
	private final static List<String> modelAuthors = Arrays.asList("author1", "author2");
	private final static String description = "description";
	private final static String documentation = "DOCUMENTATION_LINK";
	private final static String license = "bsd";
	private final static String modelName = "model name";
	private final static Map<String, Object> preprocessingKwargs = Collections.singletonMap("mean", 100);
	private final static Map<String, Object> postprocessingKwargs = Collections.singletonMap("mean", 100);
	private final static String preprocessingSpec = "MyClass::preprocessing";
	private final static String postprocessingSpec = "MyClass::postprocessing";
	private final static String source = "source";
	private final static String citationText = "Publication name, authors, yournal";
	private final static String doi = "DOI";
	private final static List<String> tags = Arrays.asList("tag1", "tag2");
	private final static String inputName = "input";
	private final static String axes = "XYZC";
	private final static List<Integer> shapeMin = Arrays.asList(4, 4, 4, 1);
	private final static List<Integer> shapeStep = Arrays.asList(16, 16, 16, 0);
	private final static List<Integer> halo = Arrays.asList(16, 16, 16, 1);
	private final static List<String> dataRange = Arrays.asList("-inf", "inf");
	private final static String dataType = "float";
	private final static String output = "output";
	private final static List<Integer> shapeOffset = Arrays.asList(0, 0, 0, 3);
	private final static List<Double> shapeScale = Arrays.asList(2., 2., 2., 1.);

	@Test
	public void testEmptySpec() throws IOException {

		// create spec

		ModelSpecification specification = new ModelSpecification();

		// write spec

		File dir = Files.createTempDirectory("modelzoo").toFile();
		specification.write(dir);

		// check if files exist and are not empty

		File modelFile = new File(dir.getAbsolutePath(), specification.getModelFileName());
		assertTrue(modelFile.exists());
		File dependencyFile = new File(dir.getAbsolutePath(), ModelSpecification.dependenciesFileName);
		assertTrue(dependencyFile.exists());
		String content = FileUtils.readFileToString(modelFile, StandardCharsets.UTF_8);
		assertFalse(content.isEmpty());
		content = FileUtils.readFileToString(dependencyFile, StandardCharsets.UTF_8);
		assertFalse(content.isEmpty());

		// read spec
		ModelSpecification newSpec = new ModelSpecification();
		assertTrue(newSpec.readFromDirectory(dir));

		// check default spec values
		assertEquals(ModelSpecification.modelZooSpecificationVersion, newSpec.getFormatVersion());
	}

	@Test
	public void testExampleSpec() throws IOException {

		// create spec

		File dir = Files.createTempDirectory("modelzoo").toFile();
		ModelSpecification specification = new ModelSpecification();
		setExampleValues(specification);

		// check values
		checkExampleValues(specification);

		// write spec
		specification.write(dir);
		File modelFile = new File(dir.getAbsolutePath(), specification.getModelFileName());
		assertTrue(modelFile.exists());
		String content = FileUtils.readFileToString(modelFile, StandardCharsets.UTF_8);
		System.out.println(content);

		// read spec
		ModelSpecification newSpec = new ModelSpecification();
		assertTrue(newSpec.readFromDirectory(dir));

		// check values
		checkExampleValues(newSpec);

	}

	private void setExampleValues(ModelSpecification specification) {
		// meta
		specification.setName(modelName);
		specification.setAuthors(modelAuthors);
		specification.setDescription(description);
		specification.setDocumentation(documentation);
		specification.setLicense(license);
		specification.setSource(source);
		specification.setTags(tags);
		CitationSpecification citation = new CitationSpecification();
		citation.setCitationText(citationText);
		citation.setDOIText(doi);
		specification.addCitation(citation);
		// training
		specification.setTrainingKwargs(trainingKwargs);
		specification.setTrainingSource(trainingSource);
		// prediction
		TransformationSpecification preprocessing = new TransformationSpecification();
		preprocessing.setKwargs(preprocessingKwargs);
		preprocessing.setSpec(preprocessingSpec);
		specification.addPredictionPreprocessing(preprocessing);
		TransformationSpecification postprocessing = new TransformationSpecification();
		postprocessing.setKwargs(postprocessingKwargs);
		postprocessing.setSpec(postprocessingSpec);
		specification.addPredictionPostprocessing(postprocessing);
		// input node
		InputNodeSpecification inputNode = new InputNodeSpecification();
		inputNode.setShapeMin(shapeMin);
		inputNode.setShapeStep(shapeStep);
		inputNode.setAxes(axes);
		inputNode.setDataRange(dataRange);
		inputNode.setDataType(dataType);
		inputNode.setName(inputName);
		inputNode.setHalo(halo);
		specification.addInputNode(inputNode);
		// output node
		OutputNodeSpecification outputNode = new OutputNodeSpecification();
		outputNode.setName(output);
		outputNode.setAxes(axes);
		outputNode.setShapeOffset(shapeOffset);
		outputNode.setShapeReferenceInput(inputName);
		outputNode.setShapeScale(shapeScale);
		specification.addOutputNode(outputNode);
	}

	private void checkExampleValues(ModelSpecification specification) {
		// meta
		assertEquals(modelName, specification.getName());
		assertEquals(modelAuthors, specification.getAuthors());
		assertEquals(description, specification.getDescription());
		assertEquals(documentation, specification.getDocumentation());
		assertEquals(license, specification.getLicense());
		assertEquals(source, specification.getSource());
		assertArrayEquals(tags.toArray(), specification.getTags().toArray());
		assertEquals(1, specification.getCitations().size());
		CitationSpecification citation = new CitationSpecification();
		citation.setCitationText(citationText);
		citation.setDOIText(doi);
		assertEquals(citation, specification.getCitations().get(0));
		// training
		assertTrue(trainingKwargs.equals(specification.getTrainingKwargs()));
		assertEquals(trainingSource, specification.getTrainingSource());
		/// prediction
		assertNotNull(specification.getPredictionPreprocessing());
		assertEquals(1, specification.getPredictionPreprocessing().size());
		assertTrue(preprocessingKwargs.equals(specification.getPredictionPreprocessing().get(0).getKwargs()));
		assertEquals(preprocessingSpec, specification.getPredictionPreprocessing().get(0).getSpecification());
		assertNotNull(specification.getPredictionPostprocessing());
		assertEquals(1, specification.getPredictionPostprocessing().size());
		assertTrue(postprocessingKwargs.equals(specification.getPredictionPostprocessing().get(0).getKwargs()));
		assertEquals(postprocessingSpec, specification.getPredictionPostprocessing().get(0).getSpecification());
		// input
		assertEquals(1, specification.getInputs().size());
		InputNodeSpecification _input = specification.getInputs().get(0);
		assertEquals(inputName, _input.getName());
		assertEquals(axes, _input.getAxes());
		assertEquals(dataType, _input.getDataType());
		assertArrayEquals(shapeMin.toArray(), _input.getShapeMin().toArray());
		assertArrayEquals(shapeStep.toArray(), _input.getShapeStep().toArray());
		assertArrayEquals(dataRange.toArray(), _input.getDataRange().toArray());
		assertArrayEquals(halo.toArray(), _input.getHalo().toArray());
		assertEquals(1, specification.getInputs().size());
		// output
		OutputNodeSpecification _output = specification.getOutputs().get(0);
		assertEquals(output, _output.getName());
		assertEquals(axes, _output.getAxes());
		assertEquals(inputName, _output.getReferenceInputName());
		assertArrayEquals(shapeOffset.toArray(), _output.getShapeOffset().toArray());
		assertArrayEquals(shapeScale.toArray(), _output.getShapeScale().toArray());
	}

}
