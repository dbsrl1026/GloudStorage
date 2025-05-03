package com.gloud.document.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DirectoryDto implements Serializable {
    Long directoryId;
    String name;
    String ownerEmail;
}