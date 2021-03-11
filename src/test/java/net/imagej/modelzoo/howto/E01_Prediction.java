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
import net.imagej.modelzoo.consumer.DefaultModelZooPrediction;
import net.imagej.modelzoo.consumer.ModelZooPredictionOptions;
import net.imagej.modelzoo.consumer.model.prediction.PredictionOutput;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.view.Views;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

public class E01_Prediction {

	private ImageJ ij;

	@After
	public void tearDown() {
		ij.context().dispose();
	}

	@Test
	public void useService() throws Exception {

		ij = new ImageJ();
		ij.launch();

		// resource paths
		String imgPath = getClass().getResource("/blobs.png").getPath();
		String modelPath = getClass().getResource("/net/imagej/modelzoo/consumer/denoise2D/dummy.model.bioimage.io.zip").getPath();

		// load image
		// TODO scifio DatasetIOPlugin fixed in future versions (fixed in 0.41.2)
		// Img input = ((Img) ij.io().open(imgPath);
		Img input = (Img) ij.scifio().datasetIO().open(imgPath);

		ij.ui().show(input);

		ModelZooService modelZooService = ij.get(ModelZooService.class);

		ModelZooArchive model = modelZooService.io().open(modelPath);
		modelZooService.predictInteractive(model);

//		ModelZooPredictionOptions options = ModelZooPredictionOptions.options();
//		options.numberOfTiles(1);
//		PredictionOutput outputs = modelZooService.predict(model, input, "XY", options);
//
//		if(outputs == null) return; // in case it got canceled
//		outputs.asMap().forEach((name, output) -> {
//			ij.ui().show(name, output);
//		});

	}

	@Test
	public void usePredictionAPI() throws Exception {

		ij = new ImageJ();

		// resource paths
		String imgPath = getClass().getResource("/blobs.png").getPath();
		String modelPath = getClass().getResource("/net/imagej/modelzoo/consumer/denoise2D/dummy.model.bioimage.io.zip").getPath();

		// load image
		// TODO scifio DatasetIOPlugin fixed in future versions (fixed in 0.41.2)
		// RandomAccessibleInterval input = ((Img) ij.io().open(imgPath);
		RandomAccessibleInterval input = (Img) ij.scifio().datasetIO().open(imgPath);

		// create prediction
		DefaultModelZooPrediction prediction = new DefaultModelZooPrediction(ij.context());

		// setup prediction
		prediction.setInput("input", input, "XY");
		prediction.setTrainedModel(modelPath);
		prediction.setOptions(ModelZooPredictionOptions.options().numberOfTiles(8));
		prediction.run();
		Map<String, Object> outputs = prediction.getOutputs();

		if(outputs == null) return; // in case it got canceled
		outputs.forEach((name, output) -> ij.ui().show(name, output));

	}

	public static void main(String... args) throws Exception {
		new E01_Prediction().useService();
//		new E01_Prediction().usePredictionAPI();
	}
}
