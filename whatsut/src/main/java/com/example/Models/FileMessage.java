package com.example.Models;

public class FileMessage extends Message {
    private String fileName;
    private Byte[] Content;

    public FileMessage(String fileName, Byte[] content, User sender) {
        super(sender);
        this.fileName = fileName;
        this.Content = content;
    }

    public String getFileName() { return fileName; }
    public Byte[] getContent() { return Content; }
}
