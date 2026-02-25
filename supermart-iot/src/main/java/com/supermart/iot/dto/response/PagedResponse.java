package com.supermart.iot.dto.response;

import lombok.*;
import org.springframework.data.domain.Page;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PagedResponse<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    public static <T> PagedResponse<T> of(Page<T> pageData) {
        return PagedResponse.<T>builder()
                .content(pageData.getContent())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .page(pageData.getNumber())
                .size(pageData.getSize())
                .build();
    }
}
