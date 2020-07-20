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

import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.model.InputImageNode;
import net.imagej.modelzoo.consumer.model.ModelZooAxis;
import net.imagej.modelzoo.consumer.model.OutputImageNode;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TiledView;
import net.imglib2.cache.img.DiskCachedCellImg;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.operators.SetZero;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.nio.file.Path;
import java.util.Arrays;

public class DefaultTiling<TO extends RealType<TO> & NativeType<TO>, TI extends RealType<TI> & NativeType<TI>> implements Tiling<TO> {

	private final InputImageNode<TI> inputNode;
	private final OutputImageNode<TO, TI> outputNode;
	private final int defaultHalo = 32;
	private int tilesNum = 1;
	private int batchSize = 10;
	private final Path cacheDir;

	private RandomAccessibleInterval<TI> originalData;
	TiledView<TI> tiledInputView;
	private TiledView<TO> tiledOutputView;
	private int doneTileCount = 0;
	private Cursor<RandomAccessibleInterval<TI>> tiledInputViewCursor;
	private Cursor<RandomAccessibleInterval<TO>> tiledOutputViewCursor;
	private DiskCachedCellImg<TO, ?> outputData;

	public DefaultTiling(OutputImageNode<TO, TI> outputNode) {
		this.inputNode = outputNode.getReference();
		this.outputNode = outputNode;
		this.originalData = inputNode.getData();
		this.cacheDir = null;
	}

	public DefaultTiling(OutputImageNode<TO, TI> outputNode, Path cacheDir) {
		this.inputNode = outputNode.getReference();
		this.outputNode = outputNode;
		this.originalData = inputNode.getData();
		this.cacheDir = cacheDir;
	}

	@Override
	public boolean hasTilesLeft() {
//		return arrayProduct(Intervals.dimensionsAsLongArray(tiledInputView)) > doneTileCount;
		return tiledOutputViewCursor.hasNext();
	}

	@Override
	public void resolveCurrentTile(RandomAccessibleInterval<TO> data) {
		RandomAccessibleInterval<TO> currentTile = tiledOutputViewCursor.next();
		long[] padding = tiledInputView.getOverlap();
		for (int i = 0; i < padding.length; i++) {
			padding[i] = -padding[i];
		}
		IntervalView<TO> dataWithoutPadding = Views.expandBorder(data, padding);
		LoopBuilder.setImages(dataWithoutPadding, currentTile).forEachPixel((in, out) -> {
			out.set(in);
		});
	}

	@Override
	public void setNumberOfTiles(int nTiles) {
		tilesNum = nTiles;
	}

	@Override
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	@Override
	public void init() {
		//TODO check if tilesNum / batchSize works?!
		resetTileCount();
		inputNode.setData(originalData);
		createTiledInputView();
		createTiledOutputView();
		tiledInputViewCursor = Views.iterable(tiledInputView).cursor();
		tiledOutputViewCursor = Views.iterable(tiledOutputView).cursor();

	}

	@Override
	public int getDoneTileCount() {
		return doneTileCount;
	}

	@Override
	public void resetTileCount() {
		doneTileCount = 0;
	}

	@Override
	public void upTileCount() {
		doneTileCount++;
	}

	@Override
	public void assignCurrentTile() {
		inputNode.setData(tiledInputViewCursor.next());
	}

	private void createTiledInputView() {

		long[] tiling = new long[inputNode.numDimensions()];
		Arrays.fill(tiling, 1);
		tiling = computeTiling(tiling);
		long[] padding = getPadding(tiling);
		tilesNum = (int) arrayProduct(tiling);
		computeBatching(tiling);
		System.out.println("Input dimensions: " + Arrays.toString(Intervals.dimensionsAsIntArray(inputNode.getData())));
		System.out.println("Axes: " + Arrays.toString(inputNode.getDataAxesArray()));
		System.out.println("Dividing image into " + arrayProduct(tiling) + " tile(s)..");

		RandomAccessibleInterval<TI> expandedInput = expandToFitBatchSize(inputNode.getData(), tiling);
//		System.out.println("expandedinput: " + Arrays.toString(Intervals.dimensionsAsIntArray(expandedInput)));
//		System.out.println("tiling: " + Arrays.toString(tiling));
		expandedInput = expandToFitBlockSize(expandedInput, tiling);
//		System.out.println("expandedinput: " + Arrays.toString(Intervals.dimensionsAsIntArray(expandedInput)));
//		System.out.println("tiling: " + Arrays.toString(tiling));
		long[] tileSize = calculateTileSize(expandedInput, tiling);

		System.out.println("Size of single image tile: " + Arrays.toString(tileSize));

		final TiledView<TI> tiledView = new TiledView<>(expandedInput, tileSize, padding);

		System.out.println("Final image tiling: " + Arrays.toString(Intervals.dimensionsAsIntArray(tiledView)));
		System.out.println("Final tile padding: " + Arrays.toString(padding));

		this.tiledInputView = tiledView;
	}


	private void createTiledOutputView() {
		long[] grid = new long[outputNode.numDimensions()];
		long[] padding = new long[outputNode.numDimensions()];
		long[] dims = new long[outputNode.numDimensions()];
		AxisType[] inputAxes = inputNode.getDataAxesArray();
		Arrays.fill(grid, 1);

		for (int i = 0; i < grid.length; i++) {
			ModelZooAxis outputAxis = outputNode.getDataAxis(i);
			Double scale = outputAxis.getScale();
			Integer offset = outputAxis.getOffset();
			if(scale == null) scale = 1.;
			if(offset == null) offset = 0;
			for (int j = 0; j < inputAxes.length; j++) {
				if (inputAxes[j].equals(outputAxis.getType())) {
					grid[i] = tiledInputView.dimension(j);
					dims[i] = (long) (tiledInputView.getBlockSize()[j]*grid[i]* scale + offset);
					break;
				}
			}
		}

		TO dataType = outputNode.getDataType();
		if(dataType == null) dataType = (TO) inputNode.getDataType();
		long[] tileSize = calculateTileSize(dims, grid);
		int[] intTileSize = new int[tileSize.length];
		for (int i = 0; i < tileSize.length; i++) {
			intTileSize[i] = (int) tileSize[i];
		}
		System.out.println("Output tiling: " + Arrays.toString(intTileSize));
		System.out.println("Output dimensions: " + Arrays.toString(dims));
//		if(outputData != null) outputData.shutdown();
		outputData = new DiskCachedCellImgFactory<>( dataType,
				DiskCachedCellImgOptions.options()
						.cacheType(DiskCachedCellImgOptions.CacheType.BOUNDED)
						.maxCacheSize(3)
						.cacheDirectory(cacheDir)
						.deleteCacheDirectoryOnExit(cacheDir == null)
						.cellDimensions(intTileSize)).create(dims);
//		outputData = new CellImgFactory<>(dataType).create(dims);
//		TODO this is just a test to see if the memory is sufficient to access the whole output data, maybe this can be done smarter
//		outputData.forEach(SetZero::setZero);
		tiledOutputView = new TiledView<>(outputData, tileSize, padding);
	}

	private long multiply(long[] grid) {
		long res = 1;
		for (long l : grid) {
			res *= l;
		}
		return res;
	}

	private void computeBatching(long[] tiling) {

		for (int i = 0; i < inputNode.numDimensions(); i++) {
			ModelZooAxis axis = inputNode.getDataAxis(i);
			if (axis.getTiling() == TilingAction.TILE_WITHOUT_PADDING) {

				long batchDimSize = inputNode.getData().dimension(i);

				// parent.debug( "batchDimSize: " + batchDimSize );

				long batchesNum = (int) Math.ceil((float) batchDimSize /
						(float) batchSize);

				// If a smaller batch size is sufficient for the same amount of batches,
				// we can use it
				batchSize = (int) Math.ceil((float) batchDimSize / (float) batchesNum);

				tiling[i] = batchesNum;

			}
		}

	}

	private static long arrayProduct(long[] array) {
		long rtn = 1;
		for (long i : array) {
			rtn *= i;
		}
		return rtn;
	}

	private long[] computeTiling(long[] tiling) {
		int currentTiles = 1;
		for (long tiles : tiling) {
			currentTiles *= tiles;
		}
		if (currentTiles >= tilesNum) {
			return tiling;
		} else {
			long[] singleTile = new long[inputNode.numDimensions()];
			int maxDim = -1;
			for (int i = 0; i < singleTile.length; i++) {
				ModelZooAxis axis = inputNode.getDataAxis(i);
				if (axis.getTiling() == TilingAction.TILE_WITH_PADDING) {
					singleTile[i] = getTileSize(inputNode.getData().dimension(i), tiling[i], axis);
					if (singleTile[i] > axis.getMin() && (maxDim < 0 ||
							singleTile[i] > singleTile[maxDim])) {
						maxDim = i;
					}
				}
			}
			if (maxDim >= 0) {
				tiling[maxDim] += 1;
				return computeTiling(tiling);
			} else {
				return tiling;
			}
		}
	}

	private long getTileSize(long imgDimension, long tiling, ModelZooAxis axis) {
		Integer step = axis.getStep();
		Integer min = axis.getMin();
		if (step == null) step = 1;
		if (min == null) min = 1;
		long stepsTotal = (long) Math.ceil((imgDimension
				/ (float) tiling) / (double) step);
		long res = stepsTotal * step;
		return Math.max(res, min);
	}

	private long[] getPadding(long[] tiling) {
		long[] padding = new long[tiling.length];
		for (int i = 0; i < padding.length; i++) {
//			if (tiling[i] > 1) {
			ModelZooAxis axis = inputNode.getDataAxis(i);
			Integer halo = axis.getHalo();
			Integer step = axis.getStep();
			if (halo == null) halo = defaultHalo;
			if(step != null) {
				halo = (int)Math.ceil((float)halo / (float)step)*step;
			}
			padding[i] = halo;
//			}
		}
		return padding;
	}

	private RandomAccessibleInterval<TI> expandToFitBlockSize(
			RandomAccessibleInterval<TI> dataset, long[] tiling) {
		for (int i = 0; i < dataset.numDimensions(); i++) {
			ModelZooAxis axis = inputNode.getDataAxis(i);
			if (axis.getTiling() == TilingAction.TILE_WITH_PADDING) {
				long tileSize = getTileSize(dataset.dimension(i), tiling[i], axis);
				System.out.println("tile size " + i + ": " + tileSize + " step: " + axis.getStep());
				dataset = expandDimToSize(dataset, i, tileSize * tiling[i]);
			}
		}
		return dataset;
	}

	private RandomAccessibleInterval<TI> expandToFitBatchSize(RandomAccessibleInterval<TI> img, long[] tiling) {
		for (int i = 0; i < inputNode.numDimensions(); i++) {
			ModelZooAxis axis = inputNode.getDataAxis(i);
			if (axis.getTiling() == TilingAction.TILE_WITHOUT_PADDING) {
				img = expandDimToSize(img, i, getTileSize(inputNode.getData().dimension(i), tiling[i], axis) *
						tiling[i]);
			}
		}
		return img;
	}

	private static <T> long[] calculateTileSize(RandomAccessibleInterval<T> img,
	                                            long[] tiling) {
		return calculateTileSize(Intervals.dimensionsAsLongArray(img), tiling);
	}

	private static long[] calculateTileSize(long[] dims,
	                                            long[] tiling) {
		final long[] tileSize = new long[dims.length];
		for (int i = 0; i < tileSize.length; i++) {
			tileSize[i] = dims[i] / tiling[i];
		}
		return tileSize;
	}

	@Override
	public RandomAccessibleInterval<TO> getResult() {
		return outputData;
	}

	@Override
	public int getTilesNum() {
		return tilesNum;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Override
	public void resetInputData() {
		inputNode.setData(originalData);
	}

	@Override
	public void dispose() {
//		if(outputData != null) outputData.shutdown();
	}

	private static <T> RandomAccessibleInterval<T> expandDimToSize(
			final RandomAccessibleInterval<T> im, final int d, final long size) {
		final int n = im.numDimensions();
		final long[] min = new long[n];
		final long[] max = new long[n];
		im.min(min);
		im.max(max);
		max[d] += (size - im.dimension(d));
		return Views.interval(Views.extendMirrorDouble(im), new FinalInterval(min,
				max));
	}

}
