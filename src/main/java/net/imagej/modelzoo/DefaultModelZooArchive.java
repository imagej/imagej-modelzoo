package net.imagej.modelzoo;

import io.scif.MissingLibraryException;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.specification.ModelSpecification;
import net.imagej.tensorflow.TensorFlowService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import org.scijava.Context;
import org.scijava.io.location.Location;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;

public class DefaultModelZooArchive<TI extends RealType<TI>, TO extends RealType<TO>> implements ModelZooArchive<TI, TO> {

	@Parameter
	private PluginService pluginService;

	@Parameter
	private TensorFlowService tensorFlowService;

	@Parameter
	private Context context;

	@Parameter
	private LogService logService;

	private Location source;
	private ModelSpecification specification;
	private RandomAccessibleInterval<TI> testInput;

	private RandomAccessibleInterval<TO> testOutput;

	@Override
	public Location getSource() {
		return source;
	}

	@Override
	public ModelSpecification getSpecification() {
		return specification;
	}

	@Override
	public RandomAccessibleInterval<TI> getTestInput() {
		return testInput;
	}

	@Override
	public RandomAccessibleInterval<TO> getTestOutput() {
		return testOutput;
	}

	@Override
	public ModelZooModel createModelInstance() throws FileNotFoundException, MissingLibraryException {
		if(specification == null || source == null) {
			System.out.println("Cannot load model without source and specification");
		}
		List<PluginInfo<ModelZooModel>> modelPlugins = pluginService.getPluginsOfType(ModelZooModel.class);
		ModelZooModel model = null;
		for (PluginInfo<ModelZooModel> pluginInfo : modelPlugins) {
			if(pluginInfo.getAnnotation().name().equals(specification.getFramework())) {
				model = pluginService.createInstance(pluginInfo);
			}
		}
		if(model != null) {
			model.loadLibrary();
			model.loadModel(source, getNameWithTimeStamp());
		} else {
			logService.error("Could not find a plugin matching the model framework " + specification.getFramework());
		}
		return model;
	}

	private String getNameWithTimeStamp() {
		String timeString = "";
		try {
			Instant lastChanged = Files.getLastModifiedTime(new File(getSource().getURI()).toPath()).toInstant();
			timeString = " " + lastChanged.toString();
		} catch (IOException e) {
			//e.printStackTrace();
		}
		return specification.getName() + timeString;
	}

	@Override
	public void clearCache() {
		//TODO implement this
		// tensorFlowService.clearCache(specification.getName());
	}

	public void setSource(Location source) {
		this.source = source;
	}

	public void setSpecification(ModelSpecification specification) {
		this.specification = specification;
	}

	@Override
	public void setTestInput(RandomAccessibleInterval<TI> testInput) {
		this.testInput = testInput;
	}

	@Override
	public void setTestOutput(RandomAccessibleInterval<TO> testOutput) {
		this.testOutput = testOutput;
	}
}
