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

import io.scif.services.DatasetIOService;
import net.imagej.modelzoo.DefaultTensorSample;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.TensorSample;
import net.imagej.modelzoo.consumer.PredictionOutput;
import net.imagej.modelzoo.specification.NodeSpecification;
import net.imagej.modelzoo.specification.OutputNodeSpecification;
import net.imglib2.RandomAccessibleInterval;
import org.scijava.command.Command;
import org.scijava.io.location.FileLocation;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Plugin(type = Command.class, name = "Update modelzoo archive demo image")
public class ModelArchiveUpdateDemoFromFileCommand implements Command {

	@Parameter
	private File inputFile;

	@Parameter
	private ModelZooArchive archive;

	@Parameter
	private ModelZooService modelZooService;

	@Parameter
	private DatasetIOService ioService;

	private static String defaultSampleOuput = "sample_out.tif";

	@Override
	public void run() {
		try {
			RandomAccessibleInterval input = ioService.open(new FileLocation(inputFile));
			PredictionOutput outputs = modelZooService.predict(archive, input, "XY");
			List<TensorSample> inputSamples = new ArrayList<>();
			inputSamples.add(new DefaultTensorSample(input, inputFile.getName()));
			List<OutputNodeSpecification> outputNodeSpecifications = archive.getSpecification().getOutputs();
			List<TensorSample> outputSamples = getTensorSamples(archive, outputs.asMap(), outputNodeSpecifications);
			archive.setSampleOutputs(outputSamples);
			archive.setSampleInputs(inputSamples);
			archive.getSpecification().updateToNewestVersion();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static List<TensorSample> getTensorSamples(ModelZooArchive archive, Map outputs, List<OutputNodeSpecification> outputNodeSpecifications) {
		List<TensorSample> outputSamples = new ArrayList<>();
		for (int i = 0; i < outputNodeSpecifications.size(); i++) {
			NodeSpecification output = outputNodeSpecifications.get(i);
			Object data = outputs.get(output.getName());
			String name = defaultSampleOuput + "_" + i;
			if(archive.getSpecification().getSampleOutputs() != null
				&& archive.getSpecification().getSampleOutputs().size() > i) {
				name = archive.getSpecification().getSampleOutputs().get(i);
			}
			TensorSample sample = new DefaultTensorSample(data, name);
			outputSamples.add(sample);
		}
		return outputSamples;
	}

}
