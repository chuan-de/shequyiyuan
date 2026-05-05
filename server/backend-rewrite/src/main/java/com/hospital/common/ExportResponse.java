package com.hospital.common;

public record ExportResponse(String taskId, String status, String downloadUrl) {
    public static ExportResponse queued(String taskId) { return new ExportResponse(taskId, "QUEUED", null); }
    public static ExportResponse ready(String downloadUrl) { return new ExportResponse(null, "READY", downloadUrl); }
}
