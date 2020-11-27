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
package net.imagej.modelzoo.consumer.command;

import net.imagej.Dataset;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.consumer.model.prediction.PredictionOutput;
import net.imglib2.img.Img;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import static net.imagej.modelzoo.consumer.command.ModelArchiveUpdateDemoFromFileCommand.updateSampleImages;

@Plugin(type = Command.class, name = "Update modelzoo archive demo image")
public class ModelArchiveUpdateDemoFromImageCommand implements Command {

	@Parameter(persist = false)
	private Dataset inputImage;

	@Parameter(label = "Axes of input (subset of XYZ)")
	private String axes;

	@Parameter
	private ModelZooArchive archive;

	@Parameter
	private ModelZooService modelZooService;

	@Override
	public void run() {
//		if(inputImage.numDimensions() > 2) {
//			logService.warn("Can't use test image with more than 2 dimensions.");
//			return;
//		}
		try {
			PredictionOutput outputs = modelZooService.predict(archive, (Img) inputImage, axes);
			updateSampleImages(archive, inputImage, outputs);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
