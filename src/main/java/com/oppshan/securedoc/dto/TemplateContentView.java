package com.oppshan.securedoc.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record TemplateContentView(@NotNull
                                  byte[] data,

                                  @NotEmpty
                                  String fileName,

                                  String mimeType) {
}
