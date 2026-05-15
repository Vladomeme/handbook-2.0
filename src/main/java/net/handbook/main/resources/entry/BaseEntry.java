package net.handbook.main.resources.entry;

abstract public class BaseEntry {

    int lastChanged; //if this is still maintained in 2038, good luck
    String title;
    String text;
    final String image;

    public BaseEntry(String title, String text, String image) {
        this.title = title;
        this.text = text;
        this.image = image;
        this.lastChanged = Math.toIntExact(System.currentTimeMillis() / 1000);
    }

    public void update(String title, String text) {
        this.title = title;
        this.text = text;
        this.lastChanged = Math.toIntExact(System.currentTimeMillis() / 1000);
    }

    public void updateTimestamp() {
        this.lastChanged = Math.toIntExact(System.currentTimeMillis() / 1000);
    }

    public abstract void mouseClicked();

    public String title() {
        return title;
    }

    public String clearTitle() {
        return title == null ? null : title.replaceAll("§.", "").replaceAll("##.", "");
    }

    public String sortableTitle() {
        return title == null ? null : title.replaceAll("§.", "");
    }

    public String displayTitle() {
        return title == null ? null : title.replaceAll("##.", "");
    }

    public String text() {
        return text;
    }

    public String image() {
        return image;
    }

    public boolean hasImage() {
        return image != null && !image.isEmpty();
    }

    public int lastChanged() {
        return lastChanged;
    }
}
