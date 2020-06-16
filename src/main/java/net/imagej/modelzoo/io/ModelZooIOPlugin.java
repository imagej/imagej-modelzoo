/*
 * #%L
 * Table structures for SciJava.
 * %%
 * Copyright (C) 2012 - 2020 Board of Regents of the University of
 * Wisconsin-Madison, and Friedrich Miescher Institute for Biomedical Research.
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

import org.scijava.io.AbstractIOPlugin;
import org.scijava.table.Table;

import java.io.IOException;

/**
 * Abstract plugin class for saving and loading tables
 *
 * @author Deborah Schmidt
 */
public class TableIOPlugin extends AbstractIOPlugin<Table> {

	@Override
	public Table<?, ?> open(String source) throws IOException {
		return open(source, new TableIOOptions());
	}

	/** Opens data from the given source. */
	@SuppressWarnings("unused")
	public Table<?, ?> open(final String source, final TableIOOptions options) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void save(Table data, String destination) throws IOException {
		save(data, destination, new TableIOOptions());
	}

	/** Saves the given data to the specified destination. */
	@SuppressWarnings("unused")
	public void save(final Table<?, ?> data, final String destination, final TableIOOptions options) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<Table> getDataType() {
		return Table.class;
	}
}
