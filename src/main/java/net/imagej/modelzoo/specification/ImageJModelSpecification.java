package net.imagej.modelzoo.specification;

import io.bioimage.specification.DefaultModelSpecification;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.bioimage.specification.util.SpecificationUtil.asMap;

public class ImageJModelSpecification extends DefaultModelSpecification {

	private final static String idConfigFiji = "fiji";
	private final static String idTraining = "training";
	private final static String idTrainingSource = "source";
	private final static String idTrainingKwargs = "kwargs";

	public ImageJConfigSpecification getImageJConfig() {
		return readConfig(getConfig());
	}

	protected void setTrainingStats(String trainingSource, Map<String, Object> trainingKwargs) {
		ImageJConfigSpecification imageJConfig = getImageJConfig();
		if(imageJConfig == null) imageJConfig = new ImageJConfigSpecification();
		imageJConfig.setTrainingSource(trainingSource);
		imageJConfig.setTrainingKwargs(trainingKwargs);
		setConfig(imageJConfig);
	}

	public void setConfig(ImageJConfigSpecification config) {
		writeConfig(config);
	}

	private ImageJConfigSpecification readConfig(Map<String, Object> config) {
		if(config == null) config = new HashMap<>();
		ImageJConfigSpecification imageJConfig = new ImageJConfigSpecification();
		Map<String, Object> fijiConfig = asMap(config.get(idConfigFiji));
		if(fijiConfig == null) return null;
		Map<String, Object> training = asMap(fijiConfig.get(idTraining));
		if(training == null) return null;
		imageJConfig.setTrainingSource((String) training.get(idTrainingSource));
		imageJConfig.setTrainingKwargs(asMap(training.get(idTrainingKwargs)));
		return imageJConfig;
	}

	private void writeConfig(ImageJConfigSpecification imageJConfig) {
		Map<String, Object> training = new LinkedHashMap<>();
		training.put(idTrainingSource, imageJConfig.getTrainingSource());
		if (imageJConfig.getTrainingKwargs() != null) {
			training.put(idTrainingKwargs, imageJConfig.getTrainingKwargs());
		}
		Map<String, Object> fijiConfig = new LinkedHashMap<>();
		fijiConfig.put(idTraining, training);
		Map<String, Object> config = getConfig();
		if(config == null) config = new HashMap<>();
		config.put(idConfigFiji, fijiConfig);
		setConfig(config);
	}
}
