/*-
 * #%L
 * UI component for image segmentation label comparison and selection
 * %%
 * Copyright (C) 2019 - 2020 DAIS developers
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

import net.imagej.display.ColorTables;
import net.imagej.display.SourceOptimizedCompositeXYProjector;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.consumer.commands.ModelArchiveTestImagesCommand;
import net.imagej.modelzoo.specification.CitationSpecification;
import net.imagej.modelzoo.specification.InputNodeSpecification;
import net.imagej.modelzoo.specification.OutputNodeSpecification;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.display.projector.composite.CompositeXYProjector;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleException;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.viewer.DisplayViewer;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class displays a {@link ModelZooArchive}.
 */
@Plugin(type = DisplayViewer.class)
public class SwingModelArchiveDisplayViewer extends EasySwingDisplayViewer<ModelZooArchive> {

	@Parameter
	private Context context;
	@Parameter
	private CommandService commandService;
	@Parameter
	private ModelZooService modelZooService;
	@Parameter
	private OpService opService;

	private static Color leftBgColor = new Color(0x454549);
	private static Color leftIconBgColor = new Color(0x313134);
	private static final Font font = new JLabel().getFont();
	private static final Font plainMonospaceFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);
	private static final Font plainFont = font.deriveFont(Font.PLAIN);
	private static final Font headerFont = font.deriveFont(Font.BOLD);
	private int previewDim = 200;

	public SwingModelArchiveDisplayViewer() {
		super(ModelZooArchive.class);
	}

	@Override
	protected boolean canView(ModelZooArchive model) {
		return true;
	}

	@Override
	protected JPanel createDisplayPanel(ModelZooArchive model) {
		JPanel panel = new JPanel(new MigLayout("fill, ins 0, gap 0"));
		JPanel leftPanel = new JPanel(new MigLayout("ins 0"));
		leftPanel.setBackground(leftBgColor);
		leftPanel.setForeground(Color.white);
		leftPanel.add(createLeftTitleIcon(), "span, growx");
		CardLayout cardLayout = new CardLayout();
		JPanel rightPanel = new JPanel(cardLayout);
		ButtonGroup group = new ButtonGroup();
		addCard(leftPanel, rightPanel, group, createOverviewPanel(model), "Overview");
		addCard(leftPanel, rightPanel, group, createMetaPanel(model), "Metadata");
		addCard(leftPanel, rightPanel, group, createInputsOutputsPanel(model), "Inputs & Outputs");
		if(model.getSpecification().getTrainingKwargs() != null) {
			addCard(leftPanel, rightPanel, group, createTrainingPanel(model), "Training");
		}
		cardLayout.first(rightPanel);
		panel.add(leftPanel, "newline, width 150:150:150, height 100%");
		panel.add(rightPanel, "width 450:450:null, height 100%");
		return panel;
	}

	private JLabel createLeftTitleIcon() {
		JLabel titleIcon = new JLabel(new ImageIcon(getClass().getResource("/icon-modelzoo.png")));
		titleIcon.setBackground(leftIconBgColor);
		titleIcon.setOpaque(true);
		return titleIcon;
	}

	private static Component createTrainingPanel(ModelZooArchive model) {
		JPanel panel = new JPanel(new MigLayout("", "[][push]", ""));
		model.getSpecification().getTrainingKwargs().forEach((s, o) -> {
			addToPanel(panel, s, o.toString());
		});
		return scroll(panel);
	}

	private Component createOverviewPanel(ModelZooArchive model) {
		JPanel panel = new JPanel(new MigLayout("fill"));
		JTextArea title = createReadTextArea(model.getSpecification().getName());
		title.setFont(headerFont);
		panel.add(title, "width 20:350:null, span");
		JTextArea descriptionText = createReadTextArea(model.getSpecification().getDescription());
		panel.add(descriptionText, "newline, span,growx,wmin 20");
		ImageIcon inputIcon = new ImageIcon();
		ImageIcon outputIcon = new ImageIcon();
		panel.add(createPreviewPanel(model, inputIcon, outputIcon), "newline, push, grow, span");
		panel.add(new JLabel("Saved to "), "newline, grow 0");
		JTextField sourceTextField = new JTextField(model.getSource().getURI().getPath());
		sourceTextField.setEditable(false);
		panel.add(sourceTextField, "growx");
		panel.add(createCopySourceBtn(model), "grow 0");
		panel.add(createActionsBar(model, inputIcon, outputIcon), "newline, span, gapbefore push");
		return panel;
	}

	private static JTextArea createReadTextArea(String text) {
		JTextArea res = new JTextArea(text);
		res.setEditable(false);
		res.setOpaque(false);
		res.setLineWrap(true);
		res.setWrapStyleWord(true);
		res.setBorder(null);
		return res;
	}

	private JButton createCopySourceBtn(ModelZooArchive model) {
		JButton copySourceBtn = new JButton(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				StringSelection stringSelection = new StringSelection(model.getSource().getURI().getPath());
				Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
				clpbrd.setContents(stringSelection, null);
			}
		});
		copySourceBtn.setMargin(new Insets(0, 0, 0, 0));
		copySourceBtn.setIcon(new ImageIcon(getClass().getResource("/icon-clipboard.png")));
		copySourceBtn.setToolTipText("Copy to clipboard");
		return copySourceBtn;
	}

	private Component createPreviewPanel(ModelZooArchive model, ImageIcon inputIcon, ImageIcon outputIcon) {
		JPanel panel = new JPanel(new MigLayout());
		try {
			reloadTestImages(model, inputIcon, outputIcon);
		} catch (IOException e) {
			e.printStackTrace();
		}
		panel.add(new JLabel(inputIcon), "height 200:200:200, width 200:200:200");
		panel.add(new JLabel(outputIcon), "height 200:200:200, width 200:200:200");
		return panel;
	}

	private Component createActionsBar(ModelZooArchive model, ImageIcon inputIcon, ImageIcon outputIcon) {
		JPanel panel = new JPanel(new MigLayout("ins 0"));
		//TODO enable once the features are all ready
//		panel.add(createActionButton("Set test image", () -> updateTestImage(model, inputIcon, outputIcon)));
//		panel.add(createActionButton("Save..", () -> saveChanges(model)));
		panel.add(createActionButton("Predict", () -> predict(model)));
		return panel;
	}

	private void predict(ModelZooArchive<?, ?> model) {
		try {
			modelZooService.predictInteractive(model);
		} catch (FileNotFoundException | ModuleException e) {
			e.printStackTrace();
		}
	}

	private void saveChanges(ModelZooArchive model) {
		try {
			String absolutePath = new File(model.getSource().getURI()).getAbsolutePath();
			System.out.println(absolutePath);
			modelZooService.save(model, absolutePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void updateTestImage(
			ModelZooArchive<?, ?> model, ImageIcon inputIcon, ImageIcon outputIcon) {
		try {
			commandService.run(ModelArchiveTestImagesCommand.class, true, "archive", model).get();
			reloadTestImages(model, inputIcon, outputIcon);
		} catch (InterruptedException | ExecutionException | IOException e) {
			e.printStackTrace();
		}
	}

	private static JButton createActionButton(String title, Runnable action) {
		JButton btn = new JButton(new AbstractAction(title) {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new Thread(action).start();
			}
		});
		return btn;
	}

	private <TI extends RealType<TI>, TO extends RealType<TO>> void reloadTestImages(ModelZooArchive<TI, TO> model, ImageIcon testInputIcon, ImageIcon testOutputIcon) throws IOException {
		if(model.getTestInput() == null || model.getTestOutput() == null) return;
		testInputIcon.setImage(toBufferedImage(model.getTestInput()));
		testOutputIcon.setImage(toBufferedImage(model.getTestOutput()));
	}

	private <T extends RealType<T>> BufferedImage toBufferedImage(RandomAccessibleInterval<T> img) {
		for (int i = 2; i < img.numDimensions(); i++) {
			img = Views.hyperSlice(img, i, 0);
		}
		img = opService.transform().scaleView(img, new double[]{(double)previewDim/(double)img.dimension(0), (double)previewDim/(double)img.dimension(1)}, new NLinearInterpolatorFactory<>());
		ARGBScreenImage screenImage = new ARGBScreenImage((int)img.dimension(0), (int)img.dimension(1));
		T min = img.randomAccess().get().copy();
		T max = img.randomAccess().get().copy();
		ComputeMinMax<T> minMax = new ComputeMinMax<>(Views.iterable(img), min, max);
		minMax.process();
		RealLUTConverter<? extends RealType<?>> converter = new RealLUTConverter(min.getRealDouble(),
				max.getRealDouble(), ColorTables.GRAYS);
		ArrayList<RealLUTConverter<? extends RealType<?>>> converters = new ArrayList<>();
		converters.add(converter);
		CompositeXYProjector projector;
		if (AbstractCellImg.class.isAssignableFrom(img.getClass())) {
			projector =
					new SourceOptimizedCompositeXYProjector(img,
							screenImage, converters, -1);
		}
		else {
			projector =
					new CompositeXYProjector(img, screenImage,
							converters, -1);
		}
		projector.setComposite(false);
		projector.map();
		return screenImage.image();
	}

	private static void addCard(JPanel leftPanel, JPanel rightPanel, ButtonGroup group, Component card, String id_card) {
		rightPanel.add(card, id_card);
		JToggleButton btn = new JToggleButton(new AbstractAction(id_card) {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				CardLayout cl = (CardLayout)(rightPanel.getLayout());
				cl.show(rightPanel, id_card);
			}
		});
		styleButton(btn);
		leftPanel.add(btn, "newline, span, growx");
		if(group.getButtonCount() == 0) btn.setSelected(true);
		group.add(btn);
	}

	private static void styleButton(JToggleButton btn) {
		btn.setBackground(leftBgColor);
		btn.setForeground(Color.white);
		btn.setFocusPainted(false);
		btn.setBorderPainted(false);
		btn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent evt) {
				btn.setBackground(Color.black);
			}

			public void mouseExited(java.awt.event.MouseEvent evt) {
				btn.setBackground(leftBgColor);
			}
		});
		btn.getModel().addChangeListener(e -> {
			ButtonModel model = (ButtonModel) e.getSource();
			if (model.isRollover()) {
				btn.setBackground(Color.black);
				btn.setForeground(Color.white);
			}
			else if(model.isSelected() || model.isPressed()) {
				btn.setBackground(UIManager.getColor("control"));
				btn.setForeground(Color.black);
			}
			else {
				btn.setBackground(leftBgColor);
				btn.setForeground(Color.white);
			}
		});
	}

	private static Component createInputsOutputsPanel(ModelZooArchive model) {
		JPanel panel = new JPanel(new MigLayout());
		addToPanel(panel, "Language", model.getSpecification().getLanguage());
		addToPanel(panel, "Framework", model.getSpecification().getFramework());
		addToPanel(panel, "Source", model.getSpecification().getSource());
		addToPanel(panel, "Inputs", asString(model.getSpecification().getInputs(), SwingModelArchiveDisplayViewer::inputToString));
		addToPanel(panel, "Outputs", asString(model.getSpecification().getOutputs(), SwingModelArchiveDisplayViewer::outputToString));
		return scroll(panel);
	}

	private static Component createMetaPanel(ModelZooArchive model) {
		JPanel panel = new JPanel(new MigLayout("fill"));
		addToPanel(panel, "Name", model.getSpecification().getName());
		addToPanel(panel, "Description", model.getSpecification().getDescription());
		addToPanel(panel, "Authors", listToString(model.getSpecification().getAuthors()));
		addToPanel(panel, "References", asString(model.getSpecification().getCitations(), SwingModelArchiveDisplayViewer::citationToString));
		addToPanel(panel, "License", model.getSpecification().getLicense());
		addToPanel(panel, "Documentation", model.getSpecification().getDocumentation());
		addToPanel(panel, "Tags", listToString(model.getSpecification().getTags()));
		return scroll(panel);
	}

	private static JScrollPane scroll(JPanel panel) {
		JScrollPane jScrollPane = new JScrollPane(panel);
		jScrollPane.setBorder(null);
		return jScrollPane;
	}

	private static void addToPanel(JPanel panel, String title, String value) {
		JLabel titleLabel = new JLabel(title);
		JTextArea valueLabel = createReadTextArea(value);
		valueLabel.setFont(plainMonospaceFont);
		panel.add(titleLabel, "newline, width 130:130:130");
		panel.add(valueLabel, "growx,wmin 270");
		JSeparator component = new JSeparator();
		component.setForeground(Color.white);
		panel.add(component, "newline, growx, span");
	}

	private static String listToString(List<String> list) {
		if(list == null) return "";
		StringBuilder str = new StringBuilder();
		list.forEach(entry -> str.append(entry).append("\n"));
		if(str.length() == 0) return "";
		return str.toString();
	}

	private static String outputToString(OutputNodeSpecification output) {
		StringBuilder str = new StringBuilder();
		str.append("name       : ").append(output.getName()).append("\n");
		str.append("axes       : ").append(output.getAxes()).append("\n");
		str.append("data type  : ").append(output.getDataType()).append("\n");
		str.append("data range : ").append(output.getDataRange()).append("\n");
		str.append("reference  : ").append(output.getReferenceInputName()).append("\n");
		str.append("scale      : ").append(output.getShapeScale()).append("\n");
		str.append("offset     : ").append(output.getShapeOffset()).append("\n");
		return str.toString();
	}

	private static String inputToString(InputNodeSpecification input) {
		StringBuilder str = new StringBuilder();
		str.append("name       : ").append(input.getName()).append("\n");
		str.append("axes       : ").append(input.getAxes()).append("\n");
		str.append("data type  : ").append(input.getDataType()).append("\n");
		str.append("data range : ").append(input.getDataRange()).append("\n");
		str.append("halo       : ").append(input.getHalo()).append("\n");
		str.append("min        : ").append(input.getShapeMin()).append("\n");
		str.append("step       : ").append(input.getShapeStep()).append("\n");
		return str.toString();
	}

	private static <T> String asString(List<T> list, Function<T, String> toStringMethod) {
		return listToString(list.stream()
				.map(toStringMethod)
				.collect(Collectors.toList()));
	}

	private static String citationToString(CitationSpecification citation) {
		return citation.getCitationText() + " " + citation.getDoiText();
	}

	@Override
	public void redraw()
	{
		getWindow().pack();
	}

	@Override
	public void redoLayout()
	{
		// ignored
	}

	@Override
	public void setLabel(final String s)
	{
		// ignored
	}
}
