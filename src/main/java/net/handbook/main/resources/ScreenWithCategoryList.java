package net.handbook.main.resources;

import net.handbook.main.resources.entry.Category;
import net.handbook.main.resources.entry.Entry;

public interface ScreenWithCategoryList {

    Category<? extends Entry> activeCategory();
}
