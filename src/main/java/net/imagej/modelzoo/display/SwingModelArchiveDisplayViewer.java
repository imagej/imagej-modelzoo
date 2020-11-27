/*-
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
package net.imagej.modelzoo.display;

import io.bioimage.specification.CitationSpecification;
import io.bioimage.specification.InputNodeSpecification;
import io.bioimage.specification.OutputNodeSpecification;
import io.bioimage.specification.TransformationSpecification;
import net.imagej.display.ColorTables;
import net.imagej.display.SourceOptimizedCompositeXYProjector;
import net.imagej.modelzoo.ModelZooArchive;
import net.imagej.modelzoo.ModelZooService;
import net.imagej.modelzoo.consumer.command.ModelArchiveUpdateDemoFromFileCommand;
import net.imagej.modelzoo.consumer.command.ModelArchiveUpdateDemoFromImageCommand;
import net.imagej.modelzoo.consumer.model.TensorSample;
import net.imagej.modelzoo.specification.ImageJModelSpecification;
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
import org.scijava.log.LogService;
import org.scijava.module.ModuleException;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.UIService;
import org.scijava.ui.viewer.DisplayViewer;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
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
	@Parameter
	private LogService logService;
	@Parameter
	private UIService uiService;

	private static Color leftBgColor = new Color(0x454549);
	private static Color leftIconBgColor = new Color(0x313134);
	private static final Font font = new JLabel().getFont();
	private static final Font plainMonospaceFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);
	private static final Font plainFont = font.deriveFont(Font.PLAIN);
	private static final Font headerFont = font.deriveFont(Font.BOLD);
	private int previewDim = 240;

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
		JPanel leftPanel = new JPanel(new MigLayout("flowy, ins 0, filly", "", "[][][][][]push[][]"));
		leftPanel.setBackground(leftBgColor);
		leftPanel.setForeground(Color.white);
		leftPanel.add(createLeftTitleIcon(), "growx");
		CardLayout cardLayout = new CardLayout();
		JPanel rightPanel = new JPanel(cardLayout);
		ButtonGroup group = new ButtonGroup();
		ImageIcon inputIcon = new ImageIcon();
		ImageIcon outputIcon = new ImageIcon();
		JPanel previewPanel = createPreviewPanel(model, inputIcon, outputIcon);
		addCard(leftPanel, rightPanel, group, createOverviewPanel(model, previewPanel), "Overview");
		addCard(leftPanel, rightPanel, group, createMetaPanel(model), "Metadata");
		addCard(leftPanel, rightPanel, group, createInputsOutputsPanel(model), "Inputs & Outputs");
		addCard(leftPanel, rightPanel, group, createTrainingPanel(model), "Training");
		JButton saveChangesBtn = createSaveChangesBtn(model);
		leftPanel.add(saveChangesBtn, "gap 6px 11px 6px 0px, growx");
		leftPanel.add(createFileActionsBtn(model, inputIcon, outputIcon, saveChangesBtn, previewPanel), "gap 6px 11px 0px 6px, growx");
		cardLayout.first(rightPanel);
		panel.add(leftPanel, "newline, width 150:150:150, height 100%");
		panel.add(rightPanel, "width 500:500:null, height 100%");
		return panel;
	}

	private JLabel createLeftTitleIcon() {
		JLabel titleIcon = new JLabel(new ImageIcon(getClass().getResource("/icon-modelzoo.png")));
		titleIcon.setBackground(leftIconBgColor);
		titleIcon.setOpaque(true);
		return titleIcon;
	}

	private static Component createTrainingPanel(ModelZooArchive model) {
		if(!ImageJModelSpecification.class.isAssignableFrom(model.getSpecification().getClass())) return null;
		ImageJModelSpecification specification = (ImageJModelSpecification) model.getSpecification();
		if(specification.getImageJConfig() != null && specification.getImageJConfig().getTrainingKwargs() != null) {
			JPanel panel = new JPanel(new MigLayout("", "[][push]", ""));
			addToPanel(panel, "source", specification.getImageJConfig().getTrainingSource());
			specification.getImageJConfig().getTrainingKwargs().forEach((s, o) -> {
				addToPanel(panel, s, o == null ? "null" : o.toString());
			});
			return scroll(panel);
		}
		return null;
	}

	private Component createOverviewPanel(ModelZooArchive model, Component previewPanel) {
		JPanel panel = new JPanel(new MigLayout("fill"));
		JTextArea title = createReadTextArea(model.getSpecification().getName());
		title.setFont(headerFont);
		panel.add(title, "width 20:350:null, span");
		JTextArea descriptionText = createReadTextArea(model.getSpecification().getDescription());
		panel.add(descriptionText, "newline, span,growx,wmin 20");
		panel.add(previewPanel, "newline, push, grow, span");
		panel.add(createActionsBar(model), "newline, span, pushx, growx");
		return panel;
	}

	private Component createFileActionsBtn(ModelZooArchive model, ImageIcon inputIcon, ImageIcon outputIcon, JButton saveChangesBtn, JPanel previewPanel) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(new AbstractAction("Save to..") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new Thread(() -> {
					saveModelTo(model);
					saveChangesBtn.setVisible(false);
				}).start();
			}
		});
		menu.add(new AbstractAction("Copy location to clipboard") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new Thread(() -> {
					StringSelection stringSelection = new StringSelection(model.getLocation().getURI().getPath());
					Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
					clpbrd.setContents(stringSelection, null);
				}).start();
			}
		});
		menu.add(new AbstractAction("Update demo image (from file)") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new Thread(() -> {
					updateTestImageFromFile(model, inputIcon, outputIcon);
					previewPanel.repaint();
					saveChangesBtn.setVisible(true);
				}).start();
			}
		});
		menu.add(new AbstractAction("Update demo image (from open images)") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new Thread(() -> {
					updateTestImageFromOpenImages(model, inputIcon, outputIcon);
					previewPanel.repaint();
					saveChangesBtn.setVisible(true);
				}).start();
			}
		});
		JButton button = new JButton();
		button.setAction(new AbstractAction("File actions") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				menu.show(button, button.getWidth() / 2, button.getHeight() / 2);
			}
		});
		return button;
	}

	private JButton createSaveChangesBtn(ModelZooArchive model) {
		JButton button = new JButton();
		button.setAction(new AbstractAction("Save changes") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				saveChanges(model);
				button.setVisible(false);
			}
		});
		button.setForeground(new Color(200, 0, 0));
		button.setVisible(false);
		return button;
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

	private JPanel createPreviewPanel(ModelZooArchive model, ImageIcon inputIcon, ImageIcon outputIcon) {
		JPanel panel = new JPanel(new MigLayout("ins 0, gap 2 2"));
		reloadTestImages(model, inputIcon, outputIcon);
		int h = previewDim-2;
		int w = previewDim-2;
		panel.add(new JLabel(inputIcon), "height " + h + ":" + h + ":" + h + ", width " + w + ":" + w + ":" + w);
		panel.add(new JLabel(outputIcon), "height " + h + ":" + h + ":" + h + ", width " + w + ":" + w + ":" + w);
		return panel;
	}

	private Component createActionsBar(ModelZooArchive model) {
		JPanel panel = new JPanel(new MigLayout("ins 0, fillx", "[]push[][]", "align bottom"));
		//TODO enable once the features are all ready
		String text = "<html>Format:</span> "
				+ model.getSpecification().getFormatVersion()
				+ " | <span style='font-weight: normal;'>Input axes:</span> "
				+ getInputAxes(model);
		panel.add(new JLabel(text), "spanx");
//		panel.add(createActionButton("Train", () -> train(model)), "newline");
		panel.add(createSanityCheckActionsBtn(model), "");
		panel.add(createPredictActionsBtn(model), "");
		return panel;
	}

	private static String getSource(ModelZooArchive model) {
		return model.getSpecification().getSource();
	}

	private String getInputAxes(ModelZooArchive model) {
		if(model.getSpecification().getInputs() != null &&
				model.getSpecification().getInputs().size() > 0)
			return model.getSpecification().getInputs().get(0).getAxes();
		return "";
	}

	private void sanityCheckFromFiles(ModelZooArchive model) {
		try {
			modelZooService.sanityCheckFromFilesInteractive(model);
		} catch (ModuleException e) {
			e.printStackTrace();
		}
	}

	private void sanityCheckFromImages(ModelZooArchive model) {
		try {
			modelZooService.sanityCheckFromImagesInteractive(model);
		} catch (ModuleException e) {
			e.printStackTrace();
		}
	}

	private Component createSanityCheckActionsBtn(ModelZooArchive model) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(new AbstractAction(".. from open images") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new Thread(() -> sanityCheckFromImages(model)).start();
			}
		});
		menu.add(new AbstractAction(".. from images on disk") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new Thread(() -> sanityCheckFromFiles(model)).start();
			}
		});
		JButton button = new JButton();
		button.setAction(new AbstractAction("Sanity check..") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				menu.show(button, button.getWidth() / 2, button.getHeight() / 2);
			}
		});
		button.setEnabled(modelZooService.canRunSanityCheckInteractive(model));
		return button;
	}

	private Component createPredictActionsBtn(ModelZooArchive model) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(new AbstractAction("Single image or stack") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new Thread(() -> predict(model)).start();
			}
		});
		menu.add(new AbstractAction("Folder of images or stacks") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new Thread(() -> batchPredict(model)).start();
			}
		});
		JButton button = new JButton();
		button.setAction(new AbstractAction("Predict..") {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				menu.show(button, button.getWidth() / 2, button.getHeight() / 2);
			}
		});
		button.setEnabled(modelZooService.canRunPredictionInteractive(model));
		return button;
	}

	private void predict(ModelZooArchive model) {
		try {
			modelZooService.predictInteractive(model);
		} catch (FileNotFoundException | ModuleException e) {
			e.printStackTrace();
		}
	}

	private void batchPredict(ModelZooArchive model) {
		try {
			modelZooService.batchPredictInteractive(model);
		} catch (FileNotFoundException | ModuleException e) {
			e.printStackTrace();
		}
	}

	private void saveModelTo(ModelZooArchive model) {
		JFrame parentFrame = new JFrame();
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Specify where to save model");
		fileChooser.setSelectedFile(new File(model.getLocation().getURI()));
		FileFilter filter = new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().endsWith(".bioimage.io.zip");
			}
			@Override
			public String getDescription() {
				return "bioimage.io archive";
			}
		};
		fileChooser.setFileFilter(filter);
		int userSelection = fileChooser.showSaveDialog(parentFrame);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			String absolutePath = selectedFile.getAbsolutePath();
			if(selectedFile.exists()) {
				DialogPrompt.Result result = uiService.showDialog("File already exists. Override?", DialogPrompt.MessageType.QUESTION_MESSAGE, DialogPrompt.OptionType.YES_NO_OPTION);
				if(!result.equals(DialogPrompt.Result.YES_OPTION)) {
					return;
				}
			}
			logService.info("Saving model to " + absolutePath);
			try {
				modelZooService.io().save(model, absolutePath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void saveChanges(ModelZooArchive model) {
		try {
			String absolutePath = new File(model.getLocation().getURI()).getAbsolutePath();
			System.out.println(absolutePath);
			modelZooService.io().save(model, absolutePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void updateTestImageFromFile(
			ModelZooArchive model, ImageIcon inputIcon, ImageIcon outputIcon) {
		try {
			commandService.run(ModelArchiveUpdateDemoFromFileCommand.class, true, "archive", model).get();
			reloadTestImages(model, inputIcon, outputIcon);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	private void updateTestImageFromOpenImages(
			ModelZooArchive model, ImageIcon inputIcon, ImageIcon outputIcon) {
		try {
			commandService.run(ModelArchiveUpdateDemoFromImageCommand.class, true, "archive", model).get();
			reloadTestImages(model, inputIcon, outputIcon);
		} catch (InterruptedException | ExecutionException e) {
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

	private void reloadTestImages(ModelZooArchive model, ImageIcon testInputIcon, ImageIcon testOutputIcon) {
		display(model, testInputIcon, model.getSampleInputs());
		display(model, testOutputIcon, model.getSampleOutputs());
	}

	private void display(ModelZooArchive model, ImageIcon icon, List<TensorSample> samples) {
		if(samples == null) return;
		if (samples.size() > 0) {
			// TODO this is not great. ideally, we could display multiple input and output tensor samples,
			//  (e.g. by having arrow buttons to go through multiple outputs)
			//  and we could also use the UIService / DisplayService to display the data and not do this type check
			TensorSample sample = samples.get(0);
			if (RandomAccessibleInterval.class.isAssignableFrom(sample.getData().getClass())) {
				icon.setImage(toBufferedImage((RandomAccessibleInterval) sample.getData()));
			}
		}
	}

	private <T extends RealType<T>> BufferedImage toBufferedImage(RandomAccessibleInterval<T> img) {
		for (int i = 2; i < img.numDimensions(); ) {
			img = Views.hyperSlice(img, i, 0);
		}
		img = opService.transform().scaleView(img, new double[]{(double)previewDim/(double)img.dimension(0), (double)previewDim/(double)img.dimension(1)}, new NLinearInterpolatorFactory<>());
		ARGBScreenImage screenImage = new ARGBScreenImage((int)img.dimension(0), (int)img.dimension(1));
		T min = img.randomAccess().get().copy();
		T max = img.randomAccess().get().copy();
		ComputeMinMax<T> minMax = new ComputeMinMax<>(Views.iterable(img), min, max);
		minMax.process();
		RealLUTConverter<? extends RealType<?>> converter = new RealLUTConverter<>(min.getRealDouble(),
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
		JToggleButton btn = new JToggleButton(new AbstractAction(id_card) {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				CardLayout cl = (CardLayout)(rightPanel.getLayout());
				cl.show(rightPanel, id_card);
			}
		});
		styleButton(btn, card != null);
		if(card != null) {
			rightPanel.add(card, id_card);
		} else {
			btn.setEnabled(false);
		}

		leftPanel.add(btn, "spanx, growx");
		if(group.getButtonCount() == 0) btn.setSelected(true);
		group.add(btn);
	}

	private static void styleButton(JToggleButton btn, boolean enabled) {
		btn.setBackground(leftBgColor);
		btn.setForeground(Color.white);
		btn.setFocusPainted(false);
		btn.setBorderPainted(false);
		if(enabled) {
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
	}

	private static Component createInputsOutputsPanel(ModelZooArchive model) {
		JPanel panel = new JPanel(new MigLayout());
		addToPanel(panel, "Language", model.getSpecification().getLanguage());
		addToPanel(panel, "Framework", model.getSpecification().getFramework());
		addToPanel(panel, "Source", getSource(model));
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
		str.append("name           : ").append(output.getName()).append("\n");
		str.append("axes           : ").append(output.getAxes()).append("\n");
		str.append("data type      : ").append(output.getDataType()).append("\n");
		str.append("data range     : ").append(output.getDataRange()).append("\n");
		str.append("reference      : ").append(output.getReferenceInputName()).append("\n");
		str.append("scale          : ").append(output.getShapeScale()).append("\n");
		str.append("offset         : ").append(output.getShapeOffset()).append("\n");
		str.append("halo           : ").append(output.getHalo()).append("\n");
		str.append("postprocessing : ").append(transformationsToString(output.getPostprocessing())).append("\n");
		return str.toString();
	}

	private static String inputToString(InputNodeSpecification input) {
		StringBuilder str = new StringBuilder();
		str.append("name         : ").append(input.getName()).append("\n");
		str.append("axes         : ").append(input.getAxes()).append("\n");
		str.append("data type    : ").append(input.getDataType()).append("\n");
		str.append("data range   : ").append(input.getDataRange()).append("\n");
		str.append("halo         : ").append(input.getHalo()).append("\n");
		str.append("min          : ").append(input.getShapeMin()).append("\n");
		str.append("step         : ").append(input.getShapeStep()).append("\n");
		str.append("preprocesing : ").append(transformationsToString(input.getPreprocessing())).append("\n");
		return str.toString();
	}

	private static String transformationsToString(List<TransformationSpecification> transformations) {
		StringBuilder str = new StringBuilder();
		for (TransformationSpecification transformation : transformations) {
			str.append(transformation.getName()).append(" ");
		}
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
