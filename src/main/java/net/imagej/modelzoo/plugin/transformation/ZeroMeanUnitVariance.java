package net.imagej.modelzoo.plugin.transformation;

import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.processor.DefaultImageNodeProcessor;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessor;
import net.imagej.modelzoo.specification.transformation.ZeroMeanUnitVarianceTransformation;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Plugin;


@Plugin(type = NodeProcessor.class, name = ZeroMeanUnitVarianceTransformation.name)
public class ZeroMeanUnitVariance extends DefaultImageNodeProcessor<ZeroMeanUnitVarianceTransformation> {

	private Number mean;
	private Number std;

	@Override
	protected <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> ImageDataReference<O> process(RandomAccessibleInterval<I> in, ImageDataReference<O> outType) {
		Converter<? super I, ? super O> converter = (Converter<I, O>) (input, output)
				-> output.setReal((input.getRealFloat() - mean.floatValue()) / std.floatValue());
		return new DefaultImageDataReference<>(Converters.convert(in, converter, outType.getDataType()), outType.getDataType());
	}

	@Override
	public void readSpecification(ZeroMeanUnitVarianceTransformation specification) {
		mean = specification.getMean();
		std = specification.getStd();
	}
}
