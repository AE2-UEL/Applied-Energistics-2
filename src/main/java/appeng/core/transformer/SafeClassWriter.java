package appeng.core.transformer;

import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Safe class writer.
 * The way COMPUTE_FRAMES works may require loading additional classes. This can cause ClassCircularityErrors.
 * The override for getCommonSuperClass will ensure that COMPUTE_FRAMES works properly by using the right ClassLoader.
 * <p>
 * Code from: https://github.com/phantamanta44/tinkers-evolution/blob/7ce95f70e5b342968d96e4a7aee694c48bddb359/src/main/java/xyz/phanta/tconevo/coremod/util/SafeClassWriter.java
 */
public class SafeClassWriter extends ClassWriter {

    private final ClassLoader resourceLoader;

    public SafeClassWriter(ClassReader reader, int flags, ClassLoader resourceLoader) {
        super(reader, flags);
        this.resourceLoader = resourceLoader;
    }

    // this is the main offender: it usually loads both classes and uses java.lang.Class to compute their meet
    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        if (type1.equals(type2)) {
            return type1;
        }
        try {
            return meet(resolve(type1, resourceLoader), resolve(type2, resourceLoader));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Map<String, ClassTreeNode> classTreeCache = new HashMap<>();

    private static ClassTreeNode resolve(String className, ClassLoader loader) throws ClassNotFoundException {
        ClassTreeNode node = classTreeCache.get(className);
        if (node != null) {
            return node;
        }

        // java standard library classes are safe to load, so we just use java.lang.Class to compute their hierarchies
        // the prefix "java" covers both java and javax; we just assume that no other package name starts with "java"
        if (className.startsWith("java")) {
            node = fromLoadedClass(Class.forName(className.replace('/', '.')));
            classTreeCache.put(className, node);
            return node;
        }

        URL classResource = loader.getResource(className + ".class");
        if (classResource == null) {
            String altName = FMLDeobfuscatingRemapper.INSTANCE.unmap(className);
            if (altName.equals(className)) {
                throw new ClassNotFoundException("Could not find class binary: " + className);
            }
            classResource = loader.getResource(altName + ".class");
            if (classResource == null) {
                throw new ClassNotFoundException("Could not find class binary: " + className + " (" + altName + ")");
            }
        }

        ClassTreeNodeFactory factory = new ClassTreeNodeFactory(className, loader);
        try (InputStream classStream = classResource.openStream()) {
            new ClassReader(classStream)
                    .accept(factory, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (Exception e) {
            throw new ClassNotFoundException("Failed to read class binary: " + className, e);
        }
        node = factory.getTreeNode();
        classTreeCache.put(className, node);
        return node;
    }

    private static ClassTreeNode fromLoadedClass(Class<?> clazz) {
        Class<?> superClass = clazz.getSuperclass();
        return new ClassTreeNode(
                clazz.getName().replace('.', '/'), superClass == null ? null : () -> fromLoadedClass(superClass));
    }

    private static String meet(ClassTreeNode a, ClassTreeNode b) throws ClassNotFoundException {
        // resolving classes is expensive, so we want to minimize the number of getSuper calls so as to resolve as few
        // classes as possible. the meet is most likely going to be either a superclass near both a and b or just some
        // java type like Object or Comparable. in the former case, it's best to walk the parents of both a and b
        // simultaneously, since they should reach the meet in roughly the same number of steps. in the latter case, it
        // it doesn't matter what we do, since we'll have to resolve pretty much the entire hierarchy for both a and b
        // anyways. this algorithm is worst in the case where one class is much further from the meet than the other,
        // but since we have no way of knowing which class that is, we can't really do better anyways
        Set<String> aParents = new HashSet<>(), bParents = new HashSet<>();
        while (true) {
            boolean progress = false;
            if (a != null) {
                if (bParents.contains(a.className)) {
                    return a.className;
                }
                aParents.add(a.className);
                a = a.getSuper();
                progress = true;
            }
            if (b != null) {
                if (aParents.contains(b.className)) {
                    return b.className;
                }
                bParents.add(b.className);
                b = b.getSuper();
                progress = true;
            }
            if (!progress) { // this almost certainly means one of the classes is a java standard library interface
                return "java/lang/Object";
            }
        }
    }

    @FunctionalInterface
    private interface ClassResolver {

        ClassTreeNode resolve() throws ClassNotFoundException;

    }

    private static class ClassTreeNode {

        final String className;
        @Nullable
        private final ClassResolver superResolver;
        @Nullable
        private ClassTreeNode superCache;

        ClassTreeNode(String className, @Nullable ClassResolver superResolver) {
            this.className = className;
            this.superResolver = superResolver;
        }

        @Nullable
        ClassTreeNode getSuper() throws ClassNotFoundException {
            if (superResolver == null) {
                return null;
            }
            if (superCache == null) {
                superCache = superResolver.resolve();
            }
            return superCache;
        }

    }

    private static class ClassTreeNodeFactory extends ClassVisitor {

        private final String className;
        private final ClassLoader loader;
        @Nullable
        private ClassTreeNode result;

        ClassTreeNodeFactory(String className, ClassLoader loader) {
            super(Opcodes.ASM5);
            this.className = className;
            this.loader = loader;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            String deobfSuperName = FMLDeobfuscatingRemapper.INSTANCE.map(superName);
            result = new ClassTreeNode(className, () -> resolve(deobfSuperName, loader));
        }

        ClassTreeNode getTreeNode() {
            if (result == null) {
                throw new IllegalStateException("Did not receive class header data!");
            }
            return result;
        }

    }

}
