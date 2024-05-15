package systems.symbol.hub;

import org.apache.commons.io.IOUtils;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

/*
 *  symbol.systems
 *  Copyright (c) 2009-2015, 2021-2023 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 *
 * See also: https://github.com/hub4j/github-api/tree/main/src/test/java/org/kohsuke/github
 */
public class GIT extends GuardedIntent {
	private final Logger log = LoggerFactory.getLogger( getClass());

	URL webhook;
	String orgName, homepage;
	String initialCommitText = "Prototype";
	private org.kohsuke.github.GitHub github;

	public GitHub()  {
	}

	public GitHub withLogin(String client, String secret) throws IOException {
		github = new GitHubBuilder().withPassword(client, secret).build();
		return this;
	}

	public GHMyself getMyself() throws IOException {
		return github.getMyself();
	}

	public GitHub withToken(String access_token) throws IOException {
		github = new GitHubBuilder().withOAuthToken(access_token).build();
		orgName = github.getMyself().getLogin();
		log.warn("iq.hub.withOAuth: "+orgName+" -> "+github.getMyself());
		return this;
	}

	public GHRepository getRepository(String repo) throws IOException {
		String fullOrgName = orgName+"/"+repo;
		return github.getRepository(fullOrgName);
	}

	public GHRepository getOrCreateRepository(String repo) throws IOException {
		String fullOrgName = orgName+"/"+repo;
		try {
			GHRepository repository = github.getRepository(fullOrgName);
			if (repository != null) return repository;
		} catch (GHFileNotFoundException e) {
			log.warn("iq.hub.repo.not-found: "+fullOrgName+" -> "+e.getMessage());
			return createRepository(repo);
		}
		return createRepository(repo);
	}

	public GHRepository createRepository(String repo) throws IOException {
		GHMyself me = github.getMyself();
		log.warn("iq.hub.repo.create: "+repo);
		GHCreateRepositoryBuilder builder = github.createRepository(repo)
			.downloads(true)
			.autoInit(false)
			.wiki(false)
			.owner(me.getName());

		GHRepository repository = builder.create();

		// meta data - name, description + visibility
		String long_name = "My:IQ: "+repo;
		repository.setDescription(long_name);
		repository.setVisibility(GHRepository.Visibility.PRIVATE);
		if (homepage!=null) repository.setHomepage(homepage);

		// webhooks
		if (webhook!=null) repository.createWebHook(webhook);

//		createProjects(me, repository, long_name);

//		initialCommit(me, repository);
//		repository.createContent().

//		repository.getBranch(repository.getDefaultBranch()).
		return repository;
	}

	public void push(String repo, File root) throws IOException {
		push(repo, root, initialCommitText);
	}

	public void push(String repo, File root, String message) throws IOException {
		GHRepository repository = getOrCreateRepository(repo);
		push(repository, root, root, message);
	}

	private void push(GHRepository repository, File root, File file, String message) throws IOException {
		if (!file.exists()) return;
		if (file.isFile()) {
			GHContentBuilder content = repository.createContent();
			String path = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
			log.info("codehub.push.path: " + path);
			GHContentBuilder builder = content.path(path).content(IOUtils.toString(new FileReader(file)));
			builder.message(message);
			GHContent fileContent = repository.getFileContent(path);
			if (fileContent != null) {
				builder.sha(fileContent.getSha());

				GHContentUpdateResponse done = builder.commit();
				log.info("codehub.push.done: " + done);
			} else if (file.isDirectory()) {
				for (File f : file.listFiles()) {
					push(repository, root, f, message);
				}
			}
		}
	}

	private void initialCommit(GHMyself me, GHRepository repository) throws IOException {
		GHCommitBuilder committer = repository.createCommit()
			.author(me.getName(), me.getEmail(), new Date())
			.committer(me.getName(), me.getEmail(), new Date());
		GHCommit commit = committer.create();
		commit.createComment(initialCommitText );
	}

	private void createProjects(GHMyself me, GHRepository repository, String long_name) throws IOException {
		// developer help  ...
		GHProject developer = repository.createProject("iq-developer-help", "Developer Portal for "+long_name);
		developer.setState(GHProject.ProjectState.OPEN);
		developer.setPublic(true);
		developer.setOrganizationPermission(GHPermissionType.WRITE);
		// user help  ...
		GHProject help = repository.createProject("iq-user-help", "Help for "+long_name);
		help.setState(GHProject.ProjectState.OPEN);
		help.setOrganizationPermission(GHPermissionType.WRITE);
		help.setPublic(true);
	}

}
