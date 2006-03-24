/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.filehistory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.core.mapping.ResourceVariantFileRevision;

public class CVSResourceVariantFileRevision extends ResourceVariantFileRevision {

	public CVSResourceVariantFileRevision(IResourceVariant variant) {
		super(variant);
	}

	public boolean isPropertyMissing() {
		return true;
	}
	
	public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException {
		return new CVSFileRevision(getCVSRemoteFile().getLogEntry(monitor));
	}

	private ICVSRemoteFile getCVSRemoteFile() {
		return (ICVSRemoteFile)getVariant();
	}

}
