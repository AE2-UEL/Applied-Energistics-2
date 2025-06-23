package appeng.api.config;

public enum LockCraftingMode {
    /**
     * Crafting is never locked.
     */
    NONE,
    /**
     * After pushing a pattern to an adjacent machine, the interface will not accept further crafts until a
     * redstone pulse is received.
     */
    LOCK_UNTIL_PULSE,
    /**
     * Crafting is locked while the interface is receiving a redstone signal.
     */
    LOCK_WHILE_HIGH,
    /**
     * Crafting is locked while the interface is not receiving a redstone signal.
     */
    LOCK_WHILE_LOW,
    /**
     * After pushing a pattern to an adjacent machine, the interface will not accept further crafts until the
     * primary pattern result is returned to the network through the interface.
     */
    LOCK_UNTIL_RESULT
}
