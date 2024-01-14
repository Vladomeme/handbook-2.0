package net.handbook.main.resources.category;

import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.resources.entry.Entry;

import java.util.List;

public class BaseCategory extends Entry {

    final transient String type;
    final transient List<? extends Entry> entries;

    public BaseCategory(String type, String title, String text, String image, List<? extends Entry> entries) {
        super(title, text, image);
        this.type = type;
        this.entries = entries;
    }

    @Override
    public void mouseClicked() {
        HandbookScreen.setEntries(this);
        HandbookScreen.activeCategory = this;
    }

    public String getType() {
        return type;
    }

    public List<? extends Entry> getEntries() {
        return entries;
    }
}
