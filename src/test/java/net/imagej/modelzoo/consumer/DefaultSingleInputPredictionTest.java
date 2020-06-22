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
package net.imagej.modelzoo.consumer;

import io.scif.MissingLibraryException;
import net.imagej.ImageJ;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;

public class DefaultSingleInputPredictionTest {

	@Test
	public void testPrediction() throws URISyntaxException, IOException, MissingLibraryException {
		// create ImageJ
		ImageJ ij = new ImageJ();

		// load input image
		Path img = Paths.get(getClass().getResource("denoise2D/input.tif").toURI());
		Img input = (Img) ij.io().open(img.toAbsolutePath().toString());
		Img imgFloat = ij.op().convert().float32(input);

		// load pretrained model
		File model = new File(getClass().getResource("denoise2D/model.bioimage.io.zip").toURI());
		ModelZooArchive modelArchive = ij.get(ModelZooService.class).open(model);

		// run prediction
		DefaultSingleImagePrediction prediction = new DefaultSingleImagePrediction(ij.context());
		prediction.setTrainedModel(modelArchive);
		prediction.setInput(imgFloat, "XY");
		prediction.run();
		RandomAccessibleInterval output = prediction.getOutput();
		assertNotNull(output);

		ij.context().dispose();
	}

}
