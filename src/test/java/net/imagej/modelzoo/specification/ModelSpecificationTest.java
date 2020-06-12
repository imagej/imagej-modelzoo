package net.imagej.modelzoo.specification;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ModelSpecificationTest {

	@Test
	public void testEmptySpec() throws IOException {

		// create spec

		ModelSpecification specification = new ModelSpecification();

		// write spec

		File dir = Files.createTempDirectory("modelzoo").toFile();
		specification.write(dir);

		// check if files exist and are not empty

		File modelFile = new File(dir.getAbsolutePath(), ModelSpecification.modelFileName);
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

		// example values

		Map<String, Object> trainingKwargs = Collections.singletonMap("arg1", 1);
		String trainingSource = "trainingSource";
		List<String> modelAuthors = Arrays.asList("author1", "author2");
		String description = "description";
		String documentation = "DOCUMENTATION_LINK";
		String license = "bsd";
		String modelName = "model name";
		Map<String, Object> preprocessingKwargs = Collections.singletonMap("mean", 100);
		Map<String, Object> postprocessingKwargs = Collections.singletonMap("mean", 100);
		String preprocessingSpec = "MyClass::preprocessing";
		String postprocessingSpec = "MyClass::postprocessing";
		String source = "source";
		String citationText = "Publication name, authors, yournal";
		String doi = "DOI";
		List<String> tags = Arrays.asList("tag1", "tag2");
		String inputName = "input";
		String axes = "XYZC";
		List<Integer> shapeMin = Arrays.asList(4, 4, 4, 1);
		List<Integer> shapeStep = Arrays.asList(16, 16, 16, 0);
		List<Integer> halo = Arrays.asList(16, 16, 16, 1);
		List<String> dataRange = Arrays.asList("-inf", "inf");
		String dataType = "float";
		String output = "output";
		List<Integer> shapeOffset = Arrays.asList(0, 0, 0, 3);
		List<Double> shapeScale = Arrays.asList(2., 2., 2., 1.);

		// create spec

		File dir = Files.createTempDirectory("modelzoo").toFile();
		ModelSpecification specification = new ModelSpecification();


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
		specification.setPredictionPreprocessing(preprocessing);
		TransformationSpecification postprocessing = new TransformationSpecification();
		postprocessing.setKwargs(postprocessingKwargs);
		postprocessing.setSpec(postprocessingSpec);
		specification.setPredictionPostprocessing(postprocessing);

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
		outputNode.setShapeOffset(shapeOffset);
		outputNode.setShapeReferenceInput(inputName);
		outputNode.setShapeScale(shapeScale);
		specification.addOutputNode(outputNode);


		// check values

		// meta
		assertEquals(modelName, specification.getName());
		assertEquals(modelAuthors, specification.getAuthors());
		assertEquals(description, specification.getDescription());
		assertEquals(documentation, specification.getDocumentation());
		assertEquals(license, specification.getLicense());
		assertEquals(source, specification.getSource());
		assertArrayEquals(tags.toArray(), specification.getTags().toArray());
		assertEquals(1, specification.getCitations().size());
		assertEquals(citation, specification.getCitations().get(0));
		// training
		assertTrue(trainingKwargs.equals(specification.getTrainingKwargs()));
		assertEquals(trainingSource, specification.getTrainingSource());
		/// prediction
		assertNotNull(specification.getPredictionPreprocessing());
		assertTrue(preprocessingKwargs.equals(specification.getPredictionPreprocessing().getKwargs()));
		assertEquals(preprocessingSpec, specification.getPredictionPreprocessing().getSpecification());
		assertNotNull(specification.getPredictionPostprocessing());
		assertTrue(postprocessingKwargs.equals(specification.getPredictionPostprocessing().getKwargs()));
		assertEquals(postprocessingSpec, specification.getPredictionPostprocessing().getSpecification());
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
		assertEquals(inputName, _output.getReferenceInputName());
		assertArrayEquals(shapeOffset.toArray(), _output.getShapeOffset().toArray());
		assertArrayEquals(shapeScale.toArray(), _output.getShapeScale().toArray());

		// write spec

		specification.write(dir);
		File modelFile = new File(dir.getAbsolutePath(), ModelSpecification.modelFileName);
		assertTrue(modelFile.exists());
		String content = FileUtils.readFileToString(modelFile, StandardCharsets.UTF_8);
		System.out.println(content);

		// read spec

		ModelSpecification newSpec = new ModelSpecification();
		assertTrue(newSpec.readFromDirectory(dir));

		// check values

		// meta
		assertEquals(modelName, newSpec.getName());
		assertEquals(modelAuthors, newSpec.getAuthors());
		assertEquals(description, newSpec.getDescription());
		assertEquals(documentation, newSpec.getDocumentation());
		assertEquals(license, newSpec.getLicense());
		assertEquals(source, newSpec.getSource());
		assertArrayEquals(tags.toArray(), newSpec.getTags().toArray());
		assertEquals(1, newSpec.getCitations().size());
		assertEquals(citation, newSpec.getCitations().get(0));
		// training
		assertTrue(trainingKwargs.equals(newSpec.getTrainingKwargs()));
		assertEquals(trainingSource, newSpec.getTrainingSource());
		/// prediction
		assertNotNull(newSpec.getPredictionPreprocessing());
		assertTrue(preprocessingKwargs.equals(newSpec.getPredictionPreprocessing().getKwargs()));
		assertEquals(preprocessingSpec, newSpec.getPredictionPreprocessing().getSpecification());
		assertNotNull(newSpec.getPredictionPostprocessing());
		assertTrue(postprocessingKwargs.equals(newSpec.getPredictionPostprocessing().getKwargs()));
		assertEquals(postprocessingSpec, newSpec.getPredictionPostprocessing().getSpecification());
		// input
		assertEquals(1, newSpec.getInputs().size());
		_input = newSpec.getInputs().get(0);
		assertEquals(inputName, _input.getName());
		assertEquals(axes, _input.getAxes());
		assertEquals(dataType, _input.getDataType());
		assertArrayEquals(shapeMin.toArray(), _input.getShapeMin().toArray());
		assertArrayEquals(shapeStep.toArray(), _input.getShapeStep().toArray());
		assertArrayEquals(dataRange.toArray(), _input.getDataRange().toArray());
		assertArrayEquals(halo.toArray(), _input.getHalo().toArray());
		assertEquals(1, newSpec.getInputs().size());
		// output
		_output = newSpec.getOutputs().get(0);
		assertEquals(output, _output.getName());
		assertEquals(inputName, _output.getReferenceInputName());
		assertArrayEquals(shapeOffset.toArray(), _output.getShapeOffset().toArray());
		assertArrayEquals(shapeScale.toArray(), _output.getShapeScale().toArray());

	}

}
