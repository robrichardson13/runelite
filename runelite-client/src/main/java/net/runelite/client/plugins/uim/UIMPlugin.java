package net.runelite.client.plugins.uim;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.SpriteID;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemStack;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@PluginDescriptor(
        name = "UIM",
        description = "Item tracking in Looting bag and Zulrah for UIM",
        tags = {"uim"}
)
@Slf4j
public class UIMPlugin extends Plugin
{
    @javax.inject.Inject
    private ClientToolbar clientToolbar;

    @Inject
    private SpriteManager spriteManager;

    @Inject
    private UIMConfig config;

    @Inject
    private Client client;

    private UIMPanel panel;
    private NavigationButton navButton;

    @Provides
    UIMConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(UIMConfig.class);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {

    }

    @Override
    protected void startUp() throws Exception
    {
        panel = new UIMPanel(this, config);
        spriteManager.getSpriteAsync(SpriteID.MAP_ICON_IRON_MAN_TUTORS, 0, panel::loadHeaderIcon);

        final BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), "panel_icon.png");

        navButton = NavigationButton.builder()
                .tooltip("UIM")
                .icon(icon)
                .priority(99)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown()
    {
        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        final ItemContainer container;
        switch (event.getGroupId())
        {
            case (WidgetID.LOOTING_BAG_GROUP_ID):
                container = client.getItemContainer(InventoryID.LOOTING_BAG);
                break;
            default:
                return;
        }

        if (container == null)
        {
            return;
        }

        // Convert container items to array of ItemStack
        final Collection<ItemStack> items = Arrays.stream(container.getItems())
                .filter(item -> item.getId() > 0)
                .map(item -> new ItemStack(item.getId(), item.getQuantity(), client.getLocalPlayer().getLocalLocation()))
                .collect(Collectors.toList());

        if (items.isEmpty())
        {
            log.debug("No items to find for Event: {} | Container: {}", "Looting bag", container);
            return;
        }
        //TODO
    }
}
