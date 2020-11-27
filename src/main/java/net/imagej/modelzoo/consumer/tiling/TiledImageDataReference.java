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
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.InputImageNode;
import net.imagej.modelzoo.consumer.model.node.ModelZooAxis;
import net.imagej.modelzoo.consumer.model.node.OutputImageNode;
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
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TiledImageDataReference<TI extends RealType<TI> & NativeType<TI>> extends DefaultImageDataReference<TI> {

	class TiledOutput<TO extends RealType<TO> & NativeType<TO>> {
		OutputImageNode outputNode;
		ImageDataReference<TO> outputReference;
		Cursor<RandomAccessibleInterval<TO>> tiledOutputViewCursor;
		DiskCachedCellImg<TO, ?> outputData;

		public TiledOutput(OutputImageNode outputImageNode, ImageDataReference<TO> imageDataReference) {
			outputNode = outputImageNode;
			outputReference = imageDataReference;
		}

		public void setOutputReference(ImageDataReference imageDataReference) {
			outputReference = imageDataReference;
		}
	}

	private final InputImageNode inputNode;
	private TiledView<TI> tiledInputView;
	private Cursor<RandomAccessibleInterval<TI>> tiledInputViewCursor;
	private Path cacheDir;
	private List<TiledOutput<?>> tiledOutputs;

	TiledImageDataReference(InputImageNode inputNode, List<OutputImageNode> outputNodes, ImageDataReference<TI> inputReference, List<ImageDataReference<?>> outputReferences, Path cacheDir) {
		super(inputReference.getData(), inputReference.getDataType());
		tiledOutputs = new ArrayList<>();
		for (int i = 0; i < outputNodes.size(); i++) {
			tiledOutputs.add(new TiledOutput<>(outputNodes.get(i), outputReferences.get(i)));
		}
		this.inputNode = inputNode;
		this.cacheDir = cacheDir;
	}

	List<TiledOutput<?>> getTiledOutputs() {
		return tiledOutputs;
	}

	TiledView<TI> getTiledInputView() {
		return tiledInputView;
	}

	long getTilesTotalCount() {
		return arrayProduct(Intervals.dimensionsAsLongArray(tiledInputView));
	}

	private static long arrayProduct(long[] array) {
		long rtn = 1;
		for (long i : array) {
			rtn *= i;
		}
		return rtn;
	}


	void resolveCurrentTile(List<ImageDataReference<?>> data) {
		for (int i = 0; i < tiledOutputs.size(); i++) {
			tiledOutputs.get(i).setOutputReference(data.get(i));
		}
		long[] padding = tiledInputView.getOverlap();
		for (int i = 0; i < padding.length; i++) {
			padding[i] = -padding[i];
		}
		for (int d = 0; d < data.size(); d++) {
			TiledOutput<?> tiledOutput = tiledOutputs.get(d);
			RandomAccessibleInterval<? extends RealType<?>> currentTile = tiledOutput.tiledOutputViewCursor.next();
			IntervalView<? extends RealType<?>> dataWithoutPadding = Views.expandBorder(tiledOutput.outputReference.getData(), padding);
			LoopBuilder.setImages(dataWithoutPadding, currentTile).multiThreaded().forEachPixel((in, out) -> {
				out.setReal(in.getRealDouble());
			});
		}
	}

	void createTiledInputView(int batchSize, int defaultHalo, int tilesMin) {

		long[] tiling = new long[getData().numDimensions()];
		Arrays.fill(tiling, 1);
		tiling = computeTiling(tiling, tilesMin);
		long[] padding = getPadding(tiling, defaultHalo);
		computeBatching(tiling, batchSize);
		System.out.println("Input dimensions: " + Arrays.toString(Intervals.dimensionsAsIntArray(getData())));
		System.out.println("Axes: " + Arrays.toString(inputNode.getDataAxesArray()));
		System.out.println("Dividing image into " + arrayProduct(tiling) + " tile(s)..");

		RandomAccessibleInterval<TI> expandedInput = expandToFitBatchSize(inputNode, getData(), tiling);
//		System.out.println("expandedinput: " + Arrays.toString(Intervals.dimensionsAsIntArray(expandedInput)));
//		System.out.println("tiling: " + Arrays.toString(tiling));
		expandedInput = expandToFitBlockSize(inputNode, expandedInput, tiling);
//		System.out.println("expandedinput: " + Arrays.toString(Intervals.dimensionsAsIntArray(expandedInput)));
//		System.out.println("tiling: " + Arrays.toString(tiling));
		long[] tileSize = calculateTileSize(expandedInput, tiling);

		System.out.println("Size of single image tile: " + Arrays.toString(tileSize));

		final TiledView<TI> tiledView = new TiledView<>(expandedInput, tileSize, padding);

		System.out.println("Final image tiling: " + Arrays.toString(Intervals.dimensionsAsIntArray(tiledView)));
		System.out.println("Final tile padding: " + Arrays.toString(padding));

		this.tiledInputView = tiledView;
		tiledInputViewCursor = Views.iterable(tiledInputView).cursor();
	}

	void createTiledOutputView() {
		for (int d = 0; d < tiledOutputs.size(); d++) {
			TiledOutput<?> tiledOutput = tiledOutputs.get(d);
			createTiledOutputView(tiledOutput, d);
		}
	}

	private <T extends RealType<T> & NativeType<T>> void createTiledOutputView(TiledOutput<T> tiledOutput, int d) {
		long[] grid = new long[tiledOutput.outputNode.numDimensions()];
		long[] padding = new long[tiledOutput.outputNode.numDimensions()];
		long[] dims = new long[tiledOutput.outputNode.numDimensions()];
		AxisType[] inputAxes = inputNode.getDataAxesArray();
		Arrays.fill(grid, 1);

		for (int i = 0; i < grid.length; i++) {
			ModelZooAxis outputAxis = tiledOutput.outputNode.getDataAxis(i);
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

		// is this necessary?
		T dataType = tiledOutput.outputReference.getDataType();
		if(dataType == null) dataType = (T) inputNode.getData().getDataType();
		if(dataType == null) dataType = (T) tiledInputView.randomAccess().get().randomAccess().get().copy();

		long[] tileSize = calculateTileSize(dims, grid);
		int[] intTileSize = new int[tileSize.length];
		for (int i = 0; i < tileSize.length; i++) {
			intTileSize[i] = (int) tileSize[i];
		}
		System.out.println("Size of single output tile: " + Arrays.toString(intTileSize));
		System.out.println("Output dimensions: " + Arrays.toString(dims));
		clearCacheDir();
//		if(outputData != null) outputData.shutdown();
		DiskCachedCellImg<T, ?> cellImg = new DiskCachedCellImgFactory<>(dataType,
				DiskCachedCellImgOptions.options()
						.cacheType(DiskCachedCellImgOptions.CacheType.SOFTREF)
						.cacheDirectory(cacheDir)
						.deleteCacheDirectoryOnExit(cacheDir == null)).create(dims);
		tiledOutput.outputData = cellImg;
		TiledView<T> tiledOutputView = new TiledView<>(cellImg, tileSize, padding);
		tiledOutput.tiledOutputViewCursor = Views.iterable(tiledOutputView).cursor();
	}

	private void clearCacheDir() {
		if(cacheDir == null) return;
		if(!cacheDir.toFile().exists()) {
			System.err.println("Cache directory " + cacheDir + " does not exist");
			return;
		}
		File[] files = cacheDir.toFile().listFiles();
		if(files == null) return;
		for (File file : files) {
			if(!file.isDirectory()) file.delete();
		}
	}

	private void computeBatching(long[] tiling, int batchSize) {

		for (int i = 0; i < getData().numDimensions(); i++) {
			ModelZooAxis axis = inputNode.getDataAxis(i);
			if (axis.getTiling() == TilingAction.TILE_WITHOUT_PADDING) {

				long batchDimSize = getData().dimension(i);

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

	private long[] getPadding(long[] tiling, int defaultHalo) {
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

	private long[] computeTiling(long[] tiling, int tilesMin) {
		int currentTiles = 1;
		for (long tiles : tiling) {
			currentTiles *= tiles;
		}
		if (currentTiles >= tilesMin) {
			return tiling;
		} else {
			long[] singleTile = new long[inputNode.numDimensions()];
			int maxDim = -1;
			for (int i = 0; i < singleTile.length; i++) {
				ModelZooAxis axis = inputNode.getDataAxis(i);
				if (axis.getTiling() == TilingAction.TILE_WITH_PADDING) {
					singleTile[i] = getTileSize(getData().dimension(i), tiling[i], axis);
					if (singleTile[i] > axis.getMin() && (maxDim < 0 ||
							singleTile[i] > singleTile[maxDim])) {
						maxDim = i;
					}
				}
			}
			if (maxDim >= 0) {
				tiling[maxDim] += 1;
				return computeTiling(tiling, tilesMin);
			} else {
				return tiling;
			}
		}
	}

	private RandomAccessibleInterval<TI> expandToFitBatchSize(InputImageNode inputNode, RandomAccessibleInterval<TI> img, long[] tiling) {
		for (int i = 0; i < inputNode.numDimensions(); i++) {
			ModelZooAxis axis = inputNode.getDataAxis(i);
			if (axis.getTiling() == TilingAction.TILE_WITHOUT_PADDING) {
				img = expandDimToSize(img, i, getTileSize(img.dimension(i), tiling[i], axis) *
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

	private RandomAccessibleInterval<TI> expandToFitBlockSize(
			InputImageNode inputNode, RandomAccessibleInterval<TI> dataset, long[] tiling) {
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

	void assignNextTile() {
		inputNode.setData(new DefaultImageDataReference<>(tiledInputViewCursor.next(), getDataType()));
	}

	public void assignFullOutput() {
		for (TiledOutput<?> tiledOutput : tiledOutputs) {
			tiledOutput.outputNode.setData(new DefaultImageDataReference(tiledOutput.outputData, tiledOutput.outputNode.getData().getDataType()));
		}
	}
}
