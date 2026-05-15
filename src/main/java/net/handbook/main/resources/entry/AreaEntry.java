package net.handbook.main.resources.entry;

public class AreaEntry extends PositionEntry {

    int[] area;

    public AreaEntry(String title, String text, String image, String shard, int[] position, int[] area) {
        super(title, text, image, shard, position);
        this.area = area;
    }

    public AreaEntry(String title, String text, String image, String shard, int[] position, int[] area, String icon) {
        this(title, text, image, shard, position, area);
        this.icon = icon;
    }

    @Override
    public int[] area() {
        return area;
    }

    public void update(String title, String text, int[] position, String icon, int[] area) {
        super.update(title, text, position, icon);
        this.area = area;
    }
}
