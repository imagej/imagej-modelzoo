package net.imagej.modelzoo.consumer.commands.preprocessing;

import net.imagej.modelzoo.consumer.network.model.InputNode;
import net.imagej.modelzoo.consumer.network.model.Model;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, name="input harvesting")
public class InputHarvestingCommand extends DynamicCommand {

	@Parameter
	Model model;

	@Override
	public void run() {
		if(model != null) {
			for (InputNode inputNode : model.getInputNodes()) {
				Object data = getInput(inputNode.getName());
				if(data == null) continue;
				inputNode.setData(data);
			}
		}
	}

}
