plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    application
}

dependencies {
    implementation(project(":utils"))
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.kotlinxEcosystem)
}

application {
    mainClass = "com.wingedsheep.app.AppKt"
}
