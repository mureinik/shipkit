package org.shipkit.gradle.configuration;

import org.shipkit.internal.gradle.configuration.ShipkitConfigurationStore;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.shipkit.internal.gradle.util.team.TeamParser.validateTeamMembers;

/**
 * Shipkit configuration.
 * Contains configuration that is used by Shipkit plugins to configure Shipkit tasks.
 * <p>
 * Example of a release configuration of a working example project
 * <a href="https://github.com/mockito/shipkit-example/blob/master/gradle/shipkit.gradle">on GitHub</a>.
 * <p>
 * Sophisticated example based on Mockito project:
 * <a href="https://github.com/mockito/mockito/blob/release/2.x/gradle/shipkit.gradle">Mockito project</a>.
 */
public class ShipkitConfiguration {

    private final ShipkitConfigurationStore store;

    private final GitHub gitHub = new GitHub();
    private final ReleaseNotes releaseNotes = new ReleaseNotes();
    private final Git git = new Git();
    private final Team team = new Team();

    private String previousReleaseVersion;

    public ShipkitConfiguration() {
        this(new ShipkitConfigurationStore());

        //Configure default values
        git.setTagPrefix("v"); //so that tags are "v1.0", "v2.3.4"
        git.setReleasableBranchRegex("master|release/.+");  // matches 'master', 'release/2.x', 'release/3.x', etc.
        git.setCommitMessagePostfix("[ci skip]");
        git.setUser("shipkit-org");
        git.setEmail("<shipkit.org@gmail.com>");

        gitHub.setUrl("https://github.com");
        gitHub.setApiUrl("https://api.github.com");

        //It does not seem that write auth user is used by GitHub in any way
        gitHub.setWriteAuthUser("dummy");

        releaseNotes.setFile("docs/release-notes.md");
        releaseNotes.setIgnoreCommitsContaining(singletonList("[ci skip]"));
        releaseNotes.setLabelMapping(Collections.<String, String>emptyMap());

        team.setContributors(Collections.<String>emptyList());
        team.setDevelopers(Collections.<String>emptyList());
    }

    ShipkitConfiguration(ShipkitConfigurationStore store) {
        this.store = store;
    }

    private boolean dryRun;

    /**
     * See {@link #isDryRun()}
     */
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    /**
     * If the release steps should be invoked in "dry run" mode.
     * Relevant only to some kinds of release steps,
     * such as bintray upload, git push.
     */
    public boolean isDryRun() {
        return dryRun;
    }

    public GitHub getGitHub() {
        return gitHub;
    }

    public ReleaseNotes getReleaseNotes() {
        return releaseNotes;
    }

    public Git getGit() {
        return git;
    }

    public Team getTeam() {
        return team;
    }

    /**
     * See {@link #getPreviousReleaseVersion()}
     */
    public void setPreviousReleaseVersion(String previousReleaseVersion) {
        this.previousReleaseVersion = previousReleaseVersion;
    }

    /**
     * Return last previously released version number.
     */
    public String getPreviousReleaseVersion() {
        return previousReleaseVersion;
    }

    public class GitHub {

        /**
         * GitHub URL address, for example: https://github.com.
         * Useful when you are using on-premises GitHub Enterprise
         * and your url is different than the default GitHub instance for Open Source
         */
        public String getUrl() {
            return store.getStringUrl("gitHub.url");
        }

        /**
         * See {@link #getUrl()}
         */
        public void setUrl(String url) {
            store.put("gitHub.url", url);
        }

        /**
         * GitHub API endpoint address, for example:  https://api.github.com
         */
        public String getApiUrl() {
            return store.getStringUrl("gitHub.apiUrl");
        }

        /**
         * See {@link #getApiUrl()}
         */
        public void setApiUrl(String apiUrl) {
            store.put("gitHub.apiUrl", apiUrl);
        }

        /**
         * GitHub repository name, for example: "mockito/shipkit"
         */
        public String getRepository() {
            return store.getString("gitHub.repository");
        }

        /**
         * See {@link #getRepository()}
         *
         * @param repository name of the repo, including user or organization section, for example: "mockito/shipkit"
         */
        public void setRepository(String repository) {
            store.put("gitHub.repository", repository);
        }

        /**
         * GitHub user associated with the write auth token.
         * Needed for the release process to push changes.
         */
        public String getWriteAuthUser() {
            return store.getString("gitHub.writeAuthUser");
        }

        /**
         * See {@link #getWriteAuthUser()}
         */
        public void setWriteAuthUser(String user) {
            store.put("gitHub.writeAuthUser", user);
        }

        /**
         * GitHub read only auth token.
         * Since the token is read-only it is ok to check that in to VCS.
         */
        public String getReadOnlyAuthToken() {
            return store.getString("gitHub.readOnlyAuthToken");
        }

        /**
         * See {@link #getReadOnlyAuthToken()}
         */
        public void setReadOnlyAuthToken(String token) {
            store.put("gitHub.readOnlyAuthToken", token);
        }

        /**
         * GitHub write auth token to be used for pushing code to GitHub.
         * Please do not configure write auth token in plain text / commit to VCS!
         * Instead use env variable and that you can securely store in CI server configuration.
         * Shipkit automatically uses "GH_WRITE_TOKEN" env variable
         * if this value is not specified.
         */
        public String getWriteAuthToken() {
            return (String) store.getValue("gitHub.writeAuthToken", "GH_WRITE_TOKEN", "Please export 'GH_WRITE_TOKEN' variable first!\n" +
                "It is highly recommended to keep write token secure and store env variable 'GH_WRITE_TOKEN' with your CI configuration." +
                "Alternatively, you can configure GitHub write auth token explicitly (don't check this in to Git!):\n" +
                "  shipkit.gitHub.writeAuthToken = 'secret'");
        }

        public void setWriteAuthToken(String writeAuthToken) {
            store.put("gitHub.writeAuthToken", writeAuthToken);
        }
    }

    public class ReleaseNotes {

        /**
         * Release notes file relative path, for example: "docs/release-notes.md"
         */
        public String getFile() {
            return store.getString("releaseNotes.file");
        }

        /**
         * See {@link #getFile()}
         */
        public void setFile(String file) {
            store.put("releaseNotes.file", file);
        }

        /**
         * Issue tracker label mappings.
         * The mapping of issue tracker labels (for example "GitHub label") to human readable and presentable name.
         * The order of labels is important and will influence the order
         * in which groups of issues are generated in release notes.
         * Examples: ['java-9': 'Java 9 support', 'BDD': 'Behavior-Driven Development support']
         */
        public Map<String, String> getLabelMapping() {
            return store.getMap("releaseNotes.labelMapping");
        }

        /**
         * See {@link #getLabelMapping()}
         */
        public void setLabelMapping(Map<String, String> labelMapping) {
            store.put("releaseNotes.labelMapping", labelMapping);
        }

        /**
         * Release notes are generated based on information in commit messages.
         * If a commit message contains any of texts from this collection,
         * that commit will be ignored and not used for generating release notes.
         */
        public Collection<String> getIgnoreCommitsContaining() {
            return store.getCollection("releaseNotes.ignoreCommitsContaining");
        }

        /**
         * See {@link #getIgnoreCommitsContaining()}
         */
        public void setIgnoreCommitsContaining(Collection<String> commitMessageParts) {
            store.put("releaseNotes.ignoreCommitsContaining", commitMessageParts);
        }
    }

    public class Git {

        /**
         * Git user to be used for automated commits made by release automation
         * (version bumps, release notes commits, etc.).
         * For example: "shipkit"
         */
        public String getUser() {
            return store.getString("git.user");
        }

        /**
         * See {@link #getUser()} ()}
         */
        public void setUser(String user) {
            store.put("git.user", user);
        }

        /**
         * Git email to be used for automated commits made by release automation
         * (version bumps, release notes commits, etc.).
         * For example "shipkit.org@gmail.com"
         */
        public String getEmail() {
            return store.getString("git.email");
        }

        /**
         * See {@link #getEmail()}
         */
        public void setEmail(String email) {
            store.put("git.email", email);
        }

        /**
         * Regex to be used to identify branches that are entitled to be released, for example "master|release/.+"
         */
        public String getReleasableBranchRegex() {
            return store.getString("git.releasableBranchRegex");
        }

        /**
         * See {@link #getReleasableBranchRegex()}
         */
        public void setReleasableBranchRegex(String releasableBranchRegex) {
            store.put("git.releasableBranchRegex", releasableBranchRegex);
        }

        /**
         * Prefix added to the version to create VCS-addressable tag,
         * for example: "v".
         * Empty string is ok and it means that there is not prefix.
         */
        public String getTagPrefix() {
            return store.getString("git.tagPrefix");
        }

        /**
         * See {@link #getTagPrefix()}
         */
        public void setTagPrefix(String tagPrefix) {
            store.put("git.tagPrefix", tagPrefix);
        }

        /**
         * Text which will be included in the commit message for all commits automatically created by the release
         * automation.
         * By default it is configured to append "[ci skip]" keyword which will prevent CI builds on Travis CI.
         */
        public String getCommitMessagePostfix() {
            return store.getString("git.commitMessagePostfix");
        }

        /**
         * See {@link #getCommitMessagePostfix()}
         */
        public void setCommitMessagePostfix(String commitMessagePostfix) {
            //TODO protect this setter and other relevant from invalid input (null value)
            store.put("git.commitMessagePostfix", commitMessagePostfix);
        }
    }

    /**
     * Team configuration
     */
    public class Team {

        /**
         * Developers to include in generated pom file.
         * It should be a collection of elements like "GITHUB_USER:FULL_NAME", example:
         * ['szczepiq:Szczepan Faber', 'mstachniuk:Marcin Stachniuk'].
         * <p>
         * See POM reference for <a href="https://maven.apache.org/pom.html#Developers">Developers</a>.
         */
        public Collection<String> getDevelopers() {
            return store.getCollection("team.developers");
        }

        /**
         * See {@link #getDevelopers()}
         */
        public void setDevelopers(Collection<String> developers) {
            validateTeamMembers(developers);
            store.put("team.developers", developers);
        }

        /**
         * Contributors to include in generated pom file.
         * It should be a collection of elements like "GITHUB_USER:FULL_NAME", example:
         * ['szczepiq:Szczepan Faber', 'mstachniuk:Marcin Stachniuk']
         * <p>
         * See POM reference for <a href="https://maven.apache.org/pom.html#Contributors">Contributors</a>.
         */
        public Collection<String> getContributors() {
            return store.getCollection("team.contributors");
        }

        /**
         * See {@link #getContributors()}
         */
        public void setContributors(Collection<String> contributors) {
            validateTeamMembers(contributors);
            store.put("team.contributors", contributors);
        }
    }

    /**
     * Provides 'lenient' copy of this configuration instance,
     * that does not fail fast when one accesses a property that is not configured (e.g. is null).
     * <p>
     * By default, release configuration object fails fast in this scenario.
     * This is a good default because it helps us identify missing configuration early.
     * However sometimes we want to check if the user has configured a property (like for GitHub write token) without failing.
     *
     * @return lenient copy of this configuration instance
     */
    public ShipkitConfiguration getLenient() {
        return new ShipkitConfiguration(store.getLenient());
    }
}
