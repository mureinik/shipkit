package org.shipkit.internal.gradle.release;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.shipkit.gradle.exec.ShipkitExecTask;
import org.shipkit.gradle.exec.ExecCommand;
import org.shipkit.internal.gradle.GitPlugin;
import org.shipkit.internal.gradle.ReleaseNeededPlugin;
import org.shipkit.internal.gradle.ReleaseNotesPlugin;
import org.shipkit.internal.gradle.VersioningPlugin;
import org.shipkit.internal.gradle.util.TaskMaker;

import static java.util.Arrays.asList;
import static org.shipkit.internal.gradle.ReleaseNeededPlugin.RELEASE_NEEDED;
import static org.shipkit.internal.gradle.ReleaseNotesPlugin.UPDATE_NOTES_TASK;

/**
 * Applies plugins:
 * <ul>
 *     <li>{@link ReleaseNotesPlugin}</li>
 *     <li>{@link VersioningPlugin}</li>
 *     <li>{@link GitPlugin}</li>
 * </ul>
 *
 * Adds tasks:
 * <ul>
 *     <li>performRelease</li>
 *     <li>testRelease</li>
 *     <li>releaseCleanUp</li>
 * </ul>
 */
public class ReleasePlugin implements Plugin<Project> {

    public static final String PERFORM_RELEASE_TASK = "performRelease";
    public static final String TEST_RELEASE_TASK = "testRelease";
    public static final String RELEASE_CLEAN_UP_TASK = "releaseCleanUp";

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(ReleaseNotesPlugin.class);
        project.getPlugins().apply(VersioningPlugin.class);
        project.getPlugins().apply(GitPlugin.class);
        project.getPlugins().apply(ReleaseNeededPlugin.class);

        TaskMaker.task(project, PERFORM_RELEASE_TASK, new Action<Task>() {
            public void execute(final Task t) {
                t.setDescription("Performs release. " +
                        "For testing, use: './gradlew testRelease'");

                t.dependsOn(VersioningPlugin.BUMP_VERSION_FILE_TASK, UPDATE_NOTES_TASK);
                t.dependsOn(GitPlugin.PERFORM_GIT_PUSH_TASK);
            }
        });

        TaskMaker.task(project, TEST_RELEASE_TASK, ShipkitExecTask.class, new Action<ShipkitExecTask>() {
            //TODO rename ShipkitExecTask because it can have one action
            public void execute(ShipkitExecTask task) {
                task.setDescription("Tests the release procedure and cleans up. Safe to be invoked multiple times.");
                //releaseCleanUp is already set up to run all his "subtasks" after performRelease is performed
                //releaseNeeded is used here only to execute the code paths in the release needed task (extra testing)
                task.getExecCommands().add(new ExecCommand(asList(
                    "./gradlew", RELEASE_NEEDED, PERFORM_RELEASE_TASK, RELEASE_CLEAN_UP_TASK, "-Pshipkit.dryRun")));
            }
        });

        TaskMaker.task(project, RELEASE_CLEAN_UP_TASK, new Action<Task>() {
            public void execute(final Task t) {
                t.setDescription("Cleans up the working copy, useful after dry running the release");

                //using finalizedBy so that all clean up tasks run, even if one of them fails
                t.finalizedBy(GitPlugin.PERFORM_GIT_COMMIT_CLEANUP_TASK);
                t.finalizedBy(GitPlugin.TAG_CLEANUP_TASK);
            }
        });
    }
}
