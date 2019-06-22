package net.runelite.client.plugins.uim;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.screenmarkers.ScreenMarkerPlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Slf4j
class UIMPanel extends PluginPanel
{
    private static final ImageIcon ADD_ICON;
    private static final ImageIcon ADD_HOVER_ICON;

    private final JLabel addMarker = new JLabel(ADD_ICON);

    private static final Gson GSON = new Gson();

    private final PluginErrorPanel errorPanel = new PluginErrorPanel();
    private final JPanel logsContainer = new JPanel();
    private final JLabel overallIcon = new JLabel();

    // Log collection
    private UIMBox lootingBagBox;
    private UIMBox deathBox;

    private final ItemManager itemManager;

    private final UIMPlugin plugin;
    private final UIMConfig config;

    static
    {
        final BufferedImage addIcon = ImageUtil.getResourceStreamFromClass(ScreenMarkerPlugin.class, "add_icon.png");
        ADD_ICON = new ImageIcon(addIcon);
        ADD_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(addIcon, 0.53f));
    }

    UIMPanel(final UIMPlugin plugin, final ItemManager itemManager, final UIMConfig config)
    {
        this.itemManager = itemManager;
        this.plugin = plugin;
        this.config = config;

        setBorder(new EmptyBorder(6, 6, 6, 6));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        northPanel.setPreferredSize(new Dimension(0, 30));
        northPanel.setBorder(new EmptyBorder(5, 5, 5, 10));

        northPanel.add(addMarker, BorderLayout.EAST);
        add(northPanel, BorderLayout.NORTH);

        addMarker.setToolTipText("Create death items");
        addMarker.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                plugin.addDeath();
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent)
            {
                addMarker.setIcon(ADD_HOVER_ICON);
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent)
            {
                addMarker.setIcon(ADD_ICON);
            }
        });

        // Create layout panel for wrapping
        final JPanel layoutPanel = new JPanel();
        layoutPanel.setLayout(new BoxLayout(layoutPanel, BoxLayout.Y_AXIS));
        add(layoutPanel, BorderLayout.CENTER);

        // Create loot boxes wrapper
        logsContainer.setLayout(new BoxLayout(logsContainer, BoxLayout.Y_AXIS));
        layoutPanel.add(logsContainer);

        // Re-populate saved looting bag and death data
        boolean recordsExist = false;
        final String savedDeathJson = config.deathData();
        if(!savedDeathJson.equals("") && !savedDeathJson.equals("[]")) {
            recordsExist = true;
            Type listType = new TypeToken<ArrayList<UIMRecord>>(){}.getType();
            List<UIMRecord> records = GSON.fromJson(savedDeathJson, listType);
            for (UIMRecord record : records) {
                addDeathItems(record.getItems());
            }
        }

        final String savedLootingBagJson = config.lootingBagData();
        if(!savedLootingBagJson.equals("") && !savedDeathJson.equals("[]")) {
            recordsExist = true;
            Type listType = new TypeToken<ArrayList<UIMRecord>>(){}.getType();
            List<UIMRecord> records = GSON.fromJson(savedLootingBagJson, listType);
            for (UIMRecord record : records) {
                addLootingBagItems(record.getItems(), false);
            }
        }

        if(!recordsExist) {
            errorPanel.setContent("UIM", "You have no looting bag or death items");
            add(errorPanel);
        }
    }

    void loadHeaderIcon(BufferedImage img)
    {
        overallIcon.setIcon(new ImageIcon(img));
    }

    void addLootingBagItems(UIMItem[] items, boolean clear) {
        final UIMRecord newRecord = new UIMRecord("Looting Bag", items);
        if(lootingBagBox != null) {
            if(clear) {
                lootingBagBox.clearRecords();
            }

            if(lootingBagBox.getNumberOfItems() < 28) {
                lootingBagBox.combine(newRecord);
            }
        } else {
            lootingBagBox = buildBox(newRecord);
        }
        lootingBagBox.rebuild();

        logsContainer.revalidate();
        logsContainer.repaint();

        final String json = GSON.toJson(lootingBagBox.getRecords());
        config.setLootingBagData(json);
    }

    void addDeathItems(UIMItem[] items) {
        final UIMRecord newRecord = new UIMRecord("Death", items);
        if(deathBox != null) {
            deathBox.combine(newRecord);
        } else {
            deathBox = buildBox(newRecord);
        }
        deathBox.rebuild();

        deathBox.revalidate();
        deathBox.repaint();

        final String json = GSON.toJson(deathBox.getRecords());
        config.setDeathData(json);
    }

    void diedWithLootingBag() {
        //Take all the records from the looting bag and insert them into the death box
        if(lootingBagBox == null || lootingBagBox.getRecords().size() == 0) {
            return;
        }

        for(UIMRecord record: lootingBagBox.getRecords()) {
            addDeathItems(record.getItems());
        }

        lootingBagBox.clearRecords();
        config.setLootingBagData(GSON.toJson(lootingBagBox.getRecords()));
        logsContainer.remove(lootingBagBox);
        lootingBagBox = null;
        logsContainer.revalidate();
        logsContainer.repaint();
    }

    /**
     * This method decides what to do with a new record, if a similar log exists, it will
     * add its items to it, updating the log's overall price and kills. If not, a new log will be created
     * to hold this entry's information.
     */
    private UIMBox buildBox(UIMRecord record) {
        // Show main view
        remove(errorPanel);

        // Create box
        final UIMBox box = new UIMBox(itemManager, record.getTitle(), this::toggleItem, this::onClear);
        box.combine(record);

        // Add box to panel
        logsContainer.add(box, -1);

        return box;
    }

    private void onClear() {
        logsContainer.removeAll();

        List<UIMRecord> lootingBagRecords;
        if(lootingBagBox != null) {
            lootingBagRecords = new ArrayList<>(lootingBagBox.getRecords());
        } else {
            lootingBagRecords = new ArrayList<>();
        }

        deathBox = null;
        lootingBagBox = null;
        config.setDeathData("[]");

        for(UIMRecord lootingBagRecord : lootingBagRecords) {
            addLootingBagItems(lootingBagRecord.getItems(), false);
        }

        logsContainer.revalidate();
        logsContainer.repaint();

        if(deathBox == null && lootingBagBox == null) {
            errorPanel.setContent("Items", "You have no looting bag or death items");
            add(errorPanel);
        }
    }

    private void toggleItem(Integer id) {
        logsContainer.removeAll();

        final List<UIMRecord> deathRecords = new ArrayList<>(deathBox.getRecords());
        List<UIMRecord> lootingBagRecords;
        if(lootingBagBox != null) {
            lootingBagRecords = new ArrayList<>(lootingBagBox.getRecords());
        } else {
            lootingBagRecords = new ArrayList<>();
        }

        deathBox = null;
        lootingBagBox = null;

        int numberOfItems = 0;
        int numberOfItemsHidden = 0;
        for(UIMRecord deathRecord : deathRecords) {
            for(UIMItem item: deathRecord.getItems()) {
                numberOfItems++;
                if(item.getId() == id && item.isStackable()) {
                    item.setIgnored(true);
                }
                if(item.isIgnored()) {
                    numberOfItemsHidden++;
                }
            }
            addDeathItems(deathRecord.getItems());
        }

        if(numberOfItemsHidden == numberOfItems) {
            if(deathBox != null) {
                logsContainer.remove(deathBox);
                deathBox = null;
                config.setDeathData("[]");
            }
        }

        for(UIMRecord lootingBagRecord : lootingBagRecords) {
            addLootingBagItems(lootingBagRecord.getItems(), false);
        }

        logsContainer.revalidate();
        logsContainer.repaint();

        if(deathBox == null && lootingBagBox == null) {
            errorPanel.setContent("UIM", "You have no looting bag or death items");
            add(errorPanel);
        }
    }
}
