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
