package net.imagej.modelzoo.consumer.commands.preprocessing;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.network.model.InputNode;
import net.imagej.modelzoo.consumer.network.model.Model;
import net.imagej.modelzoo.consumer.network.model.ModelZooNode;
import net.imglib2.RandomAccessibleInterval;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.module.ModuleItem;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PredictionInputHarvesting implements Runnable {

	@Parameter
	LogService log;

	@Parameter
	CommandService commandService;

	@Parameter
	Context context;

	private Model model;
	private boolean success;

	private final Map<String, Object> inputs = new HashMap<>();

	@Override
	public void run() {
		try {
			runInputHarvesting();
			runInputMapping();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		success = new InputHandler().validateAndFitInputs(model);
	}

	private void runInputHarvesting() throws InterruptedException, ExecutionException {
		Map inputMap = new HashMap(inputs);
		inputMap.put("model", model);
		InputHarvestingCommand harvesting = new InputHarvestingCommand();
		context.inject(harvesting);
		for (InputNode inputNode : model.getInputNodes()) {
			harvesting.addInput(inputNode.getName(), inputNode.getDataType());
		}
		commandService.moduleService().run(harvesting, true, inputMap).get();
	}

	private void runInputMapping() throws InterruptedException, ExecutionException {
		InputMappingCommand mapping = new InputMappingCommand();
		context.inject(mapping);
		addText(mapping, "Inputs");
		boolean mappingCommandNeeded = false;
		for (InputNode node : model.getInputNodes()) {
			// TODO only do this for nodes of type image
			if(handleMapping(mapping, node)) {
				mappingCommandNeeded = true;
			}
		}
		if(mappingCommandNeeded) {
			commandService.moduleService().run(mapping, true, "model", model).get();
		}
	}

	private boolean handleMapping(InputMappingCommand mapping, ModelZooNode node) {
		try {
			RandomAccessibleInterval rai = (RandomAccessibleInterval) node.getData();
			if(rai.numDimensions() > 2) {
				createMappingChoice(mapping, node, rai);
				return true;
			} else {
				node.setDataMapping(get2DMapping());
				return false;
			}
		} catch (ClassCastException ignored) {}
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

	private ModuleItem<?> createMappingChoice(DynamicCommand command, ModelZooNode node, RandomAccessibleInterval rai) {
		String name = node.getName();
		MutableModuleItem<String> moduleItem = command.addInput(name, String.class);
		final List<String> choices = InputMappingCommand.getMappingOptions(rai.numDimensions());
		moduleItem.setChoices(choices);
		moduleItem.setDefaultValue(choices.get(0));
		return moduleItem;
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public boolean getSuccess() {
		return success;
	}

	public void addInput(String name, Object value) {
		inputs.put(name, value);
	}
}
