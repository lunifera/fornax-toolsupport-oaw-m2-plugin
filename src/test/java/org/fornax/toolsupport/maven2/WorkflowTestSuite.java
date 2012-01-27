package org.fornax.toolsupport.maven2;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.powermock.modules.junit3.PowerMockSuite;

/**
 * Test Suite to enable PowerMock features with JUnit 3.
 *
 * @author Dietrich Schulten
 *
 */
public class WorkflowTestSuite extends PowerMockSuite {

	public WorkflowTestSuite(Class<? extends TestCase>[] testCases) throws Exception {
		super(testCases);
	}

	public static TestSuite suite() throws Exception {

		return new PowerMockSuite(WorkflowSuiteComponent.class);
	}

	public static void main(String[] args) throws Exception {

		junit.textui.TestRunner.run(suite());
	}

}
