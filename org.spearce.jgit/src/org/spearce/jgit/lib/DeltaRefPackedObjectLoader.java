package org.spearce.jgit.lib;

import java.io.IOException;

import org.spearce.jgit.errors.MissingObjectException;

/** Reads a deltified object which uses an {@link ObjectId} to find its base. */
class DeltaRefPackedObjectLoader extends DeltaPackedObjectLoader {
	private final ObjectId deltaBase;

	DeltaRefPackedObjectLoader(final PackFile pr, final long offset,
			final int deltaSz, final ObjectId base) {
		super(pr, offset, deltaSz);
		deltaBase = base;
	}

	protected PackedObjectLoader getBaseLoader() throws IOException {
		final PackedObjectLoader or = pack.get(deltaBase);
		if (or == null)
			throw new MissingObjectException(deltaBase, "delta base");
		return or;
	}
}
