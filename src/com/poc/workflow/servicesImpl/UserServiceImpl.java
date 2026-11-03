package com.poc.workflow.servicesImpl;
import com.poc.workflow.exception.*;
import com.poc.workflow.model.AbstractUser;
import com.poc.workflow.service.UserService;
import com.poc.workflow.util.DataStore;
import java.util.*;

public class UserServiceImpl implements UserService {

    // implemet user register user

    @Override
    public void registerUser(AbstractUser user) throws DuplicateUserException {
        // 1. check user exist
        if (DataStore.users.containsKey(user.getUsername())) {

            throw new DuplicateUserException("This is a existing User");

        }

        // if not exist take the user to register.put method is used.
        DataStore.users.put(user.getUsername(), user);
        System.out.println(
                "User registered successfully.");

    }

    // login the user

    @Override
    public AbstractUser login(String username, String password)
            throws InvalidCredentialsException, UserAlreadyLoggedInException {

        // check the user exist or not

        // store the value in the user all the value like get call the object user now contains username , password, role , loggedIN.

        AbstractUser user = DataStore.users.get(username);
        if (user == null) {
            throw new InvalidCredentialsException("No User with this name");
        }

        if (!user.checkPassword(password)) {

            //checkPassword ALREADY DEFINED IN THE MODEL.

            throw new InvalidCredentialsException(
                    "Invalid Password");
        }

         if (user.isLoggedIn()) {

        throw new UserAlreadyLoggedInException(
                "User already logged in");
    }

    // Login successful
    user.setLoggedIn(true);

    return user;

    }


// logout user.

    @Override
    public void logout (String username) throws UserNotFoundException {

        AbstractUser user = DataStore.users.get(username);

        user.setLoggedIn(false);

    }


    //AbstractUser getUserByUsername
    public AbstractUser getUserByUsername (String username) throws UserNotFoundException{
        AbstractUser user = DataStore.users.get(username);
        if (user== null){
            throw new UserNotFoundException("No user with tihs this name");
        }

        return user;

    }


    // 


    @Override
public List<AbstractUser> getAllUsers() {

    return new ArrayList<>(
            DataStore.users.values());
}

}
