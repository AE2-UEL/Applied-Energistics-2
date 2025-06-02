package appeng.loot;

public interface ILootTallyer {
    boolean canRoll(int max, String itemName, int contextId);

    void tally(String itemName, int contextId);
}
