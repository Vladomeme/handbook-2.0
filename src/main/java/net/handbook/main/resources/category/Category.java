package net.handbook.main.resources.category;

import net.handbook.main.HandbookClient;
import net.handbook.main.resources.entry.BaseEntry;
import net.handbook.main.resources.entry.Entry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Category extends BaseEntry implements Comparable<Category> {

    final String type;
    final List<Entry> entries;

    public Category(String type, String title, String text, String image, List<Entry> entries) {
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

    public List<Entry> getEntries() {
        return entries;
    }

    @Override
    public int compareTo(@NotNull Category entry) {
        return getClearTitle().compareTo(entry.getClearTitle());
    }
}
