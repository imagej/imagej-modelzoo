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

import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.modelzoo.DefaultModelZooArchive;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.specification.DefaultModelSpecification;
import org.scijava.io.AbstractIOPlugin;
import org.scijava.io.IOPlugin;
import org.scijava.io.location.Location;
import org.scijava.io.location.LocationService;
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
import java.util.Enumeration;
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

	@Override
	public ModelZooArchive open(String source) throws IOException {
		Location location = null;
		try {
			LocationService locationService = getContext().service(LocationService.class);
			location = locationService.resolve(source);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		DefaultModelZooArchive archive = new DefaultModelZooArchive();
		getContext().inject(archive);
		archive.setSource(location);
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
			specification.read(inSpec);
			specification.setModelFileName(modelFile.getName());
			inSpec.close();
			archive.setSpecification(specification);
			String testInputPath = specification.getTestInput();
			String testOutputPath = specification.getTestOutput();
			if(testInputPath != null) archive.setTestInput(extractImage(zf, testInputPath));
			if(testOutputPath != null) archive.setTestOutput(extractImage(zf, testOutputPath));
		}
		return archive;
	}

	@Override
	public void save(ModelZooArchive archive, String destination) throws IOException {
		Path destinationPath = Paths.get(destination);
		Path sourcePath = new File(archive.getSource().getURI()).toPath();
		Files.copy(sourcePath, destinationPath, REPLACE_EXISTING);
		Path tmpTestInput = null;
		Path tmpTestOutput = null;
		if(archive.getTestInput() != null && archive.getTestOutput() != null) {
			tmpTestInput = Files.createTempFile("input", archive.getSpecification().getTestInput());
			tmpTestOutput = Files.createTempFile("output", archive.getSpecification().getTestOutput());
			DatasetIOService datasetIOService = getContext().service(DatasetIOService.class);
			DatasetService datasetService = getContext().service(DatasetService.class);
			datasetIOService.save(datasetService.create(archive.getTestInput()), tmpTestInput.toFile().getAbsolutePath());
			datasetIOService.save(datasetService.create(archive.getTestOutput()), tmpTestOutput.toFile().getAbsolutePath());
		}

		try (FileSystem fileSystem = FileSystems.newFileSystem(destinationPath, null)) {
			Path specPath = fileSystem.getPath(archive.getSpecification().getModelFileName());
			archive.getSpecification().write(specPath);
			if(tmpTestInput != null) {
				Path testInputPath = fileSystem.getPath(archive.getSpecification().getTestInput());
				Path testOutputPath = fileSystem.getPath(archive.getSpecification().getTestOutput());
				Files.copy(tmpTestInput, testInputPath, REPLACE_EXISTING);
				Files.copy(tmpTestOutput, testOutputPath, REPLACE_EXISTING);
			}
		}
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

	private Dataset extractImage(ZipFile zf, String testInputLocation) throws IOException {
		ZipEntry entry = zf.getEntry(testInputLocation);
		if(entry == null) return null;
		InputStream inputStream = zf.getInputStream(entry);
		File tmpTestInput = Files.createTempFile("img", ".tif").toFile();
		byte[] buffer = new byte[8 * 1024];
		int bytesRead;
		OutputStream outStream = new FileOutputStream(tmpTestInput);
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outStream.write(buffer, 0, bytesRead);
		}
		outStream.close();
		inputStream.close();
		DatasetIOService datasetIOService = getContext().service(DatasetIOService.class);
		return datasetIOService.open(tmpTestInput.getAbsolutePath());
	}
}
