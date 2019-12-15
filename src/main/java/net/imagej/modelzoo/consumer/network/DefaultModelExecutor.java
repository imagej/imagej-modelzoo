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

package net.imagej.modelzoo.consumer.network;

import net.imagej.modelzoo.consumer.network.model.Model;
import net.imagej.modelzoo.consumer.task.DefaultTask;
import net.imagej.modelzoo.consumer.tiling.AdvancedTiledView;
import net.imglib2.type.numeric.RealType;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class DefaultModelExecutor<T extends RealType<T>> extends DefaultTask
	implements ModelExecutor<T>
{

	private ExecutorService pool = null;
	private Model model = null;
	private boolean canceled = false;
//
//	@Override
//	public List<AdvancedTiledView<T>> run(final List<AdvancedTiledView<T>> input,
//		final Model network) throws OutOfMemoryError, ExecutionException {
//		if(!isCanceled()) {
//			setStarted();
//			this.network = network;
//			if (input.size() > 0) {
//				DatasetHelper.logDim(this, "Network input size", input.get(0)
//						.randomAccess().get());
//			}
//
//			setCurrentStep(0);
//			network.resetTileCount();
//			setNumSteps(getSteps(input));
//
//			pool = Executors.newWorkStealingPool();
//			final List<AdvancedTiledView<T>> output = new ArrayList<>();
//			for (AdvancedTiledView<T> tile : input) {
//				output.add(run(tile, network));
//				if(isCanceled()) return null;
//			}
//			pool.shutdown();
//			if(isCanceled()) return null;
//			if (output.size() > 0) {
//				DatasetHelper.logDim(this, "Network output size", output.get(0)
//						.getProcessedTiles().get(0));
//			}
//			setFinished();
//			return output;
//		}
//		return null;
//	}

	@Override
	public void run(Model model) throws ExecutionException {
		if(!isCanceled()) {
			setStarted();
			this.model = model;
			setCurrentStep(0);
			model.resetTileCount();
			//FIXME
			setNumSteps(1);
			pool = Executors.newWorkStealingPool();
			runTile(model);
			pool.shutdown();
			setFinished();
		}
	}

	private int getSteps(List<AdvancedTiledView<T>> input) {
		int numSteps = 0;
		for (AdvancedTiledView<T> tile : input) {
			int steps = 1;
			for (int i = 0; i < tile.numDimensions(); i++) {
				steps *= tile.dimension(i);
			}
			numSteps += steps;
		}
		return numSteps;
	}

	private void runTile(final Model model) throws OutOfMemoryError, IllegalArgumentException, ExecutionException {

		try {
//			CountDownLatch latch = new CountDownLatch(1);
//			pool.execute(model);
			model.run();
//			latch.await();
		}
		catch(final CancellationException | RejectedExecutionException e) {
			//canceled
			setFailed();
			String PROGRESS_CANCELED = "Canceled";
			log(PROGRESS_CANCELED);
			cancel(PROGRESS_CANCELED);
		}
		catch(final IllegalArgumentException e) {
			setFailed();
			throw e;
		}
		catch (final IllegalStateException exc) {
			if(exc.getMessage() != null && exc.getMessage().contains("OOM")) {
				setIdle();
				throw new OutOfMemoryError();
			}
			exc.printStackTrace();
			setFailed();
			throw exc;
		}
	}

//	private AdvancedTiledView<T> run(final AdvancedTiledView<T> input,
//		final Model network) throws OutOfMemoryError, IllegalArgumentException, ExecutionException {
//
//		input.getProcessedTiles().clear();
//
//		try {
//			network.setTiledView(input);
//			Future<List<RandomAccessibleInterval<T>>> resultFuture = pool.submit(network);
//			List<RandomAccessibleInterval<T>> result = resultFuture.get();
//			if(result != null) {
//				input.getProcessedTiles().addAll(result);
//			}
//
//		}
//		catch(final CancellationException | RejectedExecutionException | InterruptedException e) {
//			//canceled
//			setFailed();
//			String PROGRESS_CANCELED = "Canceled";
//			log(PROGRESS_CANCELED);
//			cancel(PROGRESS_CANCELED);
//			return null;
//		}
//		catch(final IllegalArgumentException e) {
//			setFailed();
//			throw e;
//		}
//		catch (final ExecutionException | IllegalStateException exc) {
//			if(exc.getMessage() != null && exc.getMessage().contains("OOM")) {
//				setIdle();
//				throw new OutOfMemoryError();
//			}
//			exc.printStackTrace();
//			setFailed();
//			throw exc;
//		}
//
//		return input;
//	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	@Override
	public void cancel(final String reason) {
		canceled = true;
		if (pool != null && !pool.isShutdown()) {
			pool.shutdownNow();
		}
		if(model != null) {
			model.cancel(reason);
		}
	}

	@Override
	public String getCancelReason() {
		return null;
	}

}
