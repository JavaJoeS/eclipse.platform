/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.ISubscriberChangeEvent;
import org.eclipse.team.core.subscribers.ISubscriberChangeListener;
import org.eclipse.team.core.subscribers.SubscriberChangeEvent;
import org.eclipse.team.internal.ccvs.core.syncinfo.CVSResourceVariantTree;
import org.eclipse.team.internal.ccvs.core.syncinfo.MultiTagResourceVariantTree;
import org.eclipse.team.internal.core.subscribers.caches.ResourceVariantTree;
import org.eclipse.team.internal.core.subscribers.caches.SessionResourceVariantByteStore;

/**
 * This subscriber is used when comparing the local workspace with its
 * corresponding remote.
 */
public class CVSCompareSubscriber extends CVSSyncTreeSubscriber implements ISubscriberChangeListener {

	public static final String ID = "org.eclipse.team.cvs.ui.compare-participant";
	public static final String QUALIFIED_NAME = CVSProviderPlugin.ID + ".compare"; //$NON-NLS-1$
	private static final String UNIQUE_ID_PREFIX = "compare-"; //$NON-NLS-1$
	
	private IResource[] resources;
	private CVSResourceVariantTree tree;
	
	public CVSCompareSubscriber(IResource[] resources, CVSTag tag) {
		super(getUniqueId(), Policy.bind("CVSCompareSubscriber.2", tag.getName()), Policy.bind("CVSCompareSubscriber.3")); //$NON-NLS-1$ //$NON-NLS-2$
		this.resources = resources;
		tree = new CVSResourceVariantTree(new SessionResourceVariantByteStore(), tag, getCacheFileContentsHint());
		initialize();
	}
	
	public CVSCompareSubscriber(IProject[] projects, CVSTag[] tags, String name) {
		super(getUniqueId(), Policy.bind("CVSCompareSubscriber.2", name), Policy.bind("CVSCompareSubscriber.3")); //$NON-NLS-1$ //$NON-NLS-2$
		this.resources = projects;
		MultiTagResourceVariantTree multiTree = new MultiTagResourceVariantTree(new SessionResourceVariantByteStore(), getCacheFileContentsHint());
		for (int i = 0; i < tags.length; i++) {
			multiTree.addProject(projects[i], tags[i]);
		}
		initialize();
	}

	private void initialize() {
		CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().addListener(this);
	}

	public void dispose() {	
		CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().removeListener(this);	
		tree.dispose();	
	}
	
	private static QualifiedName getUniqueId() {
		String uniqueId = Long.toString(System.currentTimeMillis());
		return new QualifiedName(QUALIFIED_NAME, UNIQUE_ID_PREFIX + uniqueId); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSSyncTreeSubscriber#getBaseSynchronizationCache()
	 */
	protected ResourceVariantTree getBaseTree() {
		// No base cache needed since it's a two way compare
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSSyncTreeSubscriber#getRemoteSynchronizationCache()
	 */
	protected ResourceVariantTree getRemoteTree() {
		return tree;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.TeamSubscriber#isThreeWay()
	 */
	public boolean isThreeWay() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.TeamSubscriber#roots()
	 */
	public IResource[] roots() {
		return resources;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ITeamResourceChangeListener#teamResourceChanged(org.eclipse.team.core.subscribers.TeamDelta[])
	 */
	public void subscriberResourceChanged(ISubscriberChangeEvent[] deltas) {
		List outgoingDeltas = new ArrayList(deltas.length);
		for (int i = 0; i < deltas.length; i++) {
			ISubscriberChangeEvent delta = deltas[i];
			if ((delta.getFlags() & ISubscriberChangeEvent.ROOT_REMOVED) != 0) {
				IResource resource = delta.getResource();
				outgoingDeltas.addAll(Arrays.asList(handleRemovedRoot(resource)));
			} else if ((delta.getFlags() & ISubscriberChangeEvent.SYNC_CHANGED) != 0) {
				IResource resource = delta.getResource();
				try {
					if (isSupervised(resource)) {
						outgoingDeltas.add(new SubscriberChangeEvent(this, delta.getFlags(), resource));
					}
				} catch (TeamException e) {
					// Log and ignore
					CVSProviderPlugin.log(e);
				}
			}
		}
		
		fireTeamResourceChange((SubscriberChangeEvent[]) outgoingDeltas.toArray(new SubscriberChangeEvent[outgoingDeltas.size()]));
	}

	private SubscriberChangeEvent[] handleRemovedRoot(IResource removedRoot) {
		// Determine if any of the roots of the compare are affected
		List removals = new ArrayList(resources.length);
		for (int j = 0; j < resources.length; j++) {
			IResource root = resources[j];
			if (removedRoot.getFullPath().isPrefixOf(root.getFullPath())) {
				// The root is no longer managed by CVS
				removals.add(root);
				try {
					tree.removeRoot(root);
				} catch (TeamException e) {
					CVSProviderPlugin.log(e);
				}
			}
		}
		if (removals.isEmpty()) {
			return new SubscriberChangeEvent[0];
		}
		
		// Adjust the roots of the subscriber
		List newRoots = new ArrayList(resources.length);
		newRoots.addAll(Arrays.asList(resources));
		newRoots.removeAll(removals);
		resources = (IResource[]) newRoots.toArray(new IResource[newRoots.size()]);
		 
		// Create the deltas for the removals
		SubscriberChangeEvent[] deltas = new SubscriberChangeEvent[removals.size()];
		for (int i = 0; i < deltas.length; i++) {
			deltas[i] = new SubscriberChangeEvent(this, ISubscriberChangeEvent.ROOT_REMOVED, (IResource)removals.get(i));
		}
		return deltas;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.TeamSubscriber#isSupervised(org.eclipse.core.resources.IResource)
	 */
	public boolean isSupervised(IResource resource) throws TeamException {
		if (super.isSupervised(resource)) {
			if (!resource.exists() && !getRemoteTree().hasResourceVariant(resource)) {
				// Exclude conflicting deletions
				return false;
			}
			for (int i = 0; i < resources.length; i++) {
				IResource root = resources[i];
				if (root.getFullPath().isPrefixOf(resource.getFullPath())) {
					return true;
				}
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSSyncTreeSubscriber#getCacheFileContentsHint()
	 */
	protected boolean getCacheFileContentsHint() {
		return true;
	}
		
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if(this == other) return true;
		if(! (other instanceof CVSCompareSubscriber)) return false;
		CVSCompareSubscriber s = (CVSCompareSubscriber)other;
		return getRemoteTag().equals(s.getRemoteTag()) && rootsEqual(s);		
	}
}
