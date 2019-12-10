/*-
 * #%L
 * ImageJ ModelZoo Consumer
 * %%
 * Copyright (C) 2019 MPI-CBG
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

package net.imagej.modelzoo.consumer.network.model.tensorflow;

import net.imagej.modelzoo.consumer.converter.RealIntConverter;
import net.imagej.tensorflow.Tensors;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.tensorflow.DataType;
import org.tensorflow.Tensor;

public class DatasetTensorFlowConverter {

	public static <T extends RealType<T>>
		RandomAccessibleInterval<T> tensorToDataset(final Tensor tensor,
			final T res, final int[] mapping, final boolean dropSingletonDims)
	{

		final Img outImg;

		if (tensor.dataType().equals(DataType.DOUBLE)) {
			if (res instanceof DoubleType) {
				outImg = Tensors.imgDouble(tensor, mapping);
			}
			else {
				outImg = Tensors.imgDouble(tensor, mapping);
			}
		}
		else if (tensor.dataType().equals(DataType.FLOAT)) {
			if (res instanceof FloatType) {
				outImg = Tensors.imgFloat(tensor, mapping);
			}
			else {
				outImg = Tensors.imgFloat(tensor, mapping);
			}
		}
		else if (tensor.dataType().equals(DataType.INT64)) {
			if (res instanceof LongType) {
				outImg = Tensors.imgLong(tensor, mapping);
			}
			else {
				outImg = Tensors.imgLong(tensor, mapping);
			}
		}
		else if (tensor.dataType().equals(DataType.INT32)) {
			if (res instanceof IntType) {
				outImg = Tensors.imgInt(tensor, mapping);
			}
			else {
				outImg = Tensors.imgInt(tensor, mapping);
			}
		}
		else if (tensor.dataType().equals(DataType.UINT8)) {
			if (res instanceof ByteType) {
				outImg = Tensors.imgByte(tensor, mapping);
			}
			else {
				outImg = Tensors.imgByte(tensor, mapping);
			}
		}
		else {
			outImg = null;
		}

		return dropSingletonDims ? Views.dropSingletonDimensions(outImg) : outImg;
	}

	public static <T extends RealType<T>> Tensor datasetToTensor(
		RandomAccessibleInterval<T> image, final int[] mapping)
	{

		Tensor tensor;
		try {
			tensor = Tensors.tensor(image, mapping);
		}
		catch (IllegalArgumentException e) {
			if (image.randomAccess().get() instanceof UnsignedShortType) {
				tensor = Tensors.tensor(Converters.convert(image,
						new RealIntConverter<>(), new IntType()), mapping);
			}
			else {
				tensor = Tensors.tensor(Converters.convert(image,
						new RealFloatConverter<>(), new FloatType()), mapping);
			}
		}
		return tensor;
	}
}
