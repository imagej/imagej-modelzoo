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
package net.imagej.modelzoo.plugin.transformation.postprocessing;

import io.bioimage.specification.transformation.ScaleLinearTransformation;
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.InputImageNode;
import net.imagej.modelzoo.consumer.model.node.OutputImageNode;
import net.imagej.modelzoo.consumer.model.node.processor.DefaultImageNodePostprocessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodePostprocessor;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

@Plugin(type = NodePostprocessor.class, name = ScaleLinearTransformation.name)
public class ScaleLinearPostprocessing
		extends DefaultImageNodePostprocessor<ScaleLinearTransformation> {

	@Parameter
	private OpService opService;

	@Parameter
	private UIService uiService;

	private Number offset;
	private Number gain;

	@Override
	protected <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> ImageDataReference<O> process(ImageDataReference<I> in, ImageDataReference<O> outType) {
		InputImageNode inputReference = ((OutputImageNode) getImageNode()).getReference();
		O resOutType = outType.getDataType();
		if(inputReference != null && getOptions().convertIntoInputFormat()) {
			resOutType = inputReference.getOriginalDataType();
		}
		RandomAccessibleInterval<O> out;
		if(sameType(in.getDataType(), resOutType)) {
			out = (RandomAccessibleInterval<O>) in.getData();
		} else {
			out = opService.create().img(in.getData(), resOutType);
		}
		O finalResOutType = resOutType;
		LoopBuilder.setImages(in.getData(), out).forEachPixel((i, o) -> {
			double real = i.getRealDouble() * gain.doubleValue() + offset.doubleValue();
			o.setReal(inBounds(real, finalResOutType));
		});

		return new DefaultImageDataReference(out, resOutType);
	}

	private <T extends RealType<T> & NativeType<T>> double inBounds(double value, T resOutType) {
		return Math.min(Math.max(resOutType.getMinValue(), value), resOutType.getMaxValue());
	}

	@Override
	public void readSpecification(ScaleLinearTransformation specification) {
		offset = specification.getOffset();
		gain = specification.getGain();
	}
}
