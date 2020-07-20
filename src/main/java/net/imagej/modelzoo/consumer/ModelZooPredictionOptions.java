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
