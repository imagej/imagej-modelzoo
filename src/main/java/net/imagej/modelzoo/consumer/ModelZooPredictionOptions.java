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

import net.imglib2.cache.img.DiskCachedCellImgOptions;
import org.scijava.optional.AbstractOptions;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ModelZooPredictionOptions extends AbstractOptions<ModelZooPredictionOptions> {

	public final Values values = new Values();
	private static final String batchSizeKey = "batchSize";
	private static final String numberOfTilesKey = "numberOfTiles";
	private static final String tilingEnabledKey = "tilingEnabled";
	private static final String cacheDirectoryKey = "cacheDirectory";

	/**
	 * @return Default {@link ModelZooPredictionOptions} instance
	 */
	public static ModelZooPredictionOptions options()
	{
		return new ModelZooPredictionOptions();
	}

	/**
	 * @param batchSize Size of a batch processed at once
	 */
	public ModelZooPredictionOptions batchSize(int batchSize) {
		return setValue(batchSizeKey, batchSize);
	}

	/**
	 * @param numberOfTiles How many tiles each batch of the input image should be split into
	 */
	public ModelZooPredictionOptions numberOfTiles(int numberOfTiles) {
		return setValue(numberOfTilesKey, numberOfTiles);
	}

	/**
	 * @param enabled Whether tiling is enabled
	 */
	public ModelZooPredictionOptions tilingEnabled(boolean enabled) {
		return setValue(tilingEnabledKey, enabled);
	}

	/**
	 * see (@link {@link DiskCachedCellImgOptions#cacheDirectory(Path)}
	 */
	public ModelZooPredictionOptions cacheDirectory(Path cacheDirectory) {
		return setValue(cacheDirectoryKey, cacheDirectory);
	}

	/**
	 * see (@link {@link DiskCachedCellImgOptions#cacheDirectory(Path)}
	 */
	public ModelZooPredictionOptions cacheDirectory(String cacheDirectory) {
		return setValue(cacheDirectoryKey, Paths.get(cacheDirectory));
	}

	public class Values extends AbstractValues {
		/**
		 * @return Size of a batch processed at once
		 */
		public int batchSize() {
			return getValueOrDefault(batchSizeKey, 10);
		}
		/**
		 * @return How many tiles each batch of the input image should be split into
		 */
		public int numberOfTiles() {
			return getValueOrDefault(numberOfTilesKey, 1);
		}
		/**
		 * @return Whether tiling is enabled
		 */
		public boolean tilingEnabled() {
			return getValueOrDefault(tilingEnabledKey, true);
		}
		/**
		 * @return see {@link DiskCachedCellImgOptions.Values#cacheDirectory()}
		 */
		public Path cacheDirectory() {
			return getValueOrDefault(cacheDirectoryKey, null);
		}
	}
}
