package org.eclipse.core.tests.pki.auth;

/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
//import org.eclipse.core.pki.PKISetup;
import java.util.Properties;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import junit.framework.TestCase;

//import static org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
//import static org.assertj.swing.fixture.JPanelFixture;
//import static org.assertj.swing.testing.AssertJSwingTestCaseTemplate;

import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.anyInt;
import org.mockito.MockedStatic;

//import org.mockito.MockedStatic.staticMethod;
//import org.powermock.modules.junit4.PowerMockRunner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;

import org.eclipse.core.pki.auth.PKISetup;



//@RunWith(PowerMockRunner.class)
public class PKISetupTest {
	
	PKISetup pkiSetupMock = null;
	
	@Mock
	private SSLContext context;
	
	@Mock
	private BundleContext bundleContext;

	public PKISetupTest() {}
	
	@Before
	public void Initialize() throws Exception {
		MockitoAnnotations.initMocks(this);
		pkiSetupMock = mock(PKISetup.class);
		//context = SSLContext.getDefault();
	}

	@Test
	public void testRun() {
		Object o = new Object();
		
	}
	@Test
	public void testStart() {
		//doNothing().when(pkiSetupMock).start(bundleContext).thenThrows(new Exception());
		try {
			doNothing().when(pkiSetupMock).start(bundleContext);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			assertThrows(Exception.class, () -> pkiSetupMock.start(bundleContext));
		}
	}
	@Test
	public void testStop() {
		//doNothing().when(pkiSetupMock).start(bundleContext).thenThrows(new Exception());
		try {
			doNothing().when(pkiSetupMock).stop(bundleContext);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			assertThrows(Exception.class, () -> pkiSetupMock.stop(bundleContext));
		}
	}
	@Test
	public void testlog() {
		doNothing().when(pkiSetupMock).log(isA(String.class));
	}
	@Test
	public void testStartup() {
		doNothing().when(pkiSetupMock).Startup();
	}
	@Test
	public void testgetInstance() {
		PKISetup pki = Mockito.spy(PKISetup.getInstance());
		
		try (MockedStatic <PKISetup> pkiSetupMock = Mockito.mockStatic(PKISetup.class)) {
			pkiSetupMock.when(() -> { PKISetup.getInstance(); })
	      .thenReturn(pki);
		}
	}
	@Test
	public void testgetSSLContext() {
		
		when(pkiSetupMock.getSSLContext()).thenReturn(context);
		//when(pkiSetupMock.getSSLContext()).thenThrow(new NoSuchAlgorithmException("no no no!"));
		/*
		 * try (MockedStatic <PKISetup> pkiSetupMock =
		 * Mockito.mockStatic(PKISetup.class)) { pkiSetupMock.when(() -> {
		 * PKISetup.getSSLContext(); }) .thenReturn(SSLContext.class); }
		 */
		
	}
	@Test
	public void testsetSSLContext() {
		doNothing().when(pkiSetupMock).setSSLContext(context);
	}
}
