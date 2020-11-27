package net.imagej.modelzoo.plugin.transformation.postprocessing;

import io.bioimage.specification.transformation.ClipTransformation;
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.processor.DefaultImageNodePostprocessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodePostprocessor;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = NodePostprocessor.class, name = ClipTransformation.name)
public class ClipPostprocessing extends DefaultImageNodePostprocessor<ClipTransformation> {

	private Number min;
	private Number max;

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
			o.setReal(Math.max(min.doubleValue(), Math.min(i.getRealDouble(), max.doubleValue())));
		});
		return new DefaultImageDataReference<>(out, outType.getDataType());
 	}

	@Override
	public void readSpecification(ClipTransformation specification) {
		min = specification.getMin();
		max = specification.getMax();
	}
}
