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
import net.imagej.modelzoo.consumer.network.model.OutputImageNode;
import net.imagej.modelzoo.consumer.task.DefaultTask;
import net.imagej.modelzoo.consumer.tiling.DefaultTiling;
import net.imagej.modelzoo.consumer.tiling.Tiling;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RejectedExecutionException;

public class ModelExecutor<TI extends RealType<TI>, TO extends RealType<TO>> extends DefaultTask
{

	@Parameter
	LogService log;

	private final Model model;
	private Tiling<TO> tiling;
	private int nTiles;
	private int oldNTiles;
	private int oldBatchesSize;
	private boolean processedTiles = false;
	private boolean canceled = false;
	private List<RandomAccessibleInterval<TO>> results = new ArrayList<>();

	public ModelExecutor(Model model, Context context) {
		this.model = model;
		context.inject(this);
	}

	public void run() throws OutOfMemoryError, IllegalArgumentException {

		try {
			if(model.getInputNodes().size() > 1) {
				// no tiling possible for now
				model.predict();
			} else {
				initTiling();
				while(processNextTile()) {
					model.predict();
					setTileResult((OutputImageNode<TO, TI>) model.getOutputNodes().get(0));
				}
				concatenateTiles((OutputImageNode<TO, TI>) model.getOutputNodes().get(0));
			}
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

	public void setNumberOfTiles(int nTiles) {
		this.nTiles = nTiles;
	}

	public boolean increaseTiling() {

		// We expect it to be an out of memory exception and
		// try it again with more tiles or smaller batches.
		int nTiles = tiling.getTilesNum();
		int batchSize = tiling.getBatchSize();
		if(oldNTiles == nTiles && oldBatchesSize == batchSize) {
			return false;
		}
		oldNTiles = nTiles;
		oldBatchesSize = batchSize;

		batchSize /= 2;
		if (batchSize < 1) {
			batchSize = 1;
			nTiles *= 2;
		}

		tiling.setNumberOfTiles(nTiles);
		tiling.setBatchSize(batchSize);

		tiling.init();

		log.warn(
				"Out of memory exception occurred. Trying with " + nTiles +
						" tiles, batch size " + batchSize + "...");

		return true;
	}

	public boolean processNextTile() {
		// go to next tile
		if(processedTiles || tiling.getDoneTileCount() == tiling.getTilesNum()) {
			processedTiles = true;
			return false;
		}
		tiling.assignCurrentTile();
		tiling.upTileCount();
		log.info("Processing tile " + (tiling.getDoneTileCount()) + "..");
		return true;
	}

	public void initTiling() {
		//TODO reset tiling
		// start with first tile
		processedTiles = false;
		tiling = new DefaultTiling<TO, TI>((OutputImageNode<TO, TI>) model.getOutputNodes().get(0));
		tiling.setNumberOfTiles(nTiles);
		tiling.init();
		results.clear();
	}

	public void setTileResult(OutputImageNode<TO, TI> outputNode) {
		//TODO store
		results.add(outputNode.getData());
	}

	public void concatenateTiles(OutputImageNode<TO, TI> outputImageNode) {
		tiling.setResults(results);
		RandomAccessibleInterval<TO> result = tiling.getResult();
		outputImageNode.setData(result);
	}


	@Override
	public boolean isCanceled() {
		return canceled;
	}

	@Override
	public void cancel(final String reason) {
		canceled = true;
	}

	@Override
	public String getCancelReason() {
		return null;
	}

}
