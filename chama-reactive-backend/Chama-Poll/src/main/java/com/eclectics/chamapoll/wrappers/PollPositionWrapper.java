package com.eclectics.chamapoll.wrappers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Alex Maina
 * @created 28/12/2021
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PollPositionWrapper {
    private boolean chairman;
    private boolean treasurer;
    private boolean secretary;
}
