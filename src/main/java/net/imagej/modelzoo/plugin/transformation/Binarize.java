package net.imagej.modelzoo.plugin.transformation;

import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.processor.DefaultImageNodeProcessor;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessor;
import io.bioimage.specification.transformation.postprocessing.BinarizeTransformation;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Plugin;

@Plugin(type = NodeProcessor.class, name = BinarizeTransformation.name)
public class Binarize extends DefaultImageNodeProcessor<BinarizeTransformation> {

	private Number threshold;

	@Override
	protected <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> ImageDataReference<O> process(RandomAccessibleInterval<I> in, ImageDataReference<O> outType) {
		Converter<? super I, ? super O> converter = (input, output) -> {
			if (input.getRealDouble() < threshold.doubleValue()) output.setZero();
			else output.setOne();
		};
		RandomAccessibleInterval<O> converted = Converters.convert(in, converter, outType.getDataType());
		return new DefaultImageDataReference<>(converted, outType.getDataType());
	}

	@Override
	public void readSpecification(BinarizeTransformation specification) {
		threshold = specification.getThreshold();
	}
}
