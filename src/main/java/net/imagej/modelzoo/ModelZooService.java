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

import net.imagej.ImageJService;
import net.imagej.modelzoo.consumer.ModelZooPrediction;
import net.imagej.modelzoo.consumer.ModelZooPredictionOptions;
import net.imagej.modelzoo.specification.ModelSpecification;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import org.scijava.io.location.Location;
import org.scijava.module.ModuleException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ModelZooService extends ImageJService {
	ModelZooArchive open(String location) throws IOException;
	ModelZooArchive open(File location) throws IOException;
	ModelZooArchive open(Location location) throws IOException;
	void save(ModelZooArchive trainedModel, String location) throws IOException;
	void save(ModelZooArchive trainedModel, File location);
	void save(ModelZooArchive trainedModel, Location location);
	void save(String archivePath, ModelSpecification specification, String location) throws IOException;
	boolean canRunPredictionInteractive(ModelZooArchive trainedModel);
	boolean canRunSanityCheckInteractive(ModelZooArchive trainedModel);
	Map<String, Object> predict(ModelZooArchive trainedModel, List<Object> input, List<String> axes) throws Exception;
	<T extends RealType<T>> Map<String, Object> predict(ModelZooArchive trainedModel, RandomAccessibleInterval<T> input, String axes) throws Exception;
	Map<String, Object> predict(ModelZooArchive trainedModel, List<Object> input, List<String> axes, ModelZooPredictionOptions options) throws Exception;
	<T extends RealType<T>> Map<String, Object> predict(ModelZooArchive trainedModel, RandomAccessibleInterval<T> input, String axes, ModelZooPredictionOptions options) throws Exception;
	void predictInteractive(ModelZooArchive trainedModel) throws FileNotFoundException, ModuleException;
	void batchPredictInteractive(ModelZooArchive trainedModel) throws FileNotFoundException, ModuleException;
	void sanityCheckFromFilesInteractive(ModelZooArchive model) throws ModuleException;
	void sanityCheckFromImagesInteractive(ModelZooArchive model) throws ModuleException;
	void sanityCheckInteractive(ModelZooArchive model, RandomAccessibleInterval input, RandomAccessibleInterval groundTruth) throws ModuleException;
	ModelZooPrediction getPrediction(ModelZooArchive model);
}
