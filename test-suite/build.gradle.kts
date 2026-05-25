plugins {
    id("io.micronaut.build.internal.java-base")
}
dependencies {
    testImplementation(projects.mutinyHttpClient)
    testAnnotationProcessor(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)

    testAnnotationProcessor(mn.micronaut.inject.java)
    testImplementation(mnTest.micronaut.test.junit5)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testRuntimeOnly(mnTest.junit.platform.launcher)

    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.netty)
}
tasks.withType<Test> {
    useJUnitPlatform()
}
