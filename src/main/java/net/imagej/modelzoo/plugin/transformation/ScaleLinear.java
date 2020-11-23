package net.imagej.modelzoo.plugin.transformation;

import net.imagej.modelzoo.consumer.model.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.DefaultImageNodeProcessor;
import net.imagej.modelzoo.consumer.model.ImageDataReference;
import net.imagej.modelzoo.consumer.model.NodeProcessor;
import net.imagej.modelzoo.specification.transformation.ScaleLinearTransformation;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Plugin;

@Plugin(type = NodeProcessor.class, name = ScaleLinearTransformation.name)
public class ScaleLinear
		extends DefaultImageNodeProcessor<ScaleLinearTransformation> {

	private Number offset;
	private Number gain;

	@Override
	protected <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> ImageDataReference<O> process(RandomAccessibleInterval<I> in, ImageDataReference<O> outType) {
		Converter<? super I, ? super O> converter = (input, output)
				-> output.setReal(input.getRealFloat()*gain.floatValue() + offset.floatValue());
		RandomAccessibleInterval<O> converted = Converters.convert(in, converter, outType.getDataType());
		return new DefaultImageDataReference<>(converted, outType.getDataType());
	}

	@Override
	public void readSpecification(ScaleLinearTransformation specification) {
		offset = specification.getOffset();
		gain = specification.getGain();
	}
}
