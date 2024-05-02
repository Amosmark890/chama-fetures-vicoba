package com.eclecticskenya.chamareactivebackend;

public class Test {
    enum Weight{
        Metres("m"),
        Kilometres("km");

        String initials;

        Weight (String name){
            initials=name;
        }

    }
    public static void main(String[] args) {
        System.out.println(Weight.valueOf("m"));
    }
}
