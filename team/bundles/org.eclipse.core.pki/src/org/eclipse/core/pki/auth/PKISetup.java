/*******************************************************************************
 * Copyright (c) 2023 Eclipse Platform, Security Group and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Platform - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.pki.auth;

import org.eclipse.core.runtime.ServiceCaller;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Properties;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.io.File;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.core.pki.AuthenticationBase;
import org.eclipse.core.pki.pkiselection.PKIProperties;
import org.eclipse.core.pki.util.ConfigureTrust;
import org.eclipse.core.pki.util.KeyStoreFormat;
import org.eclipse.core.pki.util.KeyStoreManager;
import org.eclipse.core.pki.util.LogUtil;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Platform;
//import org.eclipse.jface.preference.IPreferenceStore;
//import org.eclipse.osgi.framework.eventmgr.EventManager;
//import org.eclipse.osgi.framework.eventmgr.ListenerQueue;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class PKISetup implements BundleActivator, IStartup {
	public static final String ID = "org.eclipse.core.pki"; //$NON-NLS-1$
	protected final String pin = "#Gone2Boat@Bay"; //$NON-NLS-1$
	private static PKISetup instance;
	static boolean isPkcs11Installed = false;
	static boolean isKeyStoreLoaded = false;
	private BundleContext context;
	SSLContext sslContext = null;
	private static final ServiceCaller<ILog> logger = new ServiceCaller(PKISetup.class, ILog.class);
	// ListenerQueue<PKISetup, Object, EventManager> queue = null;
	protected static KeyStore keyStore = null;
	PKIProperties pkiInstance = null;
	Properties pkiProperties = null;
	Optional<KeyStore> keystoreContainer = null;
	private static final int DIGITAL_SIGNATURE = 0;
	private static final int KEY_CERT_SIGN = 5;
	private static final int CRL_SIGN = 6;

	public PKISetup() {
		super();
		setInstance(this);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		// System.out.println("PKISetup PKISetup ------------------- START");
		this.context = context;

		Startup();
	}

	@Override
	public void earlyStartup() {
		// TODO Auto-generated method stub
		// System.out.println("PKISetup PKISetup -------------------early
		// START");//$NON-NLS-1$
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

	public static PKISetup getInstance() {
		return instance;
	}

	public static void setInstance(PKISetup instance) {
		PKISetup.instance = instance;
	}

	public void log(String message) {
		logger.call(logger -> logger.info(message));
	}

	public void Startup() {

		// log("Startup method is now running"); //$NON-NLS-1$

		Optional<String> type = null;
		Optional<String> decryptedPw;

		PKIState.CONTROL.setPKCS11on(false);
		PKIState.CONTROL.setPKCS12on(false);
		/*
		 * First see if parameters were passed into eclipse via the command line -D
		 */
		type = Optional.ofNullable(System.getProperty("javax.net.ssl.keyStoreType")); //$NON-NLS-1$

		if (type.isEmpty()) {
			//
			// Incoming parameter as -DkeystoreType was empty so CHECK in .pki file
			//
			PKIState.CONTROL.setPKCS11on(false);
			PKIState.CONTROL.setPKCS12on(false);
			if (PublicKeySecurity.INSTANCE.isTurnedOn()) {
				PublicKeySecurity.INSTANCE.getPkiPropertyFile(pin);
			}
		}
		// LogUtil.logInfo("PKISetup - now looking at incoming"); //$NON-NLS-1$
		if (IncomingSystemProperty.SETTINGS.checkType()) {
			if (IncomingSystemProperty.SETTINGS.checkKeyStore(pin)) {
				KeystoreSetup setup = KeystoreSetup.getInstance();
				if (PKIState.CONTROL.isPKCS12on()) {
					
					setup.installKeystore();
					
				}
				if (PKIState.CONTROL.isPKCS11on()) {
					String pkcs11Pin = "";
					LogUtil.logInfo("PKISetup - Processing PKCS11"); //$NON-NLS-1$
					decryptedPw = Optional.ofNullable(System.getProperty("javax.net.ssl.keyStorePassword"));
					if (!decryptedPw.isEmpty()) {
						pkcs11Pin = decryptedPw.get();
					}
					keystoreContainer = Optional
							.ofNullable(AuthenticationBase.INSTANCE.initialize(pkcs11Pin.toCharArray()));// $NON-NLS-1$
					if (keystoreContainer.isEmpty()) {
						LogUtil.logError("PKISetup - Failed to Load a Keystore.", null); //$NON-NLS-1$
						PKIState.CONTROL.setPKCS11on(false);
						System.clearProperty("javax.net.ssl.keyStoreType"); //$NON-NLS-1$
						System.clearProperty("javax.net.ssl.keyStore"); //$NON-NLS-1$
						System.clearProperty("javax.net.ssl.keyStoreProvider"); //$NON-NLS-1$
						System.clearProperty("javax.net.ssl.keyStorePassword"); //$NON-NLS-1$
						SecurityFileSnapshot.INSTANCE.restoreProperties();
					} else {
						LogUtil.logError("A Keystore and Password are detected.", null); //$NON-NLS-1$
						keyStore = keystoreContainer.get();
						KeyStoreManager.INSTANCE.setKeyStore(keyStore);
						setKeyStoreLoaded(true);
						setup.setPkiContext();
					}
				}
				
//				if ((IncomingSystemProperty.SETTINGS.checkTrustStoreType()) && (isKeyStoreLoaded())) {
//					if ((IncomingSystemProperty.SETTINGS.checkTrustStore())
//							&& (KeyStoreManager.INSTANCE.isKeyStoreInitialized())) {
//						LogUtil.logInfo("A KeyStore and Truststore are detected."); //$NON-NLS-1$
//						Optional<X509TrustManager> PKIXtrust = ConfigureTrust.MANAGER.setUp();
//
//						try {
//							KeyManager[] km = new KeyManager[] { KeyStoreManager.INSTANCE };
//							TrustManager[] tm = new TrustManager[] { ConfigureTrust.MANAGER };
//							if (PKIXtrust.isEmpty()) {
//								LogUtil.logError("Invalid TrustManager Initialization.", null); //$NON-NLS-1$
//							} else {
//								SSLContext ctx = SSLContext.getInstance("TLS");//$NON-NLS-1$
//								ctx.init(km, tm, new SecureRandom());
//								SSLContext.setDefault(ctx);
//								HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
//								setSSLContext(ctx);
//								pkiInstance = PKIProperties.getInstance();
//								pkiInstance.load();
//								setUserEmail();
//								// Grab a handle to registry
//								File[] storageDirs = null;
//								boolean[] cacheReadOnly = null;
//								String token = "core.pki";
//								try {
//									IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
//									// IResource resource =
//									// ResourcesPlugin.getWorkspace().getAdapter(IResource.class);
//									IMarker marker = root.createMarker("virtual.core.pki.context");
//									marker.setAttribute(token, ctx);
//								} catch (Exception imarkerErr) {
//									imarkerErr.printStackTrace();
//								}
//								/*
//								 * IExtensionRegistry registry = Platform.getExtensionRegistry();
//								 * IConfigurationElement[] extensions =
//								 * registry.getConfigurationElementsFor(EXTENSION_POINT); for
//								 * (IConfigurationElement element : extensions) { [..] }
//								 */
//
//								// InjectorFactory.getDefault().addBinding(MyPart.class).implementedBy(MyFactory.class)
//								// RegistryStrategy strategy = RegistryFactory.createOSGiStrategy(File[]
//								// storageDirs, boolean[] cacheReadOnly, Object token)
//								RegistryStrategy strategy = RegistryFactory.createOSGiStrategy(storageDirs,
//										cacheReadOnly, token);
//								// IExtensionRegistry registry = RegistryFactory.getRegistry();
//								IExtensionRegistry registry = RegistryFactory.createRegistry(strategy, token, ctx);
//
//								setupAdapter();
//								LogUtil.logInfo("PKISetup default SSLContext has been configured."); //$NON-NLS-1$
//							}
//						} catch (NoSuchAlgorithmException e) {
//							// TODO Auto-generated catch block
//							LogUtil.logError("Initialization Error", e); //$NON-NLS-1$
//						} catch (KeyManagementException e) {
//							// TODO Auto-generated catch block
//							LogUtil.logError("Initialization Error", e); //$NON-NLS-1$
//						}
//					} else {
//						LogUtil.logError("Valid KeyStore and Truststore not found.", null); //$NON-NLS-1$
//					}
//
//				}
			}
		}
	}

	public SSLContext getSSLContext() {
		return sslContext;
	}

	public void setSSLContext(SSLContext context) {
		this.sslContext = context;
	}

	private boolean isKeyStoreLoaded() {
		return isKeyStoreLoaded;
	}

	private void setKeyStoreLoaded(boolean isKeyStoreLoaded) {
		PKISetup.isKeyStoreLoaded = isKeyStoreLoaded;
	}

	private static boolean isDigitalSignature(boolean[] ba) {
		if (ba != null) {
			return ba[DIGITAL_SIGNATURE] && !ba[KEY_CERT_SIGN] && !ba[CRL_SIGN];
		} else {
			return false;
		}
	}

	private void setupAdapter() {
	
		IAdapterFactory pr = new IAdapterFactory() {
	        @Override
	        public Class[] getAdapterList() {
	                return new Class[] { SSLContext.class };
	        }
	        
			@Override
			public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
					IResource res = (IResource) adaptableObject;
					SSLContext v = null;
					QualifiedName key = new QualifiedName("org.eclipse.core.pki", "context");
					try {
						v = (SSLContext) res.getSessionProperty(key);
						if (v == null) {
							v = getSSLContext();
							res.setSessionProperty(key, v);
						}
					} catch (CoreException e) {
						// unable to access session property - ignore
					}
					return (T)v;
			}
		};
		Platform.getAdapterManager().registerAdapters(pr,IResource.class);
	}

/**
 * @see AuthenticationPlugin#setSystemProperties()
 */

/*
 * private void installTrustStore() { final String USER_HOME =
 * System.getProperty("user.home"); //$NON-NLS-1$ final File PKI_ECLIPSE_DIR =
 * new File(USER_HOME, ".eclipse_pki"); //$NON-NLS-1$ final String PKI_DIR =
 * "eclipse_pki"; //$NON-NLS-1$ //final Path pkiHome =
 * Paths.get(PKI_ECLIPSE_DIR.getAbsolutePath() + File.separator + PKI_DIR);
 *
 * final Path pkiHome = Paths.get(PKI_ECLIPSE_DIR.getAbsolutePath() +
 * File.separator + PKI_DIR); createSigintEclipseDir(pkiHome);
 *
 * TODO: Create an enum of the IC comms and utilize the correct trust
 *
 *
 * String filename = "cacert"; //$NON-NLS-1$ File localTrustStore = new
 * File(PKI_ECLIPSE_DIR, filename);
 *
 * // System.out.println("Install Truststore - local -> " + localTrustStore);
 *
 * // // we want to install the new one anyway // //
 * if(!localTrustStore.exists()) { FileChannel fc = null; ReadableByteChannel
 * rbc = null; FileOutputStream os = null; try {
 * localTrustStore.createNewFile(); os = new FileOutputStream(localTrustStore);
 * fc = os.getChannel();
 *
 *
 * // //File ConfigurationFile = new
 * File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString() +
 * File.separator // + "configuration" + File.separator // + "cacert");
 *
 * File ConfigurationFile = new File(File.separator + "configuration" +
 * File.separator //$NON-NLS-1$ + "cacert"); //$NON-NLS-1$ InputStream is = new
 * FileInputStream(ConfigurationFile);
 *
 * // // copy the contents of the eclipse/ cacerts file to our .pki directory //
 * rbc = Channels.newChannel(is); ByteBuffer buffer = ByteBuffer.allocate(1024);
 * buffer.clear(); while (rbc.read(buffer) != -1) { buffer.flip();
 * fc.write(buffer); buffer.compact(); }
 *
 * } catch (IOException e) {
 * //LogUtil.logError("Issue writing default trust store to disk.", e); } // }
 * //AuthenticationPlugin.getDefault().getPreferenceStore().setValue(
 * AuthenticationPreferences.TRUST_STORE_LOCATION, //
 * localTrustStore.getAbsolutePath());
 *
 * }
 */

/*
 * private void createSigintEclipseDir(Path pkiHome) { Lock fsLock = new
 * ReentrantLock(); fsLock.lock(); try { if (Files.notExists(pkiHome)) {
 * Files.createDirectories(pkiHome); } } catch (IOException e) { // TODO
 * Auto-generated catch block e.printStackTrace(); } finally { fsLock.unlock();
 * } }
 */
}