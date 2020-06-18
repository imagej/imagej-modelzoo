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

import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.OutputImageNode;
import net.imagej.modelzoo.consumer.tiling.DefaultTiling;
import net.imagej.modelzoo.consumer.tiling.Tiling;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import org.scijava.Cancelable;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RejectedExecutionException;

public class TiledPredictionExecutor implements Cancelable {

	@Parameter
	private LogService log;

	private final ModelZooModel model;
	private Tiling tiling;
	private int nTiles = 8;
	private int oldNTiles;
	private int oldBatchesSize;
	private boolean processedTiles = false;
	private boolean canceled = false;
	private final List<RandomAccessibleInterval> results = new ArrayList<>();
	private int batchSize = 10;
	private boolean tilingEnabled = true;

	public TiledPredictionExecutor(ModelZooModel model, Context context) {
		this.model = model;
		context.inject(this);
	}

	public void run() throws OutOfMemoryError, IllegalArgumentException {

		try {
			if (!tilingEnabled || model.getInputNodes().size() > 1) {
				model.predict();
			} else {
				initTiling();
				while (processNextTile()) {
					model.predict();
					setTileResult(model.getOutputNodes().get(0));
				}
				concatenateTiles(model.getOutputNodes().get(0));
			}
		} catch (final CancellationException | RejectedExecutionException e) {
			//canceled
			String PROGRESS_CANCELED = "Canceled";
			log.warn(PROGRESS_CANCELED);
			cancel(PROGRESS_CANCELED);
		} catch (final IllegalArgumentException e) {
			throw e;
		} catch (final IllegalStateException exc) {
			if (exc.getMessage() != null && exc.getMessage().contains("OOM")) {
				throw new OutOfMemoryError();
			}
			exc.printStackTrace();
			throw exc;
		}
	}

	public void setNumberOfTiles(int nTiles) {
		this.nTiles = nTiles;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public boolean increaseTiling() {

		// We expect it to be an out of memory exception and
		// try it again with more tiles or smaller batches.
		int nTiles = tiling.getTilesNum();
		int batchSize = tiling.getBatchSize();
		if (oldNTiles == nTiles && oldBatchesSize == batchSize) {
			return false;
		}
		oldNTiles = nTiles;
		oldBatchesSize = batchSize;

		batchSize /= 2;
		if (batchSize < 1) {
			batchSize = 1;
			nTiles *= 2;
		}

		setNumberOfTiles(nTiles);
		setBatchSize(batchSize);
		tiling.resetInputData();

		log.warn(
				"Out of memory exception occurred. Trying with " + nTiles +
						" tiles, batch size " + batchSize + "...");

		return true;
	}

	private boolean processNextTile() {
		// go to next tile
		if (processedTiles || !tiling.hasTilesLeft()) {
			processedTiles = true;
			return false;
		}
		tiling.assignCurrentTile();
		tiling.upTileCount();
		log.info("Processing tile " + (tiling.getDoneTileCount()) + "..");
		return true;
	}

	private <TO extends RealType<TO>, TI extends RealType<TI>> void initTiling() {
		//TODO reset tiling
		// start with first tile
		processedTiles = false;
		tiling = new DefaultTiling<>((OutputImageNode<TO, TI>) model.getOutputNodes().get(0));
		tiling.setNumberOfTiles(nTiles);
		tiling.setBatchSize(batchSize);
		tiling.init();
		results.clear();
	}

	private <TO extends RealType<TO>, TI extends RealType<TI>> void setTileResult(OutputImageNode<TO, TI> outputNode) {
		results.add(outputNode.getData());
	}

	private void concatenateTiles(OutputImageNode outputImageNode) {
		tiling.setResults(results);
		RandomAccessibleInterval result = tiling.getResult();
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

	public void setTilingEnabled(boolean enabled) {
		this.tilingEnabled = enabled;
	}

	public int getNumberOfTiles() {
		return nTiles;
	}

	public int getBatchSize() {
		return batchSize;
	}
}
