package dev.openrs2.deob

import com.github.michaelbull.logging.InlineLogger
import com.google.common.collect.ImmutableList
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.classpath.Library.Companion.readJar
import dev.openrs2.asm.classpath.Library.Companion.readPack
import dev.openrs2.bundler.Bundler
import dev.openrs2.deob.SignedClassUtils.move
import dev.openrs2.deob.remap.PrefixRemapper.create
import dev.openrs2.deob.transform.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class Deobfuscator(private val input: Path, private val output: Path) {
    fun run() {
        // read input jars/packs
        logger.info { "Reading input jars" }
        val unpacker = readJar(input.resolve("game_unpacker.dat"))
        val glUnpacker = Library(unpacker)
        val loader = readJar(input.resolve("loader.jar"))
        val glLoader = readJar(input.resolve("loader_gl.jar"))
        val gl = readPack(input.resolve("jaggl.pack200"))
        val client = readJar(input.resolve("runescape.jar"))
        val glClient = readPack(input.resolve("runescape_gl.pack200"))

        // TODO(gpe): it'd be nice to have separate signlink.jar and
        // signlink-unsigned.jar files so we don't (effectively) deobfuscate
        // runescape.jar twice with different sets of names, but thinking about
        // how this would work is tricky (as the naming must match)
        val unsignedClient = Library(client)

        // overwrite client's classes with signed classes from the loader
        logger.info { "Moving signed classes from loader" }
        val signLink = Library()
        move(loader, client, signLink)

        logger.info { "Moving signed classes from loader_gl" }
        val glSignLink = Library()
        move(glLoader, glClient, glSignLink)

        // move unpack class out of the loader (so the unpacker and loader can both depend on it)
        logger.info { "Moving unpack from loader to unpack" }
        val unpack = Library()
        unpack.add(loader.remove("unpack")!!)

        logger.info { "Moving unpack from loader_gl to unpack_gl" }
        val glUnpack = Library()
        glUnpack.add(glLoader.remove("unpack")!!)

        // move DRI classes out of jaggl (so we can place javah-generated headers in a separate directory)
        logger.info { "Moving DRI classes from jaggl to jaggl_dri" }
        val glDri = Library()
        glDri.add(gl.remove("com/sun/opengl/impl/x11/DRIHack")!!)
        glDri.add(gl.remove("com/sun/opengl/impl/x11/DRIHack$1")!!)
        glDri.add(gl.remove("jaggl/X11/dri")!!)

        // prefix remaining loader/unpacker classes (to avoid conflicts when we rename in the same classpath as the client)
        logger.info { "Prefixing loader and unpacker class names" }
        loader.remap(create(loader, "loader_"))
        glLoader.remap(create(glLoader, "loader_"))
        unpacker.remap(create(unpacker, "unpacker_"))
        glUnpacker.remap(create(glUnpacker, "unpacker_"))

        // bundle libraries together into a common classpath
        val runtime = ClassLoader.getPlatformClassLoader()
        val classPath = ClassPath(
            runtime,
            ImmutableList.of(),
            ImmutableList.of(client, loader, signLink, unpack, unpacker)
        )
        val glClassPath = ClassPath(
            runtime,
            ImmutableList.of(gl, glDri),
            ImmutableList.of(glClient, glLoader, glSignLink, glUnpack, glUnpacker)
        )
        val unsignedClassPath = ClassPath(
            runtime,
            ImmutableList.of(),
            ImmutableList.of(unsignedClient)
        )

        // deobfuscate
        logger.info { "Transforming client" }
        for (transformer in TRANSFORMERS) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName} " }
            transformer.transform(classPath)
        }

        logger.info { "Transforming client_gl" }
        for (transformer in TRANSFORMERS) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName} " }
            transformer.transform(glClassPath)
        }

        logger.info { "Transforming client_unsigned" }
        for (transformer in TRANSFORMERS) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName} " }
            transformer.transform(unsignedClassPath)
        }

        // write output jars
        logger.info { "Writing output jars" }

        Files.createDirectories(output)

        client.writeJar(output.resolve("runescape.jar"))
        loader.writeJar(output.resolve("loader.jar"))
        signLink.writeJar(output.resolve("signlink.jar"))
        unpack.writeJar(output.resolve("unpack.jar"))
        unpacker.writeJar(output.resolve("unpacker.jar"))

        gl.writeJar(output.resolve("jaggl.jar"))
        glDri.writeJar(output.resolve("jaggl_dri.jar"))
        glClient.writeJar(output.resolve("runescape_gl.jar"))
        glLoader.writeJar(output.resolve("loader_gl.jar"))
        glSignLink.writeJar(output.resolve("signlink_gl.jar"))
        glUnpack.writeJar(output.resolve("unpack_gl.jar"))
        glUnpacker.writeJar(output.resolve("unpacker_gl.jar"))

        unsignedClient.writeJar(output.resolve("runescape_unsigned.jar"))
    }

    companion object {
        private val logger = InlineLogger()
        private val TRANSFORMERS = listOf(
            OriginalNameTransformer(),
            *Bundler.TRANSFORMERS.toTypedArray(),
            OpaquePredicateTransformer(),
            ExceptionTracingTransformer(),
            BitShiftTransformer(),
            CounterTransformer(),
            CanvasTransformer(),
            FieldOrderTransformer(),
            BitwiseOpTransformer(),
            RemapTransformer(),
            DummyArgTransformer(),
            DummyLocalTransformer(),
            UnusedArgTransformer(),
            ResetTransformer(),
            AccessTransformer()
        )

        @JvmStatic
        fun main(args: Array<String>) {
            val deobfuscator = Deobfuscator(Paths.get("nonfree/code"), Paths.get("nonfree/code/deob"))
            deobfuscator.run()
        }
    }
}