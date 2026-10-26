package com.mbv.service;

import com.mbv.model.MailMessage;
import com.mbv.parser.MailParser;
import com.mbv.parser.ParserFactory;
import com.mbv.util.FileUtils;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Service
public class ImportService {

    @Autowired
    private ParserFactory parserFactory;

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ThreadBuilder threadBuilder;

    public interface ImportProgressListener {
        void onProgress(int current, int total, String currentFile);
        void onFileImported(String fileName, int messageCount);
        void onComplete(int totalMessages);
        void onError(String error);
    }

    public List<MailMessage> importFiles(List<Path> files, ImportProgressListener listener) throws Exception {
        List<MailMessage> allMessages = new ArrayList<>();
        AtomicInteger totalImported = new AtomicInteger(0);

        for (Path file : files) {
            try {
                if (Files.isDirectory(file)) {
                    importDirectory(file, allMessages, listener);
                } else if (FileUtils.isCompressedFile(file)) {
                    importCompressed(file, allMessages, listener);
                } else if (FileUtils.isArchiveFile(file)) {
                    importArchiveFile(file, allMessages, listener);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError("Error importing " + file.getFileName() + ": " + e.getMessage());
                }
            }
        }

        // Index all messages
        if (listener != null) {
            listener.onProgress(0, allMessages.size(), "Indexing messages...");
        }

        for (int i = 0; i < allMessages.size(); i++) {
            indexingService.indexMail(allMessages.get(i));
            if (listener != null && i % 100 == 0) {
                listener.onProgress(i, allMessages.size(), "Indexing...");
            }
            if (i % 5000 == 0 && i > 0) {
                indexingService.commit();
            }
        }
        indexingService.commit();

        // Build threads
        threadBuilder.buildThreads(allMessages);

        // Cache import info (skip directories)
        for (Path file : files) {
            if (!Files.isDirectory(file)) {
                cacheService.cacheImportedFile(file.toString(),
                        computeFileHash(file), countMessagesInFile(file));
            }
        }

        if (listener != null) {
            listener.onComplete(allMessages.size());
        }

        return allMessages;
    }

    private void importDirectory(Path dir, List<MailMessage> messages, ImportProgressListener listener) throws Exception {
        try (Stream<Path> walk = Files.walk(dir)) {
            List<Path> files = walk.filter(Files::isRegularFile).toList();
            for (Path file : files) {
                try {
                    if (FileUtils.isCompressedFile(file)) {
                        importCompressed(file, messages, listener);
                    } else if (FileUtils.isArchiveFile(file)) {
                        importSingleFile(file, messages, listener);
                    }
                } catch (Exception e) {
                    if (listener != null) {
                        listener.onError("Error importing " + file.getFileName() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    private void importCompressed(Path file, List<MailMessage> messages, ImportProgressListener listener) throws Exception {
        String name = file.getFileName().toString().toLowerCase();

        if (name.endsWith(".zip")) {
            importZip(file, messages, listener);
        } else if (name.endsWith(".7z")) {
            import7z(file, messages, listener);
        }
    }

    private void importZip(Path zipFile, List<MailMessage> messages, ImportProgressListener listener) throws Exception {
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            java.util.Enumeration<? extends ZipArchiveEntry> entries = zip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String entryName = entry.getName().toLowerCase();
                    if (entryName.endsWith(".mbox") || entryName.endsWith(".eml")) {
                        try (InputStream is = zip.getInputStream(entry)) {
                            Path tempFile = Files.createTempFile("mbv-import", getExtension(entryName));
                            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);

                            MailParser parser = parserFactory.getParser(tempFile).orElse(null);
                            if (parser != null) {
                                List<MailMessage> parsed = parser.parse(tempFile).toList();
                                messages.addAll(parsed);
                                if (listener != null) {
                                    listener.onFileImported(entry.getName(), parsed.size());
                                }
                            }
                            Files.delete(tempFile);
                        }
                    }
                }
            }
        }
    }

    private void import7z(Path sevenZFile, List<MailMessage> messages, ImportProgressListener listener) throws Exception {
        try (SevenZFile sevenZ = new SevenZFile(sevenZFile.toFile())) {
            SevenZArchiveEntry entry;
            while ((entry = sevenZ.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String entryName = entry.getName().toLowerCase();
                    if (entryName.endsWith(".mbox") || entryName.endsWith(".eml")) {
                        Path tempFile = Files.createTempFile("mbv-import", getExtension(entryName));
                        try (OutputStream os = Files.newOutputStream(tempFile)) {
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = sevenZ.read(buffer)) != -1) {
                                os.write(buffer, 0, len);
                            }
                        }

                        MailParser parser = parserFactory.getParser(tempFile).orElse(null);
                        if (parser != null) {
                            List<MailMessage> parsed = parser.parse(tempFile).toList();
                            messages.addAll(parsed);
                            if (listener != null) {
                                listener.onFileImported(entry.getName(), parsed.size());
                            }
                        }
                        Files.delete(tempFile);
                    }
                }
            }
        }
    }

    private void importArchiveFile(Path file, List<MailMessage> messages, ImportProgressListener listener) throws Exception {
        MailParser parser = parserFactory.getParser(file).orElse(null);
        if (parser == null) {
            throw new UnsupportedOperationException("No parser for file: " + file);
        }

        List<MailMessage> parsed = parser.parse(file).toList();
        messages.addAll(parsed);

        if (listener != null) {
            listener.onFileImported(file.getFileName().toString(), parsed.size());
        }
    }

    private void importSingleFile(Path file, List<MailMessage> messages, ImportProgressListener listener) throws Exception {
        if (FileUtils.isArchiveFile(file)) {
            importArchiveFile(file, messages, listener);
        }
    }

    private int countMessagesInFile(Path file) {
        try {
            MailParser parser = parserFactory.getParser(file).orElse(null);
            if (parser != null) {
                return (int) parser.parse(file).count();
            }
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }

    private String computeFileHash(Path file) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, len);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String getExtension(String fileName) {
        if (fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.'));
        }
        return ".tmp";
    }
}
