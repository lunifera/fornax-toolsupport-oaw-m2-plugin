package org.fornax.toolsupport.maven2;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
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
 * Do not run standalone, use WorkflowMojoTestSuite instead
 *
 * @author Dietrich Schulten
 *
 */
@PrepareForTest({ JavaTaskBuilder.class, Java.class })
public class WorkflowSuiteComponent extends AbstractMojoTestCase {

	MyProjectStub project;

	WorkflowMojo mojo;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setUp() throws Exception {
		super.setUp();
		File pom = getTestFile("src/test/projects/mocktest/pom-oaw.xml");
		project = new MyProjectStub(pom);
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

		mojo = (WorkflowMojo) lookupMojo("run-workflow", pom);

		setVariableValueToObject(mojo, "dependencies", new HashSet<Artifact>(artifacts));
		setVariableValueToObject(mojo, "pluginArtifacts", new ArrayList(project.getPluginArtifacts()));

		setVariableValueToObject(mojo, "project", project);
		for (Outlet outlet : Outlet.values()) {
			setVariableValueToObject(mojo, outlet.propertyName, outlet.defaultDir);
		}
	}

	public void testWorkflowOaw() throws Exception {

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

	public void testSkipGenerations() throws Exception {

		Project mockProject = PowerMockito.mock(Project.class);
		Java spiedJava = PowerMockito.spy(new Java()); // partial mock
		PowerMockito.whenNew(Java.class).withNoArguments().thenReturn(spiedJava);
		PowerMockito.when(spiedJava.getProject()).thenReturn(mockProject);

		// setVariableValueToObject(mojo, "force", true);
		System.setProperty("fornax.generator.omit.execution", "true");

		mojo.execute();

		Mockito.verify(mockProject);
		// String commandline = spiedJava.getCommandLine().getJavaCommand().toString();
		// assertThat(commandline, startsWith("org.openarchitectureware.workflow.WorkflowRunner workflow.oaw"));
		// assertThat(commandline, containsString("-p outlet.src.dir="));

	}

}
