package appeng.core.transformer;

import appeng.core.AE2ELCore;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class AE2ELTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        transformedName = transformedName.replace('/', '.');

        if ("net.minecraftforge.common.ForgeHooks".equals(transformedName)) {
            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new SafeClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES,
                    Launch.classLoader);
            ClassVisitor cv = new PickBlockPatch(cw);
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
            return cw.toByteArray();
        }

        if (!AE2ELCore.stackUpLoaded) {
            final ClassWriter cw;
            final ClassReader cr;
            if (transformedName.equals(PacketStackPatch.PACKET_BUFFER_CLASS) ||
                    transformedName.equals(PacketStackPatch.PACKET_UTIL_CLASS)) {
                cw = new ClassWriter(0);
                cr = new ClassReader(basicClass);
                cr.accept(new PacketStackPatch(cw, transformedName), 0);
                return cw.toByteArray();
            } else if (transformedName.equals(ItemStackPatch.ITEM_STACK_CLASS)) {
                cw = new ClassWriter(0);
                cr = new ClassReader(basicClass);
                cr.accept(new ItemStackPatch(cw), 0);
                return cw.toByteArray();
            }
        }

        return basicClass;
    }
}
