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

import net.imagej.ImageJService;
import net.imagej.modelzoo.consumer.ModelZooPrediction;
import net.imagej.modelzoo.consumer.ModelZooPredictionOptions;
import net.imagej.modelzoo.consumer.model.prediction.PredictionInput;
import net.imagej.modelzoo.consumer.model.prediction.PredictionOutput;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.module.ModuleException;

import java.io.FileNotFoundException;
import java.util.List;

public interface ModelZooService extends ImageJService {

	ModelZooIOService io();

	boolean canRunPredictionInteractive(ModelZooArchive trainedModel);
	boolean canRunSanityCheckInteractive(ModelZooArchive trainedModel);

	ModelZooPredictionOptions createOptions();

	default PredictionOutput predict(ModelZooArchive trainedModel, List<PredictionInput> inputs) throws Exception {
		return predict(trainedModel, inputs, createOptions());
	}
	PredictionOutput predict(ModelZooArchive trainedModel, List<PredictionInput> inputs, ModelZooPredictionOptions options) throws Exception;
	default <T extends RealType<T> & NativeType<T>> PredictionOutput predict(ModelZooArchive trainedModel, RandomAccessibleInterval<T> input, String axes) throws Exception {
		return predict(trainedModel, input, axes, createOptions());
	}
	<T extends RealType<T> & NativeType<T>> PredictionOutput predict(ModelZooArchive trainedModel, RandomAccessibleInterval<T> input, String axes, ModelZooPredictionOptions options) throws Exception;

	void predictInteractive(ModelZooArchive trainedModel) throws FileNotFoundException, ModuleException;
	void batchPredictInteractive(ModelZooArchive trainedModel) throws FileNotFoundException, ModuleException;

	void sanityCheckFromFilesInteractive(ModelZooArchive model) throws ModuleException;
	void sanityCheckFromImagesInteractive(ModelZooArchive model) throws ModuleException;
	void sanityCheckInteractive(ModelZooArchive model, RandomAccessibleInterval input, RandomAccessibleInterval groundTruth) throws ModuleException;

	default ModelZooPrediction getPrediction(ModelZooArchive model) {
		return getPrediction(model, createOptions());
	}

	ModelZooPrediction getPrediction(ModelZooArchive model, ModelZooPredictionOptions options);
}
