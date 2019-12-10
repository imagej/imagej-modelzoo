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

package net.imagej.modelzoo.consumer.network.model;

import net.imagej.Dataset;
import net.imagej.modelzoo.consumer.util.IOHelper;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TiledView;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.app.StatusService;
import org.scijava.io.location.Location;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class DefaultModel<T extends RealType<T>> implements
		Model<T>
{

	@Parameter
	protected
	LogService log;

	@Parameter
	StatusService statusService;

	protected ImageTensor inputNode = null;
	protected ImageTensor outputNode = null;
	protected TiledView<T> tiledView;
	protected Integer doneTileCount;
	protected boolean dropSingletonDims = false;
	protected NetworkSettings networkSettings;
	ExecutorService pool;

	public DefaultModel() {
	}

	protected abstract boolean loadModel(Location source, String modelName);

	@Override
	public boolean loadModel(final String pathOrURL, final String modelName)
		throws FileNotFoundException
	{

		final Location source = IOHelper.loadFileOrURL(pathOrURL);
		return loadModel(source, modelName);

	}

	@Override
	public abstract void preprocess();

	@Override
	public List<RandomAccessibleInterval<T>> call()
		throws IllegalArgumentException, ExecutionException, OutOfMemoryError
	{

		pool = Executors.newSingleThreadExecutor();

		final boolean multithreading = false;

		final Cursor<RandomAccessibleInterval<T>> cursor = Views.iterable(tiledView)
			.cursor();

		// Loop over the tiles and execute the prediction
		final List<RandomAccessibleInterval<T>> results = new ArrayList<>();
		final List<Future<RandomAccessibleInterval<T>>> futures = new ArrayList<>();

		while (cursor.hasNext()) {
			final RandomAccessibleInterval<T> tile = cursor.next();

			final Future<RandomAccessibleInterval<T>> future = pool.submit(() -> execute(tile));

			log.info("Processing tile " + (doneTileCount + 1) + "..");

			futures.add(future);

			if (!multithreading) {
				try {
					final RandomAccessibleInterval<T> res = future.get();
					if (res == null) return null;
					results.add(res);
					upTileCount();
				}
				catch (final IllegalArgumentException exc) {
					pool.shutdownNow();
					//FIXME fail
//					fail();
					throw  exc;
				}
				catch (final InterruptedException exc) {
					pool.shutdownNow();
					return null;
				}
			}
		}
		if (multithreading) {
			for (final Future<RandomAccessibleInterval<T>> future : futures) {
				try {
					final RandomAccessibleInterval<T> res = future.get();
					if (res == null) return null;
					results.add(res);
					upTileCount();
				}
				catch (final InterruptedException exc) {
					pool.shutdownNow();
					//FIXME fail
//					fail();
					return null;
				}
			}
		}

		return results;
	}

	protected abstract RandomAccessibleInterval<T> execute(
			RandomAccessibleInterval<T> tile) throws Exception;

	@Override
	public ImageTensor getInputNode() {
		return inputNode;
	}

	@Override
	public ImageTensor getOutputNode() {
		return outputNode;
	}

	@Override
	public void loadInputNode(final Dataset dataset) {
		inputNode = new ImageTensor();
		inputNode.initialize(dataset);
	}

	@Override
	public void loadOutputNode(Dataset dataset) {
		outputNode = new ImageTensor();
		outputNode.initialize(dataset);
	}

	@Override
	public abstract void initMapping();

	@Override
	public abstract List<Integer> dropSingletonDims();

	@Override
	public abstract boolean isInitialized();

	@Override
	public void resetTileCount() {
		doneTileCount = 0;
		//FIXME progress
//		status.setCurrentStep(doneTileCount);
	}

	protected void upTileCount() {
		doneTileCount++;
		//FIXME progress
//		status.setCurrentStep(doneTileCount);
	}

	@Override
	public void setTiledView(final TiledView<T> tiledView) {
		this.tiledView = tiledView;
	}

	@Override
	public void cancel(String reason) {
		pool.shutdownNow();
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public String getCancelReason() {
		return null;
	}

	//	@Override
//	public void setDoDimensionReduction(final boolean doDimensionReduction) {
//		setDoDimensionReduction(doDimensionReduction, Axes.Z);
//	}
//
//	@Override
//	public void setDoDimensionReduction(final boolean doDimensionReduction,
//		final AxisType axisToRemove)
//	{
//		this.doDimensionReduction = doDimensionReduction;
//		this.axisToRemove = axisToRemove;
//	}

	@Override
	public void dispose() {
		if (pool != null) {
			pool.shutdown();
		}
		pool = null;
	}

	public void clear() {
		inputNode = null;
		outputNode = null;
	}
}
