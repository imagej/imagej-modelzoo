package net.imagej.modelzoo.plugin.normalization;

import net.imagej.modelzoo.consumer.model.DefaultImageNodeProcessor;
import net.imagej.modelzoo.consumer.model.NodeProcessor;
import net.imagej.modelzoo.specification.TransformationSpecification;
import net.imagej.modelzoo.specification.transformation.ScaleLinearTransformation;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Plugin;

import java.util.Map;

@Plugin(type = NodeProcessor.class, name = ScaleLinearTransformation.name)
public class ScaleLinear
		extends DefaultImageNodeProcessor<ScaleLinearTransformation> {

	private Number offset;
	private Number gain;

	@Override
	public void run() {
		setOutput(convert(getInput()));
	}

	private<I extends RealType<I>, O extends RealType<O>> RandomAccessibleInterval<O> convert(RandomAccessibleInterval<I> in) {
		Converter<? super I, ? super O> converter = (input, output)
				-> output.setReal(input.getRealFloat()*gain.floatValue() + offset.floatValue());
		return Converters.convert(in, converter, (O)getOutputType());
	}

	@Override
	public void readSpecification(ScaleLinearTransformation specification) {
		offset = specification.getOffset();
		gain = specification.getGain();
	}
}
