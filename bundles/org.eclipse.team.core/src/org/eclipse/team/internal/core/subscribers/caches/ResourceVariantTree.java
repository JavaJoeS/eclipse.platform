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
package org.eclipse.team.internal.core.subscribers.caches;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.IResourceVariant;

/**
 * Provides caching for a {@link AbstractResourceVariantTree} using a
 * {@link ResourceVariantByteStore}.
 * 
 * @see IResourceVariantTree
 * @see AbstractResourceVariantTree
 * @see ResourceVariantByteStore
 * @since 3.0
 */
public abstract class ResourceVariantTree extends AbstractResourceVariantTree {
	
	private ResourceVariantByteStore store;

	protected ResourceVariantTree(ResourceVariantByteStore store) {
		this.store = store;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.subscribers.caches.IResourceVariantTree#members(org.eclipse.core.resources.IResource)
	 */
	public IResource[] members(IResource resource) throws TeamException {
		return getByteStore().members(resource);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.subscribers.caches.IResourceVariantTree#hasResourceVariant(org.eclipse.core.resources.IResource)
	 */
	public boolean hasResourceVariant(IResource resource) throws TeamException {
		return getByteStore().getBytes(resource) != null;
	}
	
	/**
	 * Flush any variants for the given resource to the depth specified.
	 * @param resource the local resource
	 * @param depth the depth of the flush
	 * @throws TeamException
	 */
	public void flushVariants(IResource resource, int depth) throws TeamException {
		getByteStore().flushBytes(resource, depth);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.subscribers.caches.AbstractResourceVariantTree#setVariant(org.eclipse.core.resources.IResource, org.eclipse.team.core.synchronize.IResourceVariant)
	 */
	protected boolean setVariant(IResource local, IResourceVariant remote) throws TeamException {
		ResourceVariantByteStore cache = getByteStore();
		byte[] newRemoteBytes = getBytes(local, remote);
		boolean changed;
		if (newRemoteBytes == null) {
			changed = cache.deleteBytes(local);
		} else {
			changed = cache.setBytes(local, newRemoteBytes);
		}
		return changed;
	}
	

	
	/**
	 * Get the byte store that is used to cache the serialization bytes
	 * for the resource variants of this tree. A byte store is used
	 * to reduce the memory footprint of the tree.
	 * @return the resource variant tree that is being refreshed.
	 */
	protected ResourceVariantByteStore getByteStore() {
		return store;
	}
	
	/**
	 * Get the bytes to be stored in the <code>ResourceVariantByteStore</code> 
	 * from the given resource variant.
	 * @param local the local resource
	 * @param remote the corresponding resource variant handle
	 * @return the bytes for the resource variant.
	 */
	protected abstract byte[] getBytes(IResource local, IResourceVariant remote) throws TeamException;
}
