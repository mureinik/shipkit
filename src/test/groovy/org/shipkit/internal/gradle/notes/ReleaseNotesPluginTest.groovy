package org.shipkit.internal.gradle.notes

import org.shipkit.gradle.git.GitCommitTask
import org.shipkit.internal.gradle.git.GitPlugin
import testutil.PluginSpecification

class ReleaseNotesPluginTest extends PluginSpecification {

    def "applies cleanly"() {
        expect:
        project.plugins.apply("org.shipkit.release-notes")
    }

    def "adds updates release notes to GitCommitTask if GitPlugin applied"() {
        given:
        project.plugins.apply(GitPlugin)

        when:
        project.plugins.apply("org.shipkit.release-notes")

        then:
        GitCommitTask gitCommitTask = project.tasks.getByName(GitPlugin.GIT_COMMIT_TASK)
        gitCommitTask.filesToCommit.contains(project.file("docs/release-notes.md"))
        gitCommitTask.descriptions.contains("release notes updated")
    }

}
