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

import net.imagej.modelzoo.consumer.util.IOHelper;
import net.imglib2.TiledView;
import net.imglib2.type.numeric.RealType;
import org.scijava.io.location.Location;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class DefaultModel<T extends RealType<T>> implements Model {

	@Parameter
	protected LogService log;

	protected final List<InputNode> inputNodes = new ArrayList<>();
	protected final List<OutputNode> outputNodes = new ArrayList<>();
	protected TiledView<T> tiledView;
	protected Integer doneTileCount;
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
	public void run()
		throws IllegalArgumentException, OutOfMemoryError
	{

		pool = Executors.newSingleThreadExecutor();

//		final Cursor<RandomAccessibleInterval<T>> cursor = Views.iterable(tiledView)
//			.cursor();


//		while (cursor.hasNext()) {
//			final RandomAccessibleInterval<T> tile = cursor.next();

//		CountDownLatch latch = new CountDownLatch(1);
//		pool.execute(this::execute);
		execute();

		log.info("Processing tile " + (doneTileCount + 1) + "..");

		try {
//			latch.await();
			upTileCount();
		}
		catch (final IllegalArgumentException exc) {
			pool.shutdownNow();
			//FIXME fail
//					fail();
			throw  exc;
		}
	}

	@Override
	public List<InputNode> getInputNodes() {
		return inputNodes;
	}

	@Override
	public List<OutputNode> getOutputNodes() {
		return outputNodes;
	}
//
//	@Override
//	public void loadInputNode(final Dataset dataset) {
//		inputNode = new ImageTensor();
//		inputNode.initialize(dataset);
//	}
//
//	@Override
//	public void loadOutputNode(Dataset dataset) {
//		outputNode = new ImageTensor();
//		outputNode.initialize(dataset);
//	}

	// TODO this is the tensorflow runner
	public abstract void execute() throws IllegalArgumentException, OutOfMemoryError;

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

	@Override
	public void dispose() {
		if (pool != null) {
			pool.shutdown();
		}
		pool = null;
	}

	public void clear() {
		inputNodes.clear();
		outputNodes.clear();
	}
}
