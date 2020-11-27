package net.imagej.modelzoo.consumer.preprocessing;

import net.imagej.modelzoo.consumer.ModelZooPredictionOptions;
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageNode;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessorException;
import io.bioimage.specification.TransformationSpecification;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealDoubleConverter;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

public class InputImageConverterProcessor<TO extends RealType<TO> & NativeType<TO>> implements NodeProcessor<TransformationSpecification> {
	private final TO dataType;
	private ImageNode node;

	public InputImageConverterProcessor(ImageNode node, TO dataType) {
		this.node = node;
		this.dataType = dataType;
	}

	@Override
	public void run(ModelZooPredictionOptions.Values options) throws NodeProcessorException {
		RandomAccessibleInterval<?> data = node.getData().getData();
		if(data == null) return;
		if(!dataType.getClass().isAssignableFrom(data.randomAccess().get().getClass())) {
			convert(data);
		}
	}

	private <TI> void convert(RandomAccessibleInterval<TI> data) throws NodeProcessorException {
		Converter<? super TI, ? super TO> converter = null;
		if(FloatType.class.isAssignableFrom(dataType.getClass())) {
			converter = new RealFloatConverter();
		}
		if(DoubleType.class.isAssignableFrom(dataType.getClass())) {
			converter = new RealDoubleConverter();
		}
		if(converter != null) {
			RandomAccessibleInterval<TO> converted = Converters.convert(data, converter, dataType);
			DefaultImageDataReference<TO> reference = new DefaultImageDataReference<>(converted, dataType);
			node.setData(reference);
		} else {
			throw new NodeProcessorException(getClass().getSimpleName() + ": Cannot convert " + data.randomAccess().get().getClass()
					+ " to " + dataType.getClass() + ".");
		}
	}

}
