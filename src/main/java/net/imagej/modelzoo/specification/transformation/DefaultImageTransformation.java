package net.imagej.modelzoo.specification.transformation;

public abstract class DefaultImageTransformation implements ImageTransformation {

	String idReferenceNode = "reference_input";
	private Mode mode;

	@Override
	public void setMode(Mode mode) {
		this.mode = mode;
	}

	@Override
	public Mode getMode() {
		return mode;
	}
}
