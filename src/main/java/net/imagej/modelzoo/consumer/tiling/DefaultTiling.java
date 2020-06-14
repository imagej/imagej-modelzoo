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

package net.imagej.modelzoo.consumer.tiling;

import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.network.model.InputImageNode;
import net.imagej.modelzoo.consumer.network.model.ModelZooAxis;
import net.imagej.modelzoo.consumer.network.model.OutputImageNode;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.GridView;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.list.ListImg;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.Arrays;
import java.util.List;

public class DefaultTiling<TO extends RealType<TO>, TI extends RealType<TI>> implements Tiling<TO> {

	private final InputImageNode<TI> inputNode;
	private final OutputImageNode<TO, TI> outputNode;
	private final int defaultHalo = 32;
	private int tilesNum = 1;
	private int batchSize = 10;

	AdvancedTiledView<TI, TO> tiledView;
	private int doneTileCount = 0;
	private Cursor<RandomAccessibleInterval<TI>> tiledViewCursor;

	public DefaultTiling(OutputImageNode<TO, TI> outputNode) {
		this.inputNode = outputNode.getReference();
		this.outputNode = outputNode;
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
		createTiledView();
		tiledView.getProcessedTiles().clear();
		tiledViewCursor = Views.iterable(tiledView)
				.cursor();

	}

	@Override
	public int getDoneTileCount() {
		return doneTileCount;
	}

	@Override
	public void setResults(List<RandomAccessibleInterval<TO>> results) {
		tiledView.getProcessedTiles().addAll(results);
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
		inputNode.setData(tiledViewCursor.next());
	}

	private void createTiledView()
	{

		long[] tiling = new long[inputNode.numDimensions()];
		Arrays.fill(tiling, 1);
		tiling = computeTiling(tiling);
		long[] padding = getPadding(tiling);
		computeBatching(tiling);
		tilesNum = (int) arrayProduct(tiling);
		System.out.println("Input dimensions: " + Arrays.toString(Intervals.dimensionsAsIntArray(inputNode.getData())));
		System.out.println("Axes: " + Arrays.toString(inputNode.getDataAxesArray()));
		System.out.println("Dividing image into " + arrayProduct(tiling) + " tile(s)..");

		RandomAccessibleInterval<TI> expandedInput = expandToFitBatchSize(inputNode.getData(), tiling);
		expandedInput = expandToFitBlockSize(expandedInput, tiling);
		long[] tileSize = calculateTileSize(expandedInput, tiling);

		System.out.println("Size of single image tile: " + Arrays.toString(tileSize));

		final AdvancedTiledView<TI, TO> tiledView = createTiledView(expandedInput, tileSize, padding);
		for (int i = 0; i < inputNode.numDimensions(); i++) {
			tiledView.getOriginalDims().put(inputNode.getDataAxesArray()[i], inputNode.getData().dimension(
				i));
		}

		System.out.println("Final image tiling: " + Arrays.toString(Intervals.dimensionsAsIntArray(tiledView)));
		System.out.println("Final tile padding: " + Arrays.toString(padding));

		this.tiledView = tiledView;
	}

	private void computeBatching(long[] tiling)
	{

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

	private long[] computeTiling(long[] tiling)
	{
		int currentTiles = 1;
		for (long tiles : tiling) {
			currentTiles *= tiles;
		}
		if (currentTiles >= tilesNum) {
			return tiling;
		}
		else {
			long[] singleTile = new long[inputNode.numDimensions()];
			int maxDim = -1;
			for (int i = 0; i < singleTile.length; i++) {
				ModelZooAxis axis = inputNode.getDataAxis(i);
				if (axis.getTiling() == TilingAction.TILE_WITH_PADDING) {
					singleTile[i] = getTileSize(inputNode.getData(), i, tiling, axis);
					if (singleTile[i] > axis.getMin() && (maxDim < 0 ||
						singleTile[i] > singleTile[maxDim]))
					{
						maxDim = i;
					}
				}
			}
			if (maxDim >= 0) {
				tiling[maxDim] += 1;
				return computeTiling(tiling);
			}
			else {
				return tiling;
			}
		}
	}

	private long getTileSize(RandomAccessibleInterval<TI> img, int dimension, long[] tiling, ModelZooAxis axis) {
		Integer step = axis.getStep();
		Integer min = axis.getMin();
		if (step == null) step = 1;
		if(min == null) min = 1;
		long res = (long) (Math.ceil(img.dimension(dimension)
				/ (float)tiling[dimension] / (double) step) * step);
		return Math.max(res, min);
	}

	private long[] getPadding(long[] tiling) {
		long[] padding = new long[tiling.length];
		for (int i = 0; i < padding.length; i++) {
			if (tiling[i] > 1) {
				Integer halo = inputNode.getDataAxis(i).getHalo();
				if(halo == null) halo = defaultHalo;
				padding[i] = halo;
			}
		}
		return padding;
	}

	private RandomAccessibleInterval<TI> expandToFitBlockSize(
			RandomAccessibleInterval<TI> dataset, long[] tiling)
	{
		for (int i = 0; i < dataset.numDimensions(); i++) {
			ModelZooAxis axis = inputNode.getDataAxis(i);
			if (axis.getTiling() == TilingAction.TILE_WITH_PADDING) {
				dataset = expandDimToSize(dataset, i, getTileSize(dataset, i, tiling, axis) * tiling[i]);
			}
		}
		return dataset;
	}

	private RandomAccessibleInterval<TI> expandToFitBatchSize(RandomAccessibleInterval<TI> img, long[] tiling)
	{
		for (int i = 0; i < inputNode.numDimensions(); i++) {
			ModelZooAxis axis = inputNode.getDataAxis(i);
			if (axis.getTiling() == TilingAction.TILE_WITHOUT_PADDING) {
				img = expandDimToSize(img, i, getTileSize(inputNode.getData(), i, tiling, axis) *
					tiling[i]);
			}
		}
		return img;
	}

	private static <T> long[] calculateTileSize(RandomAccessibleInterval<T> img,
	                                 long[] tiling)
	{
		final long[] tileSize = Intervals.dimensionsAsLongArray(img);
		for (int i = 0; i < tileSize.length; i++) {
			tileSize[i] /= tiling[i];
		}
		return tileSize;
	}

	private AdvancedTiledView<TI, TO> createTiledView(RandomAccessibleInterval<TI> input, long[] tileSize, long[] padding)
	{
		return new AdvancedTiledView<>(input, tileSize, padding);
	}

	@Override
	public RandomAccessibleInterval<TO> getResult()
	{

		List<RandomAccessibleInterval<TO>> resultData = tiledView.getProcessedTiles();

		if (resultData != null && resultData.size() > 0) {

			System.out.println("result 0 before padding removement" + Arrays.toString(Intervals.dimensionsAsIntArray(resultData.get(0))));

			long[] grid = new long[outputNode.numDimensions()];
			AxisType[] inputAxes = inputNode.getDataAxesArray();
			AxisType[] outputAxes = outputNode.getDataAxesArray();
			Arrays.fill(grid, 1);
			for (int i = 0; i < grid.length; i++) {
				for (int j = 0; j < inputAxes.length; j++) {
					if (inputAxes[j].equals(outputAxes[i])) {
						grid[i] = tiledView.numDimensions() > j ? tiledView.dimension(j) : 1;
						break;
					}
				}
			}
			for (int i = 0; i < resultData.size(); i++) {
				resultData.set(i, removePadding(resultData.get(i), tiledView.getOverlap(),
						inputAxes, outputAxes));
			}

			// TODO log padding / test padding
			System.out.println("result 0 after padding removement: " + Arrays.toString(Intervals.dimensionsAsIntArray(resultData.get(0))));

			System.out.println("Merging tiles..");

			final RandomAccessibleInterval<TO> mergedResult = arrangeAndCombineTiles(
				resultData, grid);

			System.out.println("merge: " + Arrays.toString(Intervals.dimensionsAsIntArray(mergedResult)));

			System.out.println("Output axes: " + Arrays.toString(outputNode.getDataAxesArray()));
//			System.out.println("fittedResult dimensions: " + Arrays.toString(Intervals.dimensionsAsIntArray(fittedResult)));

			return mergedResult;
		}

		return null;
	}

	@Override
	public int getTilesNum() {
		return tilesNum;
	}

	@Override
	public int getBatchSize() {
		return 0;
	}

	private RandomAccessibleInterval<TO> removePadding(
			RandomAccessibleInterval<TO> result, long[] padding, AxisType[] oldAxes,
			AxisType[] newAxes)
	{

		final long[] negPadding = new long[result.numDimensions()];
		for (int i = 0; i < oldAxes.length; i++) {
			for (int j = 0; j < newAxes.length; j++) {
				if (oldAxes[i] == newAxes[j]) {
					negPadding[j] = -padding[i];
				}
			}
		}
		return Views.zeroMin(Views.expandZero(result, negPadding));

	}

	private RandomAccessibleInterval<TO> arrangeAndCombineTiles(
			List<RandomAccessibleInterval<TO>> results, long[] grid)
	{
		System.out.println("grid: " + Arrays.toString(grid));
		// Arrange and combine the tiles again
		return new GridView<>(new ListImg<>(
			results, grid));
	}

	private static <T> RandomAccessibleInterval<T> expandDimToSize(
			final RandomAccessibleInterval<T> im, final int d, final long size)
	{
		final int n = im.numDimensions();
		final long[] min = new long[n];
		final long[] max = new long[n];
		im.min(min);
		im.max(max);
		max[d] += (size - im.dimension(d));
		return Views.interval(Views.extendMirrorDouble(im), new FinalInterval(min,
			max));
	}
//
//	private int getSteps(List<AdvancedTiledView<TO>> input) {
//		int numSteps = 0;
//		for (AdvancedTiledView<TO> tile : input) {
//			int steps = 1;
//			for (int i = 0; i < tile.numDimensions(); i++) {
//				steps *= tile.dimension(i);
//			}
//			numSteps += steps;
//		}
//		return numSteps;
//	}

}
