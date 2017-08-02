package magic.data.settings;

public enum BooleanSetting {

    ALWAYS_PASS("pass", true),
    ANIMATE_GAMEPLAY("animateGameplay", true),
    CUSTOM_BACKGROUND("customBackground", false),
    CUSTOM_FONTS("custom.fonts", true),
    CUSTOM_SCROLLBAR("customScrollBar", true),
    FULL_SCREEN("fullScreen", false),
    MAXIMIZE_FRAME("maximized", false),
    ;

    private final String propertyName;
    private final boolean defaultValue;

    private BooleanSetting(String propertyName, boolean defaultValue) {
        this.propertyName = propertyName;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return propertyName;
    }

    public boolean getDefault() {
        return defaultValue;
    }

}
