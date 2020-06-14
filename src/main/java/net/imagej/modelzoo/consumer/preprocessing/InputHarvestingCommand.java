package net.imagej.modelzoo.consumer.preprocessing;

import net.imagej.modelzoo.consumer.network.model.InputImageNode;
import net.imagej.modelzoo.consumer.network.model.Model;
import net.imglib2.RandomAccessibleInterval;
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
			for (InputImageNode<?> inputNode : model.getInputNodes()) {
				RandomAccessibleInterval data = (RandomAccessibleInterval) getInput(inputNode.getName());
				if(data == null) continue;
				inputNode.setData(data);
			}
		}
	}

}
