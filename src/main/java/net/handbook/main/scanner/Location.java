package net.handbook.main.scanner;

public class Location {

    final String title;
    final String text;
    final String shard;
    final String position;

    public Location(String title, String world, double x, double y, double z) {
        this.title = title;
        this.text = "";
        this.shard = "Shard: " + world.replace("monumenta:", "").split("-")[0];
        this.position = "Position: " +
                String.valueOf(x).split("\\.")[0] + ", " +
                String.valueOf(y).split("\\.")[0] + ", " +
                String.valueOf(z).split("\\.")[0];
    }
}
