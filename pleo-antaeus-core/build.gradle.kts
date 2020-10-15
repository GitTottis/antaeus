plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    api(project(":pleo-antaeus-models"))
}