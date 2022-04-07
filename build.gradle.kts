plugins {
    kotlin("jvm") version "1.6.10"
    java
    `maven-publish`
}

repositories {
    mavenCentral()
    google()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.sander"
            artifactId = "modify-string-res-key-plugin"
            version = "31-SNAPSHOT"
            from(components["java"])
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    implementation(localGroovy())
    implementation("org.ow2.asm:asm:9.2")
    implementation("org.ow2.asm:asm-tree:9.2")
    implementation("com.android.tools.build:gradle:4.1.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}