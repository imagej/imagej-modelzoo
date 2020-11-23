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
package net.imagej.modelzoo.consumer.commands;

import net.imagej.Dataset;
import net.imagej.modelzoo.DefaultTensorSample;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.TensorSample;
import net.imagej.modelzoo.consumer.PredictionOutput;
import net.imagej.modelzoo.specification.DefaultModelSpecification;
import net.imagej.modelzoo.specification.OutputNodeSpecification;
import net.imglib2.img.Img;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.imagej.modelzoo.consumer.commands.ModelArchiveUpdateDemoFromFileCommand.getTensorSamples;

@Plugin(type = Command.class, name = "Update modelzoo archive demo image")
public class ModelArchiveUpdateDemoFromImageCommand implements Command {

	@Parameter(persist = false)
	private Dataset inputImage;

	@Parameter
	private ModelZooArchive archive;

	@Parameter
	private ModelZooService modelZooService;

	private final static String defaultSampleInput = "sample_in.tif";

	@Override
	public void run() {
//		if(inputImage.numDimensions() > 2) {
//			logService.warn("Can't use test image with more than 2 dimensions.");
//			return;
//		}
		try {
			PredictionOutput outputs = modelZooService.predict(archive, (Img) inputImage, "XY");
			List<TensorSample> inputSamples = new ArrayList<>();
			inputSamples.add(new DefaultTensorSample(inputImage, getSampleInput(archive)));
			List<OutputNodeSpecification> outputNodeSpecifications = archive.getSpecification().getOutputs();
			List<TensorSample> outputSamples = getTensorSamples(archive, outputs.asMap(), outputNodeSpecifications);
			archive.setSampleOutputs(outputSamples);
			archive.setSampleInputs(inputSamples);
			archive.getSpecification().updateToNewestVersion();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String getSampleInput(ModelZooArchive archive) {
		List<String> sampleInputs = archive.getSpecification().getSampleInputs();
		if(sampleInputs != null && sampleInputs.size() > 0) {
			return sampleInputs.get(0);
		}
		return defaultSampleInput;
	}

}
