package com.ekenya.chamakyc.service.impl.permissionevaluator;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;


@Getter
@Setter
@Component("objectAction")
public class ObjectAction {
    String object;
    String action;


    public ObjectAction initFields(String object,String action){
        this.setAction(action);
        this.setObject(object);
        return  this;
    }
}
