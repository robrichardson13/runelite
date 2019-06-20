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
     * Checks if this box matches specified id
     *
     * @param id other record id
     * @return true if match is made
     */
    boolean matches(final String id) {
        if (id == null) {
            return true;
        }

        return this.id.equals(id);
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
    }

    void rebuild()
    {
        buildItems();
        validate();
        repaint();
    }

    /**
     * This method creates stacked items from the item list, calculates total price and then
     * displays all the items in the UI.
     */
    private void buildItems()
    {
        final List<UIMItem> allItems = new ArrayList<>();
        final List<UIMItem> items = new ArrayList<>();

        for (UIMRecord record : records)
        {
            allItems.addAll(Arrays.asList(record.getItems()));
        }

        for (final UIMItem entry : allItems)
        {
            int quantity = 0;
            for (final UIMItem i : items)
            {
                if (i.getId() == entry.getId())
                {
                    quantity = i.getQuantity();
                    items.remove(i);
                    break;
                }
            }

            if (quantity > 0)
            {
                int newQuantity = entry.getQuantity() + quantity;
                items.add(new UIMItem(entry.getId(), entry.getName(), newQuantity));
            }
            else
            {
                items.add(entry);
            }
        }

        // Calculates how many rows need to be display to fit all items
        final int rowSize = ((items.size() % ITEMS_PER_ROW == 0) ? 0 : 1) + items.size() / ITEMS_PER_ROW;

        itemContainer.removeAll();
        itemContainer.setLayout(new GridLayout(rowSize, ITEMS_PER_ROW, 1, 1));

        for (int i = 0; i < rowSize * ITEMS_PER_ROW; i++)
        {
            final JPanel slotContainer = new JPanel();
            slotContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

            if (i < items.size())
            {
                final UIMItem item = items.get(i);
                final JLabel imageLabel = new JLabel();
                imageLabel.setToolTipText(buildToolTip(item));
                imageLabel.setVerticalAlignment(SwingConstants.CENTER);
                imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

                AsyncBufferedImage itemImage = itemManager.getImage(item.getId(), item.getQuantity(), item.getQuantity() > 1);
                itemImage.addTo(imageLabel);

                slotContainer.add(imageLabel);
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
