/*-
 * #%L
 * ImageJ ModelZoo Consumer
 * %%
 * Copyright (C) 2019 MPI-CBG
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

import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import org.scijava.Context;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class SingleOutputPrediction extends ModelZooPrediction {

	private RandomAccessibleInterval output;

	public SingleOutputPrediction(Context context) {
		super(context);
	}

	public Map<String, Object> run() {
		Map<String, Object> res = super.run();
		if(res == null) return null;
		output = (RandomAccessibleInterval) res.values().iterator().next();
		return res;
	}

	public RandomAccessibleInterval getOutput() {
		return output;
	}

	public void setOutput(RandomAccessibleInterval output) {
		this.output = output;
	}

	public static void main(String...args) throws IOException, URISyntaxException {
		ImageJ ij = new ImageJ();
		ij.launch();

		Path img = Paths.get(SingleOutputPrediction.class.getClassLoader()
				.getResource("denoise2D/input.tif").toURI());

		Img input = (Img) ij.io().open(img.toAbsolutePath().toString());

		ij.ui().show(input);

		File model = new File(SingleOutputPrediction.class.getClassLoader()
				.getResource("denoise2D/model.zip").toURI());

		SingleOutputPrediction prediction = new SingleOutputPrediction(ij.context());
		prediction.setInput("input", input);
		prediction.setModelFile(model);
		prediction.run();
		RandomAccessibleInterval output = prediction.getOutput();

		ij.ui().show(output);
	}
}
