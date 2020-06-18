package net.imagej.modelzoo.consumer.commands;

import io.scif.MissingLibraryException;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imglib2.RandomAccessibleInterval;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.FileNotFoundException;

@Plugin(type = Command.class, name = "Set modelzoo archive test image")
public class ModelArchiveTestImagesCommand implements Command {

	@Parameter
	private RandomAccessibleInterval inputImage;

	@Parameter
	private ModelZooArchive archive;

	@Parameter
	private LogService logService;

	@Parameter
	private Context context;

	@Parameter
	private ModelZooService modelZooService;

	@Override
	public void run() {
//		if(inputImage.numDimensions() > 2) {
//			logService.warn("Can't use test image with more than 2 dimensions.");
//			return;
//		}
		try {
			RandomAccessibleInterval output = modelZooService.predict(archive, inputImage, "ZY");
			archive.setTestOutput(output);
			archive.setTestInput(inputImage);

		} catch (FileNotFoundException | MissingLibraryException e) {
			e.printStackTrace();
		}
	}

}
