/*
 *	Copyright 2006-2010 The Fornax Project Team
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 * 	You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * 	Unless required by applicable law or agreed to in writing, software
 * 	distributed under the License is distributed on an "AS IS" BASIS,
 * 	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 	See the License for the specific language governing permissions and
 * 	limitations under the License.
 */
package org.fornax.toolsupport.maven2;

import java.io.FilePermission;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.PropertyPermission;

import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.Redirector;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment.Variable;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Permissions;
import org.codehaus.classworlds.ClassRealm;

/**
 * Creates an {@link Java Ant Java task} that executes the workflow.
 *
 * @author Karsten Thoms
 * @since 3.1.1
 */
public class JavaTaskBuilder {
	private Project antProject = new Project();
	private MavenProject mvnProject;
	private Java javaTask;
	private ClassRealm realm;
	private JvmSettings jvmSettings;
	private boolean fork;


	public JavaTaskBuilder (MavenProject project, ClassRealm realm) {
		this.mvnProject = project;
		this.realm = realm;
		this.javaTask = new Java();

		antProject.setBaseDir(project.getBasedir());
		javaTask.setProject(antProject);
		javaTask.setLocation(new Location(project.getBasedir().getAbsolutePath()));
		Target target = new Target();
		antProject.addTarget("run-workflow", target);
		target.addTask(javaTask);
		configureClasspath();
	}

	public JavaTaskBuilder fork (boolean fork) {
		javaTask.setFork(fork);
		this.fork = fork;
		return this;
	}

	public JavaTaskBuilder withJvmSettings (JvmSettings jvmSettings) {
		if (jvmSettings != null) {
			for (String jvmArg : jvmSettings.getJvmArgs()) {
				Commandline.Argument newArg = javaTask.createJvmarg();
				newArg.setLine(jvmArg);
			}
			for (Entry<Object, Object> entry : jvmSettings.getSysProperties().entrySet()) {
				Variable var = new Variable();
				var.setKey(entry.getKey().toString());
				var.setValue(String.valueOf(entry.getValue()));
				javaTask.addSysproperty(var);
			}
			for (Entry<Object, Object> entry : jvmSettings.getEnvProperties().entrySet()) {
				Variable var = new Variable();
				var.setKey(entry.getKey().toString());
				var.setValue(String.valueOf(entry.getValue()));
				javaTask.addEnv(var);
			}
		}

		return this;
	}

	public JavaTaskBuilder withOutputStream (final OutputStream os) {
		Redirector redirector = new Redirector(javaTask) {
			@Override
			public OutputStream getOutputStream() {
				return os;
			}
			@Override
			public OutputStream getErrorStream() {
				return os;
			}
		};
		try {
			Field redirectorField = Java.class.getDeclaredField("redirector");
			redirectorField.setAccessible(true);
			redirectorField.set(javaTask, redirector);
			redirectorField.setAccessible(false);
		} catch (Exception e) { // SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
			throw new RuntimeException(e);
		}
		return this;
	}

	public JavaTaskBuilder withSecuritySettings (SecuritySettings securitySettings) {
		if (securitySettings != null) {
			Permissions permissions = javaTask.createPermissions();
			for (Permission p : securitySettings.getGrantedPermissions()) {
				permissions.addConfiguredGrant(p.toAntPermission());
			}
			for (Permission p : securitySettings.getGrantedPermissions()) {
				permissions.addConfiguredGrant(p.toAntPermission());
			}

			// add default permissions
			if (fork) {
				permissions.addConfiguredGrant(createPermission(RuntimePermission.class, "exitVM", null));
				permissions.addConfiguredGrant(createPermission(RuntimePermission.class, "shutdownHooks", null));
			}
			permissions.addConfiguredGrant(createPermission(RuntimePermission.class, "setContextClassLoader", null));
			permissions.addConfiguredGrant(createPermission(RuntimePermission.class, "getClassLoader", null));
			permissions.addConfiguredGrant(createPermission(RuntimePermission.class, "readFileDescriptor", null));
			permissions.addConfiguredGrant(createPermission(RuntimePermission.class, "writeFileDescriptor", null));
			permissions.addConfiguredGrant(createPermission(RuntimePermission.class, "accessDeclaredMembers", null));
			permissions.addConfiguredGrant(createPermission(PropertyPermission.class, "user.dir", "read, write"));
			permissions.addConfiguredGrant(createPermission(PropertyPermission.class, "ant.build.clonevm", "read"));

			permissions.addConfiguredGrant(createPermission(FilePermission.class, mvnProject.getBasedir().getAbsolutePath()+"/-", "read, write"));
			// see Execute#getProcEnvCommand()
			permissions.addConfiguredGrant(createPermission(FilePermission.class, "/bin/env", "read"));
			permissions.addConfiguredGrant(createPermission(FilePermission.class, "/usr/bin/env", "read, execute"));
			for (URL constituent : realm.getConstituents()) {
				permissions.addConfiguredGrant(createPermission(FilePermission.class, constituent.getFile(), "read"));
			}
			permissions.addConfiguredGrant(createPermission(FilePermission.class, javaTask.getCommandLine().getVmCommand().getExecutable(), "read, execute"));

		}
		return this;
	}

	private Permissions.Permission createPermission (Class<? extends java.security.Permission> permissionClass, String name, String actions ) {
		Permissions.Permission p = new Permissions.Permission();
		p.setClass(permissionClass.getName());
		p.setName(name);
		if (actions!=null) {
			p.setActions(actions);
		}
		return p;
	}

	public JavaTaskBuilder failOnError (boolean failOnError) {
		javaTask.setFailonerror(failOnError);
		return this;
	}

	public JavaTaskBuilder withInputString (String input) {
		javaTask.setInputString(input);
		return this;
	}

	public JavaTaskBuilder withProperties (Map<String, String> properties) {
		if (properties != null) {
			for (Object key : properties.keySet()) {
				// javaTask.createArg() would append the parameter at the beginning before the classname
				// but it must be appended at the end.
				Commandline.Argument newArg = javaTask.getCommandLine().getJavaCommand().createArgument(false);
				newArg.setLine("-p" + key + "=" + properties.get(key));
			}
		}
		return this;
	}

	public JavaTaskBuilder withWorkflow (String workflow) {
		javaTask.createArg().setLine(workflow);
		return this;
	}

	public JavaTaskBuilder withWorkflowLauncherClass (String launcherClass) {
		javaTask.setClassname(launcherClass);
		return this;
	}

	public Java build () {
		return javaTask;
	}

	private void configureClasspath () {
		String classpath = "";
		for (URL url : realm.getConstituents()) {
			if ("".equals(classpath)) {
				classpath += url.getFile();
			} else {
				classpath += System.getProperty("path.separator") + url.getFile();
			}
		}
		javaTask.setClasspath(new Path(antProject, classpath));
	}
}
