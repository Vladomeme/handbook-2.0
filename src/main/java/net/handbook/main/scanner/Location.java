package net.handbook.main.scanner;

public class Location {

    final String title;
    final String text;

    public Location(String title, String world, double x, double y, double z) {
        this.title = title;
        this.text = "Shard: " + world.replace("monumenta", "").split("-")[0] +
                "\nPosition: " +
                String.valueOf(x).split("\\.")[0] + ", " +
                String.valueOf(y).split("\\.")[0] + ", " +
                String.valueOf(z).split("\\.")[0];
    }
}
