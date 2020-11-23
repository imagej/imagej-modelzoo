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
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageNode;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.node.ModelZooNode;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;

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

	private final Map<String, Object> inputs = new HashMap<>();
	private final Map<String, String> mapping = new HashMap<>();

	public void setModel(ModelZooModel model) {
		this.model = model;
		try {
			runInputHarvesting();
			runInputMapping();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			success = false;
			return;
		}
		success = true;
	}

	private void runInputHarvesting() {
		if (model == null) return;
		Map<String, Object> inputMap = new HashMap<>(inputs);
		inputMap.put("model", model);
		for (ModelZooNode inputNode : model.getInputNodes()) {
			Object data = inputMap.get(inputNode.getName());
			data = possiblyWrap(data);
			if(inputNode.accepts(data)) {
				setData(inputNode, data);
			}
		}
	}

	private <T extends RealType<T> & NativeType<T>> Object possiblyWrap(Object data) {
		if(RandomAccessibleInterval.class.isAssignableFrom(data.getClass())) {
			RandomAccessibleInterval<T> rai = (RandomAccessibleInterval<T>) data;
			// RAIs need to be wrapped because an image node might change the data type when being processed
			return new DefaultImageDataReference<>(rai, rai.randomAccess().get().copy());
		}
		return data;
	}

	private <T> void setData(ModelZooNode<T> inputNode, T data) {
		inputNode.setData(data);
	}

	private void runInputMapping() throws InterruptedException, ExecutionException {
		InputMappingCommand mapping = new InputMappingCommand();
		context.inject(mapping);
		addText(mapping, "Inputs");
		boolean mappingCommandNeeded = false;
		for (ModelZooNode<?> node : model.getInputNodes()) {
			// TODO only do this for nodes of type image
			if(isImage(node)) {
				if (guessMappingIfAmbiguous(mapping, (ImageNode) node)) {
					mappingCommandNeeded = true;
				}
			}
		}
		if (mappingCommandNeeded) {
			commandService.moduleService().run(mapping, true, "model", model).get();
		}
	}

	private boolean isImage(ModelZooNode<?> node) {
		return ImageNode.class.isAssignableFrom(node.getClass());
	}

	private boolean guessMappingIfAmbiguous(InputMappingCommand mappingCommand, ImageNode node) {
		try {
			RandomAccessibleInterval rai = node.getData().getData();
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

	private <T extends RealType<T> & NativeType<T>> void addMappingChoice(DynamicCommand command, ImageNode node, RandomAccessibleInterval<T> rai) {
		String name = node.getName();
		MutableModuleItem<String> moduleItem = command.addInput(name, String.class);
		final List<String> choices = InputMappingCommand.getMappingOptions(rai.numDimensions());
		moduleItem.setChoices(choices);
		moduleItem.setDefaultValue(choices.get(0));
	}

	public boolean getSuccess() {
		return success;
	}

	public void addInput(String name, Object value, String mapping) {
		inputs.put(name, value);
		this.mapping.put(name, mapping);
	}

}
