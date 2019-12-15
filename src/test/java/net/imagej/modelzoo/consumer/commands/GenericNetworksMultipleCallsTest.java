
package net.imagej.modelzoo.consumer.commands;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.AbstractModelZooTest;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Test;
import org.scijava.command.CommandModule;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class GenericNetworksMultipleCallsTest extends AbstractModelZooTest {

	@Test
	public void testMultipleGenericNetworks() throws ExecutionException, InterruptedException {

		createImageJ();

		String[] networks = {"denoise2D/model.zip"};

		Dataset input = createDataset(new FloatType(), new long[] { 4, 5, 6 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

//		for (int i = 0; i < 3; i++) {
			for(String networkSrc : networks) {
				URL networkUrl = this.getClass().getResource(networkSrc);
				CommandModule module = ij.command().run(ModelZooPredictionCommand.class,
						false,
						"input", input,
						"modelFile", new File(networkUrl.getPath())).get();
				final RandomAccessibleInterval output = (RandomAccessibleInterval) module.getOutput("output");
				assertNotNull(output);
				printDim("input", input);
				printAxes("input", input);
				printDim("output", output);
				for (int j = 0; j < input.numDimensions(); j++) {
					assertEquals(input.dimension(j), output.dimension(j));
				}
			}
//		}

	}

	public static void main(String...args) throws ExecutionException, InterruptedException {
		new GenericNetworksMultipleCallsTest().testMultipleGenericNetworks();
	}

}
