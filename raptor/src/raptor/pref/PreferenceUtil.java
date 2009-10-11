/**
 * New BSD License
 * http://www.opensource.org/licenses/bsd-license.php
 * Copyright (c) 2009, RaptorProject (http://code.google.com/p/raptor-chess-interface/)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of the RaptorProject nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package raptor.pref;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.preference.PreferencePage;

import raptor.Raptor;
import raptor.connector.Connector;
import raptor.service.ConnectorService;

/**
 * A class containing utility methods for Preferences.
 */
public class PreferenceUtil {

	/**
	 * Launches the preference dialog.
	 * 
	 * All connectors in the ConnectorService have their preference nodes added.
	 */
	public static void launchPreferenceDialog() {
		// Create the preference manager
		PreferenceManager mgr = new PreferenceManager();

		mgr.addToRoot(new PreferenceNode("raptor", new RaptorPage()));
		mgr
				.addToRoot(new PreferenceNode("raptorWindow",
						new RaptorWindowPage()));
		mgr.addTo("raptorWindow", new PreferenceNode("layout1",
				new RaptorWindowLayoutPage("1", "app-Layout1")));
		mgr.addTo("raptorWindow", new PreferenceNode("layout2",
				new RaptorWindowLayoutPage("2", "app-Layout2")));
		mgr.addTo("raptorWindow", new PreferenceNode("layout3",
				new RaptorWindowLayoutPage("3", "app-Layout3")));

		mgr.addToRoot(new PreferenceNode("chatConsole", new ChatConsolePage()));
		mgr.addTo("chatConsole", new PreferenceNode("messageColors",
				new ChatConsoleMessageColors()));
		mgr.addTo("chatConsole", new PreferenceNode("channelColors",
				new ChatConsoleChannelColorsPage()));

		mgr.addToRoot(new PreferenceNode("chessBoard", new ChessBoardPage()));
		mgr.addTo("chessBoard", new PreferenceNode("behavior",
				new ChessBoardBehaviorPage()));
		mgr.addTo("chessBoard", new PreferenceNode("clocks",
				new ChessBoardClocksPage()));
		mgr.addTo("chessBoard", new PreferenceNode("colors",
				new ChessBoardColorsPage()));
		mgr.addTo("chessBoard", new PreferenceNode("fonts",
				new ChessBoardFontsPage()));

		// Add the connector preference nodes.
		Connector[] connectors = ConnectorService.getInstance().getConnectors();
		for (Connector connector : connectors) {

			PreferencePage root = connector.getRootPreferencePage();
			if (root != null) {
				mgr
						.addToRoot(new PreferenceNode(connector.getShortName(),
								root));
				PreferenceNode[] secondaries = connector
						.getSecondaryPreferenceNodes();
				if (secondaries != null && secondaries.length > 0) {
					for (PreferenceNode node : secondaries) {
						mgr.addTo(connector.getShortName(), node);
					}
				}
			}
		}

		// Create the preferences dialog
		PreferenceDialog dlg = new PreferenceDialog(Raptor.getInstance()
				.getRaptorWindow().getShell(), mgr);

		// Open the dialog
		dlg.open();
	}
}