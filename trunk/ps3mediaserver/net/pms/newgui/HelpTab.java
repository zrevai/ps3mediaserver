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

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Sets up the panel for the help tab and loads its contents from a file.
 */
public class HelpTab {
	private JEditorPane editorPane;

	/**
	 * Return the editor pane for the help tab containing the help contents.
	 * @return The editor pane for the help tab
	 */
	public JEditorPane getList() {
		return editorPane;
	}

	/**
	 * Set up the panel for the help tab and load its contents from a file.
	 * @return The component containing the help tab and its contents
	 */
	public JComponent build() {
		FormLayout layout = new FormLayout("left:pref, 0:grow", //$NON-NLS-1$
				"pref, fill:default:grow"); //$NON-NLS-1$
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setOpaque(true);
		CellConstraints cc = new CellConstraints();
		editorPane = new JEditorPane();
		editorPane.setEditable(false);
		editorPane.setContentType("text/html");
		editorPane.setBackground(Color.WHITE);

		try {
			// Read the HTML help file
			File file = new File("documentation/index.html"); //$NON-NLS-1$

			// Display the HTML help file in the editor
			editorPane.setPage(file.toURI().toURL());
		} catch (MalformedURLException e) {
			// TODO: log error
		} catch (IOException e) {
			// TODO: log error
		}

		// Enable internal anchor links
		editorPane.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent event) {
				try {
					if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
						String urlString = event.getURL().toExternalForm();

						if (urlString.startsWith("http://") || urlString.startsWith("https://")
								|| urlString.startsWith("ftp://")) {
							// Open external links in the desktop web browser
							URI uri = new URI(urlString);
		                    Desktop.getDesktop().browse(uri); 
						} else {
							// Open anchor links in the editorPane
							editorPane.setPage(event.getURL());
						}
					}
				} catch (IOException e) {
					// TODO: log error
				} catch (URISyntaxException e) {
					// TODO: log error
				}
			}
		});

		JScrollPane pane = new JScrollPane(editorPane,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.setPreferredSize(new Dimension(500, 400));
		builder.add(pane, cc.xy(2, 2));

		return builder.getPanel();
	}
}