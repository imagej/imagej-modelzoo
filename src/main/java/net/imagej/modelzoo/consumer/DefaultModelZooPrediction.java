/*-
 * #%L
 * This is the bioimage.io modelzoo library for ImageJ.
 * %%
 * Copyright (C) 2019 - 2021 Center for Systems Biology Dresden
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
package net.imagej.modelzoo.consumer;

import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.consumer.model.ModelZooModel;
import net.imagej.modelzoo.consumer.model.node.ImageNode;
import net.imagej.modelzoo.consumer.model.node.ModelZooNode;
import net.imagej.modelzoo.consumer.model.prediction.DefaultPredictionOutput;
import net.imagej.modelzoo.consumer.model.prediction.ImageInput;
import net.imagej.modelzoo.consumer.sanitycheck.ImageToImageSanityCheck;
import net.imagej.modelzoo.consumer.sanitycheck.SanityCheck;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.Context;

public class DefaultModelZooPrediction extends AbstractModelZooPrediction<ImageInput<?>, DefaultPredictionOutput> implements SingleImagePrediction<DefaultPredictionOutput> {

	public DefaultModelZooPrediction() {
	}

	public DefaultModelZooPrediction(Context context) {
		super(context);
	}

	@Override
	protected DefaultPredictionOutput createOutput(ModelZooModel model) {
		DefaultPredictionOutput outputs = new DefaultPredictionOutput();
		model.getOutputNodes().forEach(node -> {
			outputs.put(legacyRenaming(node.getName()), asOutput(node));
		});
		return outputs;
	}

	public static String legacyRenaming(String name) {
		if(name.equals("activation_11/Identity")) return "output";
		if(name.equals("out_segment/strided_slice")) return "output_segment";
		if(name.equals("out_denoise/strided_slice")) return "output_denoise";
		return name;
	}

	private Object asOutput(ModelZooNode<?> node) {
		Object data;
		if(ImageNode.class.isAssignableFrom(node.getClass())) {
			data = datasetService().create(((ImageNode) node).getData().getData());
		} else {
			data = node.getData();
		}
		return data;
	}

	public <T extends RealType<T> & NativeType<T>> void setInput(RandomAccessibleInterval<T> image, String axes) {
		setInput(new ImageInput<>("input", image, axes));
	}

	public <T extends RealType<T> & NativeType<T>> void setInput(String name, RandomAccessibleInterval<T> image, String axes) {
		setInput(new ImageInput<>(name, image, axes));
	}

	@Override
	public SanityCheck getSanityCheck() {
		return new ImageToImageSanityCheck(context());
	}

	@Override
	public boolean canRunSanityCheck(ModelZooArchive trainedModel) {
		return trainedModel.getSpecification().getOutputs().size() == 1
				&& trainedModel.getSpecification().getInputs().size() == 1
				&& trainedModel.getSpecification().getInputs().get(0).getDataType().equals(
						trainedModel.getSpecification().getOutputs().get(0).getDataType());
	}

}
