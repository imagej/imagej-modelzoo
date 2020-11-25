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

import net.imagej.ImageJ;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.consumer.commands.CAREDefaultModelConverterCommand;
import net.imagej.modelzoo.consumer.commands.DefaultSingleImagePredictionCommand;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import org.junit.Test;
import org.scijava.command.CommandModule;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertNotNull;

public class CAREConverterCommandTest {

	@Test
	public void testCommand() throws ExecutionException, InterruptedException {
		ImageJ ij = new ImageJ();

		File oldModel = new File("/home/random/Documents/2020-11 I2K/CARE/old/model-planaria.zip");
		String name = "CARE Demo planaria";
		File destinationFolder = new File("/home/random/Documents/2020-11 I2K/CARE/old");
		String destinationFileName = "care-demo-planaria";

		CommandModule module = ij.command().run(CAREDefaultModelConverterCommand.class,
				false,
				"input", oldModel,
				"name", name,
				"destinationFolder", destinationFolder,
				"destinationFileName", destinationFileName).get();

		ModelZooArchive output = (ModelZooArchive) module.getOutput("output");
		assertNotNull(output);
	}
}
