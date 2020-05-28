package org.xbib.io.archive.entry;

import java.util.Date;

/**
 * Represents an entry of an archive.
 */
public interface ArchiveEntry {

    /**
     * Special value indicating that the size is unknown
     */
    long SIZE_UNKNOWN = -1;

    ArchiveEntry setName(String name);

    /**
     * The name of the entry in the archive. May refer to a file or directory or other item
     */
    String getName();

    /**
     * Set the entry size in bytes
     */
    ArchiveEntry setEntrySize(long size);

    /**
     * The size of the entry. May be -1 (SIZE_UNKNOWN) if the size is unknown
     */
    long getEntrySize();

    ArchiveEntry setLastModified(Date date);

    /**
     * The last modified date of the entry.
     */
    Date getLastModified();

    /**
     * True if the entry refers to a directory
     */
    boolean isDirectory();

}
