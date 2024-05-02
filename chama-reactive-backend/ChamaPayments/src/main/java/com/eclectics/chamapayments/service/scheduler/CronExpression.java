package com.eclectics.chamapayments.service.scheduler;

import lombok.*;

/**
 * @author Alex Maina
 * @created 03/04/2022
 **/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CronExpression {
    private String seconds="*";
    private String minutes="*";
    private String hours="*";
    private String day="*";
    private String month="*";
    private String weekDay="*";
    private String year="*";
}
