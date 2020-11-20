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
package net.imagej.modelzoo.consumer.model.tensorflow;

import net.imagej.modelzoo.specification.DefaultInputNodeSpecification;
import net.imagej.modelzoo.specification.DefaultModelSpecification;
import net.imagej.modelzoo.specification.DefaultOutputNodeSpecification;
import net.imagej.modelzoo.specification.ModelSpecification;
import net.imagej.modelzoo.specification.OutputNodeSpecification;
import org.scijava.log.LogService;
import org.tensorflow.framework.SignatureDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class TensorFlowUtils {

	public static ModelSpecification guessSpecification(LogService log, SignatureDef sig) {
		ModelSpecification specification = new DefaultModelSpecification();
		sig.getInputsMap().forEach((name, tensorInfo) -> {
			DefaultInputNodeSpecification inputSpec = new DefaultInputNodeSpecification();
			inputSpec.setName(tensorInfo.getName().substring(0, tensorInfo.getName().lastIndexOf(":")));
			List<Integer> shapeMin  = new ArrayList<>();
			List<Integer> shapeStep = new ArrayList<>();
			List<Integer> halo = new ArrayList<>();
			inputSpec.setAxes(guessAxes(tensorInfo.getTensorShape().getDimCount()));
			for (int i = 0; i < tensorInfo.getTensorShape().getDimCount(); i++) {
				long size = tensorInfo.getTensorShape().getDim(i).getSize();
				if(size < 0) {
					// variable size
					shapeMin.add(0);
					shapeStep.add(1);
				} else {
					// fixed size
					shapeMin.add((int) size);
					shapeStep.add(0);
				}
				halo.add(0);
			}
			inputSpec.setShapeMin(shapeMin);
			inputSpec.setShapeStep(shapeStep);
			inputSpec.setHalo(halo);
			specification.addInputNode(inputSpec);
		});
		sig.getOutputsMap().forEach((name, tensorInfo) -> {
			OutputNodeSpecification outputSpec = new DefaultOutputNodeSpecification();
			outputSpec.setName(tensorInfo.getName().substring(0, tensorInfo.getName().lastIndexOf(":")));
			outputSpec.setAxes(guessAxes(tensorInfo.getTensorShape().getDimCount()));
			int dimCount = tensorInfo.getTensorShape().getDimCount();
			outputSpec.setShapeOffset(Collections.nCopies(dimCount, 0));
			outputSpec.setShapeScale(Collections.nCopies(dimCount, 1));
			if(sig.getInputsMap().size() == 1 && sig.getOutputsMap().size() == 1) {
				outputSpec.setShapeReferenceInput(specification.getInputs().get(0).getName());
			}
			specification.addOutputNode(outputSpec);
		});
		return specification;
	}

	private static String guessAxes(int dimCount) {
		if(dimCount == 4) return "BXYC";
		if(dimCount == 5) return "BXYZC";
		return "BXYZC".substring(0, dimCount);
	}

}
