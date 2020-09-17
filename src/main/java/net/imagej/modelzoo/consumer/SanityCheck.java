package net.imagej.modelzoo.consumer;

import net.imagej.Dataset;
import net.imagej.modelzoo.ModelZooArchive;

public interface SanityCheck {
	void checkInteractive(Dataset input, Dataset output, Dataset gt, ModelZooArchive model);
}
