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
package net.imagej.modelzoo.consumer.preprocessing;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.model.ImageNode;
import net.imagej.modelzoo.consumer.model.InputImageNode;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.OutputImageNode;
import net.imagej.ops.OpService;
import net.imagej.ops.convert.RealTypeConverter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealDoubleConverter;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.tensorflow.op.core.Real;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class InputMappingHandler {

	@Parameter
	private LogService log;

	@Parameter
	private CommandService commandService;

	@Parameter
	private Context context;

	@Parameter
	private OpService opService;

	private ModelZooModel model;
	private boolean success;

	private final Map<String, RandomAccessibleInterval<?>> inputs = new HashMap<>();
	private final Map<String, String> mapping = new HashMap<>();

	public void setModel(ModelZooModel model) {
		this.model = model;
		try {
			runInputHarvesting();
			runInputMapping();
			runOutputMapping();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		success = validateAndFitInputs(model);
	}

	private boolean validateAndFitInputs(final ModelZooModel model) {
		boolean failed = false;
		for (InputImageNode inputNode : model.getInputNodes()) {
			boolean validInput = inputNode.makeDataFit(log);
			if (!validInput) {
				failed = true;
			}
		}
		return !failed;
	}

	private void runInputHarvesting() {
		if (model == null) return;
		Map<String, Object> inputMap = new HashMap<>(inputs);
		inputMap.put("model", model);
		for (InputImageNode<?> inputNode : model.getInputNodes()) {
			RandomAccessibleInterval data = (RandomAccessibleInterval) inputMap.get(inputNode.getName());
			assignData(inputNode, data);
		}
	}

	private <T extends RealType<T>, U extends RealType<U>> void assignData(InputImageNode<U> inputNode, RandomAccessibleInterval<T> data) {
		if (data == null) return;
		if(inputNode.getDataType().getClass().isAssignableFrom(data.randomAccess().get().getClass())) {
			inputNode.setData((RandomAccessibleInterval<U>) data);
		} else {
			Converter<? super T, ? super U> converter = null;
			if(FloatType.class.isAssignableFrom(inputNode.getDataType().getClass())) {
				converter = new RealFloatConverter();
			}
			if(DoubleType.class.isAssignableFrom(inputNode.getDataType().getClass())) {
				converter = new RealDoubleConverter();
			}
			if(converter != null) {
				inputNode.setData(Converters.convert(data, converter, inputNode.getDataType()));
			} else {
				inputNode.setData((RandomAccessibleInterval<U>) data);
			}
		}
	}

	private void runInputMapping() throws InterruptedException, ExecutionException {
		InputMappingCommand mapping = new InputMappingCommand();
		context.inject(mapping);
		addText(mapping, "Inputs");
		boolean mappingCommandNeeded = false;
		for (InputImageNode node : model.getInputNodes()) {
			// TODO only do this for nodes of type image
			if (handleMapping(mapping, node)) {
				mappingCommandNeeded = true;
			}
		}
		if (mappingCommandNeeded) {
			commandService.moduleService().run(mapping, true, "model", model).get();
		}
	}

	private void runOutputMapping() {
		for (OutputImageNode<?, ?> outputNode : model.getOutputNodes()) {
			if (outputNode.getReference() != null) {
				outputNode.setDataMapping(outputNode.getReference().getDataMapping());
			}
		}
	}

	private boolean handleMapping(InputMappingCommand mappingCommand, ImageNode node) {
		try {
			RandomAccessibleInterval rai = node.getData();
			if (rai.numDimensions() > 2) {
				if (mapping.containsKey(node.getName())) {
					node.setDataMapping(InputMappingCommand.parseMappingStr(mapping.get(node.getName())));
					return false;
				}
				addMappingChoice(mappingCommand, node, rai);
				return true;
			} else {
				node.setDataMapping(get2DMapping());
				return false;
			}
		} catch (ClassCastException ignored) {
		}
		return false;
	}

	private List<AxisType> get2DMapping() {
		List<AxisType> res = new ArrayList<>();
		res.add(Axes.X);
		res.add(Axes.Y);
		return res;
	}

	private void addText(DynamicCommand command, String text) {
		//TODO make work, set style to message somehow
//		MutableModuleItem<String> item = command.addInput(text, String.class);
	}

	private <T extends RealType<T>> void addMappingChoice(DynamicCommand command, ImageNode<T> node, RandomAccessibleInterval<T> rai) {
		String name = node.getName();
		MutableModuleItem<String> moduleItem = command.addInput(name, String.class);
		final List<String> choices = InputMappingCommand.getMappingOptions(rai.numDimensions());
		moduleItem.setChoices(choices);
		moduleItem.setDefaultValue(choices.get(0));
	}

	public boolean getSuccess() {
		return success;
	}

	public void addInput(String name, RandomAccessibleInterval<?> value, String mapping) {
		inputs.put(name, value);
		this.mapping.put(name, mapping);
	}

}
