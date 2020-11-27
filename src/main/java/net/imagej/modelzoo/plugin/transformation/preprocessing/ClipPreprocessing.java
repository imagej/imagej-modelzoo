package net.imagej.modelzoo.plugin.transformation.preprocessing;

import io.bioimage.specification.transformation.ClipTransformation;
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.processor.DefaultImageNodePreprocessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodePreprocessor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Plugin;

@Plugin(type = NodePreprocessor.class, name = ClipTransformation.name)
public class ClipPreprocessing extends DefaultImageNodePreprocessor<ClipTransformation> {

	private Number min;
	private Number max;

	@Override
	protected <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> ImageDataReference<O> process(ImageDataReference<I> in, ImageDataReference<O> outType) {
		Converter<? super I, ? super O> converter = (input, output) -> {
			output.setReal(Math.max(min.doubleValue(), Math.min(input.getRealDouble(), max.doubleValue())));
		};
		RandomAccessibleInterval<O> converted = Converters.convert(in.getData(), converter, outType.getDataType());
		return new DefaultImageDataReference<>(converted, outType.getDataType());
	}

	@Override
	public void readSpecification(ClipTransformation specification) {
		min = specification.getMin();
		max = specification.getMax();
	}
}
