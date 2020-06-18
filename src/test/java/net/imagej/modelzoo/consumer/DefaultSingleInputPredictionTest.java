package net.imagej.modelzoo.consumer;

import io.scif.MissingLibraryException;
import net.imagej.ImageJ;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;

public class DefaultSingleInputPredictionTest {

	@Test
	public void testPrediction() throws URISyntaxException, IOException, MissingLibraryException {
		// create ImageJ
		ImageJ ij = new ImageJ();

		// load input image
		Path img = Paths.get(getClass().getResource("denoise2D/input.tif").toURI());
		Img input = (Img) ij.io().open(img.toAbsolutePath().toString());
		Img imgFloat = ij.op().convert().float32(input);

		// load pretrained model
		File model = new File(getClass().getResource("denoise2D/model.bioimage.io.zip").toURI());
		ModelZooArchive modelArchive = ij.get(ModelZooService.class).open(model);

		// run prediction
		DefaultSingleImagePrediction prediction = new DefaultSingleImagePrediction(ij.context());
		prediction.setTrainedModel(modelArchive);
		prediction.setInput(imgFloat, "XY");
		prediction.run();
		RandomAccessibleInterval output = prediction.getOutput();
		assertNotNull(output);

		ij.context().dispose();
	}

}
