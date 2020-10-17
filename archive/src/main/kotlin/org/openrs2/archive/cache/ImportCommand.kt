package org.openrs2.archive.cache

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule
import org.openrs2.cache.Store

public class ImportCommand : CliktCommand(name = "import") {
    private val input by argument().path(
        mustExist = true,
        canBeFile = false,
        mustBeReadable = true
    )

    override fun run(): Unit = runBlocking {
        val injector = Guice.createInjector(ArchiveModule)
        val importer = injector.getInstance(CacheImporter::class.java)

        Store.open(input).use { store ->
            importer.import(store)
        }
    }
}