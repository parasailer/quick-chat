/*
 * Copyright (c) 2020, Parasailer <https://github.com/parasailer>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package parasailer.quickchat;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.IconID;
import net.runelite.api.IndexedSprite;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Quick Chat",
	description = "Enable Quick Chat keyboard shortcuts",
	enabledByDefault = false
)
public class QuickChatPlugin extends Plugin
{
	private static final String SCRIPT_EVENT_SET_CHATBOX_INPUT = "setChatboxInput";

	@Inject
	private Client client;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private WorldService worldService;

	@Inject
	private ClientThread clientThread;

	// Offset for Quick Chat icon
	private int quickChatIconOffset = -1;

	// Whether or not to show the Quick Chat bubble
	private boolean showQuickChatIcon = false;

	@Override
	protected void startUp()
	{
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				showQuickChatIcon = true;
				loadQuickChatIcon();
				setChatboxName(getFullyQualifiedName());
			}
		});
	}

	@Override
	protected void shutDown()
	{
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				showQuickChatIcon = false;
				setChatboxName(getFullyQualifiedName());
			}
		});
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			loadQuickChatIcon();
		}
	}

	/**
	 * Update the player's name in the chatbox input to the specified one.
	 *
	 * @param name the name that should be displayed in the chatbox instead
	 */
	private void setChatboxName(String name)
	{
		Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
		if (chatboxInput != null)
		{
			String text = chatboxInput.getText();
			int idx = text.indexOf(':');
			if (idx != -1)
			{
				String newText = name + text.substring(idx);
				chatboxInput.setText(newText);
			}
		}
	}

	/**
	 * Gets the full name, including account-type icon, of the local player.
	 *
	 * @return String of account-type icon + name [+ Quick Chat icon]
	 */
	private String getFullyQualifiedName()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		int iconIndex = -1;
		String name;
		switch (client.getAccountType())
		{
			case IRONMAN:
				iconIndex = IconID.IRONMAN.getIndex();
				break;
			case HARDCORE_IRONMAN:
				iconIndex = IconID.HARDCORE_IRONMAN.getIndex();
				break;
			case ULTIMATE_IRONMAN:
				iconIndex = IconID.ULTIMATE_IRONMAN.getIndex();
				break;
			default:
				break;
		}

		name = getNameWithAccountType(iconIndex, player.getName());

		String quickChatIcon = "";
		if (quickChatIconOffset != -1 && showQuickChatIcon)
		{
			quickChatIcon = "<img=" + quickChatIconOffset + ">";
		}

		return name + quickChatIcon;
	}

	/**
	 * Get a player's name prepended with an account-type icon.
	 *
	 * @param accountTypeIndex index of the account-type icon
	 * @param name name of the player
	 * @return String of account-type icon + name
	 */
	private String getNameWithAccountType(int accountTypeIndex, String name)
	{
		String icon = "";
		if (accountTypeIndex != -1)
		{
			icon = "<img=" + accountTypeIndex + ">";
		}

		return icon + name;
	}

	/**
	 * Loads the Quick Chat icon into the client and caches it for future use.
	 */
	private void loadQuickChatIcon()
	{
		final IndexedSprite[] modIcons = client.getModIcons();

		if (quickChatIconOffset != -1 || modIcons == null)
		{
			return;
		}

		BufferedImage image = ImageUtil.getResourceStreamFromClass(getClass(), "/bubble.png");
		IndexedSprite indexedSprite = ImageUtil.getImageIndexedSprite(image, client);

		quickChatIconOffset = modIcons.length;

		final IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, modIcons.length + 1);
		newModIcons[newModIcons.length - 1] = indexedSprite;

		client.setModIcons(newModIcons);
	}
}
