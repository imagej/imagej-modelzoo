
package net.imagej.modelzoo.consumer;

import net.imagej.modelzoo.AbstractModelZooTest;
import net.imagej.modelzoo.consumer.commands.DefaultModelZooPredictionCommand;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
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

		URL networkUrl = this.getClass().getResource("denoise2D/model.bioimage.io.zip");

		final RandomAccessibleInterval input = new ArrayImgFactory<>(new FloatType()).create(3, 3, 3);

		final Module module = ij.command().run(DefaultModelZooPredictionCommand.class, false,
				"input", input,
				"mapping", "XYZ",
				"modelFile", new File(networkUrl.getPath())).get();
		GenericTable output = (GenericTable) module.getOutput("output");
		assertNotNull(output);
		System.out.println(output);

	}

}
