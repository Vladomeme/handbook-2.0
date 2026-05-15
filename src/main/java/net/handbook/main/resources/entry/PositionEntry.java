package net.handbook.main.resources.entry;

public class PositionEntry extends Entry {

    final String shard;
    int[] position;
    String icon; //todo should default icon name be set?

    public PositionEntry(String title, String text, String image, String shard, int[] position) {
        super(title, text, image);
        this.shard = shard;
        this.position = position;
    }

    public PositionEntry(String title, String text, String image, String shard, int[] position, String icon) {
        this(title, text, image, shard, position);
        this.icon = icon;
    }

    @Override
    public String shard() {
        return shard;
    }

    @Override
    public int[] position() {
        return position;
    }

    @Override
    public String icon() {
        return icon;
    }

    public void update(String title, String text, int[] position, String icon) {
        super.update(title, text);
        this.position = position;
        this.icon = icon;
    }
}
