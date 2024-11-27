package net.handbook.main.resources.entry;

abstract public class BaseEntry {

    String title;
    String text;
    final String image;

    public BaseEntry(String title, String text, String image) {
        this.title = title;
        this.text = text;
        this.image = image;
    }

    public void update(String title, String text) {
        this.title = title;
        this.text = text;
    }

    public abstract void mouseClicked();

    public String getTitle() {
        return title;
    }

    public String getClearTitle() {
        return title == null ? null : title.replaceAll("§.", "");
    }

    public String getText() {
        return text;
    }

    public boolean hasImage() {
        return image != null && !image.isEmpty();
    }

    public String getImage() {
        return image;
    }

    public enum Type {
        Entry,
        Category
    }
}
