plugins {
    id("boudicca-base")
}

dependencies {
    implementation("org.json:json:20231013")
    implementation(project(":boudicca.base:enricher-client"))
    implementation(project(":boudicca.base:publisher-client"))
}
