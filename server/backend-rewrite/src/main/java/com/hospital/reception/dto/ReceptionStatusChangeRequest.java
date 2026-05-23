package com.hospital.reception.dto;

import jakarta.validation.constraints.NotNull;

public record ReceptionStatusChangeRequest(@NotNull Boolean enabled) {}
