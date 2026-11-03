package com.poc.workflow.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.poc.workflow.model.AbstractUser;

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