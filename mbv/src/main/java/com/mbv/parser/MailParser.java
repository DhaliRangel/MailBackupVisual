package com.mbv.parser;

import com.mbv.model.MailMessage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface MailParser {

    boolean supports(Path file);

    Stream<MailMessage> parse(Path file) throws IOException;

    String getFormatName();

    String getFormatType();
}
