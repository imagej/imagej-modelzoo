package net.imagej.modelzoo.specification.transformation;

import net.imagej.modelzoo.specification.TransformationSpecification;

public interface ImageTransformation extends TransformationSpecification {

	enum Mode {
		FIXED("fixed"), PER_DATASET("per_dataset"), PER_SAMPLE("per_sample");
		private final String name;
		Mode(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
	}

	default void setMode(String mode) {
		for (Mode value : Mode.values()) {
			if(value.getName().equals(mode)) {
				setMode(value);
				return;
			}
		}
	}

	void setMode(Mode mode);

	Mode getMode();

}
