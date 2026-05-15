package net.handbook.main.resources.entry;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.handbook.main.editor.NPCWriter;
import net.handbook.main.mixin.StringNbtReaderAccessor;
import net.handbook.main.resources.ChatChannel;
import net.handbook.main.resources.ShareMode;
import net.handbook.main.resources.HandbookTradeOffer;
import net.handbook.main.resources.HandbookTradeOfferList;
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

public class TraderEntry extends PositionEntry {

    final String id;
    boolean persistent; //protects the entry from being auto-removed
    transient HandbookTradeOfferList offers = null;

    public TraderEntry(String title, String text, String image, String shard, int[] position) {
        super(title, text, image, shard, position);
        this.id = NPCWriter.getID(title, position[0], position[1], position[2]);
    }

    //for blacklist
    public TraderEntry(String id, String shard) {
        super(null, null, null, shard, null);
        this.id = id;
    }

    @Override
    public HandbookTradeOfferList offers() {
        if (offers == null) offers = offers(this.id);
        return offers;
    }

    public static HandbookTradeOfferList offers(String id) {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + id);
        if (!Files.exists(path)) return HandbookTradeOfferList.EMPTY;

        String currentString = "";
        try {
            ClientPlayNetworkHandler nh = MinecraftClient.getInstance().getNetworkHandler();
            if (nh == null) throw new IllegalStateException("[Handbook 2.0] TraderEntry.getOffers() got called outside of a game world.");

            currentString = NPCWriter.decompressTrades(Files.readAllBytes(path));
            if (currentString.charAt(0) == '[') { //legacy file
                StringNbtReader nbtReader = new StringNbtReader(new StringReader(currentString));
                @SuppressWarnings("DataFlowIssue")
                DataResult<Pair<List<HandbookTradeOffer>, NbtElement>> decodingResult = HandbookTradeOffer.LIST_CODEC.decode(
                        new Dynamic<>(nh.getRegistryManager().getOps(NbtOps.INSTANCE), ((StringNbtReaderAccessor) nbtReader).parseList()));

                if (decodingResult.result().isEmpty()) return HandbookTradeOfferList.EMPTY;

                HandbookTradeOfferList offerList = new HandbookTradeOfferList(decodingResult.result().get().getFirst());
                DataResult<NbtElement> encodingResult = HandbookTradeOfferList.CODEC.encodeStart(nh.getRegistryManager().getOps(NbtOps.INSTANCE), offerList);

                if (encodingResult.isError()) {
                    HandbookClient.LOGGER.info("[Handbook 2.0] Failed to encode legacy NPC offers: {}", encodingResult.error());
                }
                NPCWriter.updatedOffers.put(id, NPCWriter.compressTrades(encodingResult.result().get().toString()));
                return offerList;
            }
            else {
                DataResult<Pair<HandbookTradeOfferList, NbtElement>> dataResult = HandbookTradeOfferList.CODEC.decode(
                        new Dynamic<>(nh.getRegistryManager().getOps(NbtOps.INSTANCE), StringNbtReader.parse(currentString)));
                return dataResult.result().isPresent() ? dataResult.result().get().getFirst() : HandbookTradeOfferList.EMPTY;
            }
        }
        catch (CommandSyntaxException e) {
            HandbookClient.LOGGER.error("[Handbook 2.0] Unable to parse trader's offers: {}. {} {}", id, e.getMessage(), currentString);
            return HandbookTradeOfferList.EMPTY;
        }
        catch (IOException e) {
            HandbookClient.LOGGER.error("[Handbook 2.0] Unable to read trader's offers: {}. {}", id, e.getMessage());
            return HandbookTradeOfferList.EMPTY;
        }
    }

    @Override
    public boolean hasOffers() {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + id);
        return Files.exists(path);
    }

    public String offersRaw() {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + id);
        if (!Files.exists(path)) return null;
        try {
            return NPCWriter.decompressTrades(Files.readAllBytes(path));
        }
        catch (IOException e) {
            HandbookClient.LOGGER.error("[Handbook 2.0] Unable to read trader's offers: {}.", id);
            return null;
        }
    }

    @Override
    public String id() {
        return id;
    }

    public boolean persistent() {
        return persistent;
    }

    public void makePersistent() {
        persistent = true;
    }

    //todo update position clickevent to match the [x, y, z] format
    public void share(ChatChannel chatChannel, HandbookTradeOffer trade, ShareMode shareMode) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        StringBuilder command = new StringBuilder().append(chatChannel.chatId).append(" ");
        String traderInfo = " | " + clearTitle() + " (" + shard() + ") " + Arrays.toString(position());

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
}
