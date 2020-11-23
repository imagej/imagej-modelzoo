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
import net.imagej.modelzoo.consumer.model.node.ImageNode;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.node.ModelZooNode;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin(type = Command.class, name = "input mapping")
class InputMappingCommand extends DynamicCommand {

	@Parameter
	private ModelZooModel model;

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
		if (model != null) {
			for (ModelZooNode inputNode : model.getInputNodes()) {
				Object val = getInput(inputNode.getName());
				if (val == null) continue;
				System.out.println(inputNode.getName() + " mapping: " + val);
				List<AxisType> axisTypes = parseMappingStr((String) val);
				ImageNode imageNode = (ImageNode) inputNode;
				imageNode.setDataMapping(axisTypes);
			}
		}
	}

	public static List<String> getMappingOptions(int numDimensions) {
		List<String> res = new ArrayList<>();
		if (numDimensions == 3) {
			res.add("XYC");
			res.add("XYZ");
			res.add("XYT");
		}
		if (numDimensions == 4) {
			res.add("XYZC");
			res.add("XYZT");
			res.add("XYTC");
			res.add("XYCT");
		}
		return res;
	}

	static List<AxisType> parseMappingStr(String mappingStr) {
		List<AxisType> mapping = new ArrayList<>();
		for (int i = 0; i < mappingStr.length(); i++) {
			mapping.add(axesMap.get(mappingStr.charAt(i)));
		}
		return mapping;
	}

}
