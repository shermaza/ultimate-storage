package com.ultimatestorage;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class UltimateStoragePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(UltimateStoragePlugin.class);
		RuneLite.main(args);
	}
}