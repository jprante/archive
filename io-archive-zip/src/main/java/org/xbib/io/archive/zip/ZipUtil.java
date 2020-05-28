package org.xbib.io.archive.zip;

import org.xbib.io.archive.entry.ArchiveEntryEncodingHelper;

import java.io.IOException;
import java.util.Calendar;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

/**
 * Utility class for handling DOS and Java time conversions.
 */
public abstract class ZipUtil {
    /**
     * Smallest date/time ZIP can handle.
     */
    private static final byte[] DOS_TIME_MIN = ZipLong.getBytes(0x00002100L);

    /**
     * Convert a Date object to a DOS date/time field.
     * Stolen from InfoZip's <code>fileio.c</code>
     *
     * @param t number of milliseconds since the epoch
     * @return the date as a byte array
     */
    public static byte[] toDosTime(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);

        int year = c.get(Calendar.YEAR);
        if (year < 1980) {
            return copy(DOS_TIME_MIN); // stop callers from changing the array
        }
        int month = c.get(Calendar.MONTH) + 1;
        long value = ((year - 1980) << 25)
                | (month << 21)
                | (c.get(Calendar.DAY_OF_MONTH) << 16)
                | (c.get(Calendar.HOUR_OF_DAY) << 11)
                | (c.get(Calendar.MINUTE) << 5)
                | (c.get(Calendar.SECOND) >> 1);
        return ZipLong.getBytes(value);
    }


    /**
     * Converts DOS time to Java time (number of milliseconds since
     * epoch).
     */
    public static long dosToJavaTime(long dosTime) {
        Calendar cal = Calendar.getInstance();
        // CheckStyle:MagicNumberCheck OFF - no point
        cal.set(Calendar.YEAR, (int) ((dosTime >> 25) & 0x7f) + 1980);
        cal.set(Calendar.MONTH, (int) ((dosTime >> 21) & 0x0f) - 1);
        cal.set(Calendar.DATE, (int) (dosTime >> 16) & 0x1f);
        cal.set(Calendar.HOUR_OF_DAY, (int) (dosTime >> 11) & 0x1f);
        cal.set(Calendar.MINUTE, (int) (dosTime >> 5) & 0x3f);
        cal.set(Calendar.SECOND, (int) (dosTime << 1) & 0x3e);
        // CheckStyle:MagicNumberCheck ON
        return cal.getTime().getTime();
    }

    /**
     * If the entry has Unicode*ExtraFields and the CRCs of the
     * names/comments match those of the extra fields, transfer the
     * known Unicode values from the extra field.
     */
    static void setNameAndCommentFromExtraFields(ZipArchiveEntry ze,
                                                 byte[] originalNameBytes,
                                                 byte[] commentBytes) {
        UnicodePathExtraField name = (UnicodePathExtraField)
                ze.getExtraField(UnicodePathExtraField.UPATH_ID);
        String originalName = ze.getName();
        String newName = getUnicodeStringIfOriginalMatches(name,
                originalNameBytes);
        if (newName != null && !originalName.equals(newName)) {
            ze.setName(newName);
        }

        if (commentBytes != null && commentBytes.length > 0) {
            UnicodeCommentExtraField cmt = (UnicodeCommentExtraField)
                    ze.getExtraField(UnicodeCommentExtraField.UCOM_ID);
            String newComment =
                    getUnicodeStringIfOriginalMatches(cmt, commentBytes);
            if (newComment != null) {
                ze.setComment(newComment);
            }
        }
    }

    /**
     * If the stored CRC matches the one of the given name, return the
     * Unicode name of the given field.
     * If the field is null or the CRCs don't match, return null
     * instead.
     */
    private static String getUnicodeStringIfOriginalMatches(AbstractUnicodeExtraField f,
                                                            byte[] orig) {
        if (f != null) {
            CRC32 crc32 = new CRC32();
            crc32.update(orig);
            long origCRC32 = crc32.getValue();

            if (origCRC32 == f.getNameCRC32()) {
                try {
                    return ArchiveEntryEncodingHelper.UTF8_ENCODING.decode(f.getUnicodeName());
                } catch (IOException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Create a copy of the given array - or return null if the
     * argument is null.
     */
    static byte[] copy(byte[] from) {
        if (from != null) {
            byte[] to = new byte[from.length];
            System.arraycopy(from, 0, to, 0, to.length);
            return to;
        }
        return null;
    }

    /**
     * Whether this library is able to read or write the given entry.
     */
    static boolean canHandleEntryData(ZipArchiveEntry entry) {
        return supportsEncryptionOf(entry) && supportsMethodOf(entry);
    }

    /**
     * Whether this library supports the encryption used by the given
     * entry.
     *
     * @return true if the entry isn't encrypted at all
     */
    private static boolean supportsEncryptionOf(ZipArchiveEntry entry) {
        return !entry.getGeneralPurposeBit().usesEncryption();
    }

    /**
     * Whether this library supports the compression method used by
     * the given entry.
     *
     * @return true if the compression method is STORED or DEFLATED
     */
    private static boolean supportsMethodOf(ZipArchiveEntry entry) {
        return entry.getMethod() == ZipEntry.STORED
                || entry.getMethod() == ZipEntry.DEFLATED;
    }

    /**
     * Checks whether the entry requires features not (yet) supported
     * by the library and throws an exception if it does.
     */
    static void checkRequestedFeatures(ZipArchiveEntry ze)
            throws UnsupportedZipFeatureException {
        if (!supportsEncryptionOf(ze)) {
            throw
                    new UnsupportedZipFeatureException(UnsupportedZipFeatureException
                            .Feature.ENCRYPTION, ze);
        }
        if (!supportsMethodOf(ze)) {
            throw
                    new UnsupportedZipFeatureException(UnsupportedZipFeatureException
                            .Feature.METHOD, ze);
        }
    }
}