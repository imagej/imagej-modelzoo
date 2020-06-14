package net.imagej.modelzoo.consumer;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.network.model.ImageNode;
import net.imagej.modelzoo.consumer.network.model.InputImageNode;
import net.imagej.modelzoo.consumer.network.model.Model;
import net.imagej.modelzoo.consumer.network.model.OutputImageNode;
import net.imagej.modelzoo.consumer.preprocessing.InputHarvestingCommand;
import net.imagej.modelzoo.consumer.preprocessing.InputMappingCommand;
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

public class PredictionDataHandler {

	@Parameter
	private LogService log;

	@Parameter
	private CommandService commandService;

	@Parameter
	private Context context;

	private Model model;
	private boolean success;

	private final Map<String, RandomAccessibleInterval<?>> inputs = new HashMap<>();
	private boolean askUser = true;

	void setupInputsAndOutputs() {
		try {
			runInputHarvesting();
			runInputMapping();
			runOutputMapping();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		success = validateAndFitInputs(model);
	}

	private boolean validateAndFitInputs(final Model model) {
		boolean failed = false;
		for (InputImageNode inputNode : model.getInputNodes()) {
			boolean validInput = inputNode.makeDataFit();
			if(!validInput) {
				failed = true;
			}
		}
		return !failed;
	}

	private void runInputHarvesting() throws InterruptedException, ExecutionException {
		Map<String, Object> inputMap = new HashMap<>(inputs);
		inputMap.put("model", model);
		if(askUser) {
			InputHarvestingCommand harvesting = new InputHarvestingCommand();
			context.inject(harvesting);
			for (InputImageNode inputNode : model.getInputNodes()) {
				harvesting.addInput(inputNode.getName(), inputNode.getDataType());
			}
			//TODO check what is actually supposed to happen here
			commandService.moduleService().run(harvesting, true, inputMap).get();
		} else {
			if(model != null) {
				for (InputImageNode inputNode : model.getInputNodes()) {
					RandomAccessibleInterval data = (RandomAccessibleInterval) inputMap.get(inputNode.getName());
					if(data == null) continue;
					inputNode.setData(data);
				}
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
			if(handleMapping(mapping, node)) {
				mappingCommandNeeded = true;
			}
		}
		if(mappingCommandNeeded) {
			commandService.moduleService().run(mapping, true, "model", model).get();
		}
	}

	private void runOutputMapping() {
		for (OutputImageNode outputNode : model.getOutputNodes()) {
			if(outputNode.getReference() != null) {
				outputNode.setDataMapping(outputNode.getReference().getDataMapping());
			}
		}
	}

	private boolean handleMapping(InputMappingCommand mapping, ImageNode node) {
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

	private ModuleItem<?> createMappingChoice(DynamicCommand command, ImageNode node, RandomAccessibleInterval rai) {
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

	public void addInput(String name, RandomAccessibleInterval<?> value) {
		inputs.put(name, value);
	}

	public void setAskUser(boolean askUser) {
		this.askUser = askUser;
	}

}
