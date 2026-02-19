package com.camjewell;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import java.awt.*;

@Slf4j
@PluginDescriptor(name = "Potion Storage Deposit Toggle", description = "Adds shortcuts to open bank settings: right-click option and shift-click swap")
public class PotionStorageDeposit extends Plugin {
    private static final String TOGGLE_OPTION = "Open bank settings";

    // Widget IDs for the Potion Storage button
    private static final int POTION_STORAGE_WIDGET_ID = 786551; // Parent widget ID for potion storage button
    private static final int POTION_STORAGE_CHILD_ID = -1; // Child ID used in MenuEntryAdded event
    private static final int POTION_STORAGE_OVERLAY_CHILD_ID = 0; // Actual child widget ID for rendering overlay

    // Settings menu widget IDs
    private static final int BANK_MENU_BUTTON_WIDGET_ID = 786531; // "Show menu" button widget
    private static final int AUTO_DEPOSIT_VARBIT = 6608; // Varbit storing auto-deposit state

    @Inject
    private Client client;

    @Inject
    private ExampleConfig config;

    @Inject
    private OverlayManager overlayManager;

    private PotionStorageOverlay overlay;

    @Override
    protected void startUp() throws Exception {
        if (config.showOverlay()) {
            overlay = new PotionStorageOverlay(client);
            overlayManager.add(overlay);
        }
        log.info("Potion Storage Deposit Toggle started!");
    }

    @Override
    protected void shutDown() throws Exception {
        if (overlay != null) {
            overlayManager.remove(overlay);
        }
        log.info("Potion Storage Deposit Toggle stopped!");
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        // Check if this is the potion storage button
        int widgetId = event.getActionParam1();
        int childId = event.getActionParam0();
        String option = event.getOption();

        if (widgetId != POTION_STORAGE_WIDGET_ID || childId != POTION_STORAGE_CHILD_ID) {
            return;
        }

        // Check if we're in the bank interface
        if (!isBankOpen()) {
            return;
        }

        // Only proceed when we see the main "Potion Storage" option
        if (!option.equals("Potion Storage")) {
            return;
        }

        // Add "Open bank settings" as second menu option
        MenuEntry newEntry = client.createMenuEntry(1);
        newEntry.setOption(TOGGLE_OPTION);
        newEntry.setTarget("");
        newEntry.setType(MenuAction.CC_OP);
        newEntry.setParam0(-1);
        newEntry.setParam1(BANK_MENU_BUTTON_WIDGET_ID);
        newEntry.setIdentifier(1);
    }

    /**
     * Inner overlay class that renders the auto-deposit status on the potion
     * storage button
     */
    private class PotionStorageOverlay extends Overlay {
        private static final int BORDER_SIZE = 2;
        private static final int ENABLED_COLOR = 0x00FF00; // Green for enabled
        private static final int DISABLED_COLOR = 0xFF0000; // Red for disabled
        private final Client client;

        public PotionStorageOverlay(Client client) {
            this.client = client;
            setLayer(net.runelite.client.ui.overlay.OverlayLayer.ABOVE_WIDGETS);
        }

        @Override
        public Dimension render(Graphics2D graphics) {
            if (!isBankOpen() || !config.showOverlay()) {
                return null;
            }

            Widget potionStorageWidget = client.getWidget(POTION_STORAGE_WIDGET_ID, POTION_STORAGE_OVERLAY_CHILD_ID);
            if (potionStorageWidget == null || potionStorageWidget.isHidden()) {
                return null;
            }

            // Get the widget bounds
            Rectangle bounds = potionStorageWidget.getBounds();
            if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
                return null;
            }

            // Check if auto-deposit is enabled
            int autoDepositState = client.getVarbitValue(AUTO_DEPOSIT_VARBIT);
            boolean isEnabled = autoDepositState == 1;
            int color = isEnabled ? ENABLED_COLOR : DISABLED_COLOR;

            // Draw a border around the widget
            graphics.setColor(new Color(color, true));
            graphics.setStroke(new BasicStroke(BORDER_SIZE));
            graphics.drawRect(bounds.x, bounds.y, bounds.width - BORDER_SIZE, bounds.height - BORDER_SIZE);

            // Draw status text
            String status = isEnabled ? "ON" : "OFF";
            graphics.setColor(new Color(color));
            graphics.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics metrics = graphics.getFontMetrics();
            int x = bounds.x + (bounds.width - metrics.stringWidth(status)) / 2;
            int y = bounds.y + ((bounds.height - metrics.getHeight()) / 2) + metrics.getAscent();
            graphics.drawString(status, x, y);

            return null;
        }
    }

    private boolean isBankOpen() {
        return client.getWidget(ComponentID.BANK_CONTAINER) != null;
    }

    @Provides
    ExampleConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ExampleConfig.class);
    }
}
