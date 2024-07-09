package net.handbook.main.resources.entry;

public class PositionedEntry extends Entry {

    final String shard;
    int[] position;

    public PositionedEntry(String title, String text, String image, String shard, int[] position) {
        super(title, text, image);
        this.shard = shard;
        this.position = position;
    }

    @Override
    public String getShard() {
        return shard;
    }

    @Override
    public int[] getPosition() {
        return position;
    }

    public void update(String title, String text, int[] position) {
        super.update(title, text);
        this.position = position;
    }
}
