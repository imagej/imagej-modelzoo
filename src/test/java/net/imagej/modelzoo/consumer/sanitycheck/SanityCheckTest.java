/*-
 * #%L
 * This is the bioimage.io modelzoo library for ImageJ.
 * %%
 * Copyright (C) 2019 - 2021 Center for Systems Biology Dresden
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
package net.imagej.modelzoo.consumer.sanitycheck;

import net.imagej.ImageJ;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

public class SanityCheckTest {

	@Test
	public void testNormalizeMinimize() {
		ImageJ ij = new ImageJ();

		Img<FloatType> imgX = ij.op().create().img(new FinalDimensions(32, 32, 32), new FloatType());
		Img<FloatType> imgTarget = ij.op().create().img(imgX);

		Random random = new Random(42);
		imgTarget.forEach(floatType -> floatType.setReal(random.nextDouble() * 200. - 100.));
		double r1 = random.nextDouble() * 1000. - 500.;
		double r2 = random.nextDouble() * 1000. - 500.;
		LoopBuilder.setImages(imgX, imgTarget).multiThreaded().forEachPixel((xPx, targetPx) -> {
			xPx.setReal(r1 * targetPx.get() + r2);
		});

		ImageToImageSanityCheck.Stats<FloatType> statsX = new ImageToImageSanityCheck.Stats<>(imgX, "input", ij.op());
		ImageToImageSanityCheck.Stats<FloatType> statsTarget = new ImageToImageSanityCheck.Stats<>(imgTarget, "output", ij.op());
		RandomAccessibleInterval<FloatType> normMin = ImageToImageSanityCheck.normalizeMinimize(statsX, statsTarget, ij.op());
		ImageToImageSanityCheck.Stats<FloatType> normMinStats = new ImageToImageSanityCheck.Stats<>(normMin, "normmin", ij.op());

		System.out.println(statsX);
		System.out.println(statsTarget);
		System.out.println(normMinStats);

		checkIfAllClose(imgTarget, normMin);

		Img<FloatType> difference = ij.op().create().img(imgX);
		ij.op().math().subtract(difference, Views.iterable(normMin), (RandomAccessibleInterval<FloatType>)imgTarget);
		FloatType sumDiff = new FloatType(0);
		difference.forEach(sumDiff::add);
		assertTrue(Math.abs(sumDiff.getRealFloat()) < 1e-3);

		ij.dispose();
	}

	private void checkIfAllClose(Img<FloatType> imgTarget, RandomAccessibleInterval<FloatType> normMin) {
		double atol = 1.e-7;
		double rtol = 1.e-5;
		LoopBuilder.setImages(normMin, imgTarget).forEachPixel((normMinPix, targetPix) -> {
			boolean isClose = isClose(normMinPix, targetPix, atol, rtol);
			assertTrue(isClose);
		});
	}

	private boolean isClose(FloatType x, FloatType target, double atol, double rtol) {
		float a = x.get();
		float b = target.get();
		return Math.abs(a - b) <= atol + rtol * Math.abs(b);
	}
}
