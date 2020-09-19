package com.stirante.runechanger.util;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Pipe {

    private InputStream in;
    private Map<String, Object> metadata;

    public Pipe(InputStream in, Map<String, Object> metadata) {
        this.in = in;
        this.metadata = metadata;
    }

    public Pipe(InputStream in) {
        this(in, new HashMap<>());
    }

    public void to(OutputStream out) {
        to(out, true);
    }

    public void to(OutputStream out, boolean close) {
        try {
            pipe(in, out);
            in.close();
            if (close) {
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void to(File out, boolean close) {
        try {
            to(new FileOutputStream(out), close);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void to(Writer out, boolean close) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            to(byteArrayOutputStream, close);
            out.write(byteArrayOutputStream.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void to(Writer out) {
        to(out, true);
    }

    public void to(File out) {
        to(out, true);
    }

    public Pipe through(Function<byte[], byte[]> processor) {
        return through((bytes, stringObjectMap) -> processor.apply(bytes));
    }

    public Pipe through(BiFunction<byte[], Map<String, Object>, byte[]> processor) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pipe(in, baos);
            in.close();
            in = new ByteArrayInputStream(processor.apply(baos.toByteArray(), metadata));
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Pipe[] split(BiFunction<byte[], Map<String, Object>, byte[][]> processor) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pipe(in, baos);
            in.close();
            Map<String, Object> metadata = new HashMap<>(this.metadata);
            byte[][] bytes = processor.apply(baos.toByteArray(), metadata);
            Pipe[] result = new Pipe[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                byte[] b = bytes[i];
                result[i] = new Pipe(new ByteArrayInputStream(b), metadata);
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Pipe[] split(Function<byte[], byte[][]> processor) {
        return split((bytes, stringObjectMap) -> processor.apply(bytes));
    }

    public static Pipe from(InputStream in) {
        return new Pipe(in);
    }

    public static Pipe from(String in) {
        return from(new ByteArrayInputStream(in.getBytes()));
    }

    public static Pipe from(File in) {
        try {
            return from(new FileInputStream(in));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Pipe from(byte[] in) {
        return from(new ByteArrayInputStream(in));
    }

    private static void pipe(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

}
