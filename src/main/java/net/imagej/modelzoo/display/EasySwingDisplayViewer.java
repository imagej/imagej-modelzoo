/*-
 * #%L
 * This is the bioimage.io modelzoo library for ImageJ.
 * %%
 * Copyright (C) 2019 - 2021 Center for Systems Biology Dresden
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
package net.imagej.modelzoo.display;

import org.scijava.display.Display;
import org.scijava.display.event.DisplayDeletedEvent;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UserInterface;
import org.scijava.ui.swing.SwingUI;
import org.scijava.ui.viewer.AbstractDisplayViewer;
import org.scijava.ui.viewer.DisplayPanel;
import org.scijava.ui.viewer.DisplayViewer;
import org.scijava.ui.viewer.DisplayWindow;

import javax.swing.JPanel;
import java.awt.BorderLayout;

abstract public class EasySwingDisplayViewer< T >
		extends AbstractDisplayViewer< T > implements DisplayViewer< T >
{

	private final Class< T > classOfObject;

	@Parameter
	ObjectService objectService;

	protected EasySwingDisplayViewer(Class< T > classOfObject)
	{
		this.classOfObject = classOfObject;
	}

	@Override
	public boolean isCompatible(final UserInterface ui) {
		return ui instanceof SwingUI;
	}

	@Override
	public boolean canView(final Display< ? > d) {
		Object object = d.get(0);
		if (!classOfObject.isInstance(object)) return false;
		T value = (T) object;
		return canView(value);
	}

	protected abstract boolean canView(T value);

	protected abstract void redoLayout();

	protected abstract void setLabel(final String s);

	protected abstract void redraw();

	protected abstract JPanel createDisplayPanel(T value);

	@Override
	public void onDisplayDeletedEvent(DisplayDeletedEvent e)
	{
		super.onDisplayDeletedEvent(e);
		objectService.removeObject(getDisplay().get(0));
	}

	@Override
	public void view(final DisplayWindow w, final Display< ? > d) {
		objectService.addObject(d.get(0));
		super.view(w, d);
		final JPanel content = createDisplayPanel(getDisplay().get(0));
		setPanel(new SwingDisplayPanel(w, d, this, content));
	}

	public static class SwingDisplayPanel extends JPanel
			implements DisplayPanel
	{

		// -- instance variables --

		private final EasySwingDisplayViewer< ? > viewer;
		private final DisplayWindow window;
		private final Display< ? > display;

		// -- PlotDisplayPanel methods --

		public SwingDisplayPanel(DisplayWindow window, Display< ? > display,
				EasySwingDisplayViewer< ? > viewer, JPanel panel)
		{
			this.window = window;
			this.display = display;
			this.viewer = viewer;
			window.setContent(this);
			setLayout(new BorderLayout());
			add(panel);
		}

		@Override
		public Display< ? > getDisplay() {
			return display;
		}

		// -- DisplayPanel methods --

		@Override
		public DisplayWindow getWindow() {
			return window;
		}

		@Override
		public void redoLayout()
		{
			viewer.redoLayout();
		}

		@Override
		public void setLabel(String s)
		{
			viewer.setLabel(s);
		}

		@Override
		public void redraw()
		{
			viewer.redraw();
		}
	}
}
