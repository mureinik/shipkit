package org.mockito.release.internal.gradle;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.mockito.release.gradle.IncrementalReleaseNotes;
import org.mockito.release.gradle.ReleaseNotesPlugin;
import org.mockito.release.gradle.ReleaseToolsProperties;
import org.mockito.release.internal.gradle.util.ExtContainer;
import org.mockito.release.internal.gradle.util.LazyConfigurer;
import org.mockito.release.internal.gradle.util.TaskMaker;

import java.io.File;

import static java.util.Arrays.asList;

/**
 * --------------------------
 * ******* IMPORTANT ********
 * --------------------------
 *
 * Please update the documentation in the {@link ReleaseNotesPlugin} interface
 * when you make changes to this implementation
 * (for example: adding new tasks, renaming existing tasks, etc.).
 */
public class DefaultReleaseNotesPlugin implements ReleaseNotesPlugin {

    private static final String TEMP_SERIALIZED_NOTES_FILE = "/notableReleaseNotes.ser";

    public void apply(final Project project) {
        TaskMaker.task(project, "updateReleaseNotes", IncrementalReleaseNotes.UpdateTask.class, new Action<IncrementalReleaseNotes.UpdateTask>() {
            public void execute(final IncrementalReleaseNotes.UpdateTask t) {
                t.setDescription("Updates release notes file.");
                preconfigureIncrementalNotes(t, project);
            }
        });

        TaskMaker.task(project, "previewReleaseNotes", IncrementalReleaseNotes.PreviewTask.class, new Action<IncrementalReleaseNotes.PreviewTask>() {
            public void execute(final IncrementalReleaseNotes.PreviewTask t) {
                t.setDescription("Shows new incremental content of release notes. Useful for previewing the release notes.");
                preconfigureIncrementalNotes(t, project);
            }
        });

        project.getTasks().create("fetchNotableReleaseNotes", NotableReleaseNotesFetcherTask.class, new Action<NotableReleaseNotesFetcherTask>() {
            public void execute(NotableReleaseNotesFetcherTask task) {
                final NotesGeneration gen = task.getNotesGeneration();
                preconfigureNotableNotes(project, gen);

                LazyConfigurer.getConfigurer(project).configureLazily(task, new Runnable() {
                    public void run() {
                        configureNotableNotes(project, gen);
                    }
                });
            }
        });

        project.getTasks().create("updateNotableReleaseNotes", NotableReleaseNotesGeneratorTask.class,
                new Action<NotableReleaseNotesGeneratorTask>() {
            public void execute(NotableReleaseNotesGeneratorTask task) {
                final NotesGeneration gen = task.getNotesGeneration();
                preconfigureNotableNotes(project, gen);

                task.dependsOn("fetchNotableReleaseNotes");

                LazyConfigurer.getConfigurer(project).configureLazily(task, new Runnable() {
                    public void run() {
                        configureNotableNotes(project, gen);
                    }
                });
            }
        });
    }

    private static void preconfigureIncrementalNotes(final IncrementalReleaseNotes task, final Project project) {
        final ExtContainer ext = new ExtContainer(project);
        LazyConfigurer.getConfigurer(project).configureLazily(task, new Runnable() {
            public void run() {
                task.setGitHubLabelMapping(ext.getMap(ReleaseToolsProperties.releaseNotes_labelMapping)); //TODO make it optional
                task.setReleaseNotesFile(project.file(ext.getReleaseNotesFile())); //TODO add sensible default
                task.setGitHubReadOnlyAuthToken(ext.getGitHubReadOnlyAuthToken());
                task.setGitHubRepository(ext.getString(ReleaseToolsProperties.gh_repository));
            }
        });
    }

    private static void preconfigureNotableNotes(Project project, NotesGeneration gen){
        gen.setGitHubLabels(asList("noteworthy"));
        gen.setGitWorkingDir(project.getRootDir());
        gen.setIntroductionText("Notable release notes:\n\n");
        gen.setOnlyPullRequests(true);
        gen.setTagPrefix("v");
        gen.setTemporarySerializedNotesFile(getTemporaryReleaseNotesFile(project));
    }

    private static void configureNotableNotes(Project project, NotesGeneration gen) {
        ExtContainer ext = new ExtContainer(project);
        gen.setGitHubReadOnlyAuthToken(ext.getGitHubReadOnlyAuthToken());
        gen.setGitHubRepository(ext.getGitHubRepository());
        gen.setOutputFile(project.file(ext.getNotableReleaseNotesFile()));
        gen.setVcsCommitsLinkTemplate("https://github.com/" + ext.getGitHubRepository() + "/compare/{0}...{1}");
        gen.setDetailedReleaseNotesLink(ext.getGitHubRepository() + "/blob/" + ext.getCurrentBranch() + "/" + ext.getNotableReleaseNotesFile());
    }

    private static File getTemporaryReleaseNotesFile(Project project){
        String path = project.getBuildDir()  + TEMP_SERIALIZED_NOTES_FILE;
        return project.file(path);
    }
}
