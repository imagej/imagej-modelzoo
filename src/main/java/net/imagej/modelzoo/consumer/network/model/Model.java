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

package net.imagej.modelzoo.consumer.network.model;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.Callable;

import net.imglib2.TiledView;
import org.scijava.Cancelable;
import org.scijava.Disposable;

import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public interface Model<T extends RealType<T>> extends
	Callable<List<RandomAccessibleInterval<T>>>, Disposable, Cancelable
{

	boolean loadModel(String pathOrURL, String modelName)
		throws FileNotFoundException;

	void preprocess();

	ImageTensor getInputNode();

	ImageTensor getOutputNode();

	void loadInputNode(Dataset dataset);

	void loadOutputNode(Dataset dataset);

	void initMapping();

	boolean isInitialized();

	void resetTileCount();

	void setTiledView(TiledView<T> tiledView);

	default void loadLibrary(){}

	void calculateMapping();

	boolean libraryLoaded();

	List<Integer> dropSingletonDims();
}
