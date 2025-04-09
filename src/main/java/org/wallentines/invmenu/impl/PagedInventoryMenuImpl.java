package org.wallentines.invmenu.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.wallentines.invmenu.api.InventoryMenu;
import org.wallentines.invmenu.api.PagedInventoryMenu;
import org.wallentines.pseudonym.Message;
import org.wallentines.pseudonym.PipelineContext;
import org.wallentines.pseudonym.Placeholder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PagedInventoryMenuImpl implements PagedInventoryMenu {

    private final Message<Component> title;
    private final PagedInventoryMenu.SizeProvider sizeProvider;
    private final PipelineContext context;
    private final List<RowProvider> topReserved = new ArrayList<>();
    private final List<RowProvider> bottomReserved = new ArrayList<>();
    private List<Page> pages = new ArrayList<>();

    private int rows;

    protected PagedInventoryMenuImpl(Message<Component> title, SizeProvider sizeProvider, int rows, PipelineContext context) {
        this.title = title;
        this.sizeProvider = sizeProvider;
        this.rows = rows;
        this.context = context;
    }

    @Override
    public void open(ServerPlayer player, int page) {
        Page p = pages.get(page);

        PipelineContext ctx = PipelineContext.builder(player, this)
                .withContextPlaceholder("gui_page", String.valueOf(page + 1))
                .withContextPlaceholder(Placeholder.of("gui_pages", String.class, rCtx -> Optional.of(String.valueOf(pageCount()))))
                .build();

        p.gui.open(player, ctx);
    }


    @Override
    public void addTopReservedRow(RowProvider rowProvider) {
        topReserved.add(rowProvider);
        updatePages(size(), true);
    }

    @Override
    public void addBottomReservedRow(RowProvider rowProvider) {
        bottomReserved.add(rowProvider);
        updatePages(size(), true);
    }

    @Override
    public int pageCount() {
        return pages.size();
    }

    @Override
    public void setItem(int index, ItemStack itemStack, ClickEvent event) {
        setItem(index, pl -> itemStack, event);
    }

    @Override
    public void setItem(int index, ItemSupplier itemStack, ClickEvent event) {
        Page p = updateAndGetPage(index);
        int topOffset = topReserved.size() * 9;
        p.gui.setItem(topOffset + index - p.offset, itemStack, event);
    }

    @Override
    public void setItem(int index, ItemStack itemStack, PagedClickEvent event) {
        setItem(index, pl -> itemStack, event);
    }

    @Override
    public void setItem(int index, ItemSupplier itemStack, PagedClickEvent event) {
        Page p = updateAndGetPage(index);
        int topOffset = topReserved.size() * 9;
        p.gui.setItem(topOffset + index - p.offset, itemStack, (player, type) -> event.execute(player, type, p.index));
    }

    @Override
    public void clearItem(int index) {
        Page p = getPage(index);
        if(p != null) p.gui.clearItem(index - p.offset);
    }

    @Override
    public int rows() {
        return rows;
    }

    @Override
    public int size() {
        return rows * 9;
    }

    @Override
    public int firstEmpty() {
        for(Page p : pages) {
            int firstEmpty = p.gui.firstEmpty();
            if(firstEmpty != -1) return firstEmpty;
        }
        return -1;
    }

    @Override
    public int lastItem() {
        for(int i = pages.size() - 1 ; i > 0 ; i--) {
            int lastItem = pages.get(i).gui.lastItem();
            if(lastItem != -1) return lastItem;
        }
        return -1;
    }

    @Override
    public void clear() {
        for(Page p : pages) {
            p.gui.closeAll();
        }
        pages.clear();
        resize(0);
    }

    @Override
    public void update() {
        for(Page p : pages) {
            p.gui.update();
        }
    }

    @Override
    public void open(ServerPlayer player) {
        open(player, 0);
    }

    @Override
    public void close(ServerPlayer player) {
        if(player == null) return;
        if(player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
    }

    @Override
    public void closeAll() {
        for(Page p : pages) {
            p.gui.closeAll();
        }
    }

    @Override
    public void moveViewers(InventoryMenu other) {
        for(Page p : pages) {
            p.gui.moveViewers(other);
        }
    }

    public void resize(int max) {
        if(max > size()) {
            updatePages(max, false);
        }
    }

    private Page getPage(int index) {

        for(Page p : pages) {
            if(p.offset + p.size > index) {
                return p;
            }
        }
        return null;
    }

    private Page updateAndGetPage(int index) {
        resize(index);
        return getPage(index);
    }

    private void setupReserved(InventoryMenuImpl gui, int page) {

        int offset = 0;
        for(RowProvider rp : topReserved) {
            gui.clear(offset, offset + 9);
            rp.fillRow(page, rowFromGui(gui, page, offset), this);
        }
        offset = gui.size() - (bottomReserved.size() * 9);
        for(RowProvider rp : bottomReserved) {
            gui.clear(offset, offset + 9);
            rp.fillRow(page, rowFromGui(gui, page, offset), this);
        }
    }

    private Page createEmptyPage(int page, int offset, int size) {

        int realSize = size + (topReserved.size() * 9) + (bottomReserved.size() * 9);

        InventoryMenuImpl gui = InventoryMenuImpl.create(title, realSize, context);
        return new Page(gui, offset, page, size);
    }

    private void updatePages(int lastItem, boolean forceRefresh) {

        // Find the new page sizes
        List<Integer> newSizes = new ArrayList<>();
        int offset = 0;
        int rowOffset = 0;
        while(offset <= lastItem) {
            int pageRows = sizeProvider.getRows(offset, lastItem, newSizes.size(), this);
            int pageSize = pageRows * 9;

            newSizes.add(pageSize);
            offset += pageSize;
            rowOffset += pageRows;
        }
        rows = rowOffset;

        List<Page> newPages = new ArrayList<>();

        // Update old pages
        Page partialPage = null;
        int itemsRemaining = 0;
        int topOffset = topReserved.size() * 9;
        offset = 0;

        for(Page p : pages) {

            int pItems = p.size;
            int rpItems = pItems;

            // Finish partial page
            if(itemsRemaining > 0) {

                int realSize = partialPage.size;
                int copyStart = topOffset + realSize - itemsRemaining;
                int copied = Math.min(itemsRemaining, rpItems);

                itemsRemaining -= copied;

                System.arraycopy(p.gui.items, topOffset, partialPage.gui.items, copyStart, copied);
                partialPage.gui.update();

                if(itemsRemaining == 0) {
                    newPages.add(partialPage);
                    partialPage = null;
                }
            }

            // Copy remaining items
            if(rpItems > 0) {

                int index = newPages.size();
                int newPageSize = newSizes.get(index);

                // If the page is the same, just reinsert it.
                if(!forceRefresh && p.offset == offset && p.size == newPageSize) {
                    if(index == p.index) {
                        newPages.add(p);
                    } else {
                        newPages.add(p.reindex(index, title));
                        setupReserved(p.gui, index);
                    }

                // If not, reconstruct it.
                } else {

                    while(rpItems > 0) {

                        partialPage = createEmptyPage(newPages.size(), offset, newPageSize);
                        int contentSize = partialPage.size;

                        int copied = Math.min(contentSize, rpItems);
                        System.arraycopy(p.gui.items, topOffset, partialPage.gui.items, topOffset, copied);
                        partialPage.gui.update();
                        p.gui.moveViewers(partialPage.gui);

                        itemsRemaining = contentSize - copied;
                        if(itemsRemaining == 0) {
                            newPages.add(partialPage);
                            partialPage = null;
                        }

                        rpItems -= copied;
                    }
                }
            }

            offset += pItems;
        }

        if(partialPage != null) {
            offset += itemsRemaining;
            newPages.add(partialPage);
        }

        // Insert new pages
        for(int i = newPages.size(); i < newSizes.size() ; i++) {
            int size = newSizes.get(i);
            newPages.add(createEmptyPage(i, offset, size));
            offset += size;
        }

        this.pages = newPages;
        for(Page p : pages) {
            setupReserved(p.gui, p.index);
        }

    }


    public static PagedInventoryMenu create(Message<Component> title, PagedInventoryMenu.SizeProvider sizeProvider, int size, PipelineContext ctx) {
        return new PagedInventoryMenuImpl(title, sizeProvider, size, ctx);
    }


    public static Row rowFromGui(InventoryMenu gui, int page, int offset) {
        return (index, is, event) -> {
            if(index < 0 || index > 8) {
                throw new IllegalStateException("Attempt to place item outside of row bounds!");
            }

            gui.setItem(offset + index, is, (pl, ct) ->
                    event.execute(pl, ct, page));
        };
    }


    private record Page(InventoryMenuImpl gui, int offset, int index, int size) {

        Page reindex(int index, Message<Component> title) {
                return new Page(gui.copy(title), offset, index, size);
            }
    }


    public static class Fixed implements PagedInventoryMenu.SizeProvider {
        private final int pageSize;
        public Fixed(int pageSize) {
            this.pageSize = pageSize;
        }
        @Override
        public int getRows(int localOffset, int lastItem, int page, PagedInventoryMenu gui) {
            return pageSize;
        }
    }

    public static class Dynamic implements PagedInventoryMenu.SizeProvider {
        private final int maxPageSize;
        public Dynamic(int maxPageSize) {
            this.maxPageSize = maxPageSize;
        }
        @Override
        public int getRows(int localOffset, int lastItem, int page, PagedInventoryMenu gui) {

            int slots = lastItem - localOffset + 1;
            int fullRows = slots / 9;
            if(fullRows >= maxPageSize) return maxPageSize;

            int partialRow = slots % 9;
            if(partialRow > 0 || fullRows == 0) fullRows++;

            return fullRows;
        }
    }

}
