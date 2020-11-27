package net.imagej.modelzoo.plugin.transformation.postprocessing;

import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.processor.DefaultImageNodePostprocessor;
import net.imagej.modelzoo.consumer.model.node.processor.DefaultImageNodePreprocessor;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.processor.NodePostprocessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodeProcessor;
import io.bioimage.specification.transformation.BinarizeTransformation;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = NodePostprocessor.class, name = BinarizeTransformation.name)
public class BinarizePostprocessing extends DefaultImageNodePostprocessor<BinarizeTransformation> {

	private Number threshold;

	@Parameter
	private OpService opService;

	@Override
	protected <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> ImageDataReference<O> process(ImageDataReference<I> in, ImageDataReference<O> outType) {
		RandomAccessibleInterval<O> out;
		if(sameType(in, outType)) {
			out = (RandomAccessibleInterval<O>) in.getData();
		} else {
			out = opService.create().img(in.getData(), outType.getDataType());
		}
		LoopBuilder.setImages(in.getData(), out).multiThreaded().forEachPixel((i, o) -> {
			if (i.getRealDouble() < threshold.doubleValue()) o.setZero();
			else o.setOne();
		});
		return new DefaultImageDataReference<>(out, outType.getDataType());
	}

	@Override
	public void readSpecification(BinarizeTransformation specification) {
		threshold = specification.getThreshold();
	}
}
