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
	public void loadModel(Location location, String modelName) {
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
