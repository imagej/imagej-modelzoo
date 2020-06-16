package net.imagej.modelzoo.howto;

import net.imagej.modelzoo.specification.DefaultModelSpecification;
import net.imagej.modelzoo.specification.ModelSpecification;
import org.junit.Test;

import java.io.IOException;

public class E03_ReadSpecification {

	@Test
	public void run() throws IOException {

		// resource path
		String specificationPath = ModelSpecification.class.getResource("/example.model.yaml").getPath();

		// create specification
		ModelSpecification specification = new DefaultModelSpecification();

		// read specification
		specification.read(specificationPath);

		// access specification
		System.out.println(specification.getName());
		System.out.println(specification.getDescription());

	}

	public static void main(String... args) throws IOException {
		new E03_ReadSpecification().run();
	}
}
