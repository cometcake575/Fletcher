plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
}

group = "com.starshooterstudios.fletcher"
version = "1.0.12"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:23.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()
}