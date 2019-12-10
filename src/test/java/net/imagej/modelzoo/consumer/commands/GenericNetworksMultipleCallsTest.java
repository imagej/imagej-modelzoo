
package net.imagej.modelzoo.consumer.commands;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.ModelZooTest;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Test;
import org.scijava.command.CommandModule;
import org.scijava.module.Module;

import java.io.File;
import java.net.URL;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class GenericNetworksMultipleCallsTest extends ModelZooTest {

	@Test
	public void testMultipleGenericNetworks() {

		launchImageJ();

		String[] networks = {"denoise2D/model.zip",
				"denoise3D/model.zip"};

		Dataset input = createDataset(new FloatType(), new long[] { 10, 10, 10 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

		for (int i = 0; i < 3; i++) {
			for(String networkSrc : networks) {
				URL networkUrl = this.getClass().getResource(networkSrc);
				final Future<CommandModule> future = ij.command().run(ModelZooPrediction.class,
						false,
						"input", input,
						"modelFile", new File(networkUrl.getPath()));
				assertNotEquals(null, future);
				final Module module = ij.module().waitFor(future);
				final Dataset output = (Dataset) module.getOutput("output");
				assertNotNull(output);
				printDim("input", input);
				printAxes("input", input);
				printDim("output", output);
				printAxes("output", output);
				for (int j = 0; j < input.numDimensions(); j++) {
					assertEquals(input.dimension(j), output.dimension(j));
					assertEquals(input.axis(j).type(), output.axis(j).type());
				}
			}
		}

	}

}
