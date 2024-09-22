plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    api(projects.util)
    api(libs.bundles.asm)
    api(libs.bundles.guice)
    api(libs.jackson.databind)
    api(libs.netty.buffer)

    implementation(projects.buffer)
    implementation(projects.cache)
    implementation(projects.compress)
    implementation(projects.crypto)
    implementation(libs.cabParser)
    implementation(libs.pack200)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])

        pom {
            packaging = "jar"
            name.set("OpenRS2 ASM Utilities")
            description.set(
                """
                Common utility code used for manipulating Java bytecode with
                the ASM library.
            """.trimIndent()
            )
        }
    }
}
