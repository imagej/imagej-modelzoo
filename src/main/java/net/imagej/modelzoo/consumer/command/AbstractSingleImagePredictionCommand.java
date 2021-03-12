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

package net.imagej.modelzoo.consumer.command;

import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.consumer.ModelZooPredictionOptions;
import net.imagej.modelzoo.consumer.SingleImagePrediction;
import net.imagej.modelzoo.consumer.model.prediction.ImageInput;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CancellationException;

public abstract class AbstractSingleImagePredictionCommand<T extends RealType<T> & NativeType<T>, P extends SingleImagePrediction<?>> implements SingleImagePredictionCommand {

	@Parameter(label = "Trained model file (.zip)")
	private File modelFile;

	@Parameter(persist = false)
	private RandomAccessibleInterval<T> input;

	@Parameter(label = "Axes of prediction input (subset of XYZCB, B = batch)", description = "<html>You can predict one dimension independently per position.<br>Use B ( = batch) for this dimension.")
	private String axes = "XY";

	@Parameter(label = "Batch size", required = false, description = "<html>The batch size will only be used if a batch axis exists.<br>It can improve performance to process multiple batches at once (batch size > 1)")
	private int batchSize = 10;

	@Parameter(label = "Number of tiles (1 = no tiling)", required = false, description = "<html>Increasing the tiling can help if the memory is insufficient to deal with the whole image at once.<br>Too many tiles decrease performance because an overlap has to be computed.")
	private int numTiles = 1;

	@Parameter(label = "Display progress window during prediction", required = false)
	private boolean showProgressDialog = true;

	@Parameter(label = "Convert output into input image format", required = false)
	private boolean convertOutputToInputFormat = false;

	@Parameter
	private LogService log;

	@Parameter
	private ModelZooService modelZooService;

	@Parameter
	private Context context;

	@Parameter
	private UIService uiService;

	private ModelZooArchive archive;
	private P prediction;

	@Override
	public void run() {

		final long startTime = System.currentTimeMillis();

		try {
			P prediction = createPrediction();
			if(prediction == null) return;
			prediction.setTrainedModel(getArchive());
			setInput(prediction);
			prediction.setOptions(createOptions());
			if(showProgressDialog && !uiService.isHeadless()) {
				uiService.show(prediction);
			}
			prediction.run();
			createOutput(prediction);

		} catch (CancellationException e) {
			log.warn("ModelZoo prediction canceled.");
		} catch (OutOfMemoryError | Exception e) {
			e.printStackTrace();
		}
		log.info("ModelZoo prediction exit (took " + (System.currentTimeMillis() - startTime) + " milliseconds)");

	}

	protected ModelZooArchive getArchive() throws IOException {
		if(archive == null) {
			archive = modelZooService().io().open(getModelFile());
		}
		return archive;
	}

	protected abstract P createPrediction();

	protected abstract void createOutput(P prediction);

	private ModelZooPredictionOptions createOptions() {
		return ModelZooPredictionOptions.options()
				.numberOfTiles(numTiles)
				.batchSize(batchSize)
				.showProgressDialog(showProgressDialog)
				.convertIntoInputFormat(convertOutputToInputFormat);
	}

	protected void setInput(P prediction) {
		prediction.setInput(new ImageInput<>("input", input, axes));
	}

	public P getPrediction() {
		if(prediction == null) {
			prediction = createPrediction();
		}
		return prediction;
	};

	protected Context getContext() {
		return context;
	}

	protected String getAxes() {
		return axes;
	}

	protected File getModelFile() {
		return modelFile;
	}

	protected ModelZooService modelZooService() {
		return modelZooService;
	}

	protected LogService log() {
		return log;
	}
}
