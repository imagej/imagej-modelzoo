package net.imagej.modelzoo.transformation.normalize;

import net.imagej.modelzoo.consumer.commands.ModelZooPreprocessorCommand;
import net.imglib2.RandomAccessibleInterval;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;

@Plugin(type= ModelZooPreprocessorCommand.class)
public class PercentileNormalizationCommand implements ModelZooPreprocessorCommand {

	@Parameter(type = ItemIO.BOTH)
	List<Object> data;

	@Parameter
	private float min = 0.01f;

	@Parameter
	private float max = 0.99f;

	@Parameter
	private boolean clip = false;

	@Parameter
	private LogService log;

	@Parameter
	private Context context;

	@Override
	public void run() {
		PercentileNormalizer normalizer = new PercentileNormalizer(
				new float[]{min, max},
				new float[]{0f, 1f}, clip);
		context.inject(normalizer);
		for (int i = 0; i < data.size(); i++) {
			Object imgObj = data.get(i);
			try {
				RandomAccessibleInterval normalized = normalizer.normalize((RandomAccessibleInterval) imgObj);
				data.set(i, normalized);
			} catch (ClassCastException e) {
				log.error("Cannot normalize " + imgObj);
			}
		}
	}

}
