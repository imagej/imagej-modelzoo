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
package net.imagej.modelzoo.consumer.preprocessing;

import net.imagej.modelzoo.consumer.ModelZooPredictionOptions;
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageNode;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessorException;
import io.bioimage.specification.TransformationSpecification;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealDoubleConverter;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

public class InputImageConverterProcessor<TO extends RealType<TO> & NativeType<TO>> implements NodeProcessor<TransformationSpecification> {
	private final TO dataType;
	private ImageNode node;

	public InputImageConverterProcessor(ImageNode node, TO dataType) {
		this.node = node;
		this.dataType = dataType;
	}

	@Override
	public void run(ModelZooPredictionOptions.Values options) throws NodeProcessorException {
		RandomAccessibleInterval<?> data = node.getData().getData();
		if(data == null) return;
		if(!dataType.getClass().isAssignableFrom(data.randomAccess().get().getClass())) {
			convert(data);
		}
	}

	private <TI> void convert(RandomAccessibleInterval<TI> data) throws NodeProcessorException {
		Converter<? super TI, ? super TO> converter = null;
		if(FloatType.class.isAssignableFrom(dataType.getClass())) {
			converter = new RealFloatConverter();
		}
		if(DoubleType.class.isAssignableFrom(dataType.getClass())) {
			converter = new RealDoubleConverter();
		}
		if(converter != null) {
			RandomAccessibleInterval<TO> converted = Converters.convert(data, converter, dataType);
			DefaultImageDataReference<TO> reference = new DefaultImageDataReference<>(converted, dataType);
			node.setData(reference);
		} else {
			throw new NodeProcessorException(getClass().getSimpleName() + ": Cannot convert " + data.randomAccess().get().getClass()
					+ " to " + dataType.getClass() + ".");
		}
	}

}
