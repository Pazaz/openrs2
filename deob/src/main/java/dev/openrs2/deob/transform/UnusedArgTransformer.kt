package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.hasCode
import dev.openrs2.asm.removeArgument
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.common.collect.DisjointSet
import dev.openrs2.deob.ArgRef
import dev.openrs2.deob.analysis.ConstSourceInterpreter
import dev.openrs2.deob.remap.TypedRemapper
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.AnalyzerException

class UnusedArgTransformer : Transformer() {
    private val retainedArgs = mutableSetOf<ArgRef>()
    private lateinit var inheritedMethodSets: DisjointSet<MemberRef>
    private var deletedArgs = 0

    @Throws(AnalyzerException::class)
    override fun preTransform(classPath: ClassPath) {
        retainedArgs.clear()
        inheritedMethodSets = classPath.createInheritedMethodSets()
        deletedArgs = 0

        for (library in classPath.libraries) {
            for (clazz in library) {
                for (method in clazz.methods) {
                    if (method.hasCode()) {
                        populateRetainedArgs(classPath, clazz, method)
                    }
                }
            }
        }
    }

    @Throws(AnalyzerException::class)
    private fun populateRetainedArgs(classPath: ClassPath, clazz: ClassNode, method: MethodNode) {
        val partition = inheritedMethodSets[MemberRef(clazz, method)]!!
        val localToArgMap = createLocalToArgMap(method)

        val analyzer = Analyzer(ConstSourceInterpreter())
        val frames = analyzer.analyze(clazz.name, method)

        frame@ for ((i, frame) in frames.withIndex()) {
            if (frame == null) {
                continue
            }

            val stackSize = frame.stackSize

            val insn = method.instructions[i]
            when (insn) {
                is VarInsnNode -> {
                    if (insn.opcode != Opcodes.ILOAD) {
                        continue@frame
                    }

                    val arg = localToArgMap[insn.`var`]
                    if (arg != null) {
                        retainedArgs.add(ArgRef(partition, arg))
                    }
                }
                is MethodInsnNode -> {
                    val invokePartition = inheritedMethodSets[MemberRef(insn)]
                    if (invokePartition == null || TypedRemapper.isMethodImmutable(classPath, invokePartition)) {
                        continue@frame
                    }

                    val args = Type.getArgumentTypes(insn.desc).size
                    for (j in 0 until args) {
                        val source = frame.getStack(stackSize - args + j)
                        if (!source.isSingleSourceConstant) {
                            retainedArgs.add(ArgRef(invokePartition, j))
                        }
                    }
                }
            }
        }
    }

    @Throws(AnalyzerException::class)
    override fun preTransformMethod(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        // delete unused int args from call sites
        if (!method.hasCode()) {
            return false
        }

        val analyzer = Analyzer(ConstSourceInterpreter())
        val frames = analyzer.analyze(clazz.name, method)
        val deadInsns = mutableListOf<AbstractInsnNode>()

        for ((i, frame) in frames.withIndex()) {
            if (frame == null) {
                continue
            }

            val stackSize = frame.stackSize

            val insn = method.instructions[i]
            if (insn !is MethodInsnNode) {
                continue
            }

            val partition = inheritedMethodSets[MemberRef(insn)]
            if (partition == null || TypedRemapper.isMethodImmutable(classPath, partition)) {
                continue
            }

            val type = Type.getType(insn.desc)
            val argTypes = type.argumentTypes
            val newArgTypes = mutableListOf<Type>()

            for ((j, argType) in argTypes.withIndex()) {
                if (INT_SORTS.contains(argType.sort) && !retainedArgs.contains(ArgRef(partition, j))) {
                    val source = frame.getStack(stackSize - argTypes.size + j).source
                    deadInsns.add(source)
                } else {
                    newArgTypes.add(argType)
                }
            }

            insn.desc = Type.getMethodDescriptor(type.returnType, *newArgTypes.toTypedArray())
        }

        deadInsns.forEach(method.instructions::remove)

        return false
    }

    override fun postTransformMethod(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        // delete unused int args from the method itself
        val partition = inheritedMethodSets[MemberRef(clazz, method)]!!
        if (TypedRemapper.isMethodImmutable(classPath, partition)) {
            return false
        }

        val argTypes = Type.getType(method.desc).argumentTypes
        for ((i, argType) in argTypes.withIndex().reversed()) {
            if (INT_SORTS.contains(argType.sort) && !retainedArgs.contains(ArgRef(partition, i))) {
                method.removeArgument(i)
                deletedArgs++
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Removed $deletedArgs dummy arguments" }
    }

    companion object {
        private val logger = InlineLogger()
        private val INT_SORTS = setOf(
            Type.BOOLEAN,
            Type.BYTE,
            Type.SHORT,
            Type.INT,
            Type.CHAR
        )

        private fun createLocalToArgMap(method: MethodNode): Map<Int, Int> {
            val argTypes = Type.getType(method.desc).argumentTypes

            val map = mutableMapOf<Int, Int>()
            var localIndex = 0

            if (method.access and Opcodes.ACC_STATIC == 0) {
                localIndex++
            }

            for ((argIndex, t) in argTypes.withIndex()) {
                map[localIndex] = argIndex
                localIndex += t.size
            }

            return map
        }
    }
}