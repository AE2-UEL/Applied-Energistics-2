package appeng.core.transformer;

import appeng.core.AE2ELCore;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.Loader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Consumer;

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
