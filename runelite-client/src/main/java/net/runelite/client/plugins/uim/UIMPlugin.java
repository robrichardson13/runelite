package net.runelite.client.plugins.uim;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.LocalPlayerDeath;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.vars.AccountType;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.callback.ClientThread;
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

    @Inject
	private ClientThread clientThread;

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
    public void onMenuOptionClicked(final MenuOptionClicked event) {
//        if(client.getAccountType() != AccountType.ULTIMATE_IRONMAN) {
//            return;
//        }
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

    private void insertItemIntoBag(int id) {
        final ItemContainer container = client.getItemContainer(InventoryID.INVENTORY);
        if(container == null) {
            return;
        }
        final Item[] inventoryItems = container.getItems();
        for(Item item : inventoryItems) {
            if(item.getId() == id) {
                final UIMItem newItem = buildLootTrackerItem(item.getId(), item.getQuantity());
                final UIMItem[] items = {newItem};
                SwingUtilities.invokeLater(() -> panel.addLootingBagItems(items, false));
            }
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
//        if(client.getAccountType() != AccountType.ULTIMATE_IRONMAN) {
//            return;
//        }
        final ItemContainer container;
        if (event.getGroupId() == WidgetID.LOOTING_BAG_GROUP_ID) {
            container = client.getItemContainer(InventoryID.LOOTING_BAG);
        } else {
            return;
        }

        if (container == null) {
            return;
        }

        // Convert container items to array of ItemStack
        final Collection<ItemStack> items = Arrays.stream(container.getItems())
                .filter(item -> item.getId() > 0)
                .map(item -> new ItemStack(item.getId(), item.getQuantity(), client.getLocalPlayer().getLocalLocation()))
                .collect(Collectors.toList());

        if (items.isEmpty()) {
            return;
        }

        final UIMItem[] entries = buildEntries(items);
        SwingUtilities.invokeLater(() -> panel.addLootingBagItems(entries, true));
    }

//    @Subscribe
//    public void onLocalPlayerDeath(LocalPlayerDeath death) {
//        onDeath();
//    }

    public void addDeath() {
        clientThread.invoke(this::onDeath);
    }

    private void onDeath() {
        final boolean instanced = client.isInInstancedRegion();
//        if(client.getAccountType() != AccountType.ULTIMATE_IRONMAN) {
//            return;
//        }
        final ItemContainer inventoryContainer = client.getItemContainer(InventoryID.INVENTORY);
        final ItemContainer equipmentContainer = client.getItemContainer(InventoryID.EQUIPMENT);

        if(inventoryContainer != null) {
            final Collection<ItemStack> inventoryItems = createList(inventoryContainer, instanced);
            if (!inventoryItems.isEmpty()) {
                final UIMItem[] entries = buildEntries(inventoryItems);
                SwingUtilities.invokeLater(() -> panel.addDeathItems(entries));
            }
        }

        if(equipmentContainer != null) {
            final Collection<ItemStack> equiptmentItems = createList(equipmentContainer, instanced);
            if (!equiptmentItems.isEmpty()) {
                final UIMItem[] entries = buildEntries(equiptmentItems);
                SwingUtilities.invokeLater(() -> panel.addDeathItems(entries));
            }
        }

        SwingUtilities.invokeLater(() -> panel.diedWithLootingBag());
    }

    private Collection<ItemStack> createList(ItemContainer container, boolean instanced) {
        if(!instanced) {
            return Arrays.stream(container.getItems())
                    .filter(item -> {
                        if(item.getId() <= 0) { return false; }
                        final ItemComposition composition = itemManager.getItemComposition(item.getId());
                        if(composition.getName().equals("Looting bag")) { return false; } //Don't keep looting bag item
                        if(composition.getNote() == 799) { //Noted
                            return itemManager.getItemComposition(composition.getLinkedNoteId()).isTradeable();
                        } else {
                            return composition.isTradeable();
                        }
                    })
                    .map(item -> new ItemStack(item.getId(), item.getQuantity(), client.getLocalPlayer().getLocalLocation()))
                    .collect(Collectors.toList());
        }
        return Arrays.stream(container.getItems())
                .filter(item -> {
                    if(item.getId() <= 0) { return false; }
                    final ItemComposition composition = itemManager.getItemComposition(item.getId());
                    return !composition.getName().equals("Looting bag"); //Don't keep looting bag item
                })
                .map(item -> new ItemStack(item.getId(), item.getQuantity(), client.getLocalPlayer().getLocalLocation()))
                .collect(Collectors.toList());
    }

    private UIMItem buildLootTrackerItem(int itemId, int quantity)
    {
        final ItemComposition itemComposition = itemManager.getItemComposition(itemId);

        return new UIMItem(itemId, itemComposition.getName(), quantity, itemComposition.isStackable(), false);
    }

    private UIMItem[] buildEntries(final Collection<ItemStack> itemStacks)
    {
        return itemStacks.stream()
                .map(itemStack -> buildLootTrackerItem(itemStack.getId(), itemStack.getQuantity()))
                .toArray(UIMItem[]::new);
    }
}
