package net.runelite.client.plugins.uim;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

class UIMPanel extends PluginPanel
{
    // When there is no loot, display this
    private final PluginErrorPanel errorPanel = new PluginErrorPanel();

    private final JPanel overallPanel = new JPanel();
    private final JLabel overallIcon = new JLabel();

    private final UIMPlugin plugin;
    private final UIMConfig config;

    UIMPanel(final UIMPlugin plugin, final UIMConfig config)
    {
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
}
