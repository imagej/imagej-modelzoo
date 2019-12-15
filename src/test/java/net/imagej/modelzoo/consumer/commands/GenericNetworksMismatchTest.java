
package net.imagej.modelzoo.consumer.commands;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.AbstractModelZooTest;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Test;
import org.scijava.module.Module;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertNotNull;

public class GenericNetworksMismatchTest extends AbstractModelZooTest {

	@Test
	public void test3DNetworkWith3DInputImage() throws ExecutionException, InterruptedException {

		createImageJ();

		Dataset input = createDataset(new FloatType(), new long[] { 10, 20, 1 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

		test("denoise3D/model.zip", input);

	}

	@Test
	public void test2DNetworkWith2DInputImage() throws ExecutionException, InterruptedException {

		createImageJ();

		Dataset input = createDataset(new FloatType(), new long[] { 5, 10, 20 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

		test("denoise2D/model.zip", input);
		test("denoise2D/model.zip", input);

	}

	@Test
	public void test3DNetworkWith2DInputImage() throws ExecutionException, InterruptedException {

		createImageJ();

		Dataset input = createDataset(new FloatType(), new long[] { 10, 20 }, new AxisType[] {
				Axes.X, Axes.Y });

		test("denoise3D/model.zip", input);

	}

	@Test
	public void test2DNetworkWith3DInputImage() throws ExecutionException, InterruptedException {

		createImageJ();

		Dataset input = createDataset(new FloatType(), new long[] { 10, 20, 30 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

		test("denoise2D/model.zip", input);

	}

	private void test(String network, Dataset input) throws ExecutionException, InterruptedException {
		URL networkUrl = this.getClass().getResource(network);
		final Module module = ij.command().run(ModelZooPredictionCommand.class,
				false,
				"input", input,
				"modelFile", new File(networkUrl.getPath())).get();
		assertNotNull(module);
		final RandomAccessibleInterval output = (RandomAccessibleInterval) module.getOutput("output");
		assertNotNull(output);
		testResultSize(input, output);
	}

}
