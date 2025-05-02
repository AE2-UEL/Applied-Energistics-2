package appeng.core.transformer;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PickBlockPatch extends ClassVisitor {

    public PickBlockPatch(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("onPickBlock".equals(name)) {
            return new OnPickBlockVisitor(mv);
        }
        return mv;
    }

    private static class OnPickBlockVisitor extends MethodVisitor implements Opcodes {

        public OnPickBlockVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitCode() {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    "appeng/core/transformer/AE2ELHooks",
                    "testColorApplicatorPickBlock",
                    "(Lnet/minecraft/util/math/RayTraceResult;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;)Z",
                    false);
            mv.visitInsn(DUP);
            Label exitLabel = new Label();
            mv.visitJumpInsn(IFEQ, exitLabel);

            mv.visitInsn(IRETURN);
            mv.visitLabel(exitLabel);
        }
    }
}
