import buildlogic.Utils

plugins {
    id("build.fabric")
    id("build.publish")
}

Utils.setupResources(project, rootProject, "fabric.mod.json")

dependencies {

    minecraft("com.mojang:minecraft:${project.properties["minecraft-version"]}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.properties["fabric-loader-version"]}")

    compileOnly(libs.jetbrains.annotations)

    modImplementation("me.lucko:fabric-permissions-api:0.5.0")

    modImplementation("org.wallentines:databridge:0.9.0")
    modImplementation("org.wallentines:pseudonym-minecraft:0.4.2")
}
