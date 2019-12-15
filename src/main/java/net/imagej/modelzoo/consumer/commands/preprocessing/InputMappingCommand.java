package net.imagej.modelzoo.consumer.commands.preprocessing;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.modelzoo.consumer.network.model.InputNode;
import net.imagej.modelzoo.consumer.network.model.Model;
import net.imagej.modelzoo.consumer.network.model.OutputNode;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin(type = Command.class, name="input mapping")
public class InputMappingCommand extends DynamicCommand {

	@Parameter
	Model model;

	private static final Map<Character, AxisType> axesMap = Collections.unmodifiableMap(new HashMap<Character, AxisType>() {
		{
			put('X', Axes.X);
			put('Y', Axes.Y);
			put('Z', Axes.Z);
			put('T', Axes.TIME);
			put('C', Axes.CHANNEL);
		}
	});

	@Override
	public void run() {
		if(model != null) {
			for (InputNode inputNode : model.getInputNodes()) {
				Object val = getInput(inputNode.getName());
				if(val == null) continue;
				System.out.println(inputNode.getName() + " mapping: " + val);
				List<AxisType> axisTypes = parseMappingStr((String) val);
				inputNode.setDataMapping(axisTypes);
			}
			for (OutputNode outputNode : model.getOutputNodes()) {
				if(outputNode.getReference() != null) {
					outputNode.setDataMapping(outputNode.getReference().getDataMapping());
				}
			}
		}
	}

	public static List<String> getMappingOptions(int numDimensions) {
		List<String> res = new ArrayList<>();
		if(numDimensions == 3) {
			res.add("XYC");
			res.add("XYZ");
			res.add("XYT");
		}
		if(numDimensions == 4) {
			res.add("XYZC");
			res.add("XYZT");
			res.add("XYTC");
			res.add("XYCT");
		}
		return res;
	}

	public static List<AxisType> parseMappingStr(String mappingStr) {
		List<AxisType> mapping = new ArrayList<>();
		for(int i = 0; i < mappingStr.length(); i++) {
			mapping.add(axesMap.get(mappingStr.charAt(i)));
		}
		return mapping;
	}

}
