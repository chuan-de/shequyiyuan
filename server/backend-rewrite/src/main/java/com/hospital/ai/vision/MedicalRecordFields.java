package com.hospital.ai.vision;

/**
 * Structured medical-record fields extracted from a vision OCR call.
 *
 * <p>All fields are nullable — the extractor fills in whatever the model
 * returned and leaves the rest untouched. The frontend "AI suggestion" panel
 * then lets the medic tick the ones they trust before merging into the form.
 *
 * <p>Field set is deliberately a flat record mirroring the user-visible
 * medical-record form so the UI can map suggestion → form field by name with
 * zero extra wiring.
 */
public record MedicalRecordFields(
        String patientName,
        String gender,
        Integer age,
        String visitDate,
        String department,
        String chiefComplaint,
        String presentIllness,
        String diagnosis,
        String prescription,
        String doctor
) {
    /** Total number of declared fields. Kept in sync with the record components. */
    public static int totalFieldCount() { return 10; }
}
