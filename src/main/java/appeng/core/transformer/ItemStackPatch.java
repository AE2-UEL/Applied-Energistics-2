package appeng.core.transformer;

import appeng.core.AE2ELCore;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Patches {@link ItemStack#ItemStack(NBTTagCompound)} and {@link ItemStack#writeToNBT} to use int counts instead of
 * byte counts.
 */
public final class ItemStackPatch extends ClassVisitor {
    public static final String ITEM_STACK_CLASS = "net.minecraft.item.ItemStack";
    private static final String COUNT_LDC = "Count";
    private static final String WRITE_TO_NBT_METHOD = AE2ELCore.isDeobf ? "writeToNBT" : "func_77955_b";
    private static final String NBT_GET_BYTE_METHOD = AE2ELCore.isDeobf ? "getByte" : "func_74771_c";
    private static final String NBT_SET_BYTE_METHOD = AE2ELCore.isDeobf ? "setByte" : "func_74774_a";
    private static final String NBT_GET_INTEGER_METHOD = AE2ELCore.isDeobf ? "getInteger" : "func_74762_e";
    private static final String NBT_SET_INTEGER_METHOD = AE2ELCore.isDeobf ? "setInteger" : "func_74768_a";

    public ItemStackPatch(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("<init>") && desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;)V")) {
            return new InitFromNBTItemStackVisitor(mv);
        } else if (name.equals(WRITE_TO_NBT_METHOD)) {
            return new WriteNBTItemStackVisitor(mv);
        }
        return mv;
    }

    private static class InitFromNBTItemStackVisitor extends MethodVisitor {
        private boolean isCount = false;

        public InitFromNBTItemStackVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            if (!isCount && cst instanceof String word
                    && word.equals(COUNT_LDC)) {
                isCount = true;
            }
            super.visitLdcInsn(cst);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (isCount && opcode == Opcodes.INVOKEVIRTUAL && name.equals(NBT_GET_BYTE_METHOD)) {
                name = NBT_GET_INTEGER_METHOD;
                desc = desc.replace('B', 'I');
                isCount = false;
                AE2ELCore.LOGGER.info("Patched ItemStack Count getter!");
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    private static class WriteNBTItemStackVisitor extends MethodVisitor {
        private boolean isCount = false;
        private boolean hasCast = false;

        public WriteNBTItemStackVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            if (!isCount && cst instanceof String word
                    && word.equals(COUNT_LDC)) {
                isCount = true;
            }
            super.visitLdcInsn(cst);
        }


        @Override
        public void visitInsn(int opcode) {
            if (isCount && !hasCast && opcode == Opcodes.I2B) {
                hasCast = true;
                // Skip cast
                return;
            }
            super.visitInsn(opcode);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (isCount && hasCast && opcode == Opcodes.INVOKEVIRTUAL && name.equals(NBT_SET_BYTE_METHOD)) {
                name = NBT_SET_INTEGER_METHOD;
                desc = desc.replace('B', 'I');
                isCount = false;
                AE2ELCore.LOGGER.info("Patched ItemStack Count setter!");
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
