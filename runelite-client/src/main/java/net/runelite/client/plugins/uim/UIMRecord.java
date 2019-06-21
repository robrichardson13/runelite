package net.runelite.client.plugins.uim;

import lombok.Value;

@Value
public class UIMRecord {
    private final String title;
    private final UIMItem[] items;
}
