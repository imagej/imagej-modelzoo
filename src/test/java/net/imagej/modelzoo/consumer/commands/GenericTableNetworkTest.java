
package net.imagej.modelzoo.consumer.commands;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.AbstractModelZooTest;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Ignore;
import org.junit.Test;
import org.scijava.module.Module;
import org.scijava.table.GenericTable;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.assertNotNull;

public class GenericTableNetworkTest extends AbstractModelZooTest {

	@Test
	@Ignore //FIXME provide a way to use the table output processor
	public void testGenericNetwork() throws ExecutionException, InterruptedException {
		createImageJ();

		URL networkUrl = this.getClass().getResource("denoise2D/model.zip");

		final Dataset input = createDataset(new FloatType(), new long[] { 3, 3, 3 }, new AxisType[] {
				Axes.X, Axes.Y, Axes.Z });

		final Module module = ij.command().run(ModelZooPredictionCommand.class,
				false, "input", input, "modelFile", new File(networkUrl.getPath())).get();
		GenericTable output = (GenericTable) module.getOutput("output");
		assertNotNull(output);
		System.out.println(output);

	}

}
