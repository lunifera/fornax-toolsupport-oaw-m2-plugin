package org.fornax.toolsupport.maven2;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.plugin.testing.stubs.StubArtifactResolver;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

/**
 * Do not run standalone, use WorkflowTestSuite instead
 *
 * @author Dietrich Schulten
 *
 */
@PrepareForTest({ JavaTaskBuilder.class, Java.class })
public class WorkflowSuiteComponent extends AbstractMojoTestCase {

	private MyProjectStub project;

	private WorkflowMojo mojo;

	private String pomOaw = "src/test/projects/mocktest/pom-oaw.xml";

	private Project mockProject;

	public void setUp() throws Exception {
		super.setUp();
		System.setProperty("fornax.generator.omit.execution", "false");
		System.setProperty("fornax.generator.force.execution", "false");

		mockProject = PowerMockito.mock(Project.class);
		Java spiedJava = PowerMockito.spy(new Java()); // partial mock
		PowerMockito.whenNew(Java.class).withNoArguments().thenReturn(spiedJava);
		PowerMockito.when(spiedJava.getProject()).thenReturn(mockProject);
	}

	public void testWorkflowOaw() throws Exception {
		setupMojo(pomOaw);

		Project mockProject = PowerMockito.mock(Project.class);
		Java spiedJava = PowerMockito.spy(new Java()); // partial mock
		PowerMockito.whenNew(Java.class).withNoArguments().thenReturn(spiedJava);
		PowerMockito.when(spiedJava.getProject()).thenReturn(mockProject);

		mojo.execute();

		Mockito.verify(mockProject).executeTarget("run-workflow");
		String commandline = spiedJava.getCommandLine().getJavaCommand().toString();
		assertThat(commandline, startsWith("org.openarchitectureware.workflow.WorkflowRunner workflow.oaw"));
		assertThat(commandline, containsString("-p outlet.src.dir="));
	}

	public void testOmitExecution() throws Exception {
		setupMojo(pomOaw);
		System.setProperty("fornax.generator.omit.execution", "true");
		mojo.execute();
		verify(mockProject, never()).executeTarget("run-workflow");
	}

	public void testForceExecution() throws Exception {
		setupMojo(pomOaw);
		System.setProperty("fornax.generator.force.execution", "true");
		mojo.execute();
		verify(mockProject).executeTarget("run-workflow");
	}

	public void testForce() throws Exception {
		setupMojo(pomOaw);
		setVariableValueToObject(mojo, "force", true);
		mojo.execute();
		verify(mockProject).executeTarget("run-workflow");
	}

	private void setupMojo(String pathToPom) throws IOException, ArtifactResolutionException, ArtifactNotFoundException,
			Exception, IllegalAccessException {
		File pom = getTestFile(pathToPom);
		project = new MyProjectStub(pom);

		@SuppressWarnings("unchecked")
		List<Dependency> projectDependencies = project.getDependencies();
		ArtifactStubFactory artifactFactory = new ArtifactStubFactory(new File(project.getBasedir(), "target"), false);
		ArtifactResolver artifactResolver = new StubArtifactResolver(artifactFactory, false, false);
		ArtifactRepository localRepository = new StubArtifactRepository(new File(project.getBasedir(), "localTestRepo")
				.getAbsolutePath());
		List<Artifact> artifacts = new ArrayList<Artifact>();
		for (Dependency dependency : projectDependencies) {
			Artifact artifact = artifactFactory.createArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency
					.getVersion());
			artifacts.add(artifact);
			artifactResolver.resolve(artifact, Collections.emptyList(), localRepository);
		}

		@SuppressWarnings("unchecked")
		ArrayList<Artifact> pluginArtifacts = new ArrayList<Artifact>(project.getPluginArtifacts());

		mojo = (WorkflowMojo) lookupMojo("run-workflow", pom);

		File timestampFile = new File(project.getBuild().getDirectory(), "oaw-generation-lastrun.timestamp");
		if (timestampFile.exists())
			if (!timestampFile.delete())
				throw new IllegalStateException("failed to delete timestampfile");

		setVariableValueToObject(mojo, "dependencies", new HashSet<Artifact>(artifacts));
		setVariableValueToObject(mojo, "pluginArtifacts", pluginArtifacts);

		setVariableValueToObject(mojo, "project", project);
		for (Outlet outlet : Outlet.values()) {
			setVariableValueToObject(mojo, outlet.propertyName, outlet.defaultDir);
		}
	}

}
