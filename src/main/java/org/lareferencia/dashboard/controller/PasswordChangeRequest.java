package org.lareferencia.dashboard.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
		@NotBlank
		@Size(min = 12, max = 512)
		String newPassword) {
}
