package net.handbook.main.resources.category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MarkCategory extends BaseCategory {

    final String type;
    final HashMap<String, List<String>> entries;

    public MarkCategory(String title, String text, String image, HashMap<String, List<String>> entries) {
        super("mark", title, text, image, null);
        this.type = "mark";
        this.entries = entries;
    }

    @Override
    public String getType() {
        return type;
    }

    public void addCategory(String name) {
        entries.put(name, new ArrayList<>());
    }

    public List<String> getMarkedEntries(String category) {
        return entries.get(category);
    }
}
