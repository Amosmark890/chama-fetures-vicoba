package com.ekenya.chamaauthorizationserver.services;

import com.ekenya.chamaauthorizationserver.entity.Users;
import com.ekenya.chamaauthorizationserver.repository.UsersRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserService implements UserDetailsService {
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private StreamBridge streamBridge;
    private final Gson gson = new Gson();
    private static final String DISABLE_MEMBER_CHANNEL = "deactivate-member-account-topic";

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Users> databaseusercontainer = getUserByUsername(username);
        User.UserBuilder builder;
        if (databaseusercontainer.isPresent()) {
            Users databaseuser = databaseusercontainer.get();
            if (databaseuser.isActive()) {
                builder = User.withUsername(username);
                builder.password(databaseuser.getPassword());
                builder.authorities(loadUserRoles(databaseuser));
            } else {
                log.info("user is inactive");
                throw new UsernameNotFoundException("Account is deactivated");
            }

        } else {
            log.info("user does not exist");
            throw new UsernameNotFoundException(username);
        }
        return builder.build();
    }

    private List<GrantedAuthority> loadUserRoles(Users user) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        grantedAuthorities.add(new SimpleGrantedAuthority(user.getRoles().getName()));
        return grantedAuthorities;
    }

    public Optional<Users> getUserByUsername(String username) {
        List<Users> dbUsersList = usersRepository.findAllByPhoneNumberAndActive(username, true);
        if (dbUsersList.isEmpty()) {
            dbUsersList = usersRepository.findAllByEmailAndActive(username, true);
        }
        if (dbUsersList.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(dbUsersList.get(0));
        }
    }

    public void updateLastLogin(String loggedPrincipal) {
        List<Users> users = usersRepository.findUsersByEmailOrPhoneNumber(loggedPrincipal, loggedPrincipal);

        users.parallelStream()
                .forEach(user -> {
                    user.setLastLogin(new Date());
                    usersRepository.save(user);
                });
    }

    public Users searchUserByUsernameAndChannel(String username, String channel) {
        if (channel.equals("portal")) {
            return usersRepository.findByEmailAndChannelEquals(username, "PORTAL").orElse(null);
        } else if (channel.equals("app")) {
            log.info("App user... {}", username);
            return usersRepository.findByPhoneNumberAndChannelEquals(username, "APP").orElse(null);
        } else
            return null;
    }

    @Async
    public void deactivateUser(Users users) {
        users.setActive(false);
        usersRepository.save(users);
        // publish event to disable member if any
        if (users.getChannel().equals("APP")) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("phoneNumber", users.getPhoneNumber());
            streamBridge.send(DISABLE_MEMBER_CHANNEL, gson.toJson(jsonObject));
        }
    }

    public void resetLoginAttempts(Users user) {
        user.setLoginAttempts(0);
        saveUser(user);
    }

    public void saveUser(Users user) {
        usersRepository.save(user);
    }
}
