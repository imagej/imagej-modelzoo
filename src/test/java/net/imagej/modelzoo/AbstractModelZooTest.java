
package net.imagej.modelzoo;

import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import org.junit.After;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class AbstractModelZooTest {

	protected ImageJ ij;

	@After
	public void disposeIJ() {
		if (ij != null) ij.context().dispose();
	}

	protected void createImageJ() {
		if (ij == null) {
			ij = new ImageJ();
			ij.ui().setHeadless(true);
		}
	}

	protected void testResultSize(final RandomAccessibleInterval input, final RandomAccessibleInterval output) {
		printDim("input", input);
		printDim("output", output);
		int j = 0;
		for (int i = 0; i < input.numDimensions(); i++) {
			if (input.dimension(i) == 1L) continue;
			assertEquals(input.dimension(i), output.dimension(j));
			j++;
		}
	}

	protected <T> void compareDimensions(final RandomAccessibleInterval<T> input,
	                                     final RandomAccessibleInterval<T> output) {
		for (int i = 0; i < input.numDimensions(); i++) {
			assertEquals(input.dimension(i), output.dimension(i));
		}
	}

	protected static void printDim(final String title, final RandomAccessibleInterval input) {
		final long[] dims = new long[input.numDimensions()];
		input.dimensions(dims);
		System.out.println(title + ": " + Arrays.toString(dims));
	}
}
