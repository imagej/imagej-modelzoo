package net.imagej.modelzoo.howto;

import net.imagej.ImageJ;
import net.imagej.modelzoo.consumer.ModelZooService;
import net.imagej.modelzoo.consumer.SingleOutputPrediction;
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
	public void useService() throws IOException {

		ij = new ImageJ();

		// resource paths
		String imgPath = "/home/random/Development/imagej/project/CSBDeep/data/N2V/pred_validation.tif";
		String modelPath = getClass().getResource("/net/imagej/modelzoo/consumer/denoise2D/model.zip").getPath();

		// load image
		Img input = (Img) ij.io().open(imgPath);

		// convert to float
		input = ij.op().convert().float32(input);

		ModelZooService modelZooService = ij.get(ModelZooService.class);

		RandomAccessibleInterval output = modelZooService.predict(modelPath, input, "XY");

		ij.ui().show(output);

	}

	@Test
	public void usePredictionAPI() throws IOException {

		ij = new ImageJ();

		// resource paths
		String imgPath = getClass().getResource("/blobs.png").getPath();
		String modelPath = getClass().getResource("/net/imagej/modelzoo/consumer/denoise2D/model.zip").getPath();

		// load image
		Img input = (Img) ij.io().open(imgPath);

		// convert to float
		input = ij.op().convert().float32(input);

		// create prediction
		SingleOutputPrediction prediction = new SingleOutputPrediction(ij.context());

		// setup prediction
		prediction.setInput("input", input, "XY");
		prediction.setModelFile(modelPath);
		prediction.setNumberOfTiles(8);
		prediction.run();
		RandomAccessibleInterval output = prediction.getOutput();

		ij.ui().show(output);

	}

	public static void main(String... args) throws IOException {
		new E01_Prediction().useService();
	}
}
