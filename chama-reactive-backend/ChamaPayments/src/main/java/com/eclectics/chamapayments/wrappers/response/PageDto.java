package com.eclectics.chamapayments.wrappers.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageDto {

    private Integer currentPage;
    private Integer totalPages;
    private Object content;

}
