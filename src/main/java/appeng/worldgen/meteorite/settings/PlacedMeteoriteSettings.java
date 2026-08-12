package appeng.worldgen.meteorite.settings;

import appeng.worldgen.meteorite.MeteorConstants;
import appeng.worldgen.meteorite.fallout.FalloutMode;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

public final class PlacedMeteoriteSettings {
    private static final String TAG_SEED = "seed";
    private static final String TAG_POS = "pos";
    private static final String TAG_RADIUS = "radius";
    private static final String TAG_CRATER = "type";
    private static final String TAG_FALLOUT = "fallout";
    private static final String TAG_PURE = "pure";
    private static final String TAG_LAKE = "lake";
    private static final String TAG_DECAY = "decay";

    private final long seed;
    private BlockPos pos;
    private final float meteoriteRadius;
    private final CraterType craterType;
    private final boolean pureCrater;
    private CraterLakeState craterLake;
    private final FalloutMode fallout;
    private final boolean doDecay;

    public PlacedMeteoriteSettings(long seed, BlockPos pos, float meteoriteRadius, CraterType craterType,
                                   boolean pureCrater, CraterLakeState craterLake, FalloutMode fallout) {
        this.seed = seed;
        this.pos = pos;
        this.meteoriteRadius = meteoriteRadius;
        this.craterType = craterType;
        this.pureCrater = pureCrater;
        this.craterLake = craterLake;
        this.fallout = fallout;
        this.doDecay = true;
    }

    public PlacedMeteoriteSettings(long seed, BlockPos pos, float meteoriteRadius, CraterType craterType,
                                   boolean pureCrater, CraterLakeState craterLake, FalloutMode fallout,
                                   boolean doDecay) {
        this.seed = seed;
        this.pos = pos;
        this.meteoriteRadius = meteoriteRadius;
        this.craterType = craterType;
        this.pureCrater = pureCrater;
        this.craterLake = craterLake;
        this.fallout = fallout;
        this.doDecay = doDecay;
    }

    public long getSeed() {
        return seed;
    }

    public BlockPos getPos() {
        return pos;
    }

    public CraterType getCraterType() {
        return craterType;
    }

    public float getMeteoriteRadius() {
        return meteoriteRadius;
    }

    public FalloutMode getFallout() {
        return fallout;
    }

    public boolean shouldPlaceCrater() {
        return this.craterType != CraterType.NONE;
    }

    public boolean isPureCrater() {
        return pureCrater;
    }

    public boolean isCraterLakeSet() {
        return craterLake != CraterLakeState.UNSET;
    }

    public boolean isCraterLake() {
        return craterLake == CraterLakeState.TRUE;
    }

    public boolean shouldDecay() {
        return doDecay;
    }

    public void setHeight(int y) {
        this.pos = new BlockPos(pos.getX(), y, pos.getZ());
    }

    public void setCraterLake(CraterLakeState state) {
        this.craterLake = state;
    }

    public static PlacedMeteoriteSettings read(NBTTagCompound nbt) {
        long seed = nbt.getLong(TAG_SEED);
        MutableBlockPos pos = new MutableBlockPos(BlockPos.fromLong(nbt.getLong(TAG_POS)));
        // Unset height gets read as y=0, reset it.
        if (pos.getY() == 0) {
            pos.setY(MeteorConstants.UNSET_HEIGHT);
        }
        float meteoriteRadius = nbt.getFloat(TAG_RADIUS);
        CraterType craterType = CraterType.values()[nbt.getByte(TAG_CRATER)];
        FalloutMode fallout = FalloutMode.values()[nbt.getByte(TAG_FALLOUT)];
        boolean pureCrater = nbt.getBoolean(TAG_PURE);
        CraterLakeState craterLake = CraterLakeState.values()[nbt.getByte(TAG_LAKE)];
        // default true
        boolean doDecay = true;
        if (nbt.hasKey(TAG_DECAY)) {
            doDecay = nbt.getBoolean(TAG_DECAY);
        }

        return new PlacedMeteoriteSettings(seed, pos.toImmutable(), meteoriteRadius, craterType, pureCrater,
                craterLake, fallout, doDecay);
    }

    public NBTTagCompound write(NBTTagCompound nbt) {
        nbt.setLong(TAG_SEED, seed);
        nbt.setLong(TAG_POS, pos.toLong());
        nbt.setFloat(TAG_RADIUS, meteoriteRadius);
        nbt.setByte(TAG_CRATER, (byte) craterType.ordinal());
        nbt.setByte(TAG_FALLOUT, (byte) fallout.ordinal());
        nbt.setBoolean(TAG_PURE, this.pureCrater);
        nbt.setByte(TAG_LAKE, (byte) this.craterLake.ordinal());
        nbt.setBoolean(TAG_DECAY, this.doDecay);
        return nbt;
    }

    @Override
    public String toString() {
        return "PlacedMeteoriteSettings [seed=" + seed + ", pos=" + pos + ", meteoriteRadius=" + meteoriteRadius +
                ", craterType=" + craterType + ", fallout=" + fallout + ", pureCrater=" + pureCrater + ", craterLake="
                + craterLake + ", doDecay=" + doDecay + "]";
    }
}