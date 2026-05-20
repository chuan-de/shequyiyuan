package com.hospital.jiuankangdangan.dto;

import com.hospital.jiuankangdangan.domain.JiuankangdanganStatus;
import jakarta.validation.constraints.NotNull;

public record JiuankangdanganStatusChangeRequest(@NotNull JiuankangdanganStatus targetStatus) {}
