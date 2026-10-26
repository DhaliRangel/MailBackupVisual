package com.mbv.parser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ParserFactory {

    private final List<MailParser> parsers = new ArrayList<>();

    @Autowired
    public ParserFactory(MboxParser mboxParser, EmlParser emlParser) {
        parsers.add(mboxParser);
        parsers.add(emlParser);
    }

    public Optional<MailParser> getParser(Path file) {
        return parsers.stream()
                .filter(parser -> parser.supports(file))
                .findFirst();
    }

    public MailParser getParserOrThrow(Path file) {
        return getParser(file)
                .orElseThrow(() -> new UnsupportedOperationException(
                        "No parser found for file: " + file.getFileName()));
    }

    public List<MailParser> getAllParsers() {
        return new ArrayList<>(parsers);
    }

    public List<String> getSupportedExtensions() {
        List<String> extensions = new ArrayList<>();
        extensions.add("*.mbox");
        extensions.add("*.mbx");
        extensions.add("*.eml");
        return extensions;
    }

    public void registerParser(MailParser parser) {
        parsers.add(parser);
    }
}
