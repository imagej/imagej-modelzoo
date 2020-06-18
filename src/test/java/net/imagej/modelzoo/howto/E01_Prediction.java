package net.imagej.modelzoo.howto;

import io.scif.MissingLibraryException;
import net.imagej.ImageJ;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.consumer.DefaultSingleImagePrediction;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;

public class E01_Prediction {

	private ImageJ ij;

	@After
	public void tearDown() {
		ij.context().dispose();
	}

	@Test
	public void useService() throws IOException, MissingLibraryException {

		ij = new ImageJ();

		// resource paths
		String imgPath = "/home/random/Development/imagej/project/CSBDeep/data/N2V/pred_validation.tif";
		String modelPath = getClass().getResource("/net/imagej/modelzoo/consumer/denoise2D/model.bioimage.io.zip").getPath();

		// load image
		Img input = (Img) ij.io().open(imgPath);

		// convert to float
		input = ij.op().convert().float32(input);

		ModelZooService modelZooService = ij.get(ModelZooService.class);

		ModelZooArchive model = modelZooService.open(modelPath);

		RandomAccessibleInterval output = modelZooService.predict(model, input, "XY");

		ij.ui().show(output);

	}

	@Test
	public void usePredictionAPI() throws IOException, MissingLibraryException {

		ij = new ImageJ();

		// resource paths
		String imgPath = getClass().getResource("/blobs.png").getPath();
		String modelPath = getClass().getResource("/net/imagej/modelzoo/consumer/denoise2D/model.bioimage.io.zip").getPath();

		// load image
		Img input = (Img) ij.io().open(imgPath);

		// convert to float
		input = ij.op().convert().float32(input);

		// create prediction
		DefaultSingleImagePrediction prediction = new DefaultSingleImagePrediction(ij.context());

		// setup prediction
		prediction.setInput("input", input, "XY");
		prediction.setTrainedModel(modelPath);
		prediction.setNumberOfTiles(8);
		prediction.run();
		RandomAccessibleInterval output = prediction.getOutput();

		ij.ui().show(output);

	}

	public static void main(String... args) throws IOException, MissingLibraryException {
//		new E01_Prediction().useService();
		new E01_Prediction().usePredictionAPI();
	}
}
