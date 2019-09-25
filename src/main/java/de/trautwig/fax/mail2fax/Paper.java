package de.trautwig.fax.mail2fax;

public enum Paper {
    A4(209, 296);

    private final int width;
    private final int height;

    Paper(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
