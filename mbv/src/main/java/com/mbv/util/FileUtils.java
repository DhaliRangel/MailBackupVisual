package com.mbv.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.stream.Stream;

public class FileUtils {

    public static boolean isCompressedFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".zip") || name.endsWith(".7z") ||
                name.endsWith(".tar") || name.endsWith(".tar.gz") ||
                name.endsWith(".tgz") || name.endsWith(".gz");
    }

    public static boolean isArchiveFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".mbox") || name.endsWith(".mbx") ||
                name.endsWith(".eml") || name.endsWith(".pst") ||
                name.endsWith(".msg") || name.endsWith(".ost");
    }

    public static String detectFileType(Path path) throws IOException {
        if (Files.isDirectory(path)) return "DIRECTORY";

        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".mbox") || name.endsWith(".mbx")) return "MBOX";
        if (name.endsWith(".eml")) return "EML";
        if (name.endsWith(".pst")) return "PST";
        if (name.endsWith(".msg")) return "MSG";
        if (name.endsWith(".zip")) return "ZIP";
        if (name.endsWith(".7z")) return "7Z";
        if (name.endsWith(".tar")) return "TAR";
        if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) return "TAR_GZ";
        if (name.endsWith(".gz")) return "GZIP";

        byte[] magic = new byte[4];
        try (var is = Files.newInputStream(path)) {
            if (is.read(magic) == 4) {
                if (magic[0] == 0x50 && magic[1] == 0x4B) return "ZIP";
                if (magic[0] == 0x37 && magic[1] == 0x7A && magic[2] == 0xBC) return "7Z";
            }
        } catch (Exception e) {
            // ignore
        }

        return "UNKNOWN";
    }

    public static long getDirectorySize(Path path) throws IOException {
        try (Stream<Path> walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try { return Files.size(p); }
                        catch (IOException e) { return 0; }
                    })
                    .sum();
        }
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
