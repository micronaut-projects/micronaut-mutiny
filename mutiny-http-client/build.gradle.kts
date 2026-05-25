import io.micronaut.build.TestFramework

plugins {
    id("io.micronaut.build.internal.mutiny-module")
}

dependencies {
    api(projects.mutiny)
    compileOnly(mn.micronaut.websocket)
    api(mn.micronaut.http.client.core)
    implementation(libs.managed.mutiny.zero.flow.adapters)

    testImplementation(mn.micronaut.http.client)
    testAnnotationProcessor(mn.micronaut.inject.java)
    testImplementation(mn.micronaut.http.server.netty)
    testRuntimeOnly(mn.micronaut.jackson.databind)
}

micronautBuild {
    binaryCompatibility.enabledAfter("1.0.0")
    testFramework = TestFramework.JUNIT6
}
