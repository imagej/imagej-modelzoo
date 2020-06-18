package net.imagej.modelzoo.consumer;

import io.scif.MissingLibraryException;
import net.imagej.ImageJ;
import net.imagej.modelzoo.ModelZooArchive;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

public class DefaultModelZooPredictionTest {

	@Test
	public void testPrediction() throws URISyntaxException, IOException, MissingLibraryException {
		ImageJ ij = new ImageJ();

		Path img = Paths.get(getClass().getResource("denoise2D/input.tif").toURI());

		Img input = (Img) ij.io().open(img.toAbsolutePath().toString());
		Img inputFloat = ij.op().convert().float32(input);

		File archiveFile = new File(getClass().getResource("denoise2D/model.bioimage.io.zip").toURI());

		DefaultModelZooPrediction prediction = new DefaultModelZooPrediction(ij.context());
		prediction.setInput("input", inputFloat, "XY");
		Object archive = ij.io().open(archiveFile.getAbsolutePath());
		prediction.setTrainedModel((ModelZooArchive) archive);
		prediction.run();
		Map<String, RandomAccessibleInterval<?>> res = prediction.getOutputs();
		RandomAccessibleInterval output = res.values().iterator().next();
		assertNotNull(output);
	}

	@Test
	public void testNoTiling() throws URISyntaxException, IOException, MissingLibraryException {
		ImageJ ij = new ImageJ();
		File archiveFile = new File(getClass().getResource("denoise2D/model.bioimage.io.zip").toURI());
		Img inputFloat = new ArrayImgFactory<>(new FloatType()).create(7, 7);
		DefaultModelZooPrediction prediction = new DefaultModelZooPrediction(ij.context());
		prediction.setInput("input", inputFloat, "XY");
		Object archive = ij.io().open(archiveFile.getAbsolutePath());
		prediction.setTrainedModel((ModelZooArchive) archive);
		prediction.run();
		Map<String, RandomAccessibleInterval<?>> res = prediction.getOutputs();
		RandomAccessibleInterval output = res.values().iterator().next();
		assertNotNull(output);
		assertArrayEquals(Intervals.dimensionsAsLongArray(inputFloat), Intervals.dimensionsAsLongArray(output));
	}
}
