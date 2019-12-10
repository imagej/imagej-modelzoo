//package net.imagej.modelzoo.consumer.commands;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ExecutionException;
//
//import net.imagej.modelzoo.consumer.network.DefaultModelExecutor;
//import net.imagej.modelzoo.consumer.tiling.DefaultInputTiler;
//import org.scijava.ItemIO;
//import org.scijava.command.Command;
//import org.scijava.plugin.Parameter;
//import org.scijava.plugin.Plugin;
//
//import net.imagej.modelzoo.consumer.network.ModelExecutor;
//import net.imagej.modelzoo.consumer.network.model.Model;
//import net.imagej.modelzoo.consumer.task.DefaultTask;
//import net.imagej.modelzoo.consumer.tiling.AdvancedTiledView;
//import net.imagej.modelzoo.consumer.tiling.Tiling;
//import net.imagej.axis.AxisType;
//import net.imglib2.RandomAccessibleInterval;
//
//@Plugin(type = Command.class)
//public class OOMThrowingNetwork extends ModelZooPrediction
//{
//
//	@Parameter
//	Tiling.TilingAction[] actions;
//
//	@Parameter(type = ItemIO.OUTPUT)
//	List nTilesHistory = new ArrayList();
//
//	@Parameter(type = ItemIO.OUTPUT)
//	List batchSizeHistory = new ArrayList();
//
//	private class OOMThrowingModelExecutor extends DefaultTask implements ModelExecutor {
//		@Override
//		public List<AdvancedTiledView> run(List input, Model network) {
//			throw new OutOfMemoryError();
//		}
//	}
//
//	@Override
//	protected ModelExecutor initModelExecutor() {
//		return new OOMThrowingModelExecutor();
//	}
//
//	@Override
//	public void run() throws OutOfMemoryError {
//
//		nTilesHistory.clear();
//		batchSizeHistory.clear();
//
//		inputTiler = new DefaultInputTiler();
//		modelExecutor = new DefaultModelExecutor();
//		initTiling();
//
//		List list = new ArrayList();
//		list.add(getInput().getImgPlus());
//
//		try {
//			tryToTileAndRunNetwork(list);
//		}
//		catch(OutOfMemoryError e) {
//		} catch (ExecutionException e) {
//			e.printStackTrace();
//		}
//
//	}
//
//	@Override
//	protected List tileAndRunNetwork(List<RandomAccessibleInterval> input) throws ExecutionException {
//		AxisType[] finalInputAxes =  new AxisType[getInput().numDimensions()];
//		Tiling.TilingAction[] tilingActions = actions;
//		dummyTiling(finalInputAxes);
//		final List<AdvancedTiledView> tiledInput = inputTiler.run(
//				input, finalInputAxes, tiling, tilingActions);
//		nTiles = tiling.getTilesNum();
//		if(tiledInput == null) return null;
//		return modelExecutor.run(tiledInput, model);
//	}
//
//
//	public void dummyTiling(final AxisType[] finalInputAxes) {
//		for (int i = 0; i < input.numDimensions(); i++) {
//			finalInputAxes[i] = input.axis(i).type();
//		}
//	}
//
//	@Override
//	protected void handleOutOfMemoryError() {
//		nTilesHistory.add(nTiles);
//		batchSizeHistory.add(batchSize);
//		super.handleOutOfMemoryError();
//	}
//
//
//}
