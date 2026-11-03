package com.poc.workflow.servicesImpl;

import java.util.ArrayList;
import java.util.List;

import com.poc.workflow.exception.DuplicateUserException;
import com.poc.workflow.exception.InvalidCredentialsException;
import com.poc.workflow.exception.UserAlreadyLoggedInException;
import com.poc.workflow.exception.UserNotFoundException;
import com.poc.workflow.model.AbstractUser;
import com.poc.workflow.service.UserService;
import com.poc.workflow.util.DataStore;

// Business rules for users. Throws exceptions only; never prints (the menu does that).
public class UserServiceImpl implements UserService {

    @Override
    public void registerUser(AbstractUser user) throws DuplicateUserException {
        // username is the map key, so a key clash means a duplicate user
        if (DataStore.users.containsKey(user.getUsername())) {
            throw new DuplicateUserException("Username already exists: " + user.getUsername());
        }
        DataStore.users.put(user.getUsername(), user);
    }

    @Override
    public AbstractUser login(String username, String password)
            throws InvalidCredentialsException, UserAlreadyLoggedInException {

        AbstractUser user = DataStore.users.get(username);

        // same message for "no such user" and "wrong password" so usernames are not revealed
        if (user == null || !user.checkPassword(password)) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // spec rule: a logged-in user cannot log in again
        if (user.isLoggedIn()) {
            throw new UserAlreadyLoggedInException("User is already logged in: " + username);
        }

        user.setLoggedIn(true);
        return user;
    }

    @Override
    public void logout(String username) throws UserNotFoundException {
        // getUserByUsername throws if the user does not exist, so no null check is needed here
        getUserByUsername(username).setLoggedIn(false);
    }

    @Override
    public AbstractUser getUserByUsername(String username) throws UserNotFoundException {
        AbstractUser user = DataStore.users.get(username);
        if (user == null) {
            throw new UserNotFoundException("No user found with username: " + username);
        }
        return user;
    }

    @Override
    public List<AbstractUser> getAllUsers() {
        // copy, so callers cannot modify the registry through the returned list
        return new ArrayList<>(DataStore.users.values());
    }
}