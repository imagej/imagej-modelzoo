package net.imagej.modelzoo.consumer.commands;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class TestGenericNetworkCommand implements Command {

	@Parameter
	private
	Dataset input;

	@Parameter
	private final boolean normalizeInput = true;
	@Parameter
	private final float percentileBottom = 3.0f;
	@Parameter
	private final float percentileTop = 99.8f;

	@Parameter
	private
	CommandService command;

	@Parameter
	private
	UIService ui;

	@Override
	public void run() {
		try {
			Module module = command.run(ModelZooPrediction.class, false, "input", input,
					"normalizeInput", normalizeInput,
					"percentileBottom", percentileBottom,
					"percentileTop", percentileTop,
					"modelUrl", "http://csbdeep.bioimagecomputing.com/model-project.zip").get();
			ui.show(module.getOutput("output"));
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}

	public static void main(final String[] args) throws IOException {
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();

		ij.launch(args);

		// ask the user for a file to open
		final File file = ij.ui().chooseFile(null, "open");

		if (file != null && file.exists()) {
			// load the dataset
			final Dataset dataset = ij.scifio().datasetIO().open(file
					.getAbsolutePath());

			// show the image
			ij.ui().show(dataset);

			// invoke the plugin
			ij.command().run(TestGenericNetworkCommand.class, true);
		}
	}

}
