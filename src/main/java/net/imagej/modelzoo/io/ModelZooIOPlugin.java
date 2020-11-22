/*
 * #%L
 * This is the bioimage.io modelzoo library for ImageJ.
 * %%
 * Copyright (C) 2019 - 2020 Center for Systems Biology Dresden
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.modelzoo.io;

import net.imagej.modelzoo.DefaultModelZooArchive;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.TensorSample;
import net.imagej.modelzoo.specification.DefaultModelSpecification;
import net.imagej.modelzoo.specification.ModelSpecification;
import org.scijava.app.StatusService;
import org.scijava.io.AbstractIOPlugin;
import org.scijava.io.IOPlugin;
import org.scijava.io.IOService;
import org.scijava.io.location.FileLocation;
import org.scijava.io.location.Location;
import org.scijava.io.location.LocationService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Abstract plugin class for saving and loading bioimage model zoo archives
 *
 * @author Deborah Schmidt
 */
@Plugin(type = IOPlugin.class, priority = 100.0)
public class ModelZooIOPlugin extends AbstractIOPlugin<ModelZooArchive> {

	@Parameter
	private StatusService statusService;

	@Parameter
	private IOService ioService;

	@Override
	public ModelZooArchive open(String source) throws IOException {
		statusService.showStatus("Opening " + source + "..");
		Location location = null;
		try {
			LocationService locationService = getContext().service(LocationService.class);
			location = locationService.resolve(source);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		DefaultModelZooArchive archive = new DefaultModelZooArchive();
		getContext().inject(archive);
		archive.setLocation(location);
		DefaultModelSpecification specification = new DefaultModelSpecification();
		try (ZipFile zf = new ZipFile(source)) {
			final Enumeration<? extends ZipEntry> entries = zf.entries();
			ZipEntry modelFile = null;
			while ( entries.hasMoreElements() ) {
				final ZipEntry entry = entries.nextElement();
				if(entry.getName().endsWith("model.yaml")) {
					modelFile = entry;
					break;
				}
			}
			if(modelFile == null) {
				throw new IOException("Can't open " + source + " as bioimage.io model archive: No *model.yaml file found.");
			}
			InputStream inSpec = zf.getInputStream(modelFile);
			boolean success = specification.read(inSpec);
			if(!success) {
				throw new IOException("Could not read model.yaml specification from source " + source);
			}
			specification.setModelFileName(modelFile.getName());
			inSpec.close();
			archive.setSpecification(specification);
			setSampleImages(archive, specification, zf);
		}
		statusService.showStatus("Done opening " + source + ".");
		return archive;
	}

	private void setSampleImages(DefaultModelZooArchive archive, DefaultModelSpecification specification, ZipFile zf) throws IOException {
		if(specification.getSampleInputs() != null) {
			List<Object> sampleInputs = new ArrayList<>();
			for (String sampleInput : specification.getSampleInputs()) {
				sampleInputs.add(extract(zf,sampleInput));
			}
			archive.setSampleInputs(sampleInputs);
		}
		if(specification.getSampleOutputs() != null) {
			List<Object> sampleOutputs = new ArrayList<>();
			for (String sampleOutput : specification.getSampleOutputs()) {
				sampleOutputs.add(extract(zf,sampleOutput));
			}
			archive.setSampleOutputs(sampleOutputs);
		}
	}

	@Override
	public void save(ModelZooArchive archive, String destination) throws IOException {
		statusService.showStatus("Saving " + destination + "..");
		Path destinationPath = Paths.get(destination);
		Path sourcePath = new File(archive.getLocation().getURI()).toPath();
		Files.copy(sourcePath, destinationPath, REPLACE_EXISTING);
		List<Path> tmpTestInputs = saveSampleTensors(archive.getSampleInputs());
		List<Path> tmpTestOutputs = saveSampleTensors(archive.getSampleOutputs());
		try (FileSystem fileSystem = FileSystems.newFileSystem(destinationPath, null)) {
			Path specPath = fileSystem.getPath(archive.getSpecification().getModelFileName());
			archive.getSpecification().write(specPath);
			copySampleTensors(tmpTestInputs, fileSystem, archive.getSpecification().getTestInputs());
			copySampleTensors(tmpTestOutputs, fileSystem, archive.getSpecification().getTestOutputs());
		}
		archive.setLocation(new FileLocation(destination));
		statusService.showStatus("Done saving " + destination + ".");
	}

	private void copySampleTensors(List<Path> sampleTensorFiles, FileSystem fileSystem, List<String> newFileNames) throws IOException {
		for (int i = 0; i < sampleTensorFiles.size(); i++) {
			Path tmpTestInput = sampleTensorFiles.get(i);
			Path path = fileSystem.getPath(newFileNames.get(i));
			Files.copy(tmpTestInput, path, REPLACE_EXISTING);
		}
	}

	private List<Path> saveSampleTensors(List<TensorSample> tensorSamples) throws IOException {
		List<Path> resultPaths = new ArrayList<>();
		if(tensorSamples != null) {
			for (int i = 0; i < tensorSamples.size(); i++) {
				TensorSample sampleInput = tensorSamples.get(i);
				Path tmpTestInput = Files.createTempFile("file", sampleInput.getFileName());
				ioService.save(sampleInput, tmpTestInput.toFile().getAbsolutePath());
				resultPaths.add(tmpTestInput);
			}
		}
		return resultPaths;
	}

	@Override
	public Class<ModelZooArchive> getDataType() {
		return ModelZooArchive.class;
	}

	@Override
	public boolean supportsOpen(String source) {
		return source.endsWith("bioimage.io.zip");
	}

	@Override
	public boolean supportsSave(String destination) {
		return destination.endsWith("bioimage.io.zip");
	}

	private Object extract(ZipFile zf, String testInputLocation) throws IOException {
		ZipEntry entry = zf.getEntry(testInputLocation);
		if(entry == null) return null;
		InputStream inputStream = zf.getInputStream(entry);
		File tmpTestInput = Files.createTempFile("", entry.getName()).toFile();
		byte[] buffer = new byte[8 * 1024];
		int bytesRead;
		OutputStream outStream = new FileOutputStream(tmpTestInput);
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outStream.write(buffer, 0, bytesRead);
		}
		outStream.close();
		inputStream.close();
		Object result =  ioService.open(tmpTestInput.getAbsolutePath());
		tmpTestInput.delete();
		return result;
	}

	public void save(String archivePath, ModelSpecification specification, String location) throws IOException {
		DefaultModelZooArchive archive = new DefaultModelZooArchive();
		archive.setLocation(new FileLocation(archivePath));
		archive.setSpecification(specification);
		save(archive, location);
	}
}
