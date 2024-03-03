package net.handbook.main.editor;

public class Location {

    final String title;
    final String text;
    final String image;
    final String shard;
    final int[] position;

    public Location(String title, String world, double x, double y, double z) {
        this.title = title;
        this.text = "";
        this.image = "";
        this.shard = world.replace("monumenta:", "").split("-")[0];
        this.position = new int[]{(int) x, (int) y, (int) z};
    }
}
