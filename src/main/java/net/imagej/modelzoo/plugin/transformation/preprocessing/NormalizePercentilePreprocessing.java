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
package net.imagej.modelzoo.plugin.transformation.preprocessing;

import io.bioimage.specification.transformation.PercentileTransformation;
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.processor.DefaultImageNodePreprocessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodePreprocessor;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = NodePreprocessor.class, name = PercentileTransformation.name)
public class NormalizePercentilePreprocessing
		extends DefaultImageNodePreprocessor<PercentileTransformation> {

	@Parameter
	private OpService opService;

	private float minPercentile;
	private float maxPercentile;

	private float minVal;

	private float minDestVal = 0;
	private float maxDestVal = 1;

	protected float factor;

	private boolean clip = false;

	@Override
	protected <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> ImageDataReference<FloatType> process(ImageDataReference<I> in, ImageDataReference<O> outType) {

		float[] resValues = computePercentiles(in.getData(), new float[]{minPercentile, maxPercentile});
		if(resValues[1] - resValues[0] < 0.0000001) factor = 1;
		else factor = (maxDestVal - minDestVal) / (resValues[1] - resValues[0]);
		minVal = resValues[0];

		Converter<? super I, FloatType> converter = (input, output) -> {
			output.set(normalize(input, minDestVal, maxDestVal));
		};
		RandomAccessibleInterval<FloatType> converted = Converters.convert(in.getData(), converter, new FloatType());
		return new DefaultImageDataReference<>(converted, new FloatType());
	}

	private <T extends RealType<T>> float normalize( final T val, float min, float max ) {
		if ( clip ) { return Math.max(
				min,
				Math.min( max, ( val.getRealFloat() - minVal ) * factor + min ) ); }
		return Math.max( 0, ( val.getRealFloat() - minVal ) * factor + min );
	}

	public <T extends RealType<T>> float[] computePercentiles(RandomAccessibleInterval<T> src, float[] percentiles) {
		final Cursor< T > cursor = ((IterableInterval)src).cursor();
		int items = 1;
		int i = 0;
		for ( ; i < src.numDimensions(); i++ ) {
			items *= src.dimension( i );
		}
		final float[] values = new float[ items ];
		i = 0;
		while ( cursor.hasNext() ) {
			cursor.fwd();
			values[ i ] = cursor.get().getRealFloat();
			i++;
		}

		Util.quicksort( values );

		final float[] res = new float[ percentiles.length ];
		for ( i = 0; i < percentiles.length; i++ ) {
			res[ i ] = values[ Math.min(
					values.length - 1,
					Math.max( 0, Math.round( ( values.length - 1 ) * percentiles[ i ] / 100.f ) ) ) ];
		}

		return res;
	}

	@Override
	public void readSpecification(PercentileTransformation specification) {
		minPercentile = specification.getMinPercentile().floatValue();
		maxPercentile = specification.getMaxPercentile().floatValue();
	}

	public Number getMinVal() {
		return minVal;
	}

	public Number getFactor() {
		return factor;
	}

	public Number getMinPercentile() {
		return minPercentile;
	}

	public Number getMaxPercentile() {
		return maxPercentile;
	}
}
