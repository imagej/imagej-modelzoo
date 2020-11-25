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

import net.imagej.modelzoo.consumer.model.tensorflow.TensorFlowModel;
import net.imagej.modelzoo.consumer.model.tensorflow.TensorFlowModelSpecification;
import org.junit.Test;
import org.scijava.Context;

import java.io.IOException;

public class E04_GuessSpecification {

	@Test
	public void run() throws IOException {

		// create context
		Context context = new Context();

		// resource path
		String archivePath = getClass().getResource("/net/imagej/modelzoo/consumer/denoise2D/model.bioimage.io.zip").getPath();

		// create specification
		TensorFlowModelSpecification specification = new TensorFlowModel(context).guessSpecification(archivePath, "example model");

		// set the shape step of the input data, in this case, X and Y need to be multiple of 32
		// since this can't be guessed by the TensorFlow model but is important for models with variable input size, this needs to be set manually
		specification.getInputs().get(0).getShapeStep().set(1, 32);
		specification.getInputs().get(0).getShapeStep().set(2, 32);

		// access specification
		System.out.println(specification);

		// dispose context
		context.dispose();

	}

	public static void main(String... args) throws IOException {
		new E04_GuessSpecification().run();
	}
}
