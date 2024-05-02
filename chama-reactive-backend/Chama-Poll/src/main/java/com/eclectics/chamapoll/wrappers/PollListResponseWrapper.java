package com.eclectics.chamapoll.wrappers;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PollListResponseWrapper {
    private long id;
    private long groupId;
    private String groupName;
    private String description;
    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern ="dd-MM-yyyy hh:mm:ss" )
    private Date startdate;
    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern ="dd-MM-yyyy hh:mm:ss" )
    private Date enddate;
}
