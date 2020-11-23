package net.imagej.modelzoo.consumer.model.tensorflow.train;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class IOUtils {

	static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
		if (fileToZip.isHidden()) {
			return;
		}
		if (fileToZip.isDirectory()) {
			if(fileName == null) {
				File[] children = fileToZip.listFiles();
				for (File childFile : children) {
					zipFile(childFile, childFile.getName(), zipOut);
				}
			} else {
				if (fileName.endsWith("/")) {
					zipOut.putNextEntry(new ZipEntry(fileName));
					zipOut.closeEntry();
				} else {
					zipOut.putNextEntry(new ZipEntry(fileName + "/"));
					zipOut.closeEntry();
				}
				File[] children = fileToZip.listFiles();
				for (File childFile : children) {
					zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
				}
			}
			return;
		}
		FileInputStream fis = new FileInputStream(fileToZip);
		ZipEntry zipEntry = new ZipEntry(fileName);
		zipOut.putNextEntry(zipEntry);
		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zipOut.write(bytes, 0, length);
		}
		fis.close();
	}


	public static void unZipAll(File source, File destination) throws IOException
	{
		System.out.println("Unzipping - " + source.getName());
		int BUFFER = 2048;

		ZipFile zip = new ZipFile(source);
		try{
			destination.getParentFile().mkdirs();
			Enumeration zipFileEntries = zip.entries();

			// Process each entry
			while (zipFileEntries.hasMoreElements())
			{
				// grab a zip file entry
				ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
				String currentEntry = entry.getName();
				File destFile = new File(destination, currentEntry);
				//destFile = new File(newPath, destFile.getName());
				File destinationParent = destFile.getParentFile();

				// create the parent directory structure if needed
				destinationParent.mkdirs();

				if (!entry.isDirectory())
				{
					BufferedInputStream is = null;
					FileOutputStream fos = null;
					BufferedOutputStream dest = null;
					try{
						is = new BufferedInputStream(zip.getInputStream(entry));
						int currentByte;
						// establish buffer for writing file
						byte data[] = new byte[BUFFER];

						// write the current file to disk
						fos = new FileOutputStream(destFile);
						dest = new BufferedOutputStream(fos, BUFFER);

						// read and write until last byte is encountered
						while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
							dest.write(data, 0, currentByte);
						}
					} catch (Exception e){
						System.out.println("unable to extract entry:" + entry.getName());
						throw e;
					} finally{
						if (dest != null){
							dest.close();
						}
						if (fos != null){
							fos.close();
						}
						if (is != null){
							is.close();
						}
					}
				}else{
					//Create directory
					destFile.mkdirs();
				}

				if (currentEntry.endsWith(".zip"))
				{
					// found a zip file, try to extract
					unZipAll(destFile, destinationParent);
					if(!destFile.delete()){
						System.out.println("Could not delete zip");
					}
				}
			}
		} catch(Exception e){
			e.printStackTrace();
			System.out.println("Failed to successfully unzip:" + source.getName());
		} finally {
			zip.close();
		}
		System.out.println("Done Unzipping:" + source.getName());
	}
}
