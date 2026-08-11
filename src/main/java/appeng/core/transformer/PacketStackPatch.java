package appeng.core.transformer;

import appeng.core.AE2ELCore;
import net.minecraftforge.common.util.PacketUtil;
import net.minecraft.network.PacketBuffer;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Patches {@link PacketBuffer#writeItemStack}, {@link PacketBuffer#readItemStack}, and
 * {@link PacketUtil#writeItemStackFromClientToServer} to use VarInt counts instead of byte counts.
 */
public class PacketStackPatch extends ClassVisitor {
    public static final String PACKET_BUFFER_CLASS = "net.minecraft.network.PacketBuffer";
    public static final String PACKET_UTIL_CLASS = "net.minecraftforge.common.util.PacketUtil";
    private static final String WRITE_ITEMSTACK_METHOD = AE2ELCore.isDeobf ? "writeItemStack" : "func_150788_a";
    private static final String READ_ITEMSTACK_METHOD = AE2ELCore.isDeobf ? "readItemStack" : "func_150791_c";
    private static final String WRITE_ITEMSTACK_FROM_CLIENT_TO_SERVER_METHOD = "writeItemStackFromClientToServer";
    private static final String WRITE_VAR_INT_METHOD = AE2ELCore.isDeobf ? "writeVarInt" : "func_150787_b";
    private static final String READ_VAR_INT_METHOD = AE2ELCore.isDeobf ? "readVarInt" : "func_150792_a";

    private final String className;

    public PacketStackPatch(ClassVisitor cv, String className) {
        super(Opcodes.ASM5, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (WRITE_ITEMSTACK_METHOD.equals(name) ||
                READ_ITEMSTACK_METHOD.equals(name) ||
                WRITE_ITEMSTACK_FROM_CLIENT_TO_SERVER_METHOD.equals(name)) {
            return new ReadWriteItemStackVisitor(mv, className);
        }
        return mv;
    }

    private static class ReadWriteItemStackVisitor extends MethodVisitor {
        private final String className;

        public ReadWriteItemStackVisitor(MethodVisitor mv, String className) {
            super(Opcodes.ASM5, mv);
            this.className = className;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if ("writeByte".equals(name)) {
                name = WRITE_VAR_INT_METHOD;
                desc = "(I)Lnet/minecraft/network/PacketBuffer;";
                AE2ELCore.LOGGER.info("Patched ItemStack Count writer in {}!", className);
            } else if ("readByte".equals(name)) {
                name = READ_VAR_INT_METHOD;
                desc = "()I";
                AE2ELCore.LOGGER.info("Patched ItemStack Count reader in {}!", className);
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
