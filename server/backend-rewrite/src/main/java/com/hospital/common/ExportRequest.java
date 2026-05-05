package com.hospital.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ExportRequest(
    @NotBlank String module,
    @NotEmpty List<String> fields,
    boolean async
) {}
