
package net.imagej.modelzoo;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.AbstractInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TiledView;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.junit.After;

import java.util.Arrays;

import static net.imagej.modelzoo.consumer.tiling.DefaultTiling.arrayProduct;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ModelZooTest {

	protected ImageJ ij;

	@After
	public void disposeIJ() {
		if(ij != null) ij.context().dispose();
	}

	protected void launchImageJ() {
		if (ij == null) {
			ij = new ImageJ();
			ij.ui().setHeadless(true);
		}
	}

	protected <T extends RealType<T> & NativeType<T>> Dataset createDataset(
		final T type, final long[] dims, final AxisType[] axes)
	{
		return ij.dataset().create(type, dims, "", axes);
	}

	protected void testResultAxesAndSize(final Dataset input,
		final Dataset output)
	{
		printDim("input", input);
		printAxes("input", input);
		printDim("output", output);
		printAxes("output", output);
		int j = 0;
		for (int i = 0; i < input.numDimensions(); i++) {
			if (input.dimension(i) == 1L) continue;
			if (input.axis(i).type() == Axes.CHANNEL) {
				assertEquals("Since the demo networks are probabilistic, the channels should double", output.dimension(j), input.dimension(i) * 2);
			}else {
				assertEquals(input.dimension(i), output.dimension(j));
			}
			assertEquals(input.axis(i).type(), output.axis(j).type());
			j++;
		}
		for (int i = 0; i < output.numDimensions(); i++) {
			if (!input.axis(output.axis(i).type()).isPresent() && !output.axis(i).type().equals(Axes.CHANNEL)) {
				assertEquals(1, output.dimension(i));
			}
		}
	}

	protected <T> void compareDimensions(final RandomAccessibleInterval<T> input,
		final RandomAccessibleInterval<T> output)
	{
		for (int i = 0; i < input.numDimensions(); i++) {
			assertEquals(input.dimension(i), output.dimension(i));
		}
	}

	protected static void printDim(final String title, final Dataset input) {
		final long[] dims = new long[input.numDimensions()];
		input.dimensions(dims);
		System.out.println(title + ": " + Arrays.toString(dims));
	}

	protected static void printDim(final String title, final AbstractInterval input) {
		final long[] dims = new long[input.numDimensions()];
		input.dimensions(dims);
		System.out.println(title + ": " + Arrays.toString(dims));
	}

	protected static void printAxes(final String title, final Dataset input) {
		final String[] axes = new String[input.numDimensions()];
		for (int i = 0; i < axes.length; i++) {
			axes[i] = input.axis(i).type().getLabel();
		}
		System.out.println(title + ": " + Arrays.toString(axes));
	}

	protected static long getNumTiles(TiledView tiledView) {
		long[] dims = new long[tiledView.numDimensions()];
		tiledView.dimensions(dims);
		return arrayProduct(dims);
	}

}
