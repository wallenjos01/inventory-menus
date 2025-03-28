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

    modImplementation("org.wallentines:databridge:0.6.0")
    modImplementation("org.wallentines:pseudonym-minecraft:0.2.0")
}