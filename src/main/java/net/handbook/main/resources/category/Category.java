package net.handbook.main.resources.category;

import net.handbook.main.HandbookClient;
import net.handbook.main.resources.entry.BaseEntry;
import net.handbook.main.resources.entry.Entry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Category<E extends Entry> extends BaseEntry implements Comparable<Category<?>> {

    final String type;
    final List<E> entries;

    public Category(String type, String title) {
        super(title, null, null);
        this.type = type;
        this.entries = new ArrayList<>();
    }

    @SuppressWarnings("unused")
    public Category(String type, String title, String text, String image, List<E> entries) {
        super(title, text, image);
        this.type = type;
        this.entries = entries;
    }

    @Override
    public void mouseClicked() {
        HandbookClient.handbookScreen.setEntries(this);
        HandbookClient.handbookScreen.activeCategory = this;
    }

    public String getType() {
        return type;
    }

    public List<E> getEntries() {
        return entries;
    }

    @Override
    public int compareTo(@NotNull Category entry) {
        return getSortableTitle().compareTo(entry.getSortableTitle());
    }
}
