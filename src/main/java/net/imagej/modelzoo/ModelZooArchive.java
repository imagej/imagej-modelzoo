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
package net.imagej.modelzoo;

import io.scif.MissingLibraryException;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.specification.ModelSpecification;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import org.scijava.io.location.Location;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface ModelZooArchive<TI extends RealType<TI>, TO extends RealType<TO>> {
	/**
	 * @return the source of the archive
	 */
	Location getSource();

	/**
	 * @return the specification of the model
	 */
	ModelSpecification getSpecification();

	/**
	 * @return an exemplary input image
	 */
	RandomAccessibleInterval<TI> getTestInput();

	/**
	 * @return an image matching the prediction of {@link #getTestInput()}
	 */
	RandomAccessibleInterval<TO> getTestOutput();

	/**
	 * @return an instance of the model specified by {@link #getSpecification()} and loaded from {@link #getSource()}
	 * @throws FileNotFoundException in case the model source is not found
	 */
	ModelZooModel createModelInstance() throws Exception;

	void setTestInput(RandomAccessibleInterval<TI> testInput);

	void setTestOutput(RandomAccessibleInterval<TO> testOutput);

	/**
	 * @param path the relative path to the file in the archive
	 * @return a temporary file extracted from the archive and the given path
	 */
	File extract(String path) throws IOException;
}
