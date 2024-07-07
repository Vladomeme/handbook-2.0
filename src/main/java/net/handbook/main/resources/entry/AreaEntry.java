package net.handbook.main.resources.entry;

public class AreaEntry extends PositionedEntry {

    int[] area;

    public AreaEntry(String title, String text, String image, String shard, int[] position, int[] area) {
        super(title, text, image, shard, position);
        this.area = area;
    }

    @Override
    public int[] getArea() {
        return area;
    }

    public void update(String title, String text, int[] position, int[] area) {
        super.update(title, text, position);
        this.area = area;
    }
}
