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
package net.imagej.modelzoo.howto;

import net.imagej.ImageJ;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imglib2.img.Img;
import org.junit.After;
import org.junit.Test;

public class E06_RunSanityCheck {
	private ImageJ ij;

	@After
	public void tearDown() {
		ij.context().dispose();
	}

	@Test
	public void run() throws Exception {

		ij = new ImageJ();
		ij.launch();

		// resource paths
		String modelPath =  getClass().getResource("/net/imagej/modelzoo/consumer/denoise2D/dummy.model.bioimage.io.zip").getPath();
		String imgPath = getClass().getResource("/blobs.png").getPath();

		// load image
		Img input = (Img) ij.io().open(imgPath);

		// create noisy image from input image
		Img noisy = ij.op().create().img(input);
		ij.op().filter().gauss(noisy, input, 5);

		// display results
		ij.ui().show("Expected (GT)", input);
		ij.ui().show("Sanity Check input", noisy);


		ModelZooService modelZooService = ij.get(ModelZooService.class);
		ModelZooArchive model = modelZooService.io().open(modelPath);
		modelZooService.sanityCheckInteractive(model, noisy, input);

	}

	public static void main(String... args) throws Exception {
		new E06_RunSanityCheck().run();
	}
}
