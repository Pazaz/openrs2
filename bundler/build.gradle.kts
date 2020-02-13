plugins {
    `maven-publish`
    application
    kotlin("jvm")
}

application {
    mainClassName = "dev.openrs2.bundler.BundlerKt"
}

dependencies {
    api(project(":asm"))

    implementation("dev.openrs2:openrs2-natives-all:${Versions.openrs2Natives}")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 Bundler")
            description.set("""
                A tool for patching the RuneScape client to allow it to connect
                to an OpenRS2 server and improve compatibility with modern JVMs.
            """.trimIndent())
        }
    }
}