package net.handbook.main.resources;

import java.util.HashMap;

public class PositionedEntry extends Entry {

    final String shard;
    final String position;

    public PositionedEntry(String title, String text, String image, String shard, String position) {
        super(title, text, image);
        this.shard = shard;
        this.position = position;
    }

    @Override
    public HashMap<String, String> getTextFields() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("shard", shard);
        hashMap.put("position", position);
        return hashMap;
    }
}
