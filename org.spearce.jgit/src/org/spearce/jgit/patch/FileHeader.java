import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

	/** The hunks of this file */
	private List<HunkHeader> hunks;

	/** @return hunks altering this file; in order of appearance in patch */
	public List<HunkHeader> getHunks() {
		if (hunks == null)
			return Collections.emptyList();
		return hunks;
	}

	void addHunk(final HunkHeader h) {
		if (h.getFileHeader() != this)
			throw new IllegalArgumentException("Hunk belongs to another file");
		if (hunks == null)
			hunks = new ArrayList<HunkHeader>();
		hunks.add(h);
	}
