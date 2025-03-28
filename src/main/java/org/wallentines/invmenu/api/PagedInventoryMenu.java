package org.wallentines.invmenu.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.wallentines.invmenu.impl.PagedInventoryMenuImpl;
import org.wallentines.pseudonym.PipelineContext;

public interface PagedInventoryMenu extends InventoryMenu {

    /**
     * Opens the GUI for the given player.
     * @param player The player to open the menu for.
     * @param page The page to open the menu to.
     */
    void open(ServerPlayer player, int page);

    /**
     * Sets the item and click event at the given index of the GUI.
     * @param index The slot index.
     * @param itemStack The item to put at that index.
     * @param event The click event to invoke when a player clicks at the index.
     */
    void setItem(int index, ItemStack itemStack, PagedClickEvent event);

    /**
     * Sets the item and click event at the given index of the GUI. The item will be resolved with respect to each player.
     * @param index The slot index.
     * @param itemStack The item to put at that index.
     * @param event The click event to invoke when a player clicks at the index.
     */
    void setItem(int index, ItemSupplier itemStack, PagedClickEvent event);


    /**
     * Gets the number of pages in this menu
     */
    int pageCount();

    /**
     * Reserves space for up to the given number of items by resizing immediately.
     * @param max The new maximum item.
     */
    void resize(int max);

    /**
     * Adds a row at the top of each page which is not counted toward the menu's maximum size.
     * @param rowProvider Some logic to fill the reserved row with menu items.
     */
    void addTopReservedRow(RowProvider rowProvider);

    /**
     * Adds a row at the bottom of each page which is not counted toward the menu's maximum size.
     * @param rowProvider Some logic to fill the reserved row with menu items.
     */
    void addBottomReservedRow(RowProvider rowProvider);



    /**
     * Creates a new paged, growable Inventory GUI.
     * @param title Some logic to get the menu's title, per player.
     * @param sizeProvider Some logic to determine the size of each page.
     * @return A new PagedInventoryGui
     */
    static PagedInventoryMenu create(ComponentSupplier title, PagedInventoryMenu.SizeProvider sizeProvider) {
        return PagedInventoryMenuImpl.create(title, sizeProvider, 0, PipelineContext.EMPTY);
    }

    /**
     * Creates a new paged, growable Inventory GUI with the given number of slots reserved.
     * @param title Some logic to get the menu's title, per player.
     * @param sizeProvider Some logic to determine the size of each page.
     * @param size The minimum number of slots to reserve when this GUI is created.
     * @return A new PagedInventoryGui
     */
    static PagedInventoryMenu create(ComponentSupplier title, PagedInventoryMenu.SizeProvider sizeProvider, int size) {
        return PagedInventoryMenuImpl.create(title, sizeProvider, size, PipelineContext.EMPTY);
    }

    /**
     * Creates a new paged, growable Inventory GUI with the given inherent context.
     * @param title Some logic to get the menu's title, per player.
     * @param sizeProvider Some logic to determine the size of each page.
     * @param context Context to be applied each time an item or the title is resolved.
     * @return A new PagedInventoryGui
     */
    static PagedInventoryMenu create(ComponentSupplier title, PagedInventoryMenu.SizeProvider sizeProvider, PipelineContext context) {
        return PagedInventoryMenuImpl.create(title, sizeProvider, 0, context);
    }

    /**
     * Creates a new paged, growable Inventory GUI with the given inherent context and the given number of slots reserved.
     * @param title Some logic to get the menu's title, per player.
     * @param sizeProvider Some logic to determine the size of each page.
     * @param size The minimum number of slots to reserve when this GUI is created.
     * @param context Context to be applied each time an item or the title is resolved.
     * @return A new PagedInventoryGui
     */
    static PagedInventoryMenu create(ComponentSupplier title, PagedInventoryMenu.SizeProvider sizeProvider, int size, PipelineContext context) {
        return PagedInventoryMenuImpl.create(title, sizeProvider, size, context);
    }


    /**
     * Determines the size of each page of a paged inventory menu.
     */
    interface SizeProvider {
        int getRows(int localOffset, int page, int lastItem, PagedInventoryMenu gui);

        /**
         * Creates a size provider for menus where each page should be the same size.
         * @param size The number of rows in each page.
         * @return A new SizeProvider
         */
        static SizeProvider fixed(int size) {
            return new PagedInventoryMenuImpl.Fixed(size);
        }

        /**
         * Creates a size provider for menus where each page should only be as big as necessary to hold its items.
         * @param maxSize The maximum number of rows in each page.
         * @return A new SizeProvider
         */
        static SizeProvider dynamic(int maxSize) {
            return new PagedInventoryMenuImpl.Dynamic(maxSize);
        }
    }


    /**
     * Represents a single row in an inventory menu.
     */
    interface Row {

        /**
         * Sets the item and click event at the given index of the row. The item will be resolved with respect to each player.
         * @param index The slot index.
         * @param itemStack The item to put at that index.
         * @param event The click event to invoke when a player clicks at the index.
         * @see InventoryMenu#setItem(int, ItemStack, ClickEvent)
         */
        void setItem(int index, ItemSupplier itemStack, PagedClickEvent event);
    }


    /**
     * Determines the size of each page of a paged inventory menu.
     */
    interface RowProvider {

        /**
         * Fills the row with items.
         * @param page The page this row is on.
         * @param row The row itself.
         * @param menu The menu this row belongs to.
         */
        void fillRow(int page, Row row, PagedInventoryMenu menu);

        /**
         * A row provider which gives page controls using the given items.
         * @param nextPage The item to use to advance to the next page.
         * @param prevPage The item to use to go back to the previous page.
         * @return A new row provider with page controls.
         */
        static RowProvider pageControls(ItemSupplier nextPage, ItemSupplier prevPage) {
            return (page, row, gui) -> {

                if(page > 0) {
                    row.setItem(0, prevPage, ((player, type, p) -> {
                        gui.open(player, p - 1);
                    }));
                }
                if(page + 1 < gui.pageCount()) {
                    row.setItem(8, nextPage, ((player, type, p) -> {
                        gui.open(player, p + 1);
                    }));
                }
            };
        }
    }


    /**
     * A page-aware click event
     * @see org.wallentines.invmenu.api.InventoryMenu.ClickEvent
     */
    interface PagedClickEvent {
        void execute(ServerPlayer player, ClickType type, int page);
    }

}
