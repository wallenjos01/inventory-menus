package org.wallentines.invmenu.api;


import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.wallentines.invmenu.impl.InventoryMenuImpl;
import org.wallentines.pseudonym.PipelineContext;
import org.wallentines.pseudonym.UnresolvedMessage;
import org.wallentines.pseudonym.mc.api.ServerPlaceholders;

public interface InventoryMenu {

    /**
     * Sets the item and click event at the given index of the GUI.
     * @param index The slot index.
     * @param itemStack The item to put at that index.
     * @param event The click event to invoke when a player clicks at the index.
     */
    void setItem(int index, ItemStack itemStack, ClickEvent event);

    /**
     * Sets the item and click event at the given index of the GUI. The item will be resolved with respect to each player.
     * @param index The slot index.
     * @param itemStack The item to put at that index.
     * @param event The click event to invoke when a player clicks at the index.
     */
    void setItem(int index, ItemSupplier itemStack, ClickEvent event);

    /**
     * Removes the item and click event at the given index of the GUI.
     * @param index The index to clear.
     */
    void clearItem(int index);

    /**
     * Gets the number of rows in the GUI.
     * @return The number of rows.
     */
    int rows();

    /**
     * Gets the number of slots in the GUI.
     * @return The number of slots.
     */
    int size();

    /**
     * Gets the index of the first empty slot in the GUI.
     * @return The index of the first empty slot.
     */
    int firstEmpty();

    /**
     * Gets the index of the last filled slot in the GUI.
     * @return The index of the last filled slot.
     */
    int lastItem();

    /**
     * Removes all items and actions from the GUI
     */
    void clear();

    /**
     * Updates the menu
     */
    void update();

    /**
     * Opens the GUI for the given player
     */
    void open(ServerPlayer player);

    /**
     * Closes the GUI for the given player
     */
    void close(ServerPlayer player);

    /**
     * Closes the GUI for all players currently viewing it.
     */
    void closeAll();

    /**
     * Moves the viewers of this inventory GUI to another GUI
     * @param other The other GUI
     */
    void moveViewers(InventoryMenu other);


    /**
     * Creates a new Inventory GUI with a single page which can hold the given number of items.
     * @param title Some logic to get the menu's title, per player.
     * @param size The minimum number of items the menu can hold. Cannot be negative or greater than 54
     * @return A new InventoryGui
     */
    static InventoryMenu create(ComponentSupplier title, int size) {
        return create(title, size, PipelineContext.EMPTY);
    }

    /**
     * Creates a new Inventory GUI with a single page which can hold the given number of items, with the given inherent context.
     * @param title Some logic to get the menu's title, per player.
     * @param size The minimum number of items the menu can hold. Cannot be negative or greater than 54
     * @param context Context to be applied each time an item or the title is resolved.
     * @return A new InventoryGui
     */
    static InventoryMenu create(ComponentSupplier title, int size, PipelineContext context) {
        return InventoryMenuImpl.create(title, size, context);
    }

    /**
     * Vanilla actions a player can take in an inventory
     */
    enum ClickType {
        LEFT,
        RIGHT,
        SHIFT_LEFT,
        SHIFT_RIGHT,
        MIDDLE,
        DOUBLE,
        THROW,
        THROW_ALL,
        NUMBER_KEY
    }

    /**
     * Called whenever a player clicks on an item in an Inventory menu
     */
    interface ClickEvent {
        void execute(ServerPlayer player, ClickType type);
    }

    /**
     * Creates or retrieves an item for an inventory menu based on the given context
     */
    interface ItemSupplier {
        ItemStack get(PipelineContext player);
    }

    /**
     * Creates or retrieves a text component based on the given context
     */
    interface ComponentSupplier {
        Component get(PipelineContext ctx);

        static ComponentSupplier completed(Component text) {
            return (ctx) -> text;
        }

        static ComponentSupplier unresolved(UnresolvedMessage<String> text) {
            return (ctx) -> ServerPlaceholders.COMPONENT_RESOLVER.accept(text, ctx);
        }
    }

}
