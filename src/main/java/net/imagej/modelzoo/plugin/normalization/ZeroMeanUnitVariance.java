package net.imagej.modelzoo.plugin.normalization;

import net.imagej.modelzoo.consumer.model.DefaultImageNodeProcessor;
import net.imagej.modelzoo.consumer.model.NodeProcessor;
import net.imagej.modelzoo.specification.transformation.ScaleLinearTransformation;
import net.imagej.modelzoo.specification.transformation.ZeroMeanUnitVarianceTransformation;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Plugin;


@Plugin(type = NodeProcessor.class, name = ZeroMeanUnitVarianceTransformation.name)
public class ZeroMeanUnitVariance extends DefaultImageNodeProcessor<ZeroMeanUnitVarianceTransformation> {

	private Number mean;
	private Number std;

	@Override
	public void run() {
		setOutput(convert(getInput()));
	}

	private<I extends RealType<I>, O extends RealType<O>> RandomAccessibleInterval<O> convert(RandomAccessibleInterval<I> in) {
		Converter<? super I, ? super O> converter = (Converter<I, O>) (input, output)
				-> output.setReal((input.getRealFloat() - mean.floatValue()) / std.floatValue());
		return Converters.convert(in, converter, (O)getOutputType());
	}

	@Override
	public void readSpecification(ZeroMeanUnitVarianceTransformation specification) {
		mean = specification.getMean();
		std = specification.getStd();
	}
}
