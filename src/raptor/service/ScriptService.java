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
package raptor.service;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import raptor.Raptor;
import raptor.connector.Connector;
import raptor.script.ChatScript;
import raptor.script.GameScript;
import raptor.script.ChatScript.ChatScriptType;
import raptor.script.GameScript.GameScriptControllerType;
import raptor.script.GameScript.GameScriptType;

/**
 * Manages the Raptor scripts. There are two types of scripts: system and user
 * scripts. User scripts are stored in the users script directory, while system
 * scripts are stored in the resources/script directory. You can't save or
 * delete a system script. When a system script is saved, it is saved as a user
 * script.
 */
public class ScriptService {
	private static final Log LOG = LogFactory.getLog(ScriptService.class);

	private static final ScriptService singletonInstance = new ScriptService();

	public static interface ScriptServiceListener {
		public void onChatScriptsChanged();

		public void onGameScriptsChanged();
	}

	public Map<String, ChatScript> chatScriptMap = new HashMap<String, ChatScript>();

	public Map<String, GameScript> gameScriptMap = new HashMap<String, GameScript>();

	public List<ScriptServiceListener> listeners = Collections
			.synchronizedList(new ArrayList<ScriptServiceListener>(5));

	public static ScriptService getInstance() {
		return singletonInstance;
	}

	private ScriptService() {
		reload();
	}

	public void addScriptServiceListener(ScriptServiceListener listener) {
		listeners.add(listener);
	}

	/**
	 * Deletes the specified script. System scripts , or the scripts in
	 * resources/script are never touched.
	 */
	public boolean deleteChatScript(String scriptName) {
		chatScriptMap.remove(scriptName);
		fireChatScriptChanged();
		return new File(Raptor.USER_RAPTOR_HOME_PATH + "/scripts/chat/"
				+ scriptName + ".properties").delete();
	}

	/**
	 * Deletes the specified script. System scripts , or the scripts in
	 * resources/script are never touched.
	 */
	public boolean deleteGameScript(String scriptName) {
		gameScriptMap.remove(scriptName);
		fireGameScriptChanged();
		return new File(Raptor.USER_RAPTOR_HOME_PATH + "/scripts/game/"
				+ scriptName + ".properties").delete();
	}

	public void dispose() {
		listeners.clear();
		chatScriptMap.clear();
		gameScriptMap.clear();
	}

	/**
	 * Returns all chat scripts sorted by name.
	 */
	public ChatScript[] getAllChatScripts() {
		ArrayList<ChatScript> chatScripts = new ArrayList<ChatScript>(
				chatScriptMap.values());
		Collections
				.sort(chatScripts, new ChatScript.ChatScriptNameComparator());
		return chatScripts.toArray(new ChatScript[0]);
	}

	/**
	 * Returns all game scripts sorted by name.
	 */
	public GameScript[] getAllGameScripts() {
		ArrayList<GameScript> gameScripts = new ArrayList<GameScript>(
				gameScriptMap.values());
		Collections
				.sort(gameScripts, new GameScript.GameScriptNameComparator());
		return gameScripts.toArray(new GameScript[0]);
	}

	public ChatScript getChatScript(String name) {
		return chatScriptMap.get(name);
	}

	/**
	 * Returns all active chat scripts that match the specified connector and
	 * chat script type. The result is sorted by the order field.
	 */
	public ChatScript[] getChatScripts(Connector connector, ChatScriptType type) {
		ArrayList<ChatScript> chatScripts = new ArrayList<ChatScript>(20);
		for (ChatScript script : chatScriptMap.values()) {
			if (script.getScriptConnectorType() == connector
					.getScriptConnectorType()
					&& script.getChatScriptType() == type && script.isActive()) {
				chatScripts.add(script);
			}
		}
		Collections.sort(chatScripts);
		return chatScripts.toArray(new ChatScript[0]);
	}

	public GameScript getGameScript(String name) {
		return gameScriptMap.get(name);
	}

	/**
	 * Returns all active game scripts that match the specified
	 * connector,controller type, and game script type.
	 */
	public GameScript[] getGameScripts(Connector connector,
			GameScriptControllerType controllerType,
			GameScriptType gameScriptType) {
		ArrayList<GameScript> gameScripts = new ArrayList<GameScript>(20);

		for (GameScript script : gameScriptMap.values()) {
			if (script.getScriptConnectorType() == connector
					.getScriptConnectorType()
					&& script.getGameScriptControllerType() == controllerType
					&& script.getGameScriptType() == gameScriptType) {
				gameScripts.add(script);
			}
		}
		Collections.sort(gameScripts);
		return gameScripts.toArray(new GameScript[0]);
	}

	/**
	 * Reloads all of the scripts.
	 */
	public void reload() {
		chatScriptMap.clear();
		gameScriptMap.clear();
		loadGameScripts();
		loadChatScripts();
	}

	public void removeScriptServiceListener(ScriptServiceListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Saves the chat script. Scripts are always saved in the users home
	 * directory. System scripts , or the scripts in resources/script are never
	 * touched.
	 */
	public void saveChatScript(ChatScript script) {
		String fileName = Raptor.USER_RAPTOR_HOME_PATH + "/scripts/chat/"
				+ script.getName() + ".properties";
		try {
			ChatScript.store(script, fileName);
		} catch (IOException ioe) {
			Raptor.getInstance().onError("Error saving chat script", ioe);
		}
		chatScriptMap.put(script.getName(), script);
		fireChatScriptChanged();
	}

	/**
	 * Saves the game script. Scripts are always saved in the users home
	 * directory. System scripts, or the scripts in resources/script are never
	 * touched.
	 */
	public void saveGameScript(GameScript script) {
		String fileName = Raptor.USER_RAPTOR_HOME_PATH + "/scripts/game/"
				+ script.getName() + ".properties";
		try {
			GameScript.store(script, fileName);
		} catch (IOException ioe) {
			Raptor.getInstance().onError("Error saving game script", ioe);
		}

		gameScriptMap.put(script.getName(), script);
		fireGameScriptChanged();
	}

	protected void fireChatScriptChanged() {
		synchronized (listeners) {
			for (ScriptServiceListener listener : listeners) {
				listener.onChatScriptsChanged();
			}
		}
	}

	protected void fireGameScriptChanged() {
		synchronized (listeners) {
			for (ScriptServiceListener listener : listeners) {
				listener.onGameScriptsChanged();
			}
		}
	}

	protected void loadChatScripts() {
		int count = 0;
		long startTime = System.currentTimeMillis();

		File systemScripts = new File("resources/scripts/chat");
		File[] files = systemScripts.listFiles(new FilenameFilter() {

			public boolean accept(File arg0, String arg1) {
				return arg1.endsWith(".properties");
			}
		});

		if (files != null) {
			for (File file : files) {
				try {
					ChatScript script = ChatScript.load(file.getAbsolutePath());
					chatScriptMap.put(script.getName(), script);
					script.setSystemScript(true);
					count++;
				} catch (IOException ioe) {
					Raptor.getInstance().onError(
							"Error loading game script " + file.getName()
									+ ",ioe");
				}
			}
		}

		File userScripts = new File(Raptor.USER_RAPTOR_HOME_PATH
				+ "/scripts/chat");
		File[] userFiles = userScripts.listFiles(new FilenameFilter() {
			public boolean accept(File arg0, String arg1) {
				return arg1.endsWith(".properties");
			}
		});

		if (userFiles != null) {
			for (File file : userFiles) {
				try {
					ChatScript script = ChatScript.load(file.getAbsolutePath());
					chatScriptMap.put(script.getName(), script);
					count++;
				} catch (IOException ioe) {
					Raptor.getInstance().onError(
							"Error loading game script " + file.getName()
									+ ",ioe");
				}
			}
		}

		if (LOG.isInfoEnabled()) {
			LOG.info("Loaded " + count + " chat scripts in "
					+ (System.currentTimeMillis() - startTime) + "ms");
		}
	}

	protected void loadGameScripts() {
		int count = 0;
		long startTime = System.currentTimeMillis();

		File systemScripts = new File("resources/scripts/game");
		File[] files = systemScripts.listFiles(new FilenameFilter() {

			public boolean accept(File arg0, String arg1) {
				return arg1.endsWith(".properties");
			}
		});

		if (files != null) {
			for (File file : files) {
				try {
					GameScript script = GameScript.load(file.getAbsolutePath());
					script.setSystemScript(true);
					gameScriptMap.put(script.getName(), script);
					count++;
				} catch (IOException ioe) {
					Raptor.getInstance().onError(
							"Error loading game script " + file.getName()
									+ ",ioe");
				}
			}
		}

		File userScripts = new File(Raptor.USER_RAPTOR_HOME_PATH
				+ "/scripts/game");
		File[] userFiles = userScripts.listFiles(new FilenameFilter() {
			public boolean accept(File arg0, String arg1) {
				return arg1.endsWith(".properties");
			}
		});

		if (userFiles != null) {
			for (File file : userFiles) {
				try {
					GameScript script = GameScript.load(file.getAbsolutePath());
					gameScriptMap.put(script.getName(), script);
					count++;
				} catch (IOException ioe) {
					Raptor.getInstance().onError(
							"Error loading game script " + file.getName()
									+ ",ioe");
				}
			}
		}

		if (LOG.isInfoEnabled()) {
			LOG.info("Loaded " + count + " game scripts in "
					+ (System.currentTimeMillis() - startTime) + "ms");
		}
	}
}