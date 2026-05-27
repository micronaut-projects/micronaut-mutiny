plugins {
    id("io.micronaut.build.internal.kotlin-ksp")
}
dependencies {
    testImplementation(projects.mutinyHttpClient)
    kspTest(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)

    kspTest(mn.micronaut.inject.kotlin)

    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testRuntimeOnly(mnTest.junit.platform.launcher)

    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.netty)
}
tasks.withType<Test> {
    useJUnitPlatform()
}
