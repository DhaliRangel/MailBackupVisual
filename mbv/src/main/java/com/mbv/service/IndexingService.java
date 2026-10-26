package com.mbv.service;

import com.mbv.model.MailMessage;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;

import jakarta.annotation.PostConstruct;

@Service
public class IndexingService {

    private static final String INDEX_DIR = "mbv-index";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private IndexWriter indexWriter;
    private DirectoryReader indexReader;
    private SearcherManager searcherManager;
    private StandardAnalyzer analyzer;

    private static final String[] ALL_FIELDS = {
            "id", "messageId", "subject", "plainBody", "htmlBody",
            "senderName", "senderEmail", "senderDomain",
            "toNames", "toEmails", "toDomains",
            "ccNames", "ccEmails",
            "plainBodyStripped", "folder", "sourceArchive",
            "sourceArchiveType", "charset", "mimeType",
            "attachmentNames", "attachmentTypes", "attachmentExtensions",
            "gmailLabels", "label", "hash"
    };

    private static final String[] SEARCHABLE_FIELDS = {
            "subject", "plainBody", "htmlBody", "senderName", "senderEmail",
            "toNames", "toEmails", "attachmentNames"
    };

    @PostConstruct
    public void init() {
        try {
            openIndex(null);
        } catch (IOException e) {
            System.err.println("Failed to open Lucene index: " + e.getMessage());
        }
    }

    public void openIndex(Path indexPath) throws IOException {
        Path dir = indexPath != null ? indexPath : Paths.get(INDEX_DIR);
        analyzer = new StandardAnalyzer();

        Directory fsDir = FSDirectory.open(dir);

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        config.setRAMBufferSizeMB(256.0);
        config.setUseCompoundFile(false);

        indexWriter = new IndexWriter(fsDir, config);
        indexWriter.commit();

        reopenReader();
    }

    public void indexMail(MailMessage mail) throws IOException {
        Document doc = new Document();

        doc.add(new StringField("id", mail.getId(), Field.Store.YES));
        doc.add(new StringField("messageId", nvl(mail.getMessageId()), Field.Store.YES));
        doc.add(new TextField("subject", nvl(mail.getSubject()), Field.Store.YES));

        String plainBody = nvl(mail.getPlainBody());
        doc.add(new TextField("plainBody", plainBody, Field.Store.YES));
        doc.add(new StoredField("plainBodyStripped", stripHtml(plainBody)));

        String htmlBody = nvl(mail.getHtmlBody());
        doc.add(new TextField("htmlBody", htmlBody, Field.Store.YES));

        if (mail.getSender() != null) {
            doc.add(new TextField("senderName", nvl(mail.getSender().getName()), Field.Store.YES));
            doc.add(new StringField("senderEmail", nvl(mail.getSender().getEmail()), Field.Store.YES));
            doc.add(new StringField("senderDomain", nvl(mail.getSender().getDomain()), Field.Store.YES));
        }

        StringBuilder toNames = new StringBuilder();
        StringBuilder toEmails = new StringBuilder();
        StringBuilder toDomains = new StringBuilder();
        for (var c : mail.getTo()) {
            if (!toNames.isEmpty()) toNames.append(" ");
            toNames.append(nvl(c.getName()));
            if (!toEmails.isEmpty()) toEmails.append(" ");
            toEmails.append(nvl(c.getEmail()));
            if (!toDomains.isEmpty()) toDomains.append(" ");
            toDomains.append(nvl(c.getDomain()));
        }
        doc.add(new TextField("toNames", toNames.toString(), Field.Store.YES));
        doc.add(new StringField("toEmails", toEmails.toString(), Field.Store.YES));
        doc.add(new StringField("toDomains", toDomains.toString(), Field.Store.YES));

        StringBuilder ccNames = new StringBuilder();
        StringBuilder ccEmails = new StringBuilder();
        for (var c : mail.getCc()) {
            if (!ccNames.isEmpty()) ccNames.append(" ");
            ccNames.append(nvl(c.getName()));
            if (!ccEmails.isEmpty()) ccEmails.append(" ");
            ccEmails.append(nvl(c.getEmail()));
        }
        doc.add(new TextField("ccNames", ccNames.toString(), Field.Store.YES));
        doc.add(new StringField("ccEmails", ccEmails.toString(), Field.Store.YES));

        doc.add(new StringField("folder", nvl(mail.getFolder()), Field.Store.YES));
        doc.add(new StringField("sourceArchive", nvl(mail.getSourceArchive()), Field.Store.YES));
        doc.add(new StringField("sourceArchiveType", nvl(mail.getSourceArchiveType()), Field.Store.YES));
        doc.add(new StringField("charset", nvl(mail.getCharset()), Field.Store.YES));
        doc.add(new StringField("mimeType", nvl(mail.getMimeType()), Field.Store.YES));

        StringBuilder attNames = new StringBuilder();
        StringBuilder attTypes = new StringBuilder();
        StringBuilder attExts = new StringBuilder();
        for (var att : mail.getAttachments()) {
            if (!attNames.isEmpty()) attNames.append(" ");
            attNames.append(nvl(att.getFileName()));
            if (!attTypes.isEmpty()) attTypes.append(" ");
            attTypes.append(nvl(att.getMimeType()));
            if (!attExts.isEmpty()) attExts.append(" ");
            attExts.append(att.getExtension());
        }
        doc.add(new TextField("attachmentNames", attNames.toString(), Field.Store.YES));
        doc.add(new StringField("attachmentTypes", attTypes.toString(), Field.Store.YES));
        doc.add(new StringField("attachmentExtensions", attExts.toString(), Field.Store.YES));

        doc.add(new StringField("gmailLabels", nvl(mail.getGmailLabels()), Field.Store.YES));
        doc.add(new StringField("label", nvl(mail.getLabel()), Field.Store.YES));
        doc.add(new StringField("hash", nvl(mail.getHash()), Field.Store.YES));

        if (mail.getSentDate() != null) {
            doc.add(new StringField("sentDate", mail.getSentDate().format(DATE_FMT), Field.Store.YES));
            doc.add(new LongPoint("sentDateLong", mail.getSentDate().toEpochSecond(java.time.ZoneOffset.UTC)));
            doc.add(new StoredField("sentDateStored", mail.getSentDate().format(DATE_FMT)));
        }

        doc.add(new IntPoint("attachmentCount", mail.getAttachmentCount()));
        doc.add(new StoredField("attachmentCountStored", mail.getAttachmentCount()));

        doc.add(new LongPoint("emailSize", mail.getSize()));
        doc.add(new StoredField("emailSizeStored", mail.getSize()));

        doc.add(new IntPoint("priority", mail.getPriority() != null ? mail.getPriority().ordinal() : 1));
        doc.add(new StoredField("priorityStored", mail.getPriority() != null ? mail.getPriority().ordinal() : 1));

        doc.add(new IntPoint("read", mail.getFlags().isRead() ? 1 : 0));
        doc.add(new IntPoint("hasAttachment", mail.hasAttachments() ? 1 : 0));
        doc.add(new IntPoint("isHtml", mail.isHtml() ? 1 : 0));

        indexWriter.addDocument(doc);
    }

    public void commit() throws IOException {
        indexWriter.commit();
        reopenReader();
    }

    public void optimize() throws IOException {
        indexWriter.forceMerge(1);
        indexWriter.commit();
        reopenReader();
    }

    private void reopenReader() throws IOException {
        if (indexReader != null) {
            indexReader.close();
        }
        indexReader = DirectoryReader.open(indexWriter.getDirectory());
        searcherManager = new SearcherManager(indexWriter, null);
    }

    public int getIndexedCount() {
        return indexReader.numDocs();
    }

    public List<MailMessage> search(String queryStr, int maxResults) throws IOException, ParseException {
        List<MailMessage> results = new ArrayList<>();

        QueryParser parser = new MultiFieldQueryParser(SEARCHABLE_FIELDS, analyzer);
        parser.setDefaultOperator(QueryParser.Operator.OR);
        Query query = parser.parse(queryStr);

        IndexSearcher searcher = indexReader.leaves().isEmpty() ? null : null;
        searcher = new IndexSearcher(indexReader);

        TopDocs topDocs = searcher.search(query, maxResults);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexReader.document(scoreDoc.doc);
            MailMessage msg = documentToMailMessage(doc);
            results.add(msg);
        }

        return results;
    }

    public List<MailMessage> searchWithFilters(String textQuery, Map<String, String> filters, int maxResults)
            throws IOException, ParseException {
        List<MailMessage> results = new ArrayList<>();

        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

        if (textQuery != null && !textQuery.trim().isEmpty()) {
            QueryParser parser = new MultiFieldQueryParser(SEARCHABLE_FIELDS, analyzer);
            parser.setDefaultOperator(QueryParser.Operator.OR);
            Query textQ = parser.parse(QueryParser.escape(textQuery));
            booleanQuery.add(textQ, BooleanClause.Occur.MUST);
        }

        if (filters != null) {
            for (var entry : filters.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    QueryParser fp = new QueryParser(entry.getKey(), analyzer);
                    booleanQuery.add(fp.parse(QueryParser.escape(entry.getValue())), BooleanClause.Occur.MUST);
                }
            }
        }

        Query finalQuery = booleanQuery.build();
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(finalQuery, maxResults);

        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexReader.document(scoreDoc.doc);
            results.add(documentToMailMessage(doc));
        }

        return results;
    }

    public MailMessage getDocumentById(String id) throws IOException, ParseException {
        QueryParser parser = new QueryParser("id", analyzer);
        Query query = parser.parse(QueryParser.escape(id));
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, 1);

        if (topDocs.scoreDocs.length > 0) {
            Document doc = indexReader.document(topDocs.scoreDocs[0].doc);
            return documentToMailMessage(doc);
        }
        return null;
    }

    public void deleteDocument(String id) throws IOException, ParseException {
        QueryParser parser = new QueryParser("id", analyzer);
        Query query = parser.parse(QueryParser.escape(id));
        indexWriter.deleteDocuments(query);
        indexWriter.commit();
    }

    private MailMessage documentToMailMessage(Document doc) {
        MailMessage msg = new MailMessage();
        msg.setId(doc.get("id"));
        msg.setMessageId(doc.get("messageId"));
        msg.setSubject(doc.get("subject"));
        msg.setPlainBody(doc.get("plainBody"));
        msg.setHtmlBody(doc.get("htmlBody"));
        msg.setFolder(doc.get("folder"));
        msg.setSourceArchive(doc.get("sourceArchive"));
        msg.setSourceArchiveType(doc.get("sourceArchiveType"));
        msg.setCharset(doc.get("charset"));
        msg.setMimeType(doc.get("mimeType"));
        msg.setGmailLabels(doc.get("gmailLabels"));
        msg.setLabel(doc.get("label"));
        msg.setHash(doc.get("hash"));

        String senderName = doc.get("senderName");
        String senderEmail = doc.get("senderEmail");
        if (senderEmail != null && !senderEmail.isEmpty()) {
            msg.setSender(new com.mbv.model.Contact(senderName, senderEmail));
        }

        String toEmails = doc.get("toEmails");
        String toNames = doc.get("toNames");
        if (toEmails != null && !toEmails.isEmpty()) {
            String[] emails = toEmails.split("\\s+");
            String[] names = (toNames != null && !toNames.isEmpty()) ? toNames.split("\\s+") : new String[0];
            for (int i = 0; i < emails.length; i++) {
                String name = i < names.length ? names[i] : "";
                msg.getTo().add(new com.mbv.model.Contact(name, emails[i]));
            }
        }

        String storedDate = doc.get("sentDateStored");
        if (storedDate != null && !storedDate.isEmpty()) {
            try {
                msg.setSentDate(java.time.LocalDateTime.parse(storedDate,
                        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")));
            } catch (Exception e) {
                // ignore
            }
        }

        var flags = new com.mbv.model.MailFlags();
        flags.setRead("1".equals(doc.get("read")));
        msg.setFlags(flags);

        msg.setAttachmentCount(doc.getField("attachmentCountStored") != null ?
                Integer.parseInt(doc.get("attachmentCountStored")) : 0);

        msg.setSize(doc.getField("emailSizeStored") != null ?
                Long.parseLong(doc.get("emailSizeStored")) : 0);

        return msg;
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public void close() throws IOException {
        if (indexWriter != null) {
            indexWriter.commit();
            indexWriter.close();
        }
        if (indexReader != null) {
            indexReader.close();
        }
    }
}
