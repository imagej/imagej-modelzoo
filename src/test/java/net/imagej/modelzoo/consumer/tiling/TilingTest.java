
package net.imagej.modelzoo.consumer.tiling;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.AbstractModelZooTest;
import net.imagej.modelzoo.consumer.model.InputImageNode;
import net.imagej.modelzoo.consumer.model.ModelZooAxis;
import net.imagej.modelzoo.consumer.model.OutputImageNode;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TiledView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;

public class TilingTest extends AbstractModelZooTest {

	@Test
	public void testTilingZXY() {

		final long[] datasetSize = {10, 50, 100};
		List<ModelZooAxis> axes = new ArrayList<>();
		axes.add(new ModelZooAxis(Axes.X));
		axes.add(new ModelZooAxis(Axes.Y));
		axes.add(new ModelZooAxis(Axes.Z));
		final AxisType[] axesTypes = {Axes.Z, Axes.X, Axes.Y};

		createImageJ();

		final RandomAccessibleInterval<FloatType> input = new ArrayImgFactory<>(new FloatType()).create(datasetSize);

		InputImageNode<FloatType> nodeIn = new InputImageNode<>();
		nodeIn.setData(input);
		OutputImageNode<FloatType, FloatType> nodeOut = new OutputImageNode<>();
		nodeIn.getAxes().addAll(axes);
		nodeIn.setDataMapping(Arrays.asList(axesTypes));
		nodeOut.setReference(nodeIn);
		nodeOut.setDataMapping(Arrays.asList(axesTypes));
		nodeOut.getAxes().addAll(axes);
		nodeOut.setData(input);

		final DefaultTiling<FloatType, FloatType> tiling = new DefaultTiling<>(nodeOut);
		tiling.setNumberOfTiles(8);
		tiling.init();
		TiledView<FloatType> tiledView = tiling.tiledView;
		tiling.getProcessedTiles().clear();
		final Cursor<RandomAccessibleInterval<FloatType>> cursor = Views.iterable(
				tiledView).cursor();
		while (cursor.hasNext()) {
			tiling.getProcessedTiles().add(cursor.next());
		}
		assertNotNull(tiledView);
		RandomAccessibleInterval<FloatType> output = tiling.getResult();
		assertNotNull(output);

		compareDimensions(input, output);
	}
//
//	@Test
//	@Ignore
//	public void testTilingZXYC() {
//
//		final Tiling tiling = new DefaultTiling(8, 1, 32, 32);
//		final long[] datasetSize = { 10, 50, 100, 1 };
//		final AxisType[] axes = { Axes.Z, Axes.X, Axes.Y, Axes.CHANNEL };
//		final Task task = new DefaultTask();
//
//		createImageJ();
//
//		final Dataset dataset = ij.dataset().create(new FloatType(), datasetSize,
//				"", axes);
//		final RandomAccessibleInterval<FloatType> input =
//				(RandomAccessibleInterval<FloatType>) dataset.getImgPlus();
//		final AdvancedTiledView<FloatType> tiledView = tiling.preprocess(input,
//				axes, getTilingActions(dataset), task);
//
//		tiledView.getProcessedTiles().clear();
//		final Cursor<RandomAccessibleInterval<FloatType>> cursor = Views.iterable(
//				tiledView).cursor();
//		while (cursor.hasNext()) {
//			tiledView.getProcessedTiles().add(cursor.next());
//		}
//
//		final RandomAccessibleInterval<FloatType> output = tiling.postprocess(task,
//				tiledView, axes);
//
//		assertNotNull(tiledView);
//		assertNotNull(output);
//
//		tiledView.dispose();
//
//		compareDimensions(input, output);
//	}
//
//	@Test
//	@Ignore
//	public void testNoTiling() {
//
//		final Tiling tiling = new DefaultTiling(8, 1, 32, 32);
//		final long[] datasetSize = { 10, 50, 100 };
//		final AxisType[] axes = { Axes.Z, Axes.X, Axes.Y };
//		Tiling.TilingAction[] actions = new Tiling.TilingAction[axes.length];
//		Arrays.fill(actions, Tiling.TilingAction.NO_TILING);
//
//		final AdvancedTiledView<FloatType> tiledView = runTiling(datasetSize, axes, tiling, actions);
//
//		assertEquals(1, getNumTiles(tiledView));
//
//		tiledView.dispose();
//	}
//
//	@Test
//	@Ignore
//	public void testSingleDimensionTiling() {
//
//		final Tiling tiling = new DefaultTiling(2, 1, 32, 32);
//		final long[] datasetSize = { 10, 50, 100 };
//		final AxisType[] axes = { Axes.Z, Axes.X, Axes.Y };
//		Tiling.TilingAction[] actions = new Tiling.TilingAction[axes.length];
//		Arrays.fill(actions, Tiling.TilingAction.TILE_WITH_PADDING);
//
//		final AdvancedTiledView<FloatType> tiledView = runTiling(datasetSize, axes, tiling, actions);
//
//		assertEquals(1, tiledView.dimension(0));
//		assertEquals(1, tiledView.dimension(1));
//		assertEquals(2, tiledView.dimension(2));
//
//		assertEquals(32, tiledView.getBlockSize()[0]);
//		assertEquals(64, tiledView.getBlockSize()[1]);
//		assertEquals(64, tiledView.getBlockSize()[2]);
//
//		assertEquals(0, tiledView.getOverlap()[0]);
//		assertEquals(0, tiledView.getOverlap()[1]);
//		assertEquals(32, tiledView.getOverlap()[2]);
//
//		tiledView.dispose();
//
//	}
//
//	@Test
//	@Ignore
//	public void testMultiDimensionTiling() {
//
//		final Tiling tiling = new DefaultTiling(8, 1, 32, 32);
//		final long[] datasetSize = { 10, 50, 100 };
//		final AxisType[] axes = { Axes.Z, Axes.X, Axes.Y };
//		Tiling.TilingAction[] actions = new Tiling.TilingAction[axes.length];
//		Arrays.fill(actions, Tiling.TilingAction.TILE_WITH_PADDING);
//
//		final AdvancedTiledView<FloatType> tiledView = runTiling(datasetSize, axes, tiling, actions);
//
//		assertEquals(1, tiledView.dimension(0));
//		assertEquals(2, tiledView.dimension(1));
//		assertEquals(4, tiledView.dimension(2));
//
//		assertEquals(32, tiledView.getBlockSize()[0]);
//		assertEquals(32, tiledView.getBlockSize()[1]);
//		assertEquals(32, tiledView.getBlockSize()[2]);
//
//		assertEquals(0, tiledView.getOverlap()[0]);
//		assertEquals(32, tiledView.getOverlap()[1]);
//		assertEquals(32, tiledView.getOverlap()[2]);
//
//		tiledView.dispose();
//
//	}
//
//	@Test
//	@Ignore
//	public void testNoPaddingTiling() {
//
//		final Tiling tiling = new DefaultTiling(10, 1, 10, 0);
//		final long[] datasetSize = { 10, 20, 30 };
//		final AxisType[] axes = { Axes.Z, Axes.X, Axes.Y };
//		Tiling.TilingAction[] actions = new Tiling.TilingAction[axes.length];
//		Arrays.fill(actions, Tiling.TilingAction.TILE_WITH_PADDING);
//
//		final AdvancedTiledView<FloatType> tiledView = runTiling(datasetSize, axes, tiling, actions);
//
//		assertEquals(1, tiledView.dimension(0));
//		assertEquals(2, tiledView.dimension(1));
//		assertEquals(3, tiledView.dimension(2));
//
//		assertEquals(10, tiledView.getBlockSize()[0]);
//		assertEquals(10, tiledView.getBlockSize()[1]);
//		assertEquals(10, tiledView.getBlockSize()[2]);
//
//		assertEquals(0, tiledView.getOverlap()[0]);
//		assertEquals(0, tiledView.getOverlap()[1]);
//		assertEquals(0, tiledView.getOverlap()[2]);
//
//		tiledView.dispose();
//
//	}
//
//
//	@Test
//	@Ignore
//	public void testBatching() {
//
//		final Tiling tiling = new DefaultTiling(1, 5, 10, 32);
//		final long[] datasetSize = { 10, 50, 100 };
//		final AxisType[] axes = { Axes.Z, Axes.X, Axes.Y };
//		Tiling.TilingAction[] actions = new Tiling.TilingAction[axes.length];
//		Arrays.fill(actions, Tiling.TilingAction.TILE_WITHOUT_PADDING);
//
//		final AdvancedTiledView<FloatType> tiledView = runTiling(datasetSize, axes, tiling, actions);
//
//		assertEquals(2, tiledView.dimension(0));
//		assertEquals(10, tiledView.dimension(1));
//		assertEquals(20, tiledView.dimension(2));
//
//		assertEquals(5, tiledView.getBlockSize()[0]);
//		assertEquals(5, tiledView.getBlockSize()[1]);
//		assertEquals(5, tiledView.getBlockSize()[2]);
//
//		assertEquals(0, tiledView.getOverlap()[0]);
//		assertEquals(0, tiledView.getOverlap()[1]);
//		assertEquals(0, tiledView.getOverlap()[2]);
//
//		tiledView.dispose();
//
//	}
//
//	@Test
//	@Ignore
//	public void testSmallDataset() {
//
//		final Tiling tiling = new DefaultTiling(8, 1, 32, 32);
//		final long[] datasetSize = { 3, 4, 5 };
//		final AxisType[] axes = { Axes.X, Axes.Y, Axes.TIME };
//		Tiling.TilingAction[] actions = new Tiling.TilingAction[axes.length];
//		Arrays.fill(actions, Tiling.TilingAction.TILE_WITH_PADDING);
//
//		final AdvancedTiledView<FloatType> tiledView = runTiling(datasetSize, axes, tiling, actions);
//
//		assertEquals(1, tiledView.dimension(0));
//		assertEquals(1, tiledView.dimension(1));
//		assertEquals(1, tiledView.dimension(2));
//
//		assertEquals(32, tiledView.getBlockSize()[0]);
//		assertEquals(32, tiledView.getBlockSize()[1]);
//		assertEquals(32, tiledView.getBlockSize()[2]);
//
//		assertEquals(0, tiledView.getOverlap()[0]);
//		assertEquals(0, tiledView.getOverlap()[1]);
//		assertEquals(0, tiledView.getOverlap()[2]);
//
//		tiledView.dispose();
//	}


//
//	@Test
//	public void testNetworkTiling() {
//
//		final long[] datasetSize = { 3, 4, 5 };
//		final AxisType[] datasetAxes = { Axes.X, Axes.Y, Axes.TIME };
//		final long[] nodeShape = {-1,-1,-1,1};
//
//		launchImageJ();
//
//		Dataset dataset = ij.dataset().create(new FloatType(), datasetSize, "", datasetAxes);
//		final Tiling tiling = new DefaultTiling(8, 1, 32, 32);
//
//		InputNode node = new InputNode();
//		node.initialize(dataset);
//		node.setNodeShape(nodeShape);
//		node.setMapping(new AxisType[]{Axes.TIME, Axes.Y, Axes.X, Axes.CHANNEL});
//
//		Tiling.TilingAction[] actions = node.getTilingActions();
//
//		System.out.println(Arrays.toString(actions));
//
//		assertEquals(4, actions.length);
//		assertEquals(Tiling.TilingAction.TILE_WITH_PADDING, actions[0]); //x
//		assertEquals(Tiling.TilingAction.TILE_WITH_PADDING, actions[1]); //y
//		assertEquals(Tiling.TilingAction.TILE_WITHOUT_PADDING, actions[2]); //t
//		assertEquals(Tiling.TilingAction.NO_TILING, actions[3]); //channel
//
//		final RandomAccessibleInterval<FloatType> input =
//				(RandomAccessibleInterval<FloatType>) dataset.getImgPlus();
//
//		final AdvancedTiledView<FloatType> tiledView = tiling.preprocess(input,
//				datasetAxes, actions, new DefaultTask());
//
//		assertEquals(1, tiledView.dimension(0));
//		assertEquals(1, tiledView.dimension(1));
//		assertEquals(5, tiledView.dimension(2));
//
//		assertEquals(32, tiledView.getBlockSize()[0]);
//		assertEquals(32, tiledView.getBlockSize()[1]);
//		assertEquals(1, tiledView.getBlockSize()[2]);
//
//		assertEquals(0, tiledView.getOverlap()[0]);
//		assertEquals(0, tiledView.getOverlap()[1]);
//		assertEquals(0, tiledView.getOverlap()[2]);
//
//		tiledView.dispose();
////	}
//
//	private AdvancedTiledView<FloatType> runTiling(long[] datasetSize, AxisType[] axes, Tiling tiling, Tiling.TilingAction[] actions) {
//
//		createImageJ();
//
//		final Dataset dataset = ij.dataset().create(new FloatType(), datasetSize,
//				"", axes);
//		final RandomAccessibleInterval<FloatType> input =
//				(RandomAccessibleInterval<FloatType>) dataset.getImgPlus();
//
//		return (AdvancedTiledView<FloatType>) tiling.preprocess(input,
//				axes, actions, new DefaultTask());
//
//	}

	private Tiling.TilingAction[] getTilingActions(Dataset input) {
		Tiling.TilingAction[] actions = new Tiling.TilingAction[input
				.numDimensions()];
		Arrays.fill(actions, Tiling.TilingAction.NO_TILING);
		for (int i = 0; i < actions.length; i++) {
			AxisType type = input.axis(i).type();
			if (type.isSpatial()) {
				actions[i] = Tiling.TilingAction.TILE_WITH_PADDING;
			}
		}
		return actions;
	}

}
