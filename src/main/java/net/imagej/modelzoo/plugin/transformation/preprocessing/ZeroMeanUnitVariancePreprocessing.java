package net.imagej.modelzoo.plugin.transformation.preprocessing;

import io.bioimage.specification.transformation.ZeroMeanUnitVarianceTransformation;
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.processor.DefaultImageNodePreprocessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodePreprocessor;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.plugin.Plugin;


@Plugin(type = NodePreprocessor.class, name = ZeroMeanUnitVarianceTransformation.name)
public class ZeroMeanUnitVariancePreprocessing extends DefaultImageNodePreprocessor<ZeroMeanUnitVarianceTransformation> {

	private Number mean;
	private Number std;

	@Override
	protected <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> ImageDataReference<FloatType> process(ImageDataReference<I> in, ImageDataReference<O> outType) {
		Converter<? super I, FloatType> converter = (Converter<I, FloatType>) (input, output)
				-> output.setReal((input.getRealFloat() - mean.floatValue()) / std.floatValue());
		return new DefaultImageDataReference<>(Converters.convert(in.getData(), converter, new FloatType()), new FloatType());
	}

	@Override
	public void readSpecification(ZeroMeanUnitVarianceTransformation specification) {
		mean = specification.getMean();
		std = specification.getStd();
	}
}
