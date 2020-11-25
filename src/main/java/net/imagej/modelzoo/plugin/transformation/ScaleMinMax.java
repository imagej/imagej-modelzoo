package net.imagej.modelzoo.plugin.transformation;

import io.bioimage.specification.transformation.postprocessing.ScaleMinMaxTransformation;
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.processor.DefaultImageNodeProcessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessor;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

@Plugin(type = NodeProcessor.class, name = ScaleMinMaxTransformation.name)
public class ScaleMinMax
		extends DefaultImageNodeProcessor<ScaleMinMaxTransformation> {

	private Number minPercentile;
	private Number maxPercentile;

	Number minVal;
	Number factor;

	@Parameter
	private OpService opService;

	@Override
	protected <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> ImageDataReference<O> process(RandomAccessibleInterval<I> in, ImageDataReference<O> outType) {
		calculateGainOffset(in);
		Converter<? super I, ? super O> converter = (input, output)
				-> output.setReal(input.getRealFloat()*factor.floatValue() + minVal.floatValue());
		RandomAccessibleInterval<O> converted = Converters.convert(in, converter, outType.getDataType());
		return new DefaultImageDataReference<>(converted, outType.getDataType());
	}

	private <I extends RealType<I> & NativeType<I>> void calculateGainOffset(RandomAccessibleInterval<I> in) {
		NormalizePercentile preprocessor = null;
		if(getProcessorInputReference() != null) {
			for (NodeProcessor processor : getProcessorInputReference().getProcessors()) {
				if(processor instanceof NormalizePercentile) {
					preprocessor = (NormalizePercentile) processor;
				}
			}
		}
		if(preprocessor != null
				&& preprocessor.getLowerPercentile().equals(minPercentile)
				&& preprocessor.getUpperPercentile().equals(maxPercentile)) {
			minVal = preprocessor.getMinVal();
			factor = preprocessor.getFactor();
		} else {
			throw new NotImplementedException();
		}
	}

	@Override
	public void readSpecification(ScaleMinMaxTransformation specification) {
		minPercentile = specification.getMinPercentile();
		maxPercentile = specification.getMaxPercentile();
	}
}
