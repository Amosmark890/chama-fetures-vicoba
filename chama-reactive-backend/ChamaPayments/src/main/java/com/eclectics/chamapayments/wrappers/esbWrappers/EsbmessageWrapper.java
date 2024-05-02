package com.eclectics.chamapayments.wrappers.esbWrappers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.util.Date;

/**
 * Class name: EsbmessageWrapper
 * Creater: wgicheru
 * Date:4/13/2020
 */
@Getter
@Setter
public class EsbmessageWrapper {
    String xref;
    String txntimestamp;
    TransData data;


    public EsbmessageWrapper(TransactionDetails transaction_details) {
        Date now = new Date();
        this.txntimestamp = String.valueOf(now.getTime());
        this.xref = this.txntimestamp;
        Channeldetails channel_details = new Channeldetails("23456", "172.20.0.132", "chama backend",
                "java springboot", "java 8", "NMB", "MOBILE");
        this.data = new TransData(transaction_details, channel_details);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private class TransData {
        TransactionDetails transaction_details;
        Channeldetails channel_details;

        public String toString() {
            try {
                return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                System.err.println(e);
            }
            return null;
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @ToString
    private class Channeldetails {
        String channel_key;
        String host;
        String geolocation;
        String user_agent_version;
        String user_agent;
        String client_id;
        String channel;
    }

    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            System.err.println(e);
        }
        return null;
    }
}
