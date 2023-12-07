package net.handbook.main.resources;

import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.minecraft.resource.*;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class HandbookResourcePack extends DirectoryResourcePack {
    private final AutoCloseable closer;

    public HandbookResourcePack(Path file, AutoCloseable closer) {
        super("Handbook 2.0 Resources", file, true);
        this.closer = closer;
    }

    @Override
    public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/textures/" + id.getPath());

        if (id.toString().endsWith("properties")) return null;
        if (Files.exists(path, new LinkOption[0])) {
            return InputSupplier.create(path);
        }
        return null;
    }


    @Override
    public void findResources(ResourceType type, String namespace, String prefix, ResultConsumer consumer) {
        if (prefix.equals("font") || prefix.startsWith("textures")
                || prefix.startsWith("citresewn") || prefix.startsWith("mcpatcher") || prefix.startsWith("optifine")) return;
        Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/textures");
        try (Stream<Path> stream2 = Files.find(path, Integer.MAX_VALUE, (path2, attributes) -> attributes.isRegularFile(), new FileVisitOption[0])){
            stream2.forEach(foundPath -> {
                String file = path.relativize(foundPath).toString();
                Identifier identifier = Identifier.of(namespace, file);
                if (identifier == null) {
                    HandbookClient.LOGGER.error("fucked up file: " + file);
                } else {
                    consumer.accept(identifier, InputSupplier.create(foundPath));
                }
            });
        } catch (IOException iOException) {
            HandbookClient.LOGGER.error("Failed to load handbook images");
        }
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        Set<String> set = new HashSet<>();
        set.add("handbook");
        return set;
    }

    @Nullable
    @Override
    public <T> T parseMetadata(ResourceMetadataReader<T> metaReader) {
        InputStream stream = new ByteArrayInputStream("{\"pack\":{\"description\":\"Images for Handbook 2.0\",\"pack_format\":15}}".getBytes(StandardCharsets.UTF_8));
        return AbstractFileResourcePack.parseMetadata(metaReader, stream);
    }

    @Override
    public String getName() {
        return "Handbook Resources";
    }

    @Override
    public void close() {
        if (closer != null) {
            try {
                closer.close();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}