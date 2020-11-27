package net.imagej.modelzoo.specification;

import java.util.Map;

public class ImageJConfigSpecification {

	private String trainingSource;
	private Map<String, Object> trainingKwargs;

	public String getTrainingSource() {
		return trainingSource;
	}

	void setTrainingSource(String trainingSource) {
		this.trainingSource = trainingSource;
	}

	public Map<String, Object> getTrainingKwargs() {
		return trainingKwargs;
	}

	public void setTrainingKwargs(Map<String, Object> trainingKwargs) {
		this.trainingKwargs = trainingKwargs;
	}
}
