package magic.model;

public interface MagicCopyable {
    default MagicCopyable copy(final MagicCopyMap copyMap) {
        return this;
    }
}
