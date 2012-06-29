package pl.com.it_crowd.cra.scanner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

public final class IOUtils {
// ------------------------------ FIELDS ------------------------------

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

// -------------------------- STATIC METHODS --------------------------

    public static int copy(Reader input, Writer output) throws IOException
    {
        long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

    public static void copy(InputStream input, StringWriter output) throws IOException
    {
        InputStreamReader in = new InputStreamReader(input);
        copy(in, output);
    }

    public static long copyLarge(Reader input, Writer output) throws IOException
    {
        char[] buffer = new char[DEFAULT_BUFFER_SIZE];
        long count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static String toString(InputStream input) throws IOException
    {
        StringWriter sw = new StringWriter();
        copy(input, sw);
        return sw.toString();
    }

    public static void write(String data, OutputStream output) throws IOException
    {
        if (data != null) {
            output.write(data.getBytes());
        }
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private IOUtils()
    {
    }
}
