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

package net.imagej.modelzoo.consumer.commands;

import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.network.DefaultModelExecutor;
import net.imagej.modelzoo.consumer.network.ModelExecutor;
import net.imagej.modelzoo.consumer.network.model.Model;
import net.imagej.modelzoo.consumer.task.Task;
import net.imagej.modelzoo.consumer.tiling.AdvancedTiledView;
import net.imagej.modelzoo.consumer.tiling.DefaultInputTiler;
import net.imagej.modelzoo.consumer.tiling.DefaultOutputTiler;
import net.imagej.modelzoo.consumer.tiling.InputTiler;
import net.imagej.modelzoo.consumer.tiling.OutputTiler;
import net.imagej.modelzoo.consumer.tiling.Tiling;
import net.imglib2.RandomAccessibleInterval;
import org.scijava.Cancelable;
import org.scijava.Disposable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PredictionExecutor implements Runnable, Disposable, Cancelable {

	private Model model;

	private int nTiles = 8;
	private int batchSize = 1;
	private Tiling tiling;

	private final ModelExecutor modelExecutor = new DefaultModelExecutor();
//	private final InputTiler inputTiler = new DefaultInputTiler();
//	private final OutputTiler outputTiler = new DefaultOutputTiler();
	private ExecutorService pool = null;
	private Future<?> future;
	private int oldNTiles;

	private int oldBatchesSize;
	private boolean canceled = false;

	@Override
	public void run() {

		pool = Executors.newSingleThreadExecutor();

		try {

			mainThread();
//			future = pool.submit(this::mainThread);
//			future.get();

		} catch(OutOfMemoryError e) {
			dispose();
			e.printStackTrace();
		}

	}

	private void mainThread() throws OutOfMemoryError {

		try {
			tryToRunModel();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

//		initTiling();
//		List<AdvancedTiledView<FloatType>> tiledOutput = null;
//		try {
//			tiledOutput = tryToTileAndRunNetwork(input);
//		} catch (ExecutionException e) {
//			e.printStackTrace();
//		}
//		if(tiledOutput != null) {
//			if(model.getOutputNode().getTilingAllowed()) {
//				output = outputTiler.run(
//						tiledOutput, tiling, model.getOutputNode().getFinalAxesArray());
//			} else {
//				output = tiledOutput.stream().map(this::getSingleTile).collect(Collectors.toList());
//			}
//			for (AdvancedTiledView obj : tiledOutput) {
//				obj.dispose();
//			}
//		}

	}

	private void initTiling() {
//		tiling = new DefaultTiling(nTiles);
	}

	private void tryToRunModel()
			throws OutOfMemoryError, ExecutionException {

		boolean isOutOfMemory = true;
		boolean canHandleOutOfMemory = true;

		while (isOutOfMemory && canHandleOutOfMemory) {
			try {
				runModel();
				isOutOfMemory = false;
			}
			catch (final OutOfMemoryError e) {
				canHandleOutOfMemory = tryHandleOutOfMemoryError();
			}
		}

		if (isOutOfMemory) throw new OutOfMemoryError(
				"Out of memory exception occurred. Plugin exit.");
	}

	private void runModel() throws ExecutionException {
		modelExecutor.run(model);
	}

//	private List tileAndRunNetwork(List<RandomAccessibleInterval> input) throws ExecutionException {
//		AxisType[] finalInputAxes = model.getInputNode().getFinalAxesArray();
//		Tiling.TilingAction[] tilingActions = model.getInputNode().getTilingActions();
//		final List<AdvancedTiledView> tiledInput;
//		if(model.getInputNode().getTilingAllowed()) {
//			tiledInput = inputTiler.run(
//					input, finalInputAxes, tiling, tilingActions);
//			nTiles = tiling.getTilesNum();
//		} else {
//			tiledInput = input.stream().map(image -> getSingleTileView(image, finalInputAxes)).collect(Collectors.toList());
//		}
//		if(tiledInput == null) return null;
//		return modelExecutor.run(tiledInput, model);
//	}
//
//	private AdvancedTiledView getSingleTileView(RandomAccessibleInterval image, AxisType[] finalInputAxes) {
//		long[] blockSize = new long[image.numDimensions()];
//		long[] overlap = new long[image.numDimensions()];
//		for (int i = 0; i < blockSize.length; i++) {
//			blockSize[i] = image.dimension(i);
//			overlap[i] = 0;
//		}
//		return new AdvancedTiledView(image, blockSize, overlap, finalInputAxes);
//	}

	private boolean tryHandleOutOfMemoryError() {
		// We expect it to be an out of memory exception and
		// try it again with more tiles or smaller batches.
		final Task modelExecutorTask = modelExecutor;
		nTiles = tiling.getTilesNum();
		if(oldNTiles == nTiles && oldBatchesSize == batchSize) {
			modelExecutorTask.setFailed();
			return false;
		}
		oldNTiles = nTiles;
		oldBatchesSize = batchSize;

		handleOutOfMemoryError();
		initTiling();
		nTiles = tiling.getTilesNum();
		modelExecutorTask.logWarning(
				"Out of memory exception occurred. Trying with " + nTiles +
						" tiles, batch size " + batchSize + "...");

		modelExecutorTask.startNewIteration();
//		inputTiler.addIteration();
		return true;
	}

	private void handleOutOfMemoryError() {
		batchSize /= 2;
		if (batchSize < 1) {
			batchSize = 1;
			nTiles *= 2;
		}
	}

	public void setModel(Model model) {
		this.model = model;
	}

	@Override
	public void dispose() {
		if (model != null) {
			model.dispose();
		}
		if(pool != null) {
			pool.shutdown();
		}
		pool = null;
	}

	@Override
	public String getCancelReason() {
		return null;
	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	@Override
	public void cancel(final String reason) {
		canceled = true;
		if(future != null) {
			future.cancel(true);
		}
		if(pool != null) {
			pool.shutdownNow();
		}
		dispose();
	}
}
