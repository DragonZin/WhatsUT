package com.example.Models;

public class FileMessage {
    private String fileName;
    private String filePath;
    private long fileSize;

    public FileMessage(String fileName, String filePath, long fileSize) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }

    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public long getFileSize() { return fileSize; }
}
