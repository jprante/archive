package org.xbib.io.archive.zip;

import org.xbib.io.archive.entry.ArchiveEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipException;

/**
 * Extension that adds better handling of extra fields and provides
 * access to the internal and external file attributes.
 * The extra data is expected to follow the recommendation of
 * <a href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">APPNOTE.txt</a>
 * <ul>
 * <li>the extra byte array consists of a sequence of extra fields</li>
 * <li>each extra fields starts by a two byte header id followed by
 * a two byte sequence holding the length of the remainder of
 * data.</li>
 * </ul>
 * Any extra data that cannot be parsed by the rules above will be
 * consumed as "unparseable" extra data and treated differently by the
 * methods of this class.
 */
public class ZipArchiveEntry extends java.util.zip.ZipEntry implements ArchiveEntry {

    public static final int PLATFORM_UNIX = 3;

    public static final int PLATFORM_FAT = 0;

    private static final int SHORT_MASK = 0xFFFF;

    private static final int SHORT_SHIFT = 16;

    /**
     * The {@link java.util.zip.ZipEntry} base class only supports
     * the compression methods STORED and DEFLATED. We override the
     * field so that any compression methods can be used.
     * The default value -1 means that the method has not been specified.
     * <a href="https://issues.apache.org/jira/browse/COMPRESS-93">COMPRESS-93</a>
     */
    private int method = -1;

    /**
     * The {@link java.util.zip.ZipEntry#setSize} method in the base
     * class throws an IllegalArgumentException if the size is bigger
     * than 2GB for Java versions < 7.  Need to keep our own size
     * information for Zip64 support.
     */
    private long size = ArchiveEntry.SIZE_UNKNOWN;

    private int internalAttributes = 0;

    private int platform = PLATFORM_FAT;

    private long externalAttributes = 0;

    private LinkedHashMap<ZipShort, ZipExtraField> extraFields = null;

    private UnparseableExtraFieldData unparseableExtra = null;

    private String name = null;

    private byte[] rawName = null;

    private GeneralPurposeBit gpb = new GeneralPurposeBit();

    public ZipArchiveEntry() {
        this("");
    }

    /**
     * Creates a new zip entry with the specified name.
     * Assumes the entry represents a directory if and only if the
     * name ends with a forward slash "/".
     *
     * @param name the name of the entry
     */
    public ZipArchiveEntry(String name) {
        super(name);
        setName(name);
    }

    /**
     * Creates a new zip entry with fields taken from the specified zip entry.
     * Assumes the entry represents a directory if and only if the
     * name ends with a forward slash "/".
     *
     * @param entry the entry to get fields from
     * @throws java.util.zip.ZipException on error
     */
    public ZipArchiveEntry(java.util.zip.ZipEntry entry) throws ZipException {
        super(entry);
        setName(entry.getName());
        byte[] extra = entry.getExtra();
        if (extra != null) {
            setExtraFields(ExtraFieldUtils.parse(extra, true,
                    ExtraFieldUtils
                            .UnparseableExtraField.READ));
        } else {
            // initializes extra data to an empty byte array
            setExtra();
        }
        setMethod(entry.getMethod());
        setEntrySize(entry.getSize());
    }

    /**
     * Creates a new zip entry with fields taken from the specified zip entry.
     * Assumes the entry represents a directory if and only if the
     * name ends with a forward slash "/".
     *
     * @param entry the entry to get fields from
     * @throws java.util.zip.ZipException on error
     */
    public ZipArchiveEntry(ZipArchiveEntry entry) throws ZipException {
        this((java.util.zip.ZipEntry) entry);
        setInternalAttributes(entry.getInternalAttributes());
        setExternalAttributes(entry.getExternalAttributes());
        setExtraFields(entry.getExtraFields(true));
    }

    /**
     * Creates a new zip entry taking some information from the given
     * file and using the provided name.
     * The name will be adjusted to end with a forward slash "/" if
     * the file is a directory.  If the file is not a directory a
     * potential trailing forward slash will be stripped from the
     * entry name.
     */
    public ZipArchiveEntry(File inputFile, String entryName) {
        this(inputFile.isDirectory() && !entryName.endsWith("/") ?
                entryName + "/" : entryName);
        if (inputFile.isFile()) {
            setSize(inputFile.length());
        }
        setTime(inputFile.lastModified());
        // TODO are there any other fields we can set here?
    }

    /**
     * Overwrite clone.
     *
     * @return a cloned copy of this ZipArchiveEntry
     */
    @Override
    public Object clone() {
        ZipArchiveEntry e = (ZipArchiveEntry) super.clone();
        e.setInternalAttributes(getInternalAttributes());
        e.setExternalAttributes(getExternalAttributes());
        e.setExtraFields(getExtraFields(true));
        return e;
    }

    /**
     * Returns the compression method of this entry, or -1 if the
     * compression method has not been specified.
     *
     * @return compression method
     */
    @Override
    public int getMethod() {
        return method;
    }

    /**
     * Sets the compression method of this entry.
     *
     * @param method compression method
     */
    @Override
    public void setMethod(int method) {
        if (method < 0) {
            throw new IllegalArgumentException(
                    "ZIP compression method can not be negative: " + method);
        }
        this.method = method;
    }

    /**
     * Retrieves the internal file attributes.
     *
     * @return the internal file attributes
     */
    public int getInternalAttributes() {
        return internalAttributes;
    }

    /**
     * Sets the internal file attributes.
     *
     * @param value an <code>int</code> value
     */
    public void setInternalAttributes(int value) {
        internalAttributes = value;
    }

    /**
     * Retrieves the external file attributes.
     *
     * @return the external file attributes
     */
    public long getExternalAttributes() {
        return externalAttributes;
    }

    /**
     * Sets the external file attributes.
     *
     * @param value an <code>long</code> value
     */
    public void setExternalAttributes(long value) {
        externalAttributes = value;
    }

    /**
     * Sets Unix permissions in a way that is understood by Info-Zip's
     * unzip command.
     *
     * @param mode an <code>int</code> value
     */
    public void setUnixMode(int mode) {
        // CheckStyle:MagicNumberCheck OFF - no point
        setExternalAttributes((mode << SHORT_SHIFT)
                // MS-DOS read-only attribute
                | ((mode & 0200) == 0 ? 1 : 0)
                // MS-DOS directory flag
                | (isDirectory() ? 0x10 : 0));
        // CheckStyle:MagicNumberCheck ON
        platform = PLATFORM_UNIX;
    }

    /**
     * Unix permission.
     *
     * @return the unix permissions
     */
    public int getUnixMode() {
        return platform != PLATFORM_UNIX ? 0 :
                (int) ((getExternalAttributes() >> SHORT_SHIFT) & SHORT_MASK);
    }

    /**
     * Platform specification to put into the &quot;version made
     * by&quot; part of the central file header.
     *
     * @return PLATFORM_FAT unless {@link #setUnixMode setUnixMode}
     * has been called, in which case PLATORM_UNIX will be returned.
     */
    public int getPlatform() {
        return platform;
    }

    /**
     * Set the platform (UNIX or FAT).
     *
     * @param platform an <code>int</code> value - 0 is FAT, 3 is UNIX
     */
    protected void setPlatform(int platform) {
        this.platform = platform;
    }

    /**
     * Replaces all currently attached extra fields with the new array.
     *
     * @param fields an array of extra fields
     */
    public void setExtraFields(ZipExtraField[] fields) {
        extraFields = new LinkedHashMap<ZipShort, ZipExtraField>();
        for (ZipExtraField field : fields) {
            if (field instanceof UnparseableExtraFieldData) {
                unparseableExtra = (UnparseableExtraFieldData) field;
            } else {
                extraFields.put(field.getHeaderId(), field);
            }
        }
        setExtra();
    }

    /**
     * Retrieves all extra fields that have been parsed successfully.
     *
     * @return an array of the extra fields
     */
    public ZipExtraField[] getExtraFields() {
        return getExtraFields(false);
    }

    /**
     * Retrieves extra fields.
     *
     * @param includeUnparseable whether to also return unparseable
     *                           extra fields as {@link UnparseableExtraFieldData} if such data
     *                           exists.
     * @return an array of the extra fields
     */
    public ZipExtraField[] getExtraFields(boolean includeUnparseable) {
        if (extraFields == null) {
            return !includeUnparseable || unparseableExtra == null
                    ? new ZipExtraField[0]
                    : new ZipExtraField[]{unparseableExtra};
        }
        List<ZipExtraField> result =
                new ArrayList<ZipExtraField>(extraFields.values());
        if (includeUnparseable && unparseableExtra != null) {
            result.add(unparseableExtra);
        }
        return result.toArray(new ZipExtraField[0]);
    }

    /**
     * Adds an extra field - replacing an already present extra field
     * of the same type.
     * If no extra field of the same type exists, the field will be
     * added as last field.
     *
     * @param ze an extra field
     */
    public void addExtraField(ZipExtraField ze) {
        if (ze instanceof UnparseableExtraFieldData) {
            unparseableExtra = (UnparseableExtraFieldData) ze;
        } else {
            if (extraFields == null) {
                extraFields = new LinkedHashMap<ZipShort, ZipExtraField>();
            }
            extraFields.put(ze.getHeaderId(), ze);
        }
        setExtra();
    }

    /**
     * Adds an extra field - replacing an already present extra field
     * of the same type.
     * The new extra field will be the first one.
     *
     * @param ze an extra field
     */
    public void addAsFirstExtraField(ZipExtraField ze) {
        if (ze instanceof UnparseableExtraFieldData) {
            unparseableExtra = (UnparseableExtraFieldData) ze;
        } else {
            LinkedHashMap<ZipShort, ZipExtraField> copy = extraFields;
            extraFields = new LinkedHashMap<ZipShort, ZipExtraField>();
            extraFields.put(ze.getHeaderId(), ze);
            if (copy != null) {
                copy.remove(ze.getHeaderId());
                extraFields.putAll(copy);
            }
        }
        setExtra();
    }

    /**
     * Remove an extra field.
     *
     * @param type the type of extra field to remove
     */
    public void removeExtraField(ZipShort type) {
        if (extraFields == null) {
            throw new java.util.NoSuchElementException();
        }
        if (extraFields.remove(type) == null) {
            throw new java.util.NoSuchElementException();
        }
        setExtra();
    }

    /**
     * Removes unparseable extra field data.
     */
    public void removeUnparseableExtraFieldData() {
        if (unparseableExtra == null) {
            throw new java.util.NoSuchElementException();
        }
        unparseableExtra = null;
        setExtra();
    }

    /**
     * Looks up an extra field by its header id.
     *
     * @return null if no such field exists.
     */
    public ZipExtraField getExtraField(ZipShort type) {
        if (extraFields != null) {
            return extraFields.get(type);
        }
        return null;
    }

    /**
     * Looks up extra field data that couldn't be parsed correctly.
     *
     * @return null if no such field exists.
     */
    public UnparseableExtraFieldData getUnparseableExtraFieldData() {
        return unparseableExtra;
    }

    /**
     * Parses the given bytes as extra field data and consumes any
     * unparseable data as an {@link UnparseableExtraFieldData}
     * instance.
     *
     * @param extra an array of bytes to be parsed into extra fields
     * @throws RuntimeException if the bytes cannot be parsed
     * @throws RuntimeException on error
     */
    @Override
    public void setExtra(byte[] extra) throws RuntimeException {
        try {
            ZipExtraField[] local =
                    ExtraFieldUtils.parse(extra, true,
                            ExtraFieldUtils.UnparseableExtraField.READ);
            mergeExtraFields(local, true);
        } catch (ZipException e) {
            throw new RuntimeException("Error parsing extra fields for entry: "
                    + getName() + " - " + e.getMessage(), e);
        }
    }

    /**
     * Unfortunately {@link java.util.zip.ZipOutputStream
     * java.util.zip.ZipOutputStream} seems to access the extra data
     * directly, so overriding getExtra doesn't help - we need to
     * modify super's data directly.
     */
    protected void setExtra() {
        super.setExtra(ExtraFieldUtils.mergeLocalFileDataData(getExtraFields(true)));
    }

    /**
     * Sets the central directory part of extra fields.
     */
    public void setCentralDirectoryExtra(byte[] b) {
        try {
            ZipExtraField[] central =
                    ExtraFieldUtils.parse(b, false,
                            ExtraFieldUtils.UnparseableExtraField.READ);
            mergeExtraFields(central, false);
        } catch (ZipException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves the extra data for the local file data.
     *
     * @return the extra data for local file
     */
    public byte[] getLocalFileDataExtra() {
        byte[] extra = getExtra();
        return extra != null ? extra : new byte[0];
    }

    /**
     * Retrieves the extra data for the central directory.
     *
     * @return the central directory extra data
     */
    public byte[] getCentralDirectoryExtra() {
        return ExtraFieldUtils.mergeCentralDirectoryData(getExtraFields(true));
    }

    /**
     * Get the name of the entry.
     *
     * @return the entry name
     */
    @Override
    public String getName() {
        return name == null ? super.getName() : name;
    }

    /**
     * Is this entry a directory?
     *
     * @return true if the entry is a directory
     */
    @Override
    public boolean isDirectory() {
        return getName().endsWith("/");
    }

    /**
     * Set the name of the entry.
     *
     * @param name the name to use
     */
    public ZipArchiveEntry setName(String name) {
        if (name != null && getPlatform() == PLATFORM_FAT
                && name.indexOf("/") == -1) {
            name = name.replace('\\', '/');
        }
        this.name = name;
        return this;
    }

    /**
     * Gets the uncompressed size of the entry data.
     *
     * @return the entry size
     */
    @Override
    public long getEntrySize() {
        return size;
    }

    /**
     * Sets the uncompressed size of the entry data.
     *
     * @param size the uncompressed size in bytes
     * @throws IllegalArgumentException if the specified size is less
     *                                  than 0
     */
    @Override
    public ZipArchiveEntry setEntrySize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("invalid entry size");
        }
        this.size = size;
        return this;
    }

    /**
     * Sets the name using the raw bytes and the string created from
     * it by guessing or using the configured encoding.
     *
     * @param name    the name to use created from the raw bytes using
     *                the guessed or configured encoding
     * @param rawName the bytes originally read as name from the
     *                archive
     */
    protected void setName(String name, byte[] rawName) {
        setName(name);
        this.rawName = rawName;
    }

    /**
     * Returns the raw bytes that made up the name before it has been
     * converted using the configured or guessed encoding.
     * This method will return null if this instance has not been
     * read from an archive.
     */
    public byte[] getRawName() {
        if (rawName != null) {
            byte[] b = new byte[rawName.length];
            System.arraycopy(rawName, 0, b, 0, rawName.length);
            return b;
        }
        return null;
    }

    /**
     * Get the hashCode of the entry.
     * This uses the name as the hashcode.
     *
     * @return a hashcode.
     */
    @Override
    public int hashCode() {
        // this method has severe consequences on performance. We cannot rely
        // on the super.hashCode() method since super.getName() always return
        // the empty string in the current implemention (there's no setter)
        // so it is basically draining the performance of a hashmap lookup
        return getName().hashCode();
    }

    /**
     * The "general purpose bit" field.
     */
    public GeneralPurposeBit getGeneralPurposeBit() {
        return gpb;
    }

    /**
     * The "general purpose bit" field.
     */
    public void setGeneralPurposeBit(GeneralPurposeBit b) {
        gpb = b;
    }

    /**
     * If there are no extra fields, use the given fields as new extra
     * data - otherwise merge the fields assuming the existing fields
     * and the new fields stem from different locations inside the
     * archive.
     *
     * @param f     the extra fields to merge
     * @param local whether the new fields originate from local data
     */
    private void mergeExtraFields(ZipExtraField[] f, boolean local)
            throws ZipException {
        if (extraFields == null) {
            setExtraFields(f);
        } else {
            for (ZipExtraField element : f) {
                ZipExtraField existing;
                if (element instanceof UnparseableExtraFieldData) {
                    existing = unparseableExtra;
                } else {
                    existing = getExtraField(element.getHeaderId());
                }
                if (existing == null) {
                    addExtraField(element);
                } else {
                    if (local) {
                        byte[] b = element.getLocalFileDataData();
                        existing.parseFromLocalFileData(b, 0, b.length);
                    } else {
                        byte[] b = element.getCentralDirectoryData();
                        existing.parseFromCentralDirectoryData(b, 0, b.length);
                    }
                }
            }
            setExtra();
        }
    }

    public ZipArchiveEntry setLastModified(Date date) {
        setTime(date.getTime());
        return this;
    }

    public Date getLastModified() {
        return new Date(getTime());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ZipArchiveEntry other = (ZipArchiveEntry) obj;
        String myName = getName();
        String otherName = other.getName();
        if (myName == null) {
            if (otherName != null) {
                return false;
            }
        } else if (!myName.equals(otherName)) {
            return false;
        }
        String myComment = getComment();
        String otherComment = other.getComment();
        if (myComment == null) {
            if (otherComment != null) {
                return false;
            }
        } else if (!myComment.equals(otherComment)) {
            return false;
        }
        return getTime() == other.getTime()
                && getInternalAttributes() == other.getInternalAttributes()
                && getPlatform() == other.getPlatform()
                && getExternalAttributes() == other.getExternalAttributes()
                && getMethod() == other.getMethod()
                && getEntrySize() == other.getEntrySize()
                && getCrc() == other.getCrc()
                && getCompressedSize() == other.getCompressedSize()
                && Arrays.equals(getCentralDirectoryExtra(),
                other.getCentralDirectoryExtra())
                && Arrays.equals(getLocalFileDataExtra(),
                other.getLocalFileDataExtra())
                && gpb.equals(other.gpb);
    }
}
