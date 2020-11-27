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

import io.bioimage.specification.DefaultInputNodeSpecification;
import io.bioimage.specification.DefaultOutputNodeSpecification;
import io.bioimage.specification.OutputNodeSpecification;
import org.scijava.log.LogService;
import org.tensorflow.framework.SignatureDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class TensorFlowUtils {

	public static TensorFlowModelSpecification guessSpecification(SignatureDef sig, int networkDepth, int kernelSize) {
		return guessSpecification(sig, networkDepth, kernelSize, 2);
	}

	public static TensorFlowModelSpecification guessSpecification(SignatureDef sig, int networkDepth, int kernelSize, int poolSize) {
		Integer halo = getHalo(networkDepth, kernelSize, poolSize);
		return guessSpecification(sig, halo == null? 0 : halo);
	}

	public static TensorFlowModelSpecification guessSpecification(SignatureDef sig, int defaultHalo) {
		TensorFlowModelSpecification specification = new TensorFlowModelSpecification();
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
				if(inputSpec.getAxes().substring(i).equals("b")) halo.add(0);
				else if(inputSpec.getAxes().substring(i).equals("c")) halo.add(0);
				else halo.add(defaultHalo);
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
		if(dimCount == 4) return "byxc";
		if(dimCount == 5) return "bzyxc";
		return "bzyxc".substring(0, dimCount);
	}

	private static Integer getHalo(int networkDepth, int kernelSize, int poolSize) {
		switch(kernelSize) {
			case 3:
				switch(poolSize) {
					case 1:	switch (networkDepth) { case 1: return 6;	case 2: return 10;	case 3: return 14;	case 4: return 18;	case 5: return 22;}
					case 2:	switch (networkDepth) { case 1: return 9;	case 2: return 22;	case 3: return 46;	case 4: return 94;	case 5: return 190;}
					case 4:	switch (networkDepth) { case 1: return 14;	case 2: return 58;	case 3: return 234;	case 4: return 938;}
				}
			case 5:
				switch(poolSize) {
					case 1:	switch (networkDepth) {	case 1: return 12;	case 2: return 20;	case 3: return 28;	case 4: return 36;	case 5: return 44;}
					case 2:	switch (networkDepth) {	case 1: return 17;	case 2: return 43;	case 3: return 92;	case 4: return 188;	case 5: return 380;}
					case 4:	switch (networkDepth) {	case 1: return 27;	case 2: return 116;	case 3: return 468;	case 4: return 1876;}
				}
			case 7:
				switch(poolSize) {
					case 1:	switch (networkDepth) {	case 1: return 18;	case 2: return 30;	case 3: return 42;	case 4: return 54;	case 5: return 66;}
					case 2:	switch (networkDepth) {	case 1: return 25;	case 2: return 62;	case 3: return 138;	case 4: return 282;	case 5: return 570;}
					case 4:	switch (networkDepth) {	case 1: return 38;	case 2: return 158;	case 3: return 638;	case 4: return 2558;}
				}
		}
		return null;
	}

}
