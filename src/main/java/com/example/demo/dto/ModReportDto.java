package com.example.demo.dto;

public record ModReportDto(
        Long modId,
        String modName,
        boolean pdfGenerated,
        int pdfSizeBytes,
        Double rating,
        Integer ratingVotes,
        boolean degraded) {

    public static ModReportDto of(ModificationDto mod, byte[] pdf, ModRating rating) {
        return new ModReportDto(
                mod.getId(),
                mod.getName(),
                pdf != null,
                pdf != null ? pdf.length : 0,
                rating != null ? rating.average() : null,
                rating != null ? rating.votes() : null,
                pdf == null || rating == null);
    }

    public static ModReportDto failed(ModificationDto mod) {
        return of(mod, null, null);
    }
}
