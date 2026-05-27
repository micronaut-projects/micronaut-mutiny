plugins {
    `groovy`
    id("io.micronaut.build.internal.java-base")
}
dependencies {
    testImplementation(projects.mutinyHttpClient)
    testCompileOnly(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)

    testCompileOnly(mn.micronaut.inject.groovy)
    testImplementation(mnTest.micronaut.test.spock)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    testRuntimeOnly(mnTest.junit.platform.launcher)

    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.netty)
}
tasks.withType<Test> {
    useJUnitPlatform()
}
