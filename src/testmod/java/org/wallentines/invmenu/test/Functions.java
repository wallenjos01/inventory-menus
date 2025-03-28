package org.wallentines.invmenu.test;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.wallentines.invmenu.api.InventoryMenu;
import org.wallentines.invmenu.api.PagedInventoryMenu;
import org.wallentines.pseudonym.MessagePipeline;
import org.wallentines.pseudonym.PlaceholderManager;
import org.wallentines.pseudonym.UnresolvedMessage;


public class Functions {

    public static void test(CommandSourceStack css,
                            CompoundTag tag,
                            ResourceLocation id,
                            CommandDispatcher<CommandSourceStack> dispatcher,
                            ExecutionContext<CommandSourceStack> exeContext,
                            Frame frame,
                            Void data) throws CommandSyntaxException {


        InventoryMenu menu = InventoryMenu.create(InventoryMenu.ComponentSupplier.completed(Component.literal("Test Menu").withStyle(ChatFormatting.RED)), 9);

        menu.setItem(0, new ItemStack(Items.DIAMOND), (player, type) -> {
            player.sendSystemMessage(Component.literal(type.name()));
            player.getInventory().add(new ItemStack(Items.DIAMOND));
        });


        menu.setItem(4, ctx -> {
            ServerPlayer player = ctx.getFirst(ServerPlayer.class).orElseThrow();
            return new ItemStack(Holder.direct(Items.PLAYER_HEAD), 1, DataComponentPatch.builder()
                    .set(DataComponents.PROFILE, new ResolvableProfile(player.getGameProfile()))
                    .build());
        }, null);

        menu.setItem(8, ItemStack.EMPTY, (player, type) -> {
            player.sendSystemMessage(player.getDisplayName());
            menu.close(player);
        });

        menu.open(css.getPlayerOrException());

    }

    public static void testPaged(CommandSourceStack css,
                            CompoundTag tag,
                            ResourceLocation id,
                            CommandDispatcher<CommandSourceStack> dispatcher,
                            ExecutionContext<CommandSourceStack> exeContext,
                            Frame frame,
                            Void data) throws CommandSyntaxException {

        InventoryMenu.ItemSupplier next = ctx -> (new ItemStack(Holder.direct(Items.LIME_STAINED_GLASS_PANE), 1, DataComponentPatch.builder()
                .set(DataComponents.ITEM_NAME, Component.literal("Next Page"))
                .build()));
        InventoryMenu.ItemSupplier prev = ctx -> (new ItemStack(Holder.direct(Items.RED_STAINED_GLASS_PANE), 1, DataComponentPatch.builder()
                .set(DataComponents.ITEM_NAME, Component.literal("Previous Page"))
                .build()));

        PlaceholderManager man = new PlaceholderManager();
        MessagePipeline<String, UnresolvedMessage<String>> parser = MessagePipeline.parser(man);

        PagedInventoryMenu menu = PagedInventoryMenu.create(InventoryMenu.ComponentSupplier.unresolved(parser.accept("Paged - <gui_page>/<gui_pages>")), PagedInventoryMenu.SizeProvider.dynamic(5), 256);
        menu.addBottomReservedRow(PagedInventoryMenu.RowProvider.pageControls(next, prev));

        Item[] cs = new Item[]{ Items.RED_WOOL, Items.YELLOW_WOOL, Items.LIME_WOOL, Items.BLUE_WOOL };
        for(int c = 0 ; c < cs.length; c++) {
            for(int i = 0 ; i < 64 ; i++) {
                ItemStack is = new ItemStack(cs[c], i + 1);
                int realIndex = c * 64 + i;
                menu.setItem(realIndex, is, (cpl, ct, page) -> {
                    cpl.sendSystemMessage(Component.literal("Page " + (page + 1) + ", Item " + (realIndex + 1)));
                });
            }
        }

        menu.open(css.getPlayerOrException(), 0);

    }

}
