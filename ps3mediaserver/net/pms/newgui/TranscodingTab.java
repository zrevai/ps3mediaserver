/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.newgui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.encoders.Player;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.sun.jna.Platform;

public class TranscodingTab {
	private final PmsConfiguration configuration;

	TranscodingTab(PmsConfiguration configuration) {
		this.configuration = configuration;
	}
	private JCheckBox disableSubs;

	public JCheckBox getDisableSubs() {
		return disableSubs;
	}
	private JTextField forcetranscode;
	private JTextField notranscode;
	private JTextField maxbuffer;
	private JComboBox nbcores;
	private DefaultMutableTreeNode parent[];
	private JPanel tabbedPane;
	private CardLayout cl;
	private JTextField abitrate;
	private JTextField maxbitrate;
	private JTree tree;
	private JCheckBox forcePCM;
	private JCheckBox hdaudiopass;
	private JCheckBox forceDTSinPCM;
	private JComboBox channels;
	private JComboBox vq;
	private JCheckBox ac3remux;
	private JCheckBox mpeg2remux;
	private JCheckBox chapter_support;
	private JTextField chapter_interval;
	private static final int MAX_CORES = 32;

	private void updateEngineModel() {
		ArrayList<String> engines = new ArrayList<String>();
		Object root = tree.getModel().getRoot();
		for (int i = 0; i < tree.getModel().getChildCount(root); i++) {
			Object firstChild = tree.getModel().getChild(root, i);
			if (!tree.getModel().isLeaf(firstChild)) {
				for (int j = 0; j < tree.getModel().getChildCount(firstChild); j++) {
					Object secondChild = tree.getModel().getChild(firstChild, j);
					if (secondChild instanceof TreeNodeSettings) {
						TreeNodeSettings tns = (TreeNodeSettings) secondChild;
						if (tns.isEnable() && tns.getPlayer() != null) {
							engines.add(tns.getPlayer().id());
						}
					}
				}
			}
		}
		configuration.setEnginesAsList(engines);
	}

	private void handleCardComponentChange(Component component) {
		tabbedPane.setPreferredSize(component.getPreferredSize());
		SwingUtilities.updateComponentTreeUI(tabbedPane);
	}

	public JComponent build() {
		FormLayout mainlayout = new FormLayout(
			"left:pref, pref, 7dlu, pref, pref, fill:10:grow",
			"fill:10:grow"
			);
		PanelBuilder builder = new PanelBuilder(mainlayout);
		builder.setBorder(Borders.DLU4_BORDER);

		builder.setOpaque(true);

		CellConstraints cc = new CellConstraints();
		builder.add(buildRightTabbedPane(), cc.xyw(4, 1, 3));
		builder.add(buildLeft(), cc.xy(2, 1));

		return builder.getPanel();
	}

	public JComponent buildRightTabbedPane() {
		cl = new CardLayout();
		tabbedPane = new JPanel(cl);
		tabbedPane.setBorder(BorderFactory.createEmptyBorder());
		JScrollPane scrollPane = new JScrollPane(tabbedPane);
		scrollPane.setBorder(null);
		return scrollPane;
	}

	public JComponent buildLeft() {
		FormLayout layout = new FormLayout(
			"left:pref, pref, pref, pref, 0:grow",
			"fill:10:grow, 3dlu, p, 3dlu, p, 3dlu, p");

		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.EMPTY_BORDER);
		builder.setOpaque(false);

		CellConstraints cc = new CellConstraints();

		JButton but = new JButton(LooksFrame.readImageIcon("kdevelop_down-32.png"));
		but.setToolTipText(Messages.getString("TrTab2.6"));
		but.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TreePath path = tree.getSelectionModel().getSelectionPath();
				if (path != null && path.getLastPathComponent() instanceof TreeNodeSettings) {
					TreeNodeSettings node = ((TreeNodeSettings) path.getLastPathComponent());
					if (node.getPlayer() != null) {
						DefaultTreeModel dtm = (DefaultTreeModel) tree.getModel();   // get the tree model
						//now get the index of the selected node in the DefaultTreeModel
						int index = dtm.getIndexOfChild(node.getParent(), node);
						// if selected node is first, return (can't move it up)
						if (index < node.getParent().getChildCount() - 1) {
							dtm.insertNodeInto(node, (DefaultMutableTreeNode) node.getParent(), index + 1);   // move the node
							dtm.reload();
							for (int i = 0; i < tree.getRowCount(); i++) {
								tree.expandRow(i);
							}
							tree.getSelectionModel().setSelectionPath(new TreePath(node.getPath()));
							updateEngineModel();
						}
					}
				}
			}
		});
		builder.add(but, cc.xy(2, 3));

		JButton but2 = new JButton(LooksFrame.readImageIcon("up-32.png"));
		but2.setToolTipText(Messages.getString("TrTab2.6"));
		but2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TreePath path = tree.getSelectionModel().getSelectionPath();
				if (path != null && path.getLastPathComponent() instanceof TreeNodeSettings) {
					TreeNodeSettings node = ((TreeNodeSettings) path.getLastPathComponent());
					if (node.getPlayer() != null) {
						DefaultTreeModel dtm = (DefaultTreeModel) tree.getModel();   // get the tree model
						//now get the index of the selected node in the DefaultTreeModel
						int index = dtm.getIndexOfChild(node.getParent(), node);
						// if selected node is first, return (can't move it up)
						if (index != 0) {
							dtm.insertNodeInto(node, (DefaultMutableTreeNode) node.getParent(), index - 1);   // move the node
							dtm.reload();
							for (int i = 0; i < tree.getRowCount(); i++) {
								tree.expandRow(i);
							}
							tree.getSelectionModel().setSelectionPath(new TreePath(node.getPath()));
							updateEngineModel();
						}
					}
				}
			}
		});
		builder.add(but2, cc.xy(3, 3));

		JButton but3 = new JButton(LooksFrame.readImageIcon("connect_no-32.png"));
		but3.setToolTipText(Messages.getString("TrTab2.0"));
		but3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TreePath path = tree.getSelectionModel().getSelectionPath();
				if (path != null && path.getLastPathComponent() instanceof TreeNodeSettings && ((TreeNodeSettings) path.getLastPathComponent()).getPlayer() != null) {
					((TreeNodeSettings) path.getLastPathComponent()).setEnable(!((TreeNodeSettings) path.getLastPathComponent()).isEnable());
					updateEngineModel();
					tree.updateUI();
				}
			}
		});
		builder.add(but3, cc.xy(4, 3));

		DefaultMutableTreeNode root = new DefaultMutableTreeNode(Messages.getString("TrTab2.11"));
		TreeNodeSettings commonEnc = new TreeNodeSettings(Messages.getString("TrTab2.5"), null, buildCommon());
		commonEnc.getConfigPanel().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				handleCardComponentChange(e.getComponent());
			}
		});
		tabbedPane.add(commonEnc.id(), commonEnc.getConfigPanel());
		root.add(commonEnc);

		parent = new DefaultMutableTreeNode[5];
		parent[0] = new DefaultMutableTreeNode(Messages.getString("TrTab2.14"));
		parent[1] = new DefaultMutableTreeNode(Messages.getString("TrTab2.15"));
		parent[2] = new DefaultMutableTreeNode(Messages.getString("TrTab2.16"));
		parent[3] = new DefaultMutableTreeNode(Messages.getString("TrTab2.17"));
		parent[4] = new DefaultMutableTreeNode(Messages.getString("TrTab2.18"));
		root.add(parent[0]);
		root.add(parent[1]);
		root.add(parent[2]);
		root.add(parent[3]);
		root.add(parent[4]);

		tree = new JTree(new DefaultTreeModel(root)) {
			private static final long serialVersionUID = -6703434752606636290L;
		};
		tree.setRootVisible(false);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if (e.getNewLeadSelectionPath() != null && e.getNewLeadSelectionPath().getLastPathComponent() instanceof TreeNodeSettings) {
					TreeNodeSettings tns = (TreeNodeSettings) e.getNewLeadSelectionPath().getLastPathComponent();
					cl.show(tabbedPane, tns.id());
				}
			}
		});

		tree.setCellRenderer(new TreeRenderer());
		JScrollPane pane = new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		builder.add(pane, cc.xyw(2, 1, 4));

		builder.addLabel(Messages.getString("TrTab2.19"), cc.xyw(2, 5, 4));
		builder.addLabel(Messages.getString("TrTab2.20"), cc.xyw(2, 7, 4));

		return builder.getPanel();
	}

	public void addEngines() {
		ArrayList<Player> disPlayers = new ArrayList<Player>();
		ArrayList<Player> ordPlayers = new ArrayList<Player>();
		PMS r = PMS.get();

		for (String id : configuration.getEnginesAsList(r.getRegistry())) {
			//boolean matched = false;
			for (Player p : PMS.get().getAllPlayers()) {
				if (p.id().equals(id)) {
					ordPlayers.add(p);
					//matched = true;
				}
			}
		}

		for (Player p : PMS.get().getAllPlayers()) {
			if (!ordPlayers.contains(p)) {
				ordPlayers.add(p);
				disPlayers.add(p);
			}
		}

		for (Player p : ordPlayers) {
			TreeNodeSettings en = new TreeNodeSettings(p.name(), p, null);
			if (disPlayers.contains(p)) {
				en.setEnable(false);
			}
			JComponent jc = en.getConfigPanel();
			if (jc == null) {
				jc = buildEmpty();
			}
			jc.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentShown(ComponentEvent e) {
					handleCardComponentChange(e.getComponent());
				}
			});
			tabbedPane.add(en.id(), jc);
			parent[p.purpose()].add(en);
		}

		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}


		tree.setSelectionRow(0);
	}

	public JComponent buildEmpty() {
		FormLayout layout = new FormLayout(
			"left:pref, 2dlu, pref:grow",
			"p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p , 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 20dlu, p, 3dlu, p, 3dlu, p");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.EMPTY_BORDER);
		builder.setOpaque(false);

		CellConstraints cc = new CellConstraints();

		builder.addSeparator(Messages.getString("TrTab2.1"), cc.xyw(1, 1, 3));

		return builder.getPanel();
	}

	public JComponent buildCommon() {
		FormLayout layout = new FormLayout(
			"left:pref, 2dlu, pref:grow",
			"p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 9dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu,  p, 2dlu, p, 9dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 9dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p");
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.EMPTY_BORDER);
		builder.setOpaque(false);

		CellConstraints cc = new CellConstraints();

		maxbuffer = new JTextField("" + configuration.getMaxMemoryBufferSize());
		maxbuffer.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				try {
					int ab = Integer.parseInt(maxbuffer.getText());
					configuration.setMaxMemoryBufferSize(ab);
				} catch (NumberFormatException nfe) {
				}
			}
		});

		JComponent cmp = builder.addSeparator(Messages.getString("NetworkTab.5"), cc.xyw(1, 1, 3));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		builder.addLabel(Messages.getString("NetworkTab.6").replaceAll("MAX_BUFFER_SIZE", configuration.getMaxMemoryBufferSizeStr()), cc.xy(1, 3));
		builder.add(maxbuffer, cc.xy(3, 3));

		builder.addLabel(Messages.getString("NetworkTab.7") + Runtime.getRuntime().availableProcessors() + ")", cc.xy(1, 5));

		String[] guiCores = new String[MAX_CORES];
		for (int i = 0; i < MAX_CORES; i++) {
			guiCores[i] = Integer.toString(i + 1);
		}
		nbcores = new JComboBox(guiCores);
		nbcores.setEditable(false);
		int nbConfCores = configuration.getNumberOfCpuCores();
		if (nbConfCores > 0 && nbConfCores <= MAX_CORES) {
			nbcores.setSelectedItem(Integer.toString(nbConfCores));
		} else {
			nbcores.setSelectedIndex(0);
		}

		nbcores.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
			        configuration.setNumberOfCpuCores(Integer.parseInt(e.getItem().toString()));
			}
		});
		builder.add(nbcores, cc.xy(3, 5));

		chapter_interval = new JTextField("" + configuration.getChapterInterval());
		chapter_interval.setEnabled(configuration.isChapterSupport());
		chapter_interval.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				try {
					int ab = Integer.parseInt(chapter_interval.getText());
					configuration.setChapterInterval(ab);
				} catch (NumberFormatException nfe) {
				}
			}
		});

		chapter_support = new JCheckBox(Messages.getString("TrTab2.52"));
		chapter_support.setContentAreaFilled(false);
		chapter_support.setSelected(configuration.isChapterSupport());

		chapter_support.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setChapterSupport((e.getStateChange() == ItemEvent.SELECTED));
				chapter_interval.setEnabled(configuration.isChapterSupport());
			}
		});

		builder.add(chapter_support, cc.xy(1, 7));

		builder.add(chapter_interval, cc.xy(3, 7));

		cmp = builder.addSeparator(Messages.getString("TrTab2.3"), cc.xyw(1, 11, 3));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		channels = new JComboBox(new Object[]{"2 channels (Stereo)", "6 channels (5.1)" /*, "8 channels 7.1" */}); // 7.1 not supported by Mplayer :\
		channels.setEditable(false);
		if (configuration.getAudioChannelCount() == 2) {
			channels.setSelectedIndex(0);
		} else {
			channels.setSelectedIndex(1);
		}
		channels.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setAudioChannelCount(Integer.parseInt(e.getItem().toString().substring(0, 1)));
			}
		});

		builder.addLabel(Messages.getString("TrTab2.50"), cc.xy(1, 13));
		builder.add(channels, cc.xy(3, 13));

		abitrate = new JTextField("" + configuration.getAudioBitrate());
		abitrate.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				try {
					int ab = Integer.parseInt(abitrate.getText());
					configuration.setAudioBitrate(ab);
				} catch (NumberFormatException nfe) {
				}
			}
		});

		builder.addLabel(Messages.getString("TrTab2.29"), cc.xy(1, 15));
		builder.add(abitrate, cc.xy(3, 15));
		
		hdaudiopass = new JCheckBox(Messages.getString("TrTab2.53") + (Platform.isWindows() ? Messages.getString("TrTab2.21") : ""));
		hdaudiopass.setContentAreaFilled(false);
		if (configuration.isHDAudioPassthrough()) {
			hdaudiopass.setSelected(true);
		}
		hdaudiopass.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				configuration.setHDAudioPassthrough(hdaudiopass.isSelected());
				if (configuration.isHDAudioPassthrough()) {
					JOptionPane.showMessageDialog(
						(JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
						Messages.getString("TrTab2.10"),
						"Information",
						JOptionPane.INFORMATION_MESSAGE);
				}
			}
		});

		builder.add(hdaudiopass, cc.xyw(1, 19, 3));

		forceDTSinPCM = new JCheckBox(Messages.getString("TrTab2.28") + (Platform.isWindows() ? Messages.getString("TrTab2.21") : ""));
		forceDTSinPCM.setContentAreaFilled(false);
		if (configuration.isDTSEmbedInPCM()) {
			forceDTSinPCM.setSelected(true);
		}
		forceDTSinPCM.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				configuration.setDTSEmbedInPCM(forceDTSinPCM.isSelected());
				if (configuration.isDTSEmbedInPCM()) {
					JOptionPane.showMessageDialog(
						(JFrame) (SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame())),
						Messages.getString("TrTab2.10"),
						"Information",
						JOptionPane.INFORMATION_MESSAGE);
				}
			}
		});

		builder.add(forceDTSinPCM, cc.xyw(1, 17, 3));

		forcePCM = new JCheckBox(Messages.getString("TrTab2.27"));
		forcePCM.setContentAreaFilled(false);
		if (configuration.isMencoderUsePcm()) {
			forcePCM.setSelected(true);
		}
		forcePCM.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setMencoderUsePcm(e.getStateChange() == ItemEvent.SELECTED);
			}
		});

		builder.add(forcePCM, cc.xyw(1, 21, 3));

		ac3remux = new JCheckBox(Messages.getString("MEncoderVideo.32") + (Platform.isWindows() ? Messages.getString("TrTab2.21") : ""));
		ac3remux.setContentAreaFilled(false);
		if (configuration.isRemuxAC3()) {
			ac3remux.setSelected(true);
		}
		ac3remux.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setRemuxAC3((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		builder.add(ac3remux, cc.xyw(1, 23, 3));

		mpeg2remux = new JCheckBox(Messages.getString("MEncoderVideo.39") + (Platform.isWindows() ? Messages.getString("TrTab2.21") : ""));
		mpeg2remux.setContentAreaFilled(false);
		if (configuration.isMencoderRemuxMPEG2()) {
			mpeg2remux.setSelected(true);
		}
		mpeg2remux.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				configuration.setMencoderRemuxMPEG2((e.getStateChange() == ItemEvent.SELECTED));
			}
		});

		builder.add(mpeg2remux, cc.xyw(1, 25, 3));

		cmp = builder.addSeparator(Messages.getString("TrTab2.4"), cc.xyw(1, 27, 3));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));


		builder.addLabel(Messages.getString("TrTab2.30"), cc.xy(1, 29));


		maxbitrate = new JTextField("" + configuration.getMaximumBitrate());
		maxbitrate.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setMaximumBitrate(maxbitrate.getText());
			}
		});
		builder.add(maxbitrate, cc.xy(3, 29));

		builder.addLabel(Messages.getString("TrTab2.32"), cc.xyw(1, 31, 3));

		Object data[] = new Object[]{configuration.getMencoderMainSettings(),
			"keyint=5:vqscale=1:vqmin=2  /* Great Quality */",
			"keyint=5:vqscale=1:vqmin=1  /* Lossless Quality */",
			"keyint=5:vqscale=2:vqmin=3  /* Good quality */",
			"keyint=25:vqmax=5:vqmin=2  /* Good quality for HD Wifi Transcoding */",
			"keyint=25:vqmax=7:vqmin=2  /* Medium quality for HD Wifi Transcoding */",
			"keyint=25:vqmax=8:vqmin=3  /* Low quality, Low-end CPU or HD Wifi Transcoding */"};
		MyComboBoxModel cbm = new MyComboBoxModel(data);

		vq = new JComboBox(cbm);
		vq.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					String s = (String) e.getItem();
					if (s.indexOf("/*") > -1) {
						s = s.substring(0, s.indexOf("/*")).trim();
					}
					configuration.setMencoderMainSettings(s);
				}
			}
		});
		vq.getEditor().getEditorComponent().addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				vq.getItemListeners()[0].itemStateChanged(new ItemEvent(vq, 0, vq.getEditor().getItem(), ItemEvent.SELECTED));
			}
		});
		vq.setEditable(true);
		builder.add(vq, cc.xyw(1, 33, 3));

		String help1 = Messages.getString("TrTab2.39");
		help1 += Messages.getString("TrTab2.40");
		help1 += Messages.getString("TrTab2.41");
		help1 += Messages.getString("TrTab2.42");
		help1 += Messages.getString("TrTab2.43");
		help1 += Messages.getString("TrTab2.44");

		JTextArea decodeTips = new JTextArea(help1);
		decodeTips.setEditable(false);
		decodeTips.setBorder(BorderFactory.createEtchedBorder());
		decodeTips.setBackground(new Color(255, 255, 192));
		builder.add(decodeTips, cc.xyw(1, 43, 3));

		disableSubs = new JCheckBox(Messages.getString("TrTab2.51"));
		disableSubs.setContentAreaFilled(false);

		cmp = builder.addSeparator(Messages.getString("TrTab2.7"), cc.xyw(1, 35, 3));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		builder.add(disableSubs, cc.xy(1, 37));

		builder.addLabel(Messages.getString("TrTab2.8"), cc.xy(1, 39));

		notranscode = new JTextField(configuration.getNoTranscode());
		notranscode.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setNoTranscode(notranscode.getText());
			}
		});
		builder.add(notranscode, cc.xy(3, 39));

		builder.addLabel(Messages.getString("TrTab2.9"), cc.xy(1, 41));

		forcetranscode = new JTextField(configuration.getForceTranscode());
		forcetranscode.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				configuration.setForceTranscode(forcetranscode.getText());
			}
		});
		builder.add(forcetranscode, cc.xy(3, 41));

		return builder.getPanel();
	}
}