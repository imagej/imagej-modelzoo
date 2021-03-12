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

import io.bioimage.specification.ModelSpecification;
import net.imagej.modelzoo.DefaultModelZooArchive;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.consumer.model.tensorflow.TensorFlowModel;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.io.location.FileLocation;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import java.io.File;
import java.io.IOException;

import static org.scijava.widget.FileWidget.DIRECTORY_STYLE;

class CAREDefaultModelConverterCommand implements Command {

	@Parameter(label = "Old CARE TF export")
	private File input;

	@Parameter(label = "Model name")
	private String name;

	@Parameter(label = "Destination directory", style = DIRECTORY_STYLE)
	private File destinationFolder;

	@Parameter(label = "Destination file name (without .zip ending)")
	private String destinationFileName;

	@Parameter
	private int networkDepth;

	@Parameter
	private int kernelSize;

	@Parameter(type = ItemIO.OUTPUT)
	private ModelZooArchive output;

	@Parameter
	private ModelZooService modelZooService;

	@Parameter
	private LogService logService;

	@Parameter
	private Context context;

	@Override
	public void run() {
		try {
			File destination = new File(destinationFolder, destinationFileName + ".model.bioimage.io.zip");
			if (destination.equals(input)) {
				logService.error("Destination file cannot be the same as the input file");
				return;
			}
			ModelSpecification specification = new TensorFlowModel(context).guessSpecification(new FileLocation(input), name, networkDepth, kernelSize);
			DefaultModelZooArchive archive = new DefaultModelZooArchive();
			archive.setSpecification(specification);
			modelZooService.io().save(archive, new FileLocation(destination));
			archive.add(input, "tensorflow_saved_model_bundle.zip");
			output = archive;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
