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

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.modelzoo.consumer.DefaultModelZooPrediction;
import net.imagej.modelzoo.consumer.model.prediction.ImageInput;
import net.imagej.modelzoo.consumer.ModelZooPrediction;
import net.imagej.modelzoo.consumer.ModelZooPredictionOptions;
import net.imagej.modelzoo.consumer.model.prediction.PredictionInput;
import net.imagej.modelzoo.consumer.model.prediction.PredictionOutput;
import net.imagej.modelzoo.consumer.commands.DefaultModelZooBatchPredictionCommand;
import net.imagej.modelzoo.consumer.commands.DefaultSingleImagePredictionCommand;
import net.imagej.modelzoo.consumer.commands.SingleImagePredictionCommand;
import net.imagej.modelzoo.consumer.sanitycheck.DefaultModelZooSanityCheckFromFileCommand;
import net.imagej.modelzoo.consumer.sanitycheck.DefaultModelZooSanityCheckFromImageCommand;
import net.imagej.modelzoo.specification.ConfigSpecification;
import net.imagej.modelzoo.specification.ModelSpecification;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.Context;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.UIService;

import java.io.File;
import java.util.Collections;
import java.util.List;

@Plugin(type = Service.class)
public class DefaultModelZooService extends AbstractService implements ModelZooService {

	@Parameter
	private Context context;

	@Parameter
	private PluginService pluginService;

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private CommandService commandService;

	@Parameter
	private UIService uiService;

	@Parameter
	private ModelZooIOService io;

	private final static String modelFileParameter = "modelFile";

	private final static String predictionCommandParameter = "prediction";
	private final static String inputParameter = "input";
	private final static String groundTruthParameter = "inputGroundTruth";
	private final static String outputParameter = "output";

	@Override
	public ModelZooIOService io() {
		return io;
	}

	@Override
	public boolean canRunPredictionInteractive(ModelZooArchive trainedModel) {
		CommandInfo predictionCommand = null;
		try {
			predictionCommand = getCommandInfo(trainedModel.getSpecification(), SingleImagePredictionCommand.class);
		} catch (ModuleException e) {
			return false;
		}
		return predictionCommand != null;
	}

	@Override
	public boolean canRunSanityCheckInteractive(ModelZooArchive trainedModel) {
		CommandInfo predictionCommand = null;
		try {
			predictionCommand = getCommandInfo(trainedModel.getSpecification(), SingleImagePredictionCommand.class);
		} catch (ModuleException e) {
			return false;
		}
		ModelZooPrediction prediction = getPrediction(trainedModel);
		return predictionCommand != null && prediction.getSanityCheck() != null;
	}

	@Override
	public ModelZooPredictionOptions createOptions() {
		return ModelZooPredictionOptions.options();
	}

	@Override
	public PredictionOutput predict(ModelZooArchive trainedModel, List<PredictionInput> inputs, ModelZooPredictionOptions options) throws Exception {
		ModelZooPrediction prediction = getPrediction(trainedModel, options);
		if (prediction == null) return null;
		prediction.setTrainedModel(trainedModel);
		prediction.addInputs(inputs);
		prediction.run();
		return prediction.getOutput();
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> PredictionOutput predict(ModelZooArchive trainedModel, RandomAccessibleInterval<T> input, String axes, ModelZooPredictionOptions options) throws Exception {
		String inputName = trainedModel.getSpecification().getInputs().get(0).getName();
		ImageInput<T> imageInput = new ImageInput<>(inputName, input, axes);
		return predict(trainedModel, Collections.singletonList(imageInput), options);
	}

	@Override
	public ModelZooPrediction getPrediction(ModelZooArchive trainedModel, ModelZooPredictionOptions options) {
		String runner = null;
		ConfigSpecification config = trainedModel.getSpecification().getConfig();
		if(config != null) runner = config.getRunner();
		ModelZooPrediction prediction = null;
		if(runner == null) {
			prediction = new DefaultModelZooPrediction(getContext());
		} else {
			List<PluginInfo<ModelZooPrediction>> predictionCommands = pluginService.getPluginsOfType(ModelZooPrediction.class);
			for (PluginInfo<ModelZooPrediction> command : predictionCommands) {
				if(command.getAnnotation().name().equals(runner)) {
					prediction = pluginService.createInstance(command);
				}
			}
			if(prediction == null) {
				log().error("Could not find prediction plugin for model runner " + runner + ".");
				return null;
			}
		}
		prediction.setOptions(options);
		return prediction;
	}

	@Override
	public void predictInteractive(ModelZooArchive trainedModel) throws ModuleException {
		Module mycommand = getModule(trainedModel.getSpecification(), SingleImagePredictionCommand.class);
		if (mycommand == null) return;
		File value = new File(trainedModel.getLocation().getURI());
		mycommand.setInput(modelFileParameter, value);
		mycommand.resolveInput(modelFileParameter);
		commandService.moduleService().run(mycommand, true);
	}

	private <P extends SciJavaPlugin, T extends Class<P>> Module getModule(ModelSpecification specification, T predictionClass) throws ModuleException {
		List<PluginInfo<P>> predictionCommands = pluginService.getPluginsOfType(predictionClass);
		String archivePrediction = specification.getSource();
		Module mycommand = null;
		if(archivePrediction != null) {
			for (PluginInfo<P> command : predictionCommands) {
				if (command.getAnnotation().name().equals(archivePrediction)) {
					CommandInfo commandInfo = commandService.getCommand(command.getClassName());
					mycommand = commandInfo.createModule();
				}
			}
		} else {
			mycommand = commandService.getCommand(DefaultSingleImagePredictionCommand.class).createModule();
		}
		if (mycommand == null) {
			uiService.showDialog("Could not find suitable prediction handler for source " + archivePrediction + ".", DialogPrompt.MessageType.ERROR_MESSAGE);
			return null;
		}
		context.inject(mycommand);
		return mycommand;
	}

	private <P extends SciJavaPlugin, T extends Class<P>> CommandInfo getCommandInfo(ModelSpecification specification, T predictionClass) throws ModuleException {
		List<PluginInfo<P>> predictionCommands = pluginService.getPluginsOfType(predictionClass);
		String archivePrediction = specification.getSource();
		CommandInfo mycommand = null;
		if(archivePrediction != null) {
			for (PluginInfo<P> command : predictionCommands) {
				if (command.getAnnotation().name().equals(archivePrediction)) {
					mycommand = commandService.getCommand(command.getClassName());
				}
			}
		} else {
			mycommand = commandService.getCommand(DefaultSingleImagePredictionCommand.class);
		}
		if (mycommand == null) {
			uiService.showDialog("Could not find suitable prediction handler for source " + archivePrediction + ".", DialogPrompt.MessageType.ERROR_MESSAGE);
			return null;
		}
		context.inject(mycommand);
		return mycommand;
	}

	@Override
	public void batchPredictInteractive(ModelZooArchive trainedModel) throws ModuleException {
		Module predictionCommand = getModule(trainedModel.getSpecification(), SingleImagePredictionCommand.class);
		if (predictionCommand == null) return;
		File value = new File(trainedModel.getLocation().getURI());
		predictionCommand.setInput(modelFileParameter, value);
		predictionCommand.resolveInput(modelFileParameter);
		predictionCommand.resolveOutput(outputParameter);
		commandService.run(DefaultModelZooBatchPredictionCommand.class, true, modelFileParameter, value, predictionCommandParameter, predictionCommand);
	}

	@Override
	public void sanityCheckFromFilesInteractive(ModelZooArchive model) throws ModuleException {
		sanityCheck(model, DefaultModelZooSanityCheckFromFileCommand.class);
	}

	@Override
	public void sanityCheckFromImagesInteractive(ModelZooArchive model) throws ModuleException {
		sanityCheck(model, DefaultModelZooSanityCheckFromImageCommand.class);
	}

	private void sanityCheck(ModelZooArchive model, Class commandClass) throws ModuleException {
		CommandInfo predictionCommand = getCommandInfo(model.getSpecification(), SingleImagePredictionCommand.class);
		if (predictionCommand == null) return;
		File value = new File(model.getLocation().getURI());
		Module module = commandService.moduleService().createModule(predictionCommand);
		module.setInput(modelFileParameter, value);
		module.resolveInput(modelFileParameter);
		module.resolveOutput(outputParameter);
		commandService.run(commandClass, true,
				modelFileParameter, value, predictionCommandParameter, module);
	}

	@Override
	public void sanityCheckInteractive(ModelZooArchive model, RandomAccessibleInterval input, RandomAccessibleInterval groundTruth) throws ModuleException {
		CommandInfo predictionCommand = getCommandInfo(model.getSpecification(), SingleImagePredictionCommand.class);
		if (predictionCommand == null) return;
		File value = new File(model.getLocation().getURI());
		Module module = commandService.moduleService().createModule(predictionCommand);
		module.setInput(modelFileParameter, value);
		module.resolveInput(modelFileParameter);
		module.resolveOutput(outputParameter);
		if(!(input instanceof Dataset)) {
			input = datasetService.create(input);
		}
		commandService.run(DefaultModelZooSanityCheckFromImageCommand.class, true,
				modelFileParameter, value,
				inputParameter, input,
				groundTruthParameter, groundTruth,
				predictionCommandParameter, module);
	}
}
