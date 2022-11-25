import org.objectweb.asm.tree.ClassNode

interface ClassTransformer {
    void transform(ClassNode node)
}