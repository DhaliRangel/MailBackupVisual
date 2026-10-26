package com.mbv.util;

import jakarta.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

public class MimeUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static String decodeMimeHeader(String header) {
        if (header == null) return "";
        try {
            return MimeUtility.decodeText(header);
        } catch (UnsupportedEncodingException e) {
            return header;
        }
    }

    public static String extractDomain(String email) {
        if (email == null || !email.contains("@")) return "";
        return email.substring(email.indexOf('@') + 1);
    }

    public static String getMimeTypeFromExtension(String ext) {
        if (ext == null) return "application/octet-stream";
        return switch (ext.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "doc", "docx" -> "application/msword";
            case "xls", "xlsx" -> "application/vnd.ms-excel";
            case "ppt", "pptx" -> "application/vnd.ms-powerpoint";
            case "txt" -> "text/plain";
            case "html", "htm" -> "text/html";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "zip" -> "application/zip";
            case "rar" -> "application/x-rar-compressed";
            case "7z" -> "application/x-7z-compressed";
            case "tar" -> "application/x-tar";
            case "gz" -> "application/gzip";
            case "csv" -> "text/csv";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            default -> "application/octet-stream";
        };
    }

    public static String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null) return "";
        return switch (mimeType.toLowerCase()) {
            case "application/pdf" -> "pdf";
            case "application/msword" -> "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            case "application/vnd.ms-excel" -> "xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx";
            case "text/plain" -> "txt";
            case "text/html", "application/xhtml+xml" -> "html";
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "application/zip" -> "zip";
            default -> "";
        };
    }
}
