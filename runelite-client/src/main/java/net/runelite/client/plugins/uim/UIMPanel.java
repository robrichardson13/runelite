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

    private final JPanel actionsContainer = new JPanel();

    private final JPanel overallPanel = new JPanel();
    private final JLabel overallIcon = new JLabel();

    // Log collection
    private final List<UIMRecord> records = new ArrayList<>();
    private final List<UIMBox> boxes = new ArrayList<>();

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


        // Create panel that will contain overall data
        overallPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(5, 0, 0, 0, ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        overallPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        overallPanel.setLayout(new BorderLayout());
        overallPanel.setVisible(false);

        overallPanel.add(overallIcon, BorderLayout.WEST);

        // Create popup menu
//        final JPopupMenu popupMenu = new JPopupMenu();
//        popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));
//        popupMenu.add(reset);
//        overallPanel.setComponentPopupMenu(popupMenu);

        // Create loot boxes wrapper
//        logsContainer.setLayout(new BoxLayout(logsContainer, BoxLayout.Y_AXIS));
//        layoutPanel.add(actionsContainer);
        layoutPanel.add(overallPanel);
//        layoutPanel.add(logsContainer);

        // Add error pane
        errorPanel.setContent("UIM", "You have no looting bag or items in Zulrah");
        add(errorPanel);
    }

    void loadHeaderIcon(BufferedImage img)
    {
        overallIcon.setIcon(new ImageIcon(img));
    }

    void add(final String eventName, final int actorLevel, UIMItem[] items)
    {
        final String subTitle = actorLevel > -1 ? "(lvl-" + actorLevel + ")" : "";
        final UIMRecord record = new UIMRecord(eventName, subTitle, items, System.currentTimeMillis());
        records.add(record);
        UIMBox box = buildBox(record);
        if (box != null)
        {
            box.rebuild();
//            updateOverall();
        }
    }

    /**
     * Rebuilds all the boxes from scratch using existing listed records, depending on the grouping mode.
     */
    private void rebuild()
    {
        logsContainer.removeAll();
        boxes.clear();
        int start = 0;
//        if (!true && records.size() > 1000)
//        {
//            start = records.size() - 1000;
//        }
        for (int i = start; i < records.size(); i++)
        {
            buildBox(records.get(i));
        }
        boxes.forEach(UIMBox::rebuild);
//        updateOverall();
        logsContainer.revalidate();
        logsContainer.repaint();
    }

    /**
     * This method decides what to do with a new record, if a similar log exists, it will
     * add its items to it, updating the log's overall price and kills. If not, a new log will be created
     * to hold this entry's information.
     */
    private UIMBox buildBox(UIMRecord record)
    {
//        // If this record is not part of current view, return
//        if (!record.matches(currentView))
//        {
//            return null;
//        }

        // Group all similar loot together
//        if (groupLoot)
//        {
            for (UIMBox box : boxes)
            {
                if (box.matches(record))
                {
                    box.combine(record);
                    return box;
                }
            }
//        }

        // Show main view
        remove(errorPanel);
        actionsContainer.setVisible(true);
        overallPanel.setVisible(true);

        // Create box
        final UIMBox box = new UIMBox(itemManager, record.getTitle(), record.getSubTitle(), false);
        box.combine(record);

        // Create popup menu
        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));
        box.setComponentPopupMenu(popupMenu);

        // Create reset menu
        final JMenuItem reset = new JMenuItem("Reset");
        reset.addActionListener(e ->
        {
            records.removeAll(box.getRecords());
            boxes.remove(box);
//            updateOverall();
            logsContainer.remove(box);
            logsContainer.repaint();

//            LootTrackerClient client = plugin.getLootTrackerClient();
//            // Without loot being grouped we have no way to identify single kills to be deleted
//            if (client != null && groupLoot && config.syncPanel())
//            {
//                client.delete(box.getId());
//            }
        });

        popupMenu.add(reset);

        // Create details menu
        final JMenuItem details = new JMenuItem("View details");
        details.addActionListener(e ->
        {
//            currentView = record.getTitle();
//            detailsTitle.setText(currentView);
//            backBtn.setVisible(true);
            rebuild();
        });

        popupMenu.add(details);

        // Add box to panel
        boxes.add(box);
        logsContainer.add(box, 0);

//        if (!true && boxes.size() > 10000)
//        {
//            logsContainer.remove(boxes.remove(0));
//        }

        return box;
    }
}
