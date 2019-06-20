package net.runelite.client.plugins.uim;

import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

class UIMPanel extends PluginPanel
{
    // When there is no loot, display this
    private final PluginErrorPanel errorPanel = new PluginErrorPanel();

    // Handle loot boxes
    private final JPanel logsContainer = new JPanel();

//    private final JPanel overallPanel = new JPanel();
    private final JLabel overallIcon = new JLabel();

    // Log collection
//    private final List<UIMRecord> records = new ArrayList<>();
    private UIMBox lootingBagBox;
//    private final List<UIMBox> boxes = new ArrayList<>();

    private final ItemManager itemManager;

    private final UIMPlugin plugin;
    private final UIMConfig config;

    UIMPanel(final UIMPlugin plugin, final ItemManager itemManager, final UIMConfig config)
    {
        this.itemManager = itemManager;
        this.plugin = plugin;
        this.config = config;

        setBorder(new EmptyBorder(6, 6, 6, 6));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        // Create layout panel for wrapping
        final JPanel layoutPanel = new JPanel();
        layoutPanel.setLayout(new BoxLayout(layoutPanel, BoxLayout.Y_AXIS));
        add(layoutPanel, BorderLayout.NORTH);

        // Create loot boxes wrapper
        logsContainer.setLayout(new BoxLayout(logsContainer, BoxLayout.Y_AXIS));
        layoutPanel.add(logsContainer);

        // Add error pane
        errorPanel.setContent("UIM", "You have no looting bag or items in Zulrah");
        add(errorPanel);
    }

    void loadHeaderIcon(BufferedImage img)
    {
        overallIcon.setIcon(new ImageIcon(img));
    }

    void add(final String eventName, UIMItem[] items) {
        if(eventName.equals("Looting Bag")) {
            final UIMRecord record = new UIMRecord(eventName, items);
            if(lootingBagBox != null) {
                lootingBagBox.clearRecords();
                lootingBagBox.combine(record);
            } else {
                lootingBagBox = buildBox(record);
            }
            lootingBagBox.rebuild();
        }
    }

    /**
     * Rebuilds all the boxes from scratch using existing listed records, depending on the grouping mode.
     */
    private void rebuild()
    {
//        logsContainer.removeAll();
//        boxes.clear();
//        int start = 0;
//        for (int i = start; i < records.size(); i++)
//        {
//            buildBox(records.get(i));
//        }
//        boxes.forEach(UIMBox::rebuild);
//        logsContainer.revalidate();
//        logsContainer.repaint();
    }

    /**
     * This method decides what to do with a new record, if a similar log exists, it will
     * add its items to it, updating the log's overall price and kills. If not, a new log will be created
     * to hold this entry's information.
     */
    private UIMBox buildBox(UIMRecord record)
    {
        // Show main view
        remove(errorPanel);

        // Create box
        final UIMBox box = new UIMBox(itemManager, record.getTitle());
        box.combine(record);

        // Add box to panel
        logsContainer.add(box, 0);

        return box;
    }
}
