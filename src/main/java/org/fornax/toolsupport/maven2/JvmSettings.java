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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * 
 * @author Karsten Thoms
 * @since 3.1.1
 */
public class JvmSettings {
	private boolean fork;
	private List<String> jvmArgs = new ArrayList<String>();
	private Properties sysProperties = new Properties();
	private Properties envProperties = new Properties();

	public void setFork(boolean fork) {
		this.fork = fork;
	}

	public boolean isFork() {
		return fork;
	}

	public void setJvmArgs(List<String> jvmArgs) {
		this.jvmArgs = jvmArgs;
	}

	public List<String> getJvmArgs() {
		return jvmArgs;
	}

	public void setSysProperties(Properties sysProperties) {
		this.sysProperties = sysProperties;
	}

	public Properties getSysProperties() {
		return sysProperties;
	}
	
	public void setEnvProperties(Properties envProperties) {
		this.envProperties = envProperties;
	}

	public Properties getEnvProperties() {
		return envProperties;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).toString();
	}

}
