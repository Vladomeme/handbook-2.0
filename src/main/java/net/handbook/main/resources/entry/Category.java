package net.handbook.main.resources.entry;

import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.feature.MapScreen;
import net.handbook.main.resources.EntryType;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Category<E extends Entry> extends BaseEntry implements Comparable<Category<?>> {

    final EntryType type;
    final List<E> entries;
    final boolean hidden;

    public Category(EntryType type, String title, boolean hidden) {
        super(title, null, null);
        this.type = type;
        this.entries = new ArrayList<>();
        this.hidden = hidden;
    }

    @SuppressWarnings("unused")
    public Category(EntryType type, String title, String text, String image, List<E> entries, boolean hidden) {
        super(title, text, image);
        this.type = type;
        this.entries = entries;
        this.hidden = hidden;
    }
    public EntryType type() {
        return type;
    }

    public List<E> entries() {
        return entries;
    }

    public boolean hidden() {
        return hidden;
    }

    @Override
    public void mouseClicked() {
        switch (MinecraftClient.getInstance().currentScreen) {
            case HandbookScreen screen -> {
                screen.setEntries(this);
                screen.activeCategory = this;
            }
            case MapScreen screen -> {
                screen.setEntries(this);
                screen.activeCategory = this;
            }
            case null, default -> {}
        }
    }

    @Override
    public int compareTo(@NotNull Category entry) {
        return sortableTitle().compareTo(entry.sortableTitle());
    }
}
