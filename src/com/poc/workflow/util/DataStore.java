package com.poc.workflow.util;

import com.poc.workflow.model.AbstractUser;
import java.util.HashMap;
import java.util.Map;

public class DataStore {

    /*
     * Key   -> Username
     * Value -> User Object
     *
     * Example:
     *
     * admin      -> AdminUser Object
     * operator1  -> OperatorUser Object
     */
    public static Map<String, AbstractUser> users =
            new HashMap<>();


    
  
}