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

package net.imagej.modelzoo.consumer.tiling;

import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.InputImageNode;
import net.imagej.modelzoo.consumer.model.node.OutputImageNode;
import net.imglib2.util.Intervals;

import java.nio.file.Path;

public class DefaultTiling {

	private final InputImageNode inputNode;
	private final OutputImageNode outputNode;
	private final int defaultHalo = 32;
	private Path cacheDir;
	private int tilesNum = 1;
	private int batchSize = 10;

	private final ImageDataReference<?> originalData;
	private TiledImageDataReference<?, ?> tiledDataReference;
	private int doneTileCount = 0;

	public DefaultTiling(OutputImageNode outputNode) {
		this.originalData = outputNode.getReference().getData();
		this.inputNode = outputNode.getReference();
		this.outputNode = outputNode;
		this.cacheDir = null;
	}

	public DefaultTiling(OutputImageNode outputNode, Path cacheDir) {
		this(outputNode);
		this.cacheDir = cacheDir;
	}

	public boolean hasTilesLeft() {
//		return arrayProduct(Intervals.dimensionsAsLongArray(tiledInputView)) > doneTileCount;
		return tiledDataReference.getTiledOutputViewCursor().hasNext();
	}

	public void resolveCurrentTile() {
		tiledDataReference.resolveCurrentTile(outputNode.getData());
	}

	public void setNumberOfTiles(int nTiles) {
		tilesNum = nTiles;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public void init() {
		//TODO check if tilesNum / batchSize works?!
		resetTileCount();
		inputNode.setData(originalData);
		tiledDataReference = new TiledImageDataReference<>(inputNode, outputNode, inputNode.getData(), outputNode.getData(), cacheDir);
		tiledDataReference.createTiledInputView(batchSize, defaultHalo, tilesNum);
		long[] tiles = Intervals.dimensionsAsLongArray(tiledDataReference.getTiledInputView());
		tilesNum = (int) arrayProduct(tiles);
		tiledDataReference.createTiledOutputView();
	}

	private static long arrayProduct(long[] array) {
		long rtn = 1;
		for (long i : array) {
			rtn *= i;
		}
		return rtn;
	}

	public int getDoneTileCount() {
		return doneTileCount;
	}

	public void resetTileCount() {
		doneTileCount = 0;
	}

	public void assignNextTile() {
		tiledDataReference.assignNextTile();
		doneTileCount++;
	}

	public int getTilesNum() {
		return tilesNum;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void resetInputData() {
		inputNode.setData(originalData);
	}

	public void dispose() {
//		if(outputData != null) outputData.shutdown();
	}

	public long getTilesTotalCount() {
		return tiledDataReference.getTilesTotalCount();
	}

	public TiledImageDataReference<?, ?> getTilingData() {
		return tiledDataReference;
	}

	public void finish() {
		tiledDataReference.assignFullOutput();
	}
}
