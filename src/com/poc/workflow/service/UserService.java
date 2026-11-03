package com.poc.workflow.service;

import com.poc.workflow.exception.DuplicateUserException;
import com.poc.workflow.exception.InvalidCredentialsException;
import com.poc.workflow.exception.UserAlreadyLoggedInException;
import com.poc.workflow.exception.UserNotFoundException;
import com.poc.workflow.model.AbstractUser;
import java.util.List;

public interface UserService {

    // available work : interfaces
    void registerUser(AbstractUser user)
            throws DuplicateUserException;

            //Login
    AbstractUser login(String username, String password)
            throws InvalidCredentialsException, UserAlreadyLoggedInException;


         //logout 
    void logout(String username) throws UserNotFoundException;


        //GetUser
    AbstractUser getUserByUsername(
            String username)
            throws UserNotFoundException;

            

    List<AbstractUser> getAllUsers();

}