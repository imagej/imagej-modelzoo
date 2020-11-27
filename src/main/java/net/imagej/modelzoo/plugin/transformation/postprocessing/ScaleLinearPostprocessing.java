package net.imagej.modelzoo.plugin.transformation.postprocessing;

import io.bioimage.specification.transformation.ScaleLinearTransformation;
import net.imagej.modelzoo.consumer.model.node.DefaultImageDataReference;
import net.imagej.modelzoo.consumer.model.node.ImageDataReference;
import net.imagej.modelzoo.consumer.model.node.InputImageNode;
import net.imagej.modelzoo.consumer.model.node.OutputImageNode;
import net.imagej.modelzoo.consumer.model.node.processor.DefaultImageNodePostprocessor;
import net.imagej.modelzoo.consumer.model.node.processor.NodePostprocessor;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

@Plugin(type = NodePostprocessor.class, name = ScaleLinearTransformation.name)
public class ScaleLinearPostprocessing
		extends DefaultImageNodePostprocessor<ScaleLinearTransformation> {

	@Parameter
	private OpService opService;

	@Parameter
	private UIService uiService;

	private Number offset;
	private Number gain;

	@Override
	protected <I extends RealType<I> & NativeType<I>, O extends RealType<O> & NativeType<O>> ImageDataReference<O> process(ImageDataReference<I> in, ImageDataReference<O> outType) {
		InputImageNode inputReference = ((OutputImageNode) getImageNode()).getReference();
		O resOutType = outType.getDataType();
		if(inputReference != null && getOptions().convertIntoInputFormat()) {
			resOutType = inputReference.getOriginalDataType();
		}
		RandomAccessibleInterval<O> out;
		if(sameType(in.getDataType(), resOutType)) {
			out = (RandomAccessibleInterval<O>) in.getData();
		} else {
			out = opService.create().img(in.getData(), resOutType);
		}
		O finalResOutType = resOutType;
		LoopBuilder.setImages(in.getData(), out).forEachPixel((i, o) -> {
			double real = i.getRealDouble() * gain.doubleValue() + offset.doubleValue();
			o.setReal(inBounds(real, finalResOutType));
		});

		return new DefaultImageDataReference(out, resOutType);
	}

	private <T extends RealType<T> & NativeType<T>> double inBounds(double value, T resOutType) {
		return Math.min(Math.max(resOutType.getMinValue(), value), resOutType.getMaxValue());
	}

	@Override
	public void readSpecification(ScaleLinearTransformation specification) {
		offset = specification.getOffset();
		gain = specification.getGain();
	}
}
