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

import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.specification.ModelSpecification;
import net.imagej.modelzoo.specification.WeightsSpecification;
import org.apache.commons.compress.utils.FileNameUtils;
import org.jetbrains.annotations.Nullable;
import org.scijava.Context;
import org.scijava.io.location.FileLocation;
import org.scijava.io.location.Location;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

public class DefaultModelZooArchive implements ModelZooArchive {

	@Parameter
	private PluginService pluginService;

	@Parameter
	private Context context;

	@Parameter
	private LogService logService;

	private Location source;
	private ModelSpecification specification;
	private List<TensorSample> sampleInputs;
	private List<TensorSample> sampleOutputs;

	@Override
	public Location getLocation() {
		return source;
	}

	@Override
	public ModelSpecification getSpecification() {
		return specification;
	}

	@Override
	public List<TensorSample> getSampleInputs() {
		return sampleInputs;
	}

	@Override
	public List<TensorSample> getSampleOutputs() {
		return sampleOutputs;
	}

	@Override
	public ModelZooModel createModelInstance() throws Exception {
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
			Location weightsSource = getCompatibleWeight(model);
			model.loadModel(weightsSource, getNameWithTimeStamp(), getSpecification());
			cleanup(weightsSource);
		} else {
			logService.error("Could not find a plugin matching the model framework " + specification.getFramework());
		}
		return model;
	}

	private void cleanup(Location weightsSource) {
		// in case the weights where extracted from the model,
		//  the temporary weights file can be deleted after loading the model
		if(!weightsSource.equals(source)) {
			if(weightsSource instanceof FileLocation) {
				((FileLocation) weightsSource).getFile().delete();
			}
		}
	}

	@Nullable
	private Location getCompatibleWeight(ModelZooModel model) throws IOException {
		for (WeightsSpecification weight : specification.getWeights()) {
			if(model.getSupportedWeights().contains(weight.getId())) {
				String source = weight.getSource();
				if(source == null) {
					// older specs did not zip the weights
					return this.source;
				}
				File weightsFile = extract(source);
				return new FileLocation(weightsFile);
			}
		}
		return null;
	}

	private String getNameWithTimeStamp() {
		String timeString = "";
		try {
			Instant lastChanged = Files.getLastModifiedTime(new File(getLocation().getURI()).toPath()).toInstant();
			Timestamp timestamp = Timestamp.from(lastChanged);
			timeString = "_" + timestamp.getTime();
		} catch (IOException e) {
			//e.printStackTrace();
		}
		String res = specification.getName() + timeString;
		res = res.replace(":", "-");
		res = res.replace(" ", "_");
		return res;
	}

	@Override
	public void setLocation(Location source) {
		this.source = source;
	}

	public void setSpecification(ModelSpecification specification) {
		this.specification = specification;
	}

	@Override
	public void setSampleInputs(List sampleInputs) {
		this.sampleInputs = sampleInputs;
	}

	@Override
	public void setSampleOutputs(List sampleOutputs) {
		this.sampleOutputs = sampleOutputs;
	}

	@Override
	public File extract(String path) throws IOException {
		try (FileSystem fileSystem = FileSystems.newFileSystem(new File(getLocation().getURI()).toPath(), null)) {
			Path fileToExtract = fileSystem.getPath(path);
			String base = FileNameUtils.getBaseName(path);
			String extension = "." + FileNameUtils.getExtension(path);
			Path res = Files.createTempFile(base, extension);
			Files.copy(fileToExtract, res, StandardCopyOption.REPLACE_EXISTING);
			return res.toFile();
		}
	}
}
