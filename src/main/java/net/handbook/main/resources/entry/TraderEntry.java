package net.handbook.main.resources.entry;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.handbook.main.editor.NPCWriter;
import net.handbook.main.mixin.StringNbtReaderInvoker;
import net.handbook.main.resources.HandbookTradeOffer;
import net.handbook.main.widget.TradesWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class TraderEntry extends PositionedEntry {

    final String id;

    public TraderEntry(String title, String text, String image, String shard, int[] position) {
        super(title, text, image, shard, position);
        this.id = NPCWriter.getID(title, position[0], position[1], position[2]);
    }

    public TraderEntry(String id, String shard) {
        super(null, null, null, shard, null);
        this.id = id;
    }

    @SuppressWarnings("DataFlowIssue")  //false warning for invoker mixin cast
    @Override
    public List<HandbookTradeOffer> getOffers() {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + id);
        if (!Files.exists(path)) return List.of();

        String currentString = "";
        try {
            ClientPlayNetworkHandler nh = MinecraftClient.getInstance().getNetworkHandler();
            if (nh == null) throw new IllegalStateException("[Handbook] TraderEntry.getOffers() got called outside of a game world.");

            currentString = NPCWriter.decompressTrades(Files.readAllBytes(path));
            StringNbtReader stringNbtReader = new StringNbtReader(new StringReader(currentString));
            DataResult<Pair<List<HandbookTradeOffer>, NbtElement>> dataResult = HandbookTradeOffer.LIST_CODEC.decode(
                    new Dynamic<>(nh.getRegistryManager().getOps(NbtOps.INSTANCE),
                            ((StringNbtReaderInvoker) stringNbtReader).invokeParseList()));
            return dataResult.result().isPresent() ? dataResult.result().get().getFirst() : List.of();
        }
        catch (CommandSyntaxException e) {
            HandbookClient.LOGGER.error("[Handbook] Unable to read trader's offers: {}. {} {}", id, e.getMessage(), currentString);
            return List.of();
        }
        catch (IOException e) {
            HandbookClient.LOGGER.error("[Handbook] Unable to read trader's offers: {}. {}", id, e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean hasOffers() {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + id);
        return Files.exists(path);
    }

    public String getOffersRaw() {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + id);
        if (!Files.exists(path)) return null;
        try {
            return NPCWriter.decompressTrades(Files.readAllBytes(path));
        }
        catch (IOException e) {
            HandbookClient.LOGGER.error("Unable to read trader's offers: {}.", id);
            return null;
        }
    }
    //todo update position clickevent to match the [x, y, z] format
    public void share(String world, HandbookTradeOffer trade, TradesWidget.Mode shareMode) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        StringBuilder command = new StringBuilder().append(world).append(" ");
        String traderInfo = " | " + getClearTitle() + " (" + getShard() + ") " + Arrays.toString(getPosition());

        switch (shareMode) {
            //Price + sold item
            case COST -> {
                appendItemInfo(command, trade.buyItem1());
                trade.buyItem2().ifPresent(stack -> appendItemInfo(command.append(" + "), stack));
                appendItemInfo(command.append(" -> "), trade.sellItem());
            }
            //Sold item + trader info
            case TRADER -> {
                appendItemInfo(command, trade.sellItem());

                command.append(traderInfo);
            }
            //Price + sold item + trader info
            case FULL -> {
                appendItemInfo(command, trade.buyItem1());
                trade.buyItem2().ifPresent(stack -> appendItemInfo(command.append(" + "), stack));
                appendItemInfo(command.append(" -> "), trade.sellItem());

                command.append(traderInfo);
            }
        }
        client.player.networkHandler.sendCommand(command.toString());
        client.currentScreen = null;
    }

    private void appendItemInfo(StringBuilder builder, ItemStack item) {
        builder.append(item.getName().getString());
        if (item.getCount() != 1) builder.append(" x").append(item.getCount());
    }

    @Override
    public String getID() {
        return id;
    }
}
