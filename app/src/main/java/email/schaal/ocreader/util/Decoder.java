package email.schaal.ocreader.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.io.CountingInputStream;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;

/**
 * Created by daniel on 15.03.17.
 */
class Decoder {
    private final static String TAG = Decoder.class.getName();

    private final static short RESERVED = 0;
    private final static short TYPE = 1;
    private final static short HEADER_SIZE = 6;
    private final static short ICONENTRY_SIZE = 16;

    @Nullable
    public static Bitmap decode(File file) throws IOException {
        return decode(new FileInputStream(file));
    }

    @Nullable
    static Bitmap decode(InputStream inputStream) throws IOException {
        InputStream markable = inputStream.markSupported() ? inputStream : new BufferedInputStream(inputStream, 128);
        CountingInputStream countingInputStream = new CountingInputStream(markable);
        LittleEndianDataInputStream in = new LittleEndianDataInputStream(countingInputStream);

        //noinspection TryFinallyCanBeTryWithResources
        try {
            in.mark(16);
            if (in.readShort() != RESERVED | in.readShort() != TYPE) {
                // Not in ico format, try to decode using BitmapFactory
                in.reset();
                return BitmapFactory.decodeStream(in);
            }

            short imageCount = in.readShort();
            if (imageCount <= 0)
                throw new IOException("no images");

            IconEntry[] entries = new IconEntry[imageCount];

            for (short s = 0; s < imageCount; s++)
                entries[s] = new IconEntry(in);

            Arrays.sort(entries);

            IconEntry biggest = entries[entries.length - 1];

            IconEntry biggestAdjustedOffset = new IconEntry(biggest, HEADER_SIZE + ICONENTRY_SIZE);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(biggest.size + HEADER_SIZE + ICONENTRY_SIZE);
            LittleEndianDataOutputStream iconOutputStream = new LittleEndianDataOutputStream(byteArrayOutputStream);
            iconOutputStream.writeShort(RESERVED);
            iconOutputStream.writeShort(TYPE);
            iconOutputStream.writeShort(1); // number of IconEntries

            biggestAdjustedOffset.write(iconOutputStream);

            int bytesToSkip = biggest.offset - (int)countingInputStream.getCount();
            while(bytesToSkip > 0) {
                final int bytesSkipped = in.skipBytes(bytesToSkip);
                if(bytesSkipped == 0)
                    throw new EOFException("Unexpected EOF");
                bytesToSkip -= bytesSkipped;
            }

            for (int i = 0; i < biggest.size; i++)
                iconOutputStream.write(in.read());

            return BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(), 0, byteArrayOutputStream.size());
        } finally {
            in.close();
        }
    }

    static class IconEntry implements Comparable<IconEntry> {
        private final static String TAG  = IconEntry.class.getName();

        private final int width, height;
        private final int colors;
        private final byte reserved;
        private final short colorPlanes;
        private final short bitsPerPixel;
        private final int size;
        private final int offset;

        IconEntry(IconEntry i, int offset) {
            this.width = i.width;
            this.height = i.height;
            this.colors = i.colors;
            this.reserved = i.reserved;
            this.colorPlanes = i.colorPlanes;
            this.bitsPerPixel = i.bitsPerPixel;
            this.size = i.size;
            this.offset = offset;
        }

        IconEntry(LittleEndianDataInputStream in) throws IOException {
            width = in.readUnsignedByte();
            height = in.readUnsignedByte();
            colors = in.readUnsignedByte();
            reserved = in.readByte();
            colorPlanes = in.readShort();
            bitsPerPixel = in.readShort();
            size = in.readInt();
            offset = in.readInt();
        }

        void write(DataOutput out) throws IOException {
            out.writeByte(width);
            out.writeByte(height);
            out.writeByte(colors);
            out.writeByte(reserved);
            out.writeShort(colorPlanes);
            out.writeShort(bitsPerPixel);
            out.writeInt(size);
            out.writeInt(offset);
        }

        @Override
        public String toString() {
            return String.format(
                    Locale.US,
                    "IconEntry{width=%d, height=%d, colors=%d, reserved=%s, colorPlanes=%s, bitsPerPixel=%s, size=%d, offset=%d}",
                    width, height, colors, reserved, colorPlanes, bitsPerPixel, size, offset);
        }

        @Override
        public int compareTo(@NonNull IconEntry o) {
            int widthCompare = Integer.valueOf(width).compareTo(o.width);

            if(widthCompare == 0)
                return Integer.valueOf(colors).compareTo(o.colors);

            return widthCompare;
        }
    }
}
