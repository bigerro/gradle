/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.smoketests

import org.apache.commons.io.FileUtils
import org.gradle.integtests.fixtures.RepoScriptBlockUtil
import org.gradle.integtests.fixtures.executer.IntegrationTestBuildContext
import org.gradle.internal.featurelifecycle.LoggingDeprecatedFeatureHandler
import org.gradle.test.fixtures.file.TestFile
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.integtests.fixtures.RepoScriptBlockUtil.createMirrorInitScript
import static org.gradle.integtests.fixtures.RepoScriptBlockUtil.gradlePluginRepositoryMirrorUrl
import static org.gradle.test.fixtures.server.http.MavenHttpPluginRepository.PLUGIN_PORTAL_OVERRIDE_URL_PROPERTY

abstract class AbstractSmokeTest extends Specification {

    static class TestedVersions {
        /**
         * May also need to update
         * @see BuildScanPluginSmokeTest
         */

        // https://plugins.gradle.org/plugin/nebula.dependency-recommender
        static nebulaDependencyRecommender = "8.0.1"

        // https://plugins.gradle.org/plugin/nebula.plugin-plugin
        static nebulaPluginPlugin = "12.4.1"

        // https://plugins.gradle.org/plugin/nebula.lint
        static nebulaLint = "14.2.0"

        // https://plugins.gradle.org/plugin/nebula.dependency-lock
        static nebulaDependencyLock = Versions.of("4.9.5", "5.0.6", "6.0.0", "7.0.1", "7.1.2", "7.3.4", "7.6.7", "8.0.0")

        // https://plugins.gradle.org/plugin/nebula.resolution-rules
        static nebulaResolutionRules = "7.4.1"

        // https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow
        static shadow = Versions.of("4.0.4", "5.1.0")

        // https://github.com/asciidoctor/asciidoctor-gradle-plugin/releases
        static asciidoctor = "2.3.0"

        // https://plugins.gradle.org/plugin/com.github.spotbugs
        static spotbugs = "2.0.0"

        // https://plugins.gradle.org/plugin/com.bmuschko.docker-java-application
        static docker = "5.0.0"

        // https://plugins.gradle.org/plugin/com.bmuschko.tomcat
        static tomcat = "2.5"

        // https://plugins.gradle.org/plugin/io.spring.dependency-management
        static springDependencyManagement = "1.0.8.RELEASE"

        // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-gradle-plugin
        static springBoot = "2.1.8.RELEASE"

        // https://developer.android.com/studio/releases/build-tools
        static androidTools = "29.0.2"
        // https://developer.android.com/studio/releases/gradle-plugin
        static androidGradle = Versions.of("3.4.2", "3.5.1", "3.6.0-beta01")

        // https://search.maven.org/search?q=g:org.jetbrains.kotlin%20AND%20a:kotlin-project&core=gav
        static kotlin = Versions.of('1.3.21', '1.3.31', '1.3.41', '1.3.50')

        // https://plugins.gradle.org/plugin/org.gretty
        static gretty = "2.3.1"

        // https://plugins.gradle.org/plugin/com.eriwen.gradle.js
        static gradleJs = "2.14.1"

        // https://plugins.gradle.org/plugin/com.eriwen.gradle.css
        static gradleCss = "2.14.0"

        // https://plugins.gradle.org/plugin/org.ajoberstar.grgit
        static grgit = "3.1.1"

        // https://plugins.gradle.org/plugin/com.github.ben-manes.versions
        static gradleVersions = "0.25.0"

        // https://plugins.gradle.org/plugin/org.gradle.playframework
        static playframework = "0.9"

        // https://plugins.gradle.org/plugin/net.ltgt.errorprone
        static errorProne = "0.8.1"
    }

    static class Versions implements Iterable<String> {
        static Versions of(String... versions) {
            new Versions(versions)
        }

        final List<String> versions

        String latest() {
            versions.last()
        }

        private Versions(String... given) {
            versions = Arrays.asList(given)
        }

        @Override
        Iterator<String> iterator() {
            return versions.iterator()
        }
    }

    private static final String INIT_SCRIPT_LOCATION = "org.gradle.smoketests.init.script"

    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    File settingsFile

    def setup() {
        buildFile = new File(testProjectDir.root, defaultBuildFileName)
        settingsFile = new File(testProjectDir.root, "settings.gradle")
    }

    protected String getDefaultBuildFileName() {
        'build.gradle'
    }

    void withKotlinBuildFile() {
        buildFile = new File(testProjectDir.root, "${getDefaultBuildFileName()}.kts")
    }

    TestFile file(String filename) {
        def file = new TestFile(testProjectDir.root, filename)
        def parentDir = file.getParentFile()
        assert parentDir.isDirectory() || parentDir.mkdirs()

        file
    }

    GradleRunner runner(String... tasks) {
        DefaultGradleRunner gradleRunner = GradleRunner.create()
            .withGradleInstallation(IntegrationTestBuildContext.INSTANCE.gradleHomeDir)
            .withTestKitDir(IntegrationTestBuildContext.INSTANCE.gradleUserHomeDir)
            .withProjectDir(testProjectDir.root)
            .forwardOutput()
            .withArguments(tasks.toList() + outputParameters() + repoMirrorParameters()) as DefaultGradleRunner
        gradleRunner.withJvmArguments("-Xmx8g", "-XX:MaxMetaspaceSize=1024m", "-XX:+HeapDumpOnOutOfMemoryError")
    }

    private static List<String> outputParameters() {
        return [
            '--stacktrace',
            '--warning-mode=all',
            "-D${LoggingDeprecatedFeatureHandler.ORG_GRADLE_DEPRECATION_TRACE_PROPERTY_NAME}=false" as String,
        ]
    }

    private static List<String> repoMirrorParameters() {
        String mirrorInitScriptPath = createMirrorInitScript().absolutePath
        return [
            '--init-script', mirrorInitScriptPath,
            "-D${PLUGIN_PORTAL_OVERRIDE_URL_PROPERTY}=${gradlePluginRepositoryMirrorUrl()}" as String,
            "-D${INIT_SCRIPT_LOCATION}=${mirrorInitScriptPath}" as String,
        ]
    }

    protected void useSample(String sampleDirectory) {
        def smokeTestDirectory = new File(this.getClass().getResource(sampleDirectory).toURI())
        FileUtils.copyDirectory(smokeTestDirectory, testProjectDir.root)
    }

    protected void replaceVariablesInBuildFile(Map binding) {
        String text = buildFile.text
        binding.each { String var, String value ->
            text = text.replaceAll("\\\$${var}".toString(), value)
        }
        buildFile.text = text
    }

    protected static String jcenterRepository() {
        RepoScriptBlockUtil.jcenterRepository()
    }

    protected static String mavenCentralRepository() {
        RepoScriptBlockUtil.mavenCentralRepository()
    }

    protected static String googleRepository() {
        RepoScriptBlockUtil.googleRepository()
    }

    protected static void expectNoDeprecationWarnings(BuildResult result) {
        verifyDeprecationWarnings(result, [])
    }

    protected static void expectDeprecationWarnings(BuildResult result, String... warnings) {
        if (warnings.length == 0) {
            throw new IllegalArgumentException("Use expectNoDeprecationWarnings() when no deprecation warnings are to be expected")
        }
        verifyDeprecationWarnings(result, warnings as List)
    }

    private static void verifyDeprecationWarnings(BuildResult result, List<String> remainingWarnings) {
        def lines = result.output.readLines()
        lines.eachWithIndex { String line, int lineIndex ->
            if (remainingWarnings.remove(line)) {
                return
            }
            assert !line.contains("deprecated"), "Found an unexpected deprecation warning on line ${lineIndex + 1}: $line"
        }
        assert remainingWarnings.empty, "Expected ${remainingWarnings.size()} deprecation warnings:\n${remainingWarnings.collect { " - $it" }.join("\n")}"
    }
}
