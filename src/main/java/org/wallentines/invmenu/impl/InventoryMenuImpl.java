package org.wallentines.invmenu.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.invmenu.api.InventoryMenu;
import org.wallentines.pseudonym.Message;
import org.wallentines.pseudonym.PipelineContext;

import java.util.ArrayList;
import java.util.List;

public class InventoryMenuImpl implements InventoryMenu {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryMenuImpl.class);

    private final Message<Component> title;
    private final int rows;
    private final PipelineContext context;
    protected final Entry[] items;
    private final List<Menu> open = new ArrayList<>();

    protected InventoryMenuImpl(Message<Component> title, int rows, PipelineContext context) {
        this.title = title;
        this.rows = rows;
        this.items = new Entry[rows * 9];
        this.context = context;
    }

    @Override
    public void setItem(int index, ItemStack itemStack, ClickEvent event) {
        items[index] = new Entry(itemStack, event);
    }

    @Override
    public void setItem(int index, ItemSupplier itemStack, ClickEvent event) {
        items[index] = new Entry(itemStack, event);
    }

    @Override
    public void clearItem(int index) {
        items[index] = null;
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
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null)
                return i;
        }
        return -1;
    }

    @Override
    public int lastItem() {
        for (int i = items.length; i > 0; i--) {
            if (items[i - 1] != null)
                return i;
        }
        return -1;
    }

    @Override
    public void clear() {
        int last = lastItem();
        for (int i = 0; i < last; i++) {
            items[i] = null;
        }
    }

    @Override
    public void update() {
        for (Menu menu : open) {
            menu.update();
        }
    }

    @Override
    public void open(ServerPlayer player) {
        open(player, PipelineContext.of(player, InventoryMenuImpl.this));
    }

    public void open(ServerPlayer player, PipelineContext context) {

        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }

        player.openMenu(new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return title.get(InventoryMenuImpl.this.context.and(context));
            }

            @Override
            public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {

                if (!(player instanceof ServerPlayer spl))
                    return null;
                Menu out = new Menu(i, spl);
                open.add(out);

                return out;
            }
        });
    }

    @Override
    public void close(ServerPlayer player) {
        if (player == null)
            return;
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
    }

    @Override
    public void closeAll() {
        for (Menu m : List.copyOf(open)) {
            m.player.closeContainer();
        }
        open.clear();
    }

    @Override
    public void moveViewers(InventoryMenu other) {
        for (Menu menu : open) {
            ServerPlayer player = menu.player;
            if (player != null) {
                other.open(player);
            }
        }
    }

    public void clearRow(int row) {
        int rowIndex = row * 9;
        clear(rowIndex, rowIndex + 9);
    }

    public void clear(int start, int end) {
        for (int i = start; i < end; i++) {
            clearItem(i);
        }
    }

    public InventoryMenuImpl copy(Message<Component> title) {

        InventoryMenuImpl other = new InventoryMenuImpl(title, rows(), context);
        System.arraycopy(items, 0, other.items, 0, size());
        other.update();

        return other;
    }

    private void onClick(int slot, ServerPlayer serverPlayer, ClickType clickType) {
        Entry ent = items[slot];
        if (ent == null || ent.event == null)
            return;

        try {
            ent.event.execute(serverPlayer, clickType);
        } catch (Throwable th) {
            LOGGER.error("Error while executing click event for slot {}", slot, th);
        }
    }

    public static InventoryMenuImpl create(Message<Component> title, int size, PipelineContext context) {

        int rows = size / 9;
        int partialRows = size % 9;
        if (rows == 0 || partialRows > 0)
            rows++;

        return new InventoryMenuImpl(title, rows, context);
    }

    public static class Entry {

        private final ItemSupplier item;
        private final ClickEvent event;

        public Entry(ItemStack item, ClickEvent event) {
            this.item = pl -> item;
            this.event = event;
        }

        public Entry(ItemSupplier item, ClickEvent event) {
            this.item = item;
            this.event = event;
        }

        public ItemStack getItem(PipelineContext ctx) {
            return item.get(ctx);
        }

        public ClickEvent getEvent() {
            return event;
        }
    }

    private static ClickType getActionType(int action, net.minecraft.world.inventory.ClickType type) {
        return switch (type) {
            case PICKUP -> action == 0 ? ClickType.LEFT : ClickType.RIGHT;
            case QUICK_MOVE -> action == 0 ? ClickType.SHIFT_LEFT : ClickType.SHIFT_RIGHT;
            case SWAP -> ClickType.NUMBER_KEY;
            case CLONE -> ClickType.MIDDLE;
            case THROW -> action == 0 ? ClickType.THROW : ClickType.THROW_ALL;
            case PICKUP_ALL -> ClickType.DOUBLE;
            default -> null;
        };
    }

    private class Menu extends AbstractContainerMenu {

        private final ServerPlayer player;
        private final PipelineContext ctx;

        Menu(int id, ServerPlayer spl) {
            super(getMenuType(InventoryMenuImpl.this.rows()), id);

            this.player = spl;
            this.ctx = context.and(PipelineContext.of(spl));

            Container container = new SimpleContainer(InventoryMenuImpl.this.size());
            int rows = InventoryMenuImpl.this.rows();

            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
                }
            }

            this.addStandardInventorySlots(spl.getInventory(), 8, 18 + rows * 2 + 13);
            update();
        }

        public void update() {

            if (player.isRemoved()) {
                return;
            }

            int stateId = incrementStateId();

            for (int i = 0; i < InventoryMenuImpl.this.size(); i++) {
                Entry ent = InventoryMenuImpl.this.items[i];
                if (ent == null)
                    continue;

                ItemStack is = ent.getItem(ctx);
                if (is != null) {
                    setItem(i, stateId, is);
                }
            }
        }

        @Override
        public @NotNull ItemStack quickMoveStack(Player player, int i) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clicked(int slot, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {

            if (player.level().isClientSide || slot < 0 || slot >= items.length)
                return;
            InventoryMenuImpl.this.onClick(slot, (ServerPlayer) player, getActionType(button, clickType));
        }

        @Override
        public void removed(Player player) {
            if (player == this.player) {
                open.remove(this);
            }
        }

        private static MenuType<?> getMenuType(int rows) {
            return switch (rows) {
                case 1 -> MenuType.GENERIC_9x1;
                case 2 -> MenuType.GENERIC_9x2;
                case 3 -> MenuType.GENERIC_9x3;
                case 4 -> MenuType.GENERIC_9x4;
                case 5 -> MenuType.GENERIC_9x5;
                default -> MenuType.GENERIC_9x6;
            };
        }

    }

}
