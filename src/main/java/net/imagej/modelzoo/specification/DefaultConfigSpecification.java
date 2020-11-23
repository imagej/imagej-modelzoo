package net.imagej.modelzoo.specification;

public class DefaultConfigSpecification implements ConfigSpecification {
	private String runner;

	@Override
	public String getRunner() {
		return runner;
	}
}
