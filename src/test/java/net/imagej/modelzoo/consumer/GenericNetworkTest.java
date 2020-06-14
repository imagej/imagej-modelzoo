
package net.imagej.modelzoo.consumer;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.AbstractModelZooTest;
import net.imagej.modelzoo.consumer.commands.ModelZooPredictionCommand;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Test;
import org.scijava.module.Module;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.assertNotNull;

public class GenericNetworkTest extends AbstractModelZooTest {

	@Test
	public void testMissingNetwork() throws ExecutionException, InterruptedException {
		createImageJ();
		final RandomAccessibleInterval input = new ArrayImgFactory<>(new FloatType()).create(2,2);
		ij.command().run(ModelZooPredictionCommand.class,
				false, "input", input, "mapping", "XY", "modelFile", new File(
						"/some/non/existing/path.zip")).get();
	}

	@Test
	public void testNonExistingNetworkPref() throws ExecutionException, InterruptedException {
		createImageJ();
		String bla = new PredictionLoader().getModelFileKey();
		ij.prefs().put(ModelZooPredictionCommand.class, bla, "/something/useless");
		final RandomAccessibleInterval input = new ArrayImgFactory<>(new FloatType()).create(2,2);
		ij.command().run(ModelZooPredictionCommand.class, true,
				"input", input,
				"mapping", "XY",
				"modelUrl", "http://csbdeep.bioimagecomputing.com/model-tubulin.zip").get();
	}

	@Test
	public void testGenericNetwork() throws ExecutionException, InterruptedException {
		createImageJ();
		for (int i = 0; i < 1; i++) {

			testDataset(new FloatType(), new long[] { 5, 10, 33 }, "XYZ");
//			testDataset(new UnsignedIntType(), new long[] { 10, 10, 10 },
//				new AxisType[] { Axes.X, Axes.Y, Axes.Z });
//			testDataset(new ByteType(), new long[] { 10, 10, 10 }, new AxisType[] {
//				Axes.X, Axes.Y, Axes.Z });

			if (i % 10 == 0) System.out.println(i);
		}

	}

	private <T extends RealType<T> & NativeType<T>> void testDataset(final T type,
	                                                                 final long[] dims, String mapping) throws ExecutionException, InterruptedException {

		URL networkUrl = this.getClass().getResource("denoise2D/model.zip");

		final RandomAccessibleInterval input = new ArrayImgFactory<>(type).create(dims);
		final Module module = ij.command().run(ModelZooPredictionCommand.class, false,
				"input", input,
				"modelFile", new File(networkUrl.getPath()),
				"mapping", mapping).get();
		RandomAccessibleInterval output = (RandomAccessibleInterval) module.getOutput("output");
		assertNotNull(output);
		testResultSize(input, output);

	}

}
