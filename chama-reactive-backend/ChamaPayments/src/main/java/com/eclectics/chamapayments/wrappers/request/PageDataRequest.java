package com.eclectics.chamapayments.wrappers.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageDataRequest {
    private Long id;
    private Integer page;
    private Integer size;
}
