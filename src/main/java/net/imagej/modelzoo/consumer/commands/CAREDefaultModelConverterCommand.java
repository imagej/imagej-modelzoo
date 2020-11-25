package net.imagej.modelzoo.consumer.commands;

import io.bioimage.specification.ModelSpecification;
import net.imagej.modelzoo.DefaultModelZooArchive;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.consumer.model.tensorflow.TensorFlowModel;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.io.location.FileLocation;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import java.io.File;
import java.io.IOException;

import static org.scijava.widget.FileWidget.DIRECTORY_STYLE;

public class CAREDefaultModelConverterCommand implements Command {

	@Parameter(label = "Old CARE TF export")
	private File input;

	@Parameter(label = "Model name")
	private String name;

	@Parameter(label = "Destination directory", style = DIRECTORY_STYLE)
	private File destinationFolder;

	@Parameter(label = "Destination file name (without .zip ending)")
	private String destinationFileName;

	@Parameter
	private int networkDepth;

	@Parameter
	private int kernelSize;

	@Parameter(type = ItemIO.OUTPUT)
	private ModelZooArchive output;

	@Parameter
	private ModelZooService modelZooService;

	@Parameter
	private LogService logService;

	@Parameter
	private Context context;

	@Override
	public void run() {
		try {
			File destination = new File(destinationFolder, destinationFileName + ".model.bioimage.io.zip");
			if (destination.equals(input)) {
				logService.error("Destination file cannot be the same as the input file");
				return;
			}
			ModelSpecification specification = new TensorFlowModel(context).guessSpecification(new FileLocation(input), name, networkDepth, kernelSize);
			DefaultModelZooArchive archive = new DefaultModelZooArchive();
			archive.setSpecification(specification);
			modelZooService.io().save(archive, new FileLocation(destination));
			archive.add(input, "tensorflow_saved_model_bundle.zip");
			output = archive;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
