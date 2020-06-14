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

import net.imagej.modelzoo.consumer.util.IOHelper;
import org.scijava.io.location.Location;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public abstract class DefaultModel implements Model {

	protected final List<InputImageNode<?>> inputNodes = new ArrayList<>();
	protected final List<OutputImageNode<?, ?>> outputNodes = new ArrayList<>();

	protected DefaultModel() {
	}

	protected abstract boolean loadModel(Location source, String modelName);

	@Override
	public boolean loadModel(final String pathOrURL, final String modelName)
		throws FileNotFoundException
	{

		final Location source = IOHelper.loadFileOrURL(pathOrURL);
		return loadModel(source, modelName);

	}

	@Override
	public List<InputImageNode<?>> getInputNodes() {
		return inputNodes;
	}

	@Override
	public List<OutputImageNode<?, ?>> getOutputNodes() {
		return outputNodes;
	}

	@Override
	public abstract boolean isInitialized();

	public void clear() {
		inputNodes.clear();
		outputNodes.clear();
	}
}
