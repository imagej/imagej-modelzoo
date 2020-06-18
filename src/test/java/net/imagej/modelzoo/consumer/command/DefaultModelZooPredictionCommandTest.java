package net.imagej.modelzoo.consumer.command;

import net.imagej.ImageJ;
import net.imagej.modelzoo.consumer.commands.DefaultModelZooPredictionCommand;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import org.junit.Test;
import org.scijava.command.CommandModule;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertNotNull;

public class DefaultModelZooPredictionCommandTest {

	@Test
	public void testCommand() throws IOException, URISyntaxException, ExecutionException, InterruptedException {
		ImageJ ij = new ImageJ();

		Path img = Paths.get(getClass().getResource("../denoise2D/input.tif").toURI());

		Img input = (Img) ij.io().open(img.toAbsolutePath().toString());

		Path model = Paths.get(getClass().getResource("../denoise2D/model.bioimage.io.zip").toURI());

		CommandModule module = ij.command().run(DefaultModelZooPredictionCommand.class,
				false,
				"input", input,
				"modelFile", model.toFile()).get();

		RandomAccessibleInterval output = (RandomAccessibleInterval) module.getOutput("output");
		assertNotNull(output);
	}
}
