package com.example.Models;

import java.util.Arrays;

public class FileMessage extends Message {
    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final byte[] content;

    public FileMessage(String fileName, byte[] content, User sender) {
        super(sender);
        this.fileName = fileName;
        this.content = content == null ? new byte[0] : Arrays.copyOf(content, content.length);
    }

    public String getFileName() { return fileName; }

    public byte[] getContent() { return Arrays.copyOf(content, content.length); }

    public long getSize() { return content.length; }
}