package net.runelite.client.plugins.uim;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

@PluginDescriptor(
        name = "UIM",
        description = "Item tracking in Looting bag and Zulrah for UIM",
        tags = {"uim"}
)
@Slf4j
public class UIMPlugin extends Plugin
{
    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ItemManager itemManager;

    @Inject
    private SpriteManager spriteManager;

    @Inject
    private UIMConfig config;

    @Inject
    private Client client;

    private UIMPanel panel;
    private NavigationButton navButton;

    private static Collection<ItemStack> stack(Collection<ItemStack> items)
    {
        final List<ItemStack> list = new ArrayList<>();

        for (final ItemStack item : items)
        {
            int quantity = 0;
            for (final ItemStack i : list)
            {
                if (i.getId() == item.getId())
                {
                    quantity = i.getQuantity();
                    list.remove(i);
                    break;
                }
            }
            if (quantity > 0)
            {
                list.add(new ItemStack(item.getId(), item.getQuantity() + quantity, item.getLocation()));
            }
            else
            {
                list.add(item);
            }
        }

        return list;
    }

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
        panel = new UIMPanel(this, itemManager, config);
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

        final UIMItem[] entries = buildEntries(stack(items));
        SwingUtilities.invokeLater(() -> panel.add("Looting Bag", -1, entries));
    }

    private UIMItem buildLootTrackerItem(int itemId, int quantity)
    {
        final ItemComposition itemComposition = itemManager.getItemComposition(itemId);

        return new UIMItem(itemId, itemComposition.getName(), quantity);
    }

    private UIMItem[] buildEntries(final Collection<ItemStack> itemStacks)
    {
        return itemStacks.stream()
                .map(itemStack -> buildLootTrackerItem(itemStack.getId(), itemStack.getQuantity()))
                .toArray(UIMItem[]::new);
    }
}
