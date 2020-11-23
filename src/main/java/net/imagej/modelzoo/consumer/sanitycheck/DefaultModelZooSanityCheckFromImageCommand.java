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

package net.imagej.modelzoo.consumer.sanitycheck;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.TensorSample;
import net.imagej.modelzoo.display.InfoWidget;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.module.Module;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.imagej.modelzoo.consumer.sanitycheck.DefaultModelZooSanityCheckFromFileCommand.descriptionText;

@Plugin(type = Command.class)
public class DefaultModelZooSanityCheckFromImageCommand extends DynamicCommand {

	@Parameter(label = "Import model (.zip) from file")
	private File modelFile;

	@Parameter
	private Module prediction;

	@Parameter(label="<html><h1>Sanity check</h1>", description = descriptionText, required = false, style = InfoWidget.STYLE)
	private String description = "";

	@Parameter(label = "Prediction input image", persist = false)
	private Dataset input;

	@Parameter(label = "Expected result image", persist = false)
	private Dataset inputGroundTruth;

	@Parameter(label = "Model prediction", type = ItemIO.OUTPUT)
	private Dataset output;

	@Parameter(label = "Difference input - prediction", type = ItemIO.OUTPUT)
	private Dataset difference;

	@Parameter
	private LogService log;

	@Parameter
	private StatusService status;

	@Parameter
	private UIService uiService;

	@Parameter
	private OpService opService;

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private ModelZooService modelZooService;

	public void run() {

		final long startTime = System.currentTimeMillis();
		log("ModelZoo sanity check start");

		try {
			prediction.setInput("input", input);
			prediction.resolveInput("input");
			context().service(ModuleService.class).run(prediction, true).get();
			output = (Dataset) prediction.getOutput("output");

			difference = datasetService.create(getDifference((RandomAccessibleInterval)input, (RandomAccessibleInterval)output, new FloatType()));
			ModelZooArchive model = modelZooService.open(modelFile);
			ImageToImageSanityCheck.compare(
					input,
					output,
					inputGroundTruth,
					model,
					opService);
		} catch (ExecutionException | InterruptedException | IOException e) {
			e.printStackTrace();
		}

		log("ModelZoo sanity check exit (took " + (System.currentTimeMillis() - startTime) + " milliseconds)");

	}

	private <TI extends RealType<TI>, TO extends RealType<TO>, TR extends RealType<TR> & NativeType<TR>> RandomAccessibleInterval<TR> getDifference(RandomAccessibleInterval<TI> input, RandomAccessibleInterval<TO> output, TR resultType) {
		Img<TR> res = opService.create().img(input, resultType);
		LoopBuilder.setImages(input, output, res).multiThreaded().forEachPixel((ti, to, tr) -> {
			tr.setReal(ti.getRealDouble()-to.getRealDouble());
		});
		return res;
	}

	private RandomAccessibleInterval getFirstSample(List<TensorSample> sampleInputs) {
		if(sampleInputs == null || sampleInputs.size() == 0) return null;
		return (RandomAccessibleInterval) sampleInputs.get(0);
	}

	private void log(String msg) {
		log.info(msg);
		status.showStatus(msg);
	}
}
