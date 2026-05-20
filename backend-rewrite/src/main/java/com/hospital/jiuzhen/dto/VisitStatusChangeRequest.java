package com.hospital.jiuzhen.dto;

import com.hospital.jiuzhen.domain.VisitStatus;
import jakarta.validation.constraints.NotNull;

public record VisitStatusChangeRequest(@NotNull VisitStatus targetStatus) {
}
