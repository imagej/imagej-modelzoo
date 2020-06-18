package net.imagej.modelzoo.consumer;

import net.imagej.modelzoo.consumer.model.InputImageNode;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.OutputImageNode;
import net.imagej.modelzoo.consumer.tiling.Tiling;
import org.scijava.ItemIO;
import org.scijava.io.location.Location;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OOMThrowingNetwork implements ModelZooModel
{

	@Parameter
	Tiling.TilingAction[] actions;

	@Parameter(type = ItemIO.OUTPUT)
	List nTilesHistory = new ArrayList();

	@Parameter(type = ItemIO.OUTPUT)
	List batchSizeHistory = new ArrayList();

	@Override
	public boolean loadModel(Location location, String modelName) throws FileNotFoundException {
		return true;
	}

	@Override
	public List<InputImageNode<?>> getInputNodes() {
		return Collections.emptyList();
	}

	@Override
	public List<OutputImageNode<?, ?>> getOutputNodes() {
		return Collections.emptyList();
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public boolean libraryLoaded() {
		return true;
	}

	@Override
	public void predict() {
		throw new OutOfMemoryError();
	}
//
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


}
