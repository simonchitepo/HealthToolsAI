package com.cypher.zealth;

public class DashboardItem {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_TILE = 1;

    public enum TileStyle {
        SQUARE,   // 1 column
        TALL,     // 1 column, taller
        WIDE      // full-width (spans all columns)
    }

    public final int type;

    // Header
    public final String headerText;

    // Tile
    public final String title;
    public final String subtitle;
    public final String backgroundDrawableName; // e.g. "tile_heatmap"
    public final TileStyle style;
    public final Class<?> targetActivity;

    private DashboardItem(int type,
                          String headerText,
                          String title,
                          String subtitle,
                          String backgroundDrawableName,
                          TileStyle style,
                          Class<?> targetActivity) {
        this.type = type;
        this.headerText = headerText;
        this.title = title;
        this.subtitle = subtitle;
        this.backgroundDrawableName = backgroundDrawableName;
        this.style = style;
        this.targetActivity = targetActivity;
    }

    public static DashboardItem header(String text) {
        return new DashboardItem(TYPE_HEADER, text, null, null, null, null, null);
    }

    public static DashboardItem tile(String title,
                                     String subtitle,
                                     String backgroundDrawableName,
                                     TileStyle style,
                                     Class<?> targetActivity) {
        return new DashboardItem(TYPE_TILE, null, title, subtitle, backgroundDrawableName, style, targetActivity);
    }

    public int spanSize(int spanCount) {
        if (type == TYPE_HEADER) return spanCount;
        if (style == TileStyle.WIDE) return spanCount;
        return 1;
    }

    public int heightDp() {
        if (type == TYPE_HEADER) return 0;
        switch (style) {
            case WIDE:
                return 184;
            case TALL:
                return 260;
            case SQUARE:
            default:
                return 156;
        }
    }
}
