package com.ultimatestorage;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@PluginDescriptor(
	name = "Ultimate Storage"
)
public class UltimateStoragePlugin extends Plugin
{
	private static final BufferedImage ICON = ImageUtil.loadImageResource(UltimateStoragePlugin.class, "icon.png");
	private final Map<Integer, Integer> inventoryHashMap = new HashMap<>();
	private static final int LOOTING_BAG_ID = 516;

	private NavigationButton navButton;
	private UltimateStoragePanel panel;
	private boolean prepared = false;

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ClientThread clientThread;

	@Inject
	private UltimateStorageConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Override
	protected void startUp() throws Exception
	{
		panel = new UltimateStoragePanel(client, config, itemManager, configManager);
		navButton = NavigationButton.builder()
				.tooltip("Ultimate Storage")
				.icon(ICON)
				.priority(6)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);

		if (!prepared)
		{
			clientThread.invoke(() ->
			{
				switch (client.getGameState())
				{
					case LOGIN_SCREEN:
					case LOGIN_SCREEN_AUTHENTICATOR:
					case LOGGING_IN:
					case LOADING:
					case LOGGED_IN:
					case CONNECTION_LOST:
					case HOPPING:
						prepared = true;
						return true;
					default:
						return false;
				}
			});
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
		inventoryHashMap.clear();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged ev)
	{
		if (ev.getContainerId() == LOOTING_BAG_ID)
		{
			updateItemsFromItemContainer(ev.getContainerId(), ev.getItemContainer());
		}
	}

	private void updateItemsFromItemContainer(final int inventoryId, final ItemContainer c)
	{
		// Check if the contents have changed.
		if (c == null)
		{
			return;
		}

		final Map<Integer, Integer> m = new HashMap<>();
		for (Item item : c.getItems())
		{
			if (item.getId() == -1)
			{
				continue;
			}

			// Account for noted items, ignore placeholders.
			int itemID = item.getId();
			final ItemComposition itemComposition = itemManager.getItemComposition(itemID);
			if (itemComposition.getPlaceholderTemplateId() != -1)
			{
				continue;
			}

			if (itemComposition.getNote() != -1)
			{
				itemID = itemComposition.getLinkedNoteId();
			}

			final int qty = m.getOrDefault(itemID, 0) + item.getQuantity();
			m.put(itemID, qty);
		}

		updateInventoryMap(inventoryId, m);
	}

	private void updateInventoryMap(final int inventoryId, final Map<Integer, Integer> m)
	{
		final int curHash = m.hashCode();
		if (curHash != inventoryHashMap.getOrDefault(inventoryId, -1))
		{
			inventoryHashMap.put(inventoryId, curHash);
//			SwingUtilities.invokeLater(() -> panel.setInventoryMap(inventoryId, m));
		}
	}

	@Provides
	UltimateStorageConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(UltimateStorageConfig.class);
	}
}
