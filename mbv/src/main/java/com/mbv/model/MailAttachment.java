package com.mbv.model;

public class MailAttachment {

    private String fileName;
    private String mimeType;
    private long size;
    private byte[] content;
    private String hash;

    public MailAttachment() {}

    public MailAttachment(String fileName, String mimeType, long size, byte[] content) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.size = size;
        this.content = content;
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getExtension() {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        }
        return "";
    }

    @Override
    public String toString() {
        return fileName + " (" + formatSize(size) + ")";
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
