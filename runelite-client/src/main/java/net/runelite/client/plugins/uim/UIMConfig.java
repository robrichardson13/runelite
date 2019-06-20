package net.runelite.client.plugins.uim;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("uim")
public interface UIMConfig extends Config
{
    @ConfigItem(
            keyName = "trackLookingBag",
            name = "Looting Bag",
            description = "Tracks what's currently inside your looting bag"
    )
    default boolean trackLookingBag()
    {
        return true;
    }

    @ConfigItem(
            keyName = "trackZulrahItems",
            name = "Zulrah Items",
            description = "Keep track of the items when you die at Zulrah"
    )
    default boolean trackZulrahItems()
    {
        return true;
    }
}
