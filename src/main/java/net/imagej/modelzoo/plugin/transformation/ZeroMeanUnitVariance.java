package net.imagej.modelzoo.plugin.transformation;

import io.bioimage.specification.transformation.preprocessing.ZeroMeanUnitVarianceTransformation;
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.processor.DefaultImageNodeProcessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.plugin.Plugin;


@Plugin(type = NodeProcessor.class, name = ZeroMeanUnitVarianceTransformation.name)
public class ZeroMeanUnitVariance extends DefaultImageNodeProcessor<ZeroMeanUnitVarianceTransformation> {

	private Number mean;
	private Number std;

	@Override
	protected <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> ImageDataReference<FloatType> process(RandomAccessibleInterval<I> in, ImageDataReference<O> outType) {
		Converter<? super I, FloatType> converter = (Converter<I, FloatType>) (input, output)
				-> output.setReal((input.getRealFloat() - mean.floatValue()) / std.floatValue());
		return new DefaultImageDataReference<>(Converters.convert(in, converter, new FloatType()), new FloatType());
	}

	@Override
	public void readSpecification(ZeroMeanUnitVarianceTransformation specification) {
		mean = specification.getMean();
		std = specification.getStd();
	}
}
