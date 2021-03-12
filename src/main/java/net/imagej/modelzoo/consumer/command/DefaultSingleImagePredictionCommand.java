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

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.modelzoo.consumer.DefaultModelZooPrediction;
import net.imagej.modelzoo.consumer.ModelZooPrediction;
import net.imagej.modelzoo.consumer.SingleImagePrediction;
import net.imagej.modelzoo.consumer.model.prediction.ImageInput;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.util.Map;

@Plugin(type = SingleImagePredictionCommand.class, name = "imagej-modelzoo", menuPath = "Plugins>bioimage.io>bioimage.io prediction")
public class DefaultSingleImagePredictionCommand<T extends RealType<T> & NativeType<T>, S extends SingleImagePrediction<?>> extends AbstractSingleImagePredictionCommand<T, S> {

	@Parameter(type = ItemIO.OUTPUT)
	private Dataset output;

	@Parameter(type = ItemIO.OUTPUT)
	private Dataset output1 = null;

	@Parameter
	private DatasetService datasetService;

	private ModelZooPrediction<ImageInput<?>, ?> prediction;

	Map<String, Object> outputs;

	@Override
	protected void createOutput(SingleImagePrediction prediction) {
		int i = 0;
		for (Map.Entry<String, Object> entry : prediction.getOutput().asMap().entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			if (i == 0) {
				output = datasetService.create((RandomAccessibleInterval) value);
				output.setName(name);
			}
			if (i == 1) {
				output1 = datasetService.create((RandomAccessibleInterval) value);
				output1.setName(name);
			}
			i++;
		}
	}

	@Override
	public S createPrediction() {
		try {
			return (S) modelZooService().getPrediction(getArchive());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
