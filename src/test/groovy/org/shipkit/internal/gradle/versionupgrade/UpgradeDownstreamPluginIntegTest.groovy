package org.shipkit.internal.gradle.versionupgrade

import testutil.GradleSpecification

class UpgradeDownstreamPluginIntegTest extends GradleSpecification {

    def "all tasks in dry run"() {
        projectDir.newFolder("gradle")
        projectDir.newFile("gradle/shipkit.gradle") << """
            shipkit {
                gitHub.url = "http://github.com"
            }
        """

        buildFile << """
            apply plugin: "org.shipkit.upgrade-downstream"
            
            upgradeDownstream{
                repositories = ['wwilk/mockito']
            }
        """

        projectDir.newFile("version.properties") << "version=1.0.0"

        expect:
        def result = pass("upgradeDownstream", "-m", "-s")
        result.tasks.join("\n") == """:cloneWwilkMockito=SKIPPED
:upgradeWwilkMockito=SKIPPED
:upgradeDownstream=SKIPPED"""
    }
}
