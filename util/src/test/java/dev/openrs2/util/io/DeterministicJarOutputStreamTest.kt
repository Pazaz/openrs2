package dev.openrs2.util.io

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

object DeterministicJarOutputStreamTest {
    private val UNIX_EPOCH = FileTime.fromMillis(0)
    private val Y2K = FileTime.fromMillis(946684800)
    private val manifest = Manifest().apply {
        mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        mainAttributes[Attributes.Name.MAIN_CLASS] = "Hello"
    }

    @Test
    fun testPutNextEntry() {
        ByteArrayOutputStream().use { out ->
            DeterministicJarOutputStream.create(out).use { jar ->
                val entry = JarEntry("Hello.class")
                entry.creationTime = Y2K
                entry.lastAccessTime = Y2K
                entry.lastModifiedTime = Y2K
                jar.putNextEntry(entry)
            }

            JarInputStream(ByteArrayInputStream(out.toByteArray())).use { jar ->
                assertNull(jar.manifest)

                val entry = jar.nextJarEntry
                assertNotNull(entry)

                assertEquals("Hello.class", entry.name)
                assertEquals(0, entry.time)
                assertEquals(UNIX_EPOCH, entry.creationTime)
                assertEquals(UNIX_EPOCH, entry.lastAccessTime)
                assertEquals(UNIX_EPOCH, entry.lastModifiedTime)

                assertNull(jar.nextJarEntry)
            }
        }
    }

    @Test
    fun testManifest() {
        ByteArrayOutputStream().use { out ->
            DeterministicJarOutputStream.create(out, manifest).use { jar ->
                jar.putNextEntry(JarEntry("Hello.class"))
            }

            JarInputStream(ByteArrayInputStream(out.toByteArray())).use { jar ->
                assertEquals(manifest, jar.manifest)
            }
        }
    }

    @Test
    fun testRepack() {
        Jimfs.newFileSystem(Configuration.unix()).use { fs ->
            val originalFile = Files.createTempFile(fs.getPath("/"), "original", ".jar")
            try {
                JarOutputStream(Files.newOutputStream(originalFile)).use { jar ->
                    val entry = JarEntry("Hello.class")
                    entry.creationTime = Y2K
                    entry.lastAccessTime = Y2K
                    entry.lastModifiedTime = Y2K
                    jar.putNextEntry(entry)
                }

                val repackedFile = Files.createTempFile(fs.getPath("/"), "repacked", ".jar")
                try {
                    DeterministicJarOutputStream.repack(originalFile, repackedFile)

                    JarInputStream(Files.newInputStream(repackedFile)).use { jar ->
                        assertNull(jar.manifest)

                        val entry = jar.nextJarEntry
                        assertNotNull(entry)

                        assertEquals("Hello.class", entry.name)
                        assertEquals(0, entry.time)
                        assertEquals(UNIX_EPOCH, entry.creationTime)
                        assertEquals(UNIX_EPOCH, entry.lastAccessTime)
                        assertEquals(UNIX_EPOCH, entry.lastModifiedTime)

                        assertNull(jar.nextJarEntry)
                    }
                } finally {
                    Files.deleteIfExists(repackedFile)
                }
            } finally {
                Files.deleteIfExists(originalFile)
            }
        }
    }
}
