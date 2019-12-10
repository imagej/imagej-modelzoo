package net.imagej.modelzoo.howto;

import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.modelzoo.consumer.commands.ModelZooPrediction;
import org.scijava.module.Module;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class RunImageToTableNetwork {

	public static void run() throws IOException {

		ImageJ ij = new ImageJ();

		URL networkUrl = new URL("http://csbdeep.bioimagecomputing.com/model-tribolium.zip");

		Dataset input = ij.get(DatasetIOService.class).open("https://samples.fiji.sc/blobs.png");
		ij.ui().show("input", input);

		try {
			final Module module = ij.command().run(ModelZooPrediction.class,
					false, "input", input, "modelUrl", new File(networkUrl.getPath())).get();
			Dataset output = (Dataset) module.getOutput("output");
			ij.ui().show("output", output);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	public static void main(String...args) throws IOException {
		run();
	}
}
