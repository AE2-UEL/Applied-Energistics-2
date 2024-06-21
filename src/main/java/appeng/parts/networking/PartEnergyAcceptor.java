package appeng.parts.networking;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnits;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.capabilities.Capabilities;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.me.GridAccessException;
import appeng.parts.AEBasePart;
import appeng.parts.PartModel;
import appeng.tile.powersink.ForgeEnergyAdapter;
import appeng.tile.powersink.IExternalPowerSink;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

public class PartEnergyAcceptor extends AEBasePart implements IExternalPowerSink {
    @PartModels
    public static final PartModel MODELS = new PartModel(new ResourceLocation(AppEng.MOD_ID, "part/energy_acceptor"));
    private final IEnergyStorage forgeEnergyAdapter;

    public PartEnergyAcceptor(ItemStack is) {
        super(is);
        this.forgeEnergyAdapter = new ForgeEnergyAdapter(this);
        this.getProxy().setIdlePowerUsage(0.0);
    }

    @Override
    public double injectAEPower(double amt, @NotNull Actionable mode) {
        return amt;
    }

    @Override
    public double getAEMaxPower() {
        return 0;
    }

    @Override
    public double getAECurrentPower() {
        return 0;
    }

    @Override
    public boolean isAEPublicPowerStorage() {
        return false;
    }

    @NotNull
    @Override
    public AccessRestriction getPowerFlow() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public double extractAEPower(double amt, @NotNull Actionable mode, @NotNull PowerMultiplier usePowerMultiplier) {
        return 0;
    }

    @Override
    public double injectExternalPower(PowerUnits input, double amount, Actionable mode) {
        return PowerUnits.AE.convertTo(input, this.funnelPowerIntoStorage(input.convertTo(PowerUnits.AE, amount), mode));
    }

    @Override
    public double getExternalPowerDemand(PowerUnits externalUnit, double maxPowerRequired) {
        return PowerUnits.AE.convertTo(externalUnit,
                Math.max(0.0,
                        this.getFunnelPowerDemand(externalUnit.convertTo(PowerUnits.AE, maxPowerRequired))));
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(2, 2, 14, 14, 14, 16);
        bch.addBox(4, 4, 12, 12, 12, 14);
    }

    protected double getFunnelPowerDemand(double maxRequired) {
        try {
            IEnergyGrid energy = getProxy().getEnergy();
            return energy.getEnergyDemand(maxRequired);
        } catch (GridAccessException e) {
            //NO-OP
        }
        return 0;
    }


    protected double funnelPowerIntoStorage(double power, Actionable mode) {
        try {
            IEnergyGrid energy = getProxy().getEnergy();
            return energy.injectPower(power, mode);
        } catch (GridAccessException e) {
            //NO-OP
        }
        return power;
    }

    @NotNull
    @Override
    public IPartModel getStaticModels() {
        return MODELS;
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 2;
    }

    @Override
    public <T> T getCapability(Capability<T> capabilityClass) {
        if (capabilityClass == Capabilities.FORGE_ENERGY){
            return (T) this.forgeEnergyAdapter;
        }
        return super.getCapability(capabilityClass);
    }

    @Override
    public boolean hasCapability(Capability<?> capabilityClass) {
        if (capabilityClass == Capabilities.FORGE_ENERGY){
            return true;
        }
        return super.hasCapability(capabilityClass);
    }


}
