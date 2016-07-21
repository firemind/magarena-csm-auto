package magic.ui.message;

public enum MessageStyle {
    PLAIN,
    PLAINBOLD,
    PLAINBOLDMONO,
    BOLD;

    public MessageStyle getNext() {
        return values()[(ordinal() + 1) % values().length];
    }
}
