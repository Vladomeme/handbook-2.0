package net.handbook.main;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.editor.CategoryWriter;
import net.handbook.main.editor.LocationWriter;
import net.handbook.main.editor.NPCWriter;
import net.handbook.main.feature.TradeScreen;
import net.handbook.main.resources.EntryType;
import net.handbook.main.resources.HandbookTradeOfferList;
import net.handbook.main.resources.entry.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

//todo update part 2: generate a map for dynamic image identifiers when a MapScreen is opened/something is changed, and use that instead of generating new identifiers every frame.
public class DataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("handbook-data");
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static final Path HOME_PATH = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook");
    private static final Path TEXTURES_PATH = Path.of(HOME_PATH + "/textures");
    private static final Path TRADES_PATH = Path.of(HOME_PATH + "/trades");
    private static final Path WAYPOINTS_PATH = Path.of(HOME_PATH + "/waypoints");

    public static final List<CategoryWriter<? extends Entry>> writers = new ArrayList<>();
    private static HashMap<String, List<String>> markedEntries = readMarkedEntries();
    private static HashMap<String, MapPosition> mapCoordinates = readMapInfo();
    static boolean firstLoad = true;
    static boolean tradesLoaded = false;
    static boolean loading = false;
    static boolean shouldUpdateMarked = false;

    static void onReload(ResourceManager manager) {
        if (HandbookConfig.INSTANCE.resetData) {
            copyAllFiles(manager);
            HandbookConfig.INSTANCE.resetData = false;
            HandbookConfig.INSTANCE.write();
            firstLoad = true;
        }
        if (firstLoad) {
            firstLoad = false;
            updateData(manager);
        }
        else save();

        writers.clear();
        loadCategories();

        tradesLoaded = false;
        if (MinecraftClient.getInstance().getNetworkHandler() != null) setTradeEntries();

        markedEntries = readMarkedEntries();
        mapCoordinates = readMapInfo();
    }

    @SuppressWarnings({"unchecked"})
    private static void loadCategories() {
        File[] files = new File(FabricLoader.getInstance().getConfigDir() + "/handbook").listFiles();
        if (files == null) {
            LOGGER.error("[Handbook 2.0] Handbook data files were not found!");
            return;
        }
        List<String> logCategories = new ArrayList<>();
        for (File file : files) {
            if (!file.getName().endsWith("json") || file.getName().equals("config.json") || file.getName().equals("favourite.json")) continue;
            try {
                String s = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                JsonObject jsonObject = JsonParser.parseString(s).getAsJsonObject();

                EntryType type = entryTypeOf(jsonObject.get("type").getAsString(), file.toPath().toString());
                if (type == null) continue;

                CategoryWriter<? extends Entry> writer = createWriter(type, file.toPath(), jsonObject);

                //category name special cases
                if (type.equals(EntryType.trader) && writer.category.title().equals("Blacklist")) NPCWriter.blacklist = (CategoryWriter<TraderEntry>) writer;
                else if (type.equals(EntryType.trader) && writer.category.title().equals("NPC")) NPCWriter.writer = (CategoryWriter<TraderEntry>) writer;
                else if (type.equals(EntryType.position) && writer.category.title().equals("Locations")) LocationWriter.writer = (CategoryWriter<PositionEntry>) writer;

                writers.add(writer);
                logCategories.add(writer.category.clearTitle() + " (" + type.name() + ")");
            }
            catch (IOException | JsonSyntaxException e) {
                LOGGER.info("[Handbook 2.0] Failed to read a data file {}", file.toPath());
                LOGGER.info(e.getMessage());
            }
        }
        writers.sort(null);
        writers.forEach(writer -> {
            if (!writer.category.hidden()) writer.entries().sort(null);
        });
        LOGGER.info("[Handbook 2.0] Loaded {} categories: {}", writers.size(), logCategories);
    }

    public static CategoryWriter<? extends Entry> createWriter(EntryType type, Path path, JsonObject jsonObject) {
        return new CategoryWriter<>(path, EntryType.typeToken(type.typeClass), jsonObject);
    }

    //legacy update method, now used for first loads and data resets
    static void copyAllFiles(ResourceManager manager) {
        LOGGER.info("[Handbook 2.0] Copying all bundled data to local configuration.");

        try {
            if (!Files.exists(TEXTURES_PATH)) Files.createDirectories(TEXTURES_PATH);
            if (!Files.exists(TRADES_PATH)) Files.createDirectories(TRADES_PATH);
            if (!Files.exists(WAYPOINTS_PATH)) Files.createDirectories(WAYPOINTS_PATH);
        }
        catch (IOException e) {
            LOGGER.error("[Handbook 2.0] Failed to create data storage directories.");
            return;
        }

        manager.findResources("handbook_default", id -> true).forEach((id, resource) -> {
            Path path = Path.of(HOME_PATH + id.getPath().replace("handbook_default", ""));
            try {
                Files.write(path, resource.getInputStream().readAllBytes());
            }
            catch (IOException e) {
                LOGGER.error("[Handbook 2.0] Failed to copy data file during data reset: {}.", id.getPath());
            }
        });
    }

    //called at initialization, before writers are available
    static void updateData(ResourceManager manager) {
        String currentVersion;

        Optional<ModContainer> optional = FabricLoader.getInstance().getModContainer("handbook2");
        if (optional.isPresent()) currentVersion = optional.get().getMetadata().getVersion().getFriendlyString();
        else throw new IllegalStateException();

        if (HandbookConfig.INSTANCE.dataVersion.equals(currentVersion)) return;

        HandbookConfig.INSTANCE.dataVersion = currentVersion;
        HandbookConfig.INSTANCE.write();

        //making sure handbook directory structure exists
        try {
            if (Files.notExists(TEXTURES_PATH)) Files.createDirectories(TEXTURES_PATH);
            if (Files.notExists(TRADES_PATH)) Files.createDirectories(TRADES_PATH);
            if (Files.notExists(WAYPOINTS_PATH)) Files.createDirectories(WAYPOINTS_PATH);
        }
        catch (IOException e) {
            LOGGER.error("[Handbook 2.0] Failed to read/create required handbook directories.");
            return;
        }

        Map<String, Integer> localTextureMap = null;
        Map<String, Integer> bundledTextureMap = null;
        Optional<Resource> bundledMapResource = manager.getResource(Identifier.of("minecraft", "handbook_default/textures/map.json"));
        if (bundledMapResource.isPresent()) {
            try {
                bundledTextureMap = getTextureMap(new String(bundledMapResource.get().getInputStream().readAllBytes(), StandardCharsets.UTF_8));

                Path mapPath = Path.of(TEXTURES_PATH + "/map.json");
                if (Files.exists(mapPath)) localTextureMap = getTextureMap(Files.readString(mapPath));
                else localTextureMap = new HashMap<>();
            }
            catch (IOException e) {
                localTextureMap = new HashMap<>();
                if (bundledTextureMap == null) bundledTextureMap = new HashMap<>();
                LOGGER.warn("[Handbook 2.0] Couldn't find a local texture map file. All bundled textures will be copied, overwriting any existing files.");
                LOGGER.error(e.getMessage());
            }
        }
        else {
            LOGGER.warn("[Handbook 2.0] Mod files do not contain a texture map. Texture updating will be skipped.");
        }

        //going through all the mod-bundled files and attempting to update/copy them
        Map<Identifier, Resource> handbookResources = manager.findResources("handbook_default", id -> true);
        for (Map.Entry<Identifier, Resource> entry : handbookResources.entrySet()) {
            Resource resource = entry.getValue();
            String clearPath = entry.getKey().getPath().substring(17); //removing the "handbook_default/"
            String prefix = clearPath.split("/")[0];
            switch (prefix) {
                case "waypoints" -> updateWaypoints(resource, clearPath);
                case "trades" -> updateTrade(resource, clearPath);
                case "textures" -> updateTexture(resource, clearPath, localTextureMap, bundledTextureMap);
                default -> updateCategory(resource, clearPath);
            }
        }
        writeTextureMap(localTextureMap);
        NPCWriter.saveTrades();
    }

    static void updateCategory(Resource resource, String path) {
        if (!path.endsWith(".json") || path.equals("config.json") || path.equals("favourite.json")) return;

        Path filePath = Path.of(HOME_PATH + "/" + path);
        if (Files.exists(filePath)) {
            //data file already exists locally, merging entries
            try {
                JsonObject jsonObjectLocal = JsonParser.parseString(Files.readString(filePath)).getAsJsonObject();
                JsonObject jsonObjectBundled = JsonParser.parseReader(resource.getReader()).getAsJsonObject();

                //creating the writer objects for both old and new files for convenience, merging entries, then writing results to local storage
                EntryType type = entryTypeOf(jsonObjectLocal.get("type").getAsString(), path);
                if (type == null) return;

                CategoryWriter<? extends Entry> localWriter = createWriter(type, filePath, jsonObjectLocal);
                CategoryWriter<? extends Entry> bundledWriter = createWriter(type, filePath, jsonObjectBundled);

                mergeEntries(localWriter, bundledWriter);
                localWriter.write();
            }
            catch (IOException e) {
                LOGGER.info("[Handbook 2.0] Failed to read an existing or bundled data file {}", path);
                LOGGER.info(e.getMessage());
            }
        }
        else {
            //data file doesn't exist, simply copying it
            try {
                Files.write(filePath, resource.getInputStream().readAllBytes());
            }
            catch (IOException e) {
                LOGGER.error("[Handbook 2.0] Failed to copy data file: {}.", path);
                LOGGER.error(e.getMessage());
            }
        }
    }

    //todo data removal update mechanism
    private static void mergeEntries(CategoryWriter<? extends Entry> localWriter, CategoryWriter<? extends Entry> bundledWriter) {
        Map<String, Entry> localEntryMap = new HashMap<>(localWriter.entries().size());
        for (Entry entry : localWriter.entries()) localEntryMap.put(entry.title(), entry);

        Entry localEntry;

        for (Entry bundledEntry : bundledWriter.entries()) {
            localEntry = localEntryMap.get(bundledEntry.title());
            if (localEntry != null) {
                int localTimestamp = localEntry.lastChanged();
                int bundledTimestamp = bundledEntry.lastChanged();

                if ((localTimestamp == 0 && bundledTimestamp == 0) || (localTimestamp < bundledTimestamp)) {
                    localWriter.delete(localEntry);
                    bundledEntry.updateTimestamp();
                }
            }
            localWriter.add(bundledEntry);
        }
    }

    static void updateTrade(Resource resource, String path) {
        Path filePath = Path.of(HOME_PATH + "/" + path);
        if (Files.exists(filePath)) {
            try {
                //data file already exists locally, so check if the bundled file has newer data
                int lastChangedLocal = HandbookTradeOfferList.lastChangedOf(Files.readAllBytes(filePath), false);
                int lastChangedBundled = HandbookTradeOfferList.lastChangedOf(resource.getInputStream().readAllBytes(), true);

                if (lastChangedLocal < lastChangedBundled) {
                    Files.write(filePath, resource.getInputStream().readAllBytes());
                }
            }
            catch (IOException e) {
                LOGGER.error("[Handbook 2.0] Failed to update a trade data file: {}.", path);
                LOGGER.error(e.getMessage());
            }
        }
        else {
            //data file doesn't exist, simply copying it
            try {
                Files.write(filePath, resource.getInputStream().readAllBytes());
            }
            catch (IOException e) {
                LOGGER.error("[Handbook 2.0] Failed to copy a new trade data file: {}.", path);
                LOGGER.error(e.getMessage());
            }
        }
    }

    static void updateWaypoints(Resource resource, String path) {
        Path filePath = Path.of(HOME_PATH + "/" + path);
        if (Files.exists(filePath)) {
            try {
                //data file already exists locally, so check if the bundled file has newer data
                JsonObject jsonObjectLocal = JsonParser.parseString(Files.readString(filePath)).getAsJsonObject();
                JsonElement jsonElementLocal = jsonObjectLocal.get("lastChanged");
                int lastChangedLocal = jsonElementLocal == null ? 0 : jsonElementLocal.getAsInt();

                JsonObject jsonObjectBundled = JsonParser.parseReader(resource.getReader()).getAsJsonObject();
                JsonElement jsonElementBundled = jsonObjectBundled.get("lastChanged");
                int lastChangedBundled = jsonElementBundled == null ? 1 : jsonElementBundled.getAsInt();

                if (lastChangedLocal < lastChangedBundled) {
                    Files.write(filePath, resource.getInputStream().readAllBytes());
                }
            }
            catch (IOException e) {
                LOGGER.error("[Handbook 2.0] Failed to update a legacy waypoint data file: {}.", path);
                LOGGER.error(e.getMessage());
            }
        }
        else {
            //data file doesn't exist, simply copying it
            try {
                Files.write(filePath, resource.getInputStream().readAllBytes());
            }
            catch (IOException e) {
                LOGGER.error("[Handbook 2.0] Failed to copy a new legacy waypoint data file: {}.", path);
                LOGGER.error(e.getMessage());
            }
        }
    }

    static void updateTexture(Resource resource, String path, Map<String, Integer> localTextureMap, Map<String, Integer> bundledTextureMap) {
        if (path.endsWith("json") || localTextureMap == null) return;

        Path filePath = Path.of(HOME_PATH + "/" + path);
        if (Files.exists(filePath)) {
            try {
                //texture file already exists locally, so check if the bundled file is newer
                int localTimestamp = localTextureMap.getOrDefault(path, 0);
                int bundledTimestamp = bundledTextureMap.getOrDefault(path, 1);

                if (localTimestamp < bundledTimestamp) {
                    Files.write(filePath, resource.getInputStream().readAllBytes());
                    localTextureMap.put(path, bundledTimestamp);
                }
            }
            catch (IOException e) {
                LOGGER.error("[Handbook 2.0] Failed to update a texture file: {}.", path);
                LOGGER.error(e.getMessage());
            }
        }
        else {
            //texture file doesn't exist, simply copying it
            try {
                Files.write(filePath, resource.getInputStream().readAllBytes());
            }
            catch (IOException e) {
                LOGGER.error("[Handbook 2.0] Failed to copy a texture file: {}.", path);
                LOGGER.error(e.getMessage());
            }
        }
    }

    private static Map<String, Integer> getTextureMap(String input) {
        TypeToken<HashMap<String, Integer>> typeToken = new TypeToken<>() {};
        return new Gson().fromJson(input, typeToken.getType());
    }

    private static void writeTextureMap(Map<String, Integer> map) {
        Path path = Path.of(TEXTURES_PATH + "/map.json");
        try {
            Files.writeString(path, new Gson().toJson(map), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            LOGGER.warn("[Handbook 2.0] Failed to write an updated texture map {}", path);
            LOGGER.warn(e.getMessage());
        }
    }

    private static EntryType entryTypeOf(String type, String currentPath) {
        try {
            return EntryType.valueOf(type);
        }
        catch (IllegalArgumentException e) {
            LOGGER.error("[Handbook 2.0] Unknown category type \"{}\" in file {}", type, currentPath);
        }
        return null;
    }

    public static List<Category<? extends Entry>> getCategories() {
        List<Category<? extends Entry>> list = new ArrayList<>();
        writers.forEach(writer -> {
            if (!writer.category.hidden()) list.add(writer.category);
        });
        return list;
    }

    static void setTradeEntries() {
        if (loading || tradesLoaded) return;

        loading = true;
        new Thread(null, () -> {
            TradeScreen.offers.clear();
            for (CategoryWriter<? extends Entry> writer : writers) {
                if (!writer.category.type().equals(EntryType.trader)) continue;

                writer.lock();
                for (Entry entry : writer.category.entries()) {
                    if (entry.hasOffers()) TradeScreen.addEntries(entry.offers().entries(), entry.id());
                }
                writer.unlock();
            }
            loading = false;
            tradesLoaded = true;
        }, "Handbook2.0-Trade-Collector").start();
    }

    public static List<String> getMarkedEntries(String name) {
        return markedEntries.computeIfAbsent(name, s -> new ArrayList<>());
    }

    //sets the marked entry data file to be updated during the next save
    public static void updateMarked() {
        shouldUpdateMarked = true;
    }

    //todo combine methods into read(path, typeToken)?
    static HashMap<String, List<String>> readMarkedEntries() {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/favourite.json");

        if (Files.exists(path)) {
            try {
                return (new Gson()).fromJson(Files.readString(path), new TypeToken<HashMap<String, List<String>>>() {}.getType());
            }
            catch (IOException e) {
                HandbookClient.LOGGER.error("[Handbook 2.0] Failed to read favourite.json.");
                HandbookClient.LOGGER.error(e.getMessage());
            }
        }
        return new HashMap<>();
    }

    public static MapPosition getMapCoordinates(String shard) {
        MapPosition position = mapCoordinates.get(shard);
        if (position == null) {
            LOGGER.warn("[Handbook 2.0] Attempted to get map coordinates for shard {}, but none are set in maps.json", shard);
        }
        return new MapPosition(0, 0);
    }

    static HashMap<String, MapPosition> readMapInfo() {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/textures/maps.json");

        if (Files.exists(path)) {
            try {
                return (new Gson()).fromJson(Files.readString(path), new TypeToken<HashMap<String, MapPosition>>() {}.getType());
            }
            catch (IOException e) {
                HandbookClient.LOGGER.error("[Handbook 2.0] Failed to read maps.json.");
                HandbookClient.LOGGER.error(e.getMessage());
            }
        }
        return new HashMap<>();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") //for .mkdirs()
    static void writeMarkedEntries() {
        Gson gson = new Gson();
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = null;
        try {
            File file = new File(FabricLoader.getInstance().getConfigDir() + "/handbook", "favourite.json");
            file.getParentFile().mkdirs();
            jsonWriter = gson.newJsonWriter(stringWriter);
            jsonWriter.setIndent("    ");

            gson.toJson(markedEntries, HashMap.class, jsonWriter);
            String s = stringWriter.toString();
            if (s.length() < 2) {
                HandbookClient.LOGGER.warn("[Handbook 2.0] Data size for marked entries file is too small: {}. Skipping writing.", s.length());
                return;
            }
            Files.writeString(file.toPath(), s, StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            HandbookClient.LOGGER.error("[Handbook 2.0] Failed to write favourite.json.");
            HandbookClient.LOGGER.error(e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(stringWriter);
            IOUtils.closeQuietly(jsonWriter);
        }
    }

    static void save() {
        if (isReady(false)) {
            NPCWriter.saveTrades();
            if (shouldUpdateMarked) {
                writeMarkedEntries();
                shouldUpdateMarked = false;
            }
            writers.forEach(CategoryWriter::write);
            writers.clear();
        }
    }

    public static boolean isReady(boolean sendMessage) {
        if (loading && sendMessage) client.inGameHud.getChatHud().addMessage(Text.of("Handbook 2.0 is still loading, please wait."));
        return !loading;
    }

    static void writerMissingError() {
        client.inGameHud.getChatHud().addMessage(Text.literal("Handbook 2.0 NPC data file is missing or invalid. Expect errors! " +
                        "You can try to restore in by setting `Reset data` in config to true and hitting F3+T. If it doesn't work, " +
                        "please message Vladomeme on discord.")
                .setStyle(Style.EMPTY.withColor(Formatting.RED)));
    }

    //Contains the in-game coordinates of a map's top-left corner
    //Y coordinate is irrelevant (for now?)
    public record MapPosition(int x, int z) {

    }

    //saved just in case I'll need it again
//    static int convertTradeFiles() {
//        ClientPlayNetworkHandler nh = MinecraftClient.getInstance().getNetworkHandler();
//        if (nh == null) return 0;
//        DynamicRegistryManager.Immutable rm = nh.getRegistryManager();
//
//        loop:
//        for (File file : Objects.requireNonNull(TRADES_PATH.toFile().listFiles((dir, name) -> name.endsWith(".txt")))) {
//            try {
//                Path path = file.toPath();
//
//                String stringNbt = NPCWriter.decompressTradesOld(Files.readString(path));
//                stringNbt = stringNbt.replace("minecraft:sweeping", "minecraft:sweeping_edge");
//
//                NbtList offerListNbt = StringNbtReader.parse(stringNbt).getList("Recipes", 10);
//
//                List<HandbookTradeOffer> tradeOfferList = new ArrayList<>(offerListNbt.size());
//                for (NbtElement offer : offerListNbt) {
//                    NbtCompound offerNbt = (NbtCompound) offer;
//
//                    NbtCompound buyNbt = offerNbt.getCompound("buy");
//                    NbtCompound buyBNbt = offerNbt.getCompound("buyB");
//                    NbtCompound sellNbt = offerNbt.getCompound("sell");
//
//                    if (buyNbt.isEmpty() || sellNbt.isEmpty()) {
//                        LOGGER.warn("A BROKEN HANDBOOK TRADE, SKIPPING CONVERSION 1: {}", offerNbt.asString());
//                        continue loop;
//                    }
//
//                    ItemStack buyItem1 = fixStack(rm, buyNbt);
//                    ItemStack buyItem2 = buyBNbt.isEmpty() ? ItemStack.EMPTY : fixStack(rm, buyBNbt);
//                    ItemStack sellItem = fixStack(rm, sellNbt);
//
//                    if (buyItem1.isEmpty() || sellItem.isEmpty()) {
//                        LOGGER.error("A BROKEN HANDBOOK TRADE, SKIPPING CONVERSION 2: {} {} {}",
//                                buyItem1.isEmpty(), sellItem.isEmpty(), offerNbt.asString());
//                        continue loop;
//                    }
//                    if (buyItem2.isEmpty()) tradeOfferList.add(new HandbookTradeOffer(buyItem1, sellItem));
//                    else tradeOfferList.add(new HandbookTradeOffer(buyItem1, buyItem2, sellItem));
//                }
//                DataResult<NbtElement> dataResult = HandbookTradeOfferList.CODEC.encodeStart(rm.getOps(NbtOps.INSTANCE), new HandbookTradeOfferList(tradeOfferList));
//                if (dataResult.isError() && dataResult.error().isPresent()) {
//                    LOGGER.error("FAILED TO RE-ENCODE OFFERS 1: {}", dataResult.error().get().message());
//                }
//                else {
//                    dataResult.ifSuccess(nbtElement -> {
//                        String offersString = nbtElement.asString();
//                        try {
//                            Files.write(Path.of(path.toString().replace(".txt", "")), NPCWriter.compressTrades(offersString));
//                        }
//                        catch (IOException e) {
//                            LOGGER.error("FAILED TO WRITE CONVERTED OFFERS: {}", path.toString().replace(".txt", ""));
//                        }
//                    });
//                }
//            }
//            catch (CommandSyntaxException e) {
//                LOGGER.error("FAILED TO PARSE OFFERS");
//                LOGGER.error(e.getMessage());
//            }
//            catch (IOException e) {
//                LOGGER.error("FAILED TO READ OFFERS FILE");
//                LOGGER.error(e.getMessage());
//            }
//        }
//        LOGGER.info("TRADE CONVERSION COMPLETED");
//        client.inGameHud.getChatHud().addMessage(Text.of("TRADE CONVERSION COMPLETED"));
//        return 1;
//    }
//
//    static ItemStack fixStack(DynamicRegistryManager.Immutable rm, NbtCompound itemNbt) {
//        String id = itemNbt.getString("id");
//        byte count = itemNbt.getByte("Count");
//        if (id.equals("minecraft:air") || count == 0) return ItemStack.EMPTY;
//
//        //FIXES
//        //-----------------------------------------------------------
//        if (id.equals("minecraft:scute")) id = "minecraft:turtle_scute";
//
//        if (itemNbt.getCompound("tag").contains("BlockEntityTag")) {
//            if (id.equals("minecraft:shield")) {
//                itemNbt.getCompound("tag").getCompound("BlockEntityTag").putString("id", "minecraft:banner");
//            }
//            else {
//                BlockState blockState = Registries.BLOCK.get(Identifier.of(id)).getDefaultState();
//                for (BlockEntityType<?> blockEntityType : Registries.BLOCK_ENTITY_TYPE) {
//                    if (!blockEntityType.supports(blockState)) continue;
//
//                    assert blockEntityType.getRegistryEntry() != null;
//                    itemNbt.getCompound("tag").getCompound("BlockEntityTag").putString("id", blockEntityType.getRegistryEntry().getIdAsString());
//                    break;
//                }
//            }
//        }
//
//        if (itemNbt.getCompound("tag").contains("EntityTag")) {
//            Item item = Registries.ITEM.get(Identifier.of(id));
//            if (item instanceof SpawnEggItem spawnEggItem) {
//                for (Map.Entry<EntityType<? extends MobEntity>, SpawnEggItem> entry : SpawnEggItemAccessor.getSpawnEggs().entrySet()) {
//                    if (entry.getValue().equals(spawnEggItem)) {
//                        itemNbt.getCompound("tag").getCompound("EntityTag").putString("id", Registries.ENTITY_TYPE.getId(entry.getKey()).toString());
//                        break;
//                    }
//                }
//            }
//            else itemNbt.getCompound("tag").getCompound("EntityTag").putString("id", id);
//        }
//        //-----------------------------------------------------------
//
//        ItemStackComponentizationFix.StackData stackData = new ItemStackComponentizationFix.StackData(
//                id, count, new Dynamic<>(rm.getOps(NbtOps.INSTANCE), itemNbt));
//        ItemStackComponentizationFix.fixStack(stackData, stackData.nbt);
//
//        DataResult<? extends Pair<ItemStack, ?>> dataResult = ItemStack.CODEC.decode(stackData.finalize());
//
//        if (dataResult.isError() && dataResult.error().isPresent()) {
//            LOGGER.error("FAILED TO RE-ENCODE OFFERS 2: {}", dataResult.error().get().message());
//        }
//        return dataResult.result().isPresent() ? dataResult.result().get().getFirst() : ItemStack.EMPTY;
//    }
}
