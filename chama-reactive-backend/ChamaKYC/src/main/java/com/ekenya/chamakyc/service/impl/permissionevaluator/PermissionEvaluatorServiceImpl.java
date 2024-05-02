package com.ekenya.chamakyc.service.impl.permissionevaluator;

import com.ekenya.chamakyc.repository.chama.GroupMembersRepository;
import com.ekenya.chamakyc.service.Interfaces.ChamaUserService;
import com.ekenya.chamakyc.service.Interfaces.PermissionEvaluatorService;
import com.ekenya.chamakyc.service.impl.cache.CacheService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * @author Alex Maina
 * @created 21/03/2022
 * @modified 02/05/2022 by Willy The Dev
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionEvaluatorServiceImpl implements PermissionEvaluatorService {
    private final ChamaUserService userService;
    private final GroupMembersRepository groupMembersRepository;
    private final CacheService cacheService;
    private final Gson gson = new Gson();

    /**
     * Use the role matrix to check if users have the required
     * privileges to perform actions on Groups.
     *
     * @param auth         the authentication object
     * @param targetId     the resource you wish to access
     * @param scope        the scope of what you are trying to accomplish - not used in this case though
     * @param objectAction the permission and action
     * @return true or false
     */
    public boolean hasPermission(Authentication auth, Serializable targetId, String scope, Object objectAction) {
        if (String.valueOf(targetId).equals("0")) return true;

        ObjectAction permissionsObject = (ObjectAction) objectAction;
        String action = permissionsObject.getAction();
        String permission = permissionsObject.getObject();

        Optional<String> permissions = cacheService.getMemberPermission((long) targetId, getPhoneNumber(auth));

        if (permissions.isEmpty()) return false;

        JsonObject jsonObject = gson.fromJson(permissions.get(), JsonObject.class);

        if (!jsonObject.has(permission)) {
            log.info("Permission not found in Json object!");
            return false;
        }

            String memberPermissions = jsonObject.get(permission).getAsJsonArray().toString();

        return memberPermissions.contains(action);
    }

    /**
     * Extracts the username from the OAuth2 JWT object.
     *
     * @param auth the spring authentication object
     * @return the username
     */
    private String getPhoneNumber(Authentication auth) {
        Jwt principal = (Jwt) auth.getPrincipal();
        Map<String, Object> claims = principal.getClaims();

        return (String) claims.get("user_name");
    }


}
