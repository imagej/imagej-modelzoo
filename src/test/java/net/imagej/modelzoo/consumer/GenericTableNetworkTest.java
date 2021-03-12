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

package net.imagej.modelzoo.consumer;

import net.imagej.modelzoo.AbstractModelZooTest;
import net.imagej.modelzoo.consumer.command.DefaultSingleImagePredictionCommand;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Ignore;
import org.junit.Test;
import org.scijava.module.Module;
import org.scijava.table.GenericTable;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import static junit.framework.TestCase.assertNotNull;

public class GenericTableNetworkTest extends AbstractModelZooTest {

	@Test
	@Ignore //FIXME provide a way to use the table output processor
	public void testGenericNetwork() throws ExecutionException, InterruptedException {
		createImageJ();

		URL networkUrl = this.getClass().getResource("denoise2D/dummy.model.bioimage.io.zip");

		final RandomAccessibleInterval input = new ArrayImgFactory<>(new FloatType()).create(3, 3, 3);

		final Module module = ij.command().run(DefaultSingleImagePredictionCommand.class, false,
				"input", input,
				"mapping", "XYZ",
				"modelFile", new File(networkUrl.getPath())).get();
		GenericTable output = (GenericTable) module.getOutput("output");
		assertNotNull(output);
		System.out.println(output);

	}

}
