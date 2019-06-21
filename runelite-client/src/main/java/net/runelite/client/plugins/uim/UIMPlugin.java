package net.runelite.client.plugins.uim;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.cluescrolls.clues.MapClue;
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
    private static final WorldArea WILDERNESS_ABOVE_GROUND = new WorldArea(2944, 3525, 448, 448, 0);
    private static final WorldArea WILDERNESS_UNDERGROUND = new WorldArea(2944, 9920, 320, 442, 0);

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
    private ItemComposition usingItem;

    @Provides
    UIMConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(UIMConfig.class);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        log.debug("config changed");
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
    public void onMenuOptionClicked(final MenuOptionClicked event)
    {
        if(!isInWilderness(client.getLocalPlayer().getWorldLocation())) {
            return;
        }
        if (event.getMenuOption() != null && event.getMenuOption().equals("Use")) {
            final ItemComposition itemComposition = itemManager.getItemComposition(event.getId());
            if (itemComposition != null && itemComposition.getName().equals("Looting bag")) {
                //Used an item on the Looting Bag
                final ItemComposition usedItem = usingItem;
                if(usedItem.getNote() == 799) { //Noted
                    if(itemManager.getItemComposition(usedItem.getLinkedNoteId()).isTradeable()) {
                        insertItemIntoBag(usedItem.getId());
                    }
                } else {
                    if(usedItem.isTradeable()) {
                        insertItemIntoBag(usedItem.getId());
                    }
                }
                usingItem = null;
            } else {
                usingItem = itemComposition;
            }
        }
    }

    private static boolean isInWilderness(WorldPoint location) {
        return WILDERNESS_ABOVE_GROUND.distanceTo2D(location) == 0 || WILDERNESS_UNDERGROUND.distanceTo2D(location) == 0;
    }

    public void insertItemIntoBag(int id) {
        final ItemContainer container = client.getItemContainer(InventoryID.INVENTORY);
        if(container == null) {
            return;
        }
        final Item[] inventoryItems = container.getItems();
        for(Item item : inventoryItems) {
            if(item.getId() == id) {
                final UIMItem newItem = buildLootTrackerItem(item.getId(), item.getQuantity());
                log.debug("Insert item into the bag: ({}){}", item.getQuantity(), newItem.getName());
                final UIMItem[] items = {newItem};
                SwingUtilities.invokeLater(() -> panel.add("Looting Bag", items, false));
            }
        }
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

        final UIMItem[] entries = buildEntries(items);
        SwingUtilities.invokeLater(() -> panel.add("Looting Bag", entries, true));
    }

    private UIMItem buildLootTrackerItem(int itemId, int quantity)
    {
        final ItemComposition itemComposition = itemManager.getItemComposition(itemId);

        return new UIMItem(itemId, itemComposition.getName(), quantity, itemComposition.isStackable());
    }

    private UIMItem[] buildEntries(final Collection<ItemStack> itemStacks)
    {
        return itemStacks.stream()
                .map(itemStack -> buildLootTrackerItem(itemStack.getId(), itemStack.getQuantity()))
                .toArray(UIMItem[]::new);
    }
}
