package net.imagej.modelzoo.howto;

import net.imagej.ImageJ;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;

public class E04_OpenArchivedModel {
	private ImageJ ij;

	@After
	public void tearDown() {
		ij.context().dispose();
	}

	@Test
	public void run() throws IOException {

		ij = new ImageJ();
		ij.launch();

		// resource paths
		String modelPath = "/home/random/Development/imagej/project/CSBDeep/training/DenoiSeg/mouse/latest.bioimage.io.zip";

		Object model = ij.io().open(modelPath);
		ij.ui().show(model);

	}

	public static void main(String... args) throws IOException {
		new E04_OpenArchivedModel().run();
	}
}
