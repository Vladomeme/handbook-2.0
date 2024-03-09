package net.handbook.main.resources.entry;

public class AreaEntry extends PositionedEntry {

    final int[] area;

    public AreaEntry(String title, String text, String image, String shard, int[] position, int[] area) {
        super(title, text, image, shard, position);
        this.area = area;
    }

    @Override
    public int[] getArea() {
        return area;
    }
}
