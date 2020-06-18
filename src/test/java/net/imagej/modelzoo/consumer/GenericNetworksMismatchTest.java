
package net.imagej.modelzoo.consumer;

import net.imagej.modelzoo.AbstractModelZooTest;
import net.imagej.modelzoo.consumer.commands.DefaultModelZooPredictionCommand;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Before;
import org.junit.Test;
import org.scijava.module.Module;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertNotNull;

public class GenericNetworksMismatchTest extends AbstractModelZooTest {

	@Before
	public void setup() {
		createImageJ();
	}

	@Test
	public void test3DNetworkWith3DInputImage() throws ExecutionException, InterruptedException {
		RandomAccessibleInterval input = new ArrayImgFactory<>(new FloatType()).create(10, 20, 1);
		test("denoise3D/model.bioimage.io.zip", input, "XYZ");

	}

	@Test
	public void test2DNetworkWith2DInputImage() throws ExecutionException, InterruptedException {
		RandomAccessibleInterval input = new ArrayImgFactory<>(new FloatType()).create(5, 10, 20);
		test("denoise2D/model.bioimage.io.zip", input, "XYZ");
		test("denoise2D/model.bioimage.io.zip", input, "XYZ");

	}

	@Test
	public void test3DNetworkWith2DInputImage() throws ExecutionException, InterruptedException {
		RandomAccessibleInterval input = new ArrayImgFactory<>(new FloatType()).create(10, 20);
		test("denoise3D/model.bioimage.io.zip", input, "XY");
	}

	@Test
	public void test2DNetworkWith3DInputImage() throws ExecutionException, InterruptedException {
		RandomAccessibleInterval input = new ArrayImgFactory<>(new FloatType()).create(10, 20, 30);
		test("denoise2D/model.bioimage.io.zip", input, "XYZ");

	}

	private void test(String network, RandomAccessibleInterval input, String axes) throws ExecutionException, InterruptedException {
		URL networkUrl = this.getClass().getResource(network);
		final Module module = ij.command().run(DefaultModelZooPredictionCommand.class,
				false,
				"input", input,
				"axes", axes,
				"modelFile", new File(networkUrl.getPath())).get();
		assertNotNull(module);
		final RandomAccessibleInterval output = (RandomAccessibleInterval) module.getOutput("output");
		assertNotNull(output);
		testResultSize(input, output);
	}

}
