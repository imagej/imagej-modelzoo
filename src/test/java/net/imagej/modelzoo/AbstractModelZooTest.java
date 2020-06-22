/*-
 * #%L
 * This is the bioimage.io modelzoo library for ImageJ.
 * %%
 * Copyright (C) 2019 - 2020 Center for Systems Biology Dresden
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

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
