package net.imagej.modelzoo.specification.transformation;

public class ZeroMeanUnitVarianceTransformation extends DefaultImageTransformation {
	public static final String name = "zero_mean_unit_variance";
	private Number mean;
	private Number std;

	public Number getMean() {
		return mean;
	}

	public void setMean(Number mean) {
		this.mean = mean;
	}

	public Number getStd() {
		return std;
	}

	public void setStd(Number std) {
		this.std = std;
	}

	@Override
	public String getName() {
		return name;
	}
}
