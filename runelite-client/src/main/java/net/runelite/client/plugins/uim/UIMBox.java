package net.runelite.client.plugins.uim;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.client.game.AsyncBufferedImage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.Text;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class UIMBox extends JPanel {
    private static final int ITEMS_PER_ROW = 4;

    private final JPanel itemContainer = new JPanel();
    private final ItemManager itemManager;

    @Getter(AccessLevel.PACKAGE)
    private final String id;

    @Getter
    private final List<UIMRecord> records = new ArrayList<>();

    @Getter
    private int numberOfItems = 0;

    UIMBox(
            final ItemManager itemManager,
            final String id) {
        this.id = id;
        this.itemManager = itemManager;

        setLayout(new BorderLayout(0, 1));
        setBorder(new EmptyBorder(5, 0, 0, 0));

        final JPanel logTitle = new JPanel(new BorderLayout(5, 0));
        logTitle.setBorder(new EmptyBorder(7, 7, 7, 7));
        logTitle.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());

        final JLabel titleLabel = new JLabel(Text.removeTags(id));
        titleLabel.setFont(FontManager.getRunescapeSmallFont());
        titleLabel.setForeground(Color.WHITE);

        logTitle.add(titleLabel, BorderLayout.WEST);

        add(logTitle, BorderLayout.NORTH);
        add(itemContainer, BorderLayout.CENTER);
    }

    /**
     * Checks if this box matches specified record
     *
     * @param record loot record
     * @return true if match is made
     */
    boolean matches(final UIMRecord record) {
        return record.getTitle().equals(id);
    }

    /**
     * Adds an record's data into a loot box.
     * This will add new items to the list, re-calculating price and kill count.
     */
    void combine(final UIMRecord record) {
        if (!matches(record)) {
            throw new IllegalArgumentException(record.toString());
        }

        records.add(record);
    }

    void clearRecords()
    {
        records.clear();
        numberOfItems = 0;
    }

    void rebuild() {
        buildItems();
        validate();
        repaint();
    }

    /**
     * This method creates stacked items from the item list, calculates total price and then
     * displays all the items in the UI.
     */
    private void buildItems() {
        final List<UIMItem> allItems = new ArrayList<>();
        final List<UIMItem> items = new ArrayList<>();

        for (UIMRecord record : records) {
            allItems.addAll(Arrays.asList(record.getItems()));
        }

        for (final UIMItem entry : allItems) { //Loop through all items
            boolean updated = false;
            if(entry.isStackable()) { //This item is stackable, search all prior items to see if I should combine when them
                int index = 0;
                for (final UIMItem i : items) { //Loop through all inserted items
                    if (i.getId() == entry.getId()) {
                        final UIMItem updatedItem = new UIMItem(entry.getId(), entry.getName(), entry.getQuantity() + i.getQuantity(), entry.isStackable());
                        items.set(index, updatedItem);
                        updated = true;
                        break;
                    }
                    index++;
                }
            }

            if(!updated) {
                items.add(entry);
            }
        }

        numberOfItems = items.size();

        // Calculates how many rows need to be display to fit all items
        final int rowSize = ((items.size() % ITEMS_PER_ROW == 0) ? 0 : 1) + items.size() / ITEMS_PER_ROW;

        itemContainer.removeAll();
        itemContainer.setLayout(new GridLayout(rowSize, ITEMS_PER_ROW, 1, 1));

        for (int i = 0; i < rowSize * ITEMS_PER_ROW; i++) {
            final JPanel slotContainer = new JPanel();
            slotContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

            if (i < items.size()) {
                final UIMItem item = items.get(i);
                final JLabel imageLabel = new JLabel();
                imageLabel.setToolTipText(buildToolTip(item));
                imageLabel.setVerticalAlignment(SwingConstants.CENTER);
                imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

                AsyncBufferedImage itemImage = itemManager.getImage(item.getId(), item.getQuantity(), item.getQuantity() > 1);
                itemImage.addTo(imageLabel);

                slotContainer.add(imageLabel);

                // Create popup menu
                final JPopupMenu popupMenu = new JPopupMenu();
                popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));
                slotContainer.setComponentPopupMenu(popupMenu);
            }

            itemContainer.add(slotContainer);
        }

        itemContainer.repaint();
    }

    private static String buildToolTip(UIMItem item)
    {
        final String name = item.getName();
        final int quantity = item.getQuantity();
        return name + " x " + quantity;
    }
}
