package com.eclectics.chamapoll.wrappers;

/**
 * @author Alex Maina
 * @created 12/01/2022
 */
public enum PositionName {
    CHAIRPERSON("Chairperson"),
    SECRETARY("Secretary"),
    TREASURER("Treasurer"),
    CREATOR("Creator"),
    MEMBER("Member");
    String name;

    PositionName(String name){
        this.name=name;
    }
}
