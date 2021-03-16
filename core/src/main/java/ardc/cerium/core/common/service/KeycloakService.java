package ardc.cerium.core.common.service;

import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.DataCenter;
import ardc.cerium.core.common.model.Scope;
import ardc.cerium.core.common.model.User;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A service that allows interaction with the Keycloak server manually
 */
@Service
public class KeycloakService {

	Logger logger = LoggerFactory.getLogger(KeycloakService.class);

	@Value("${keycloak.auth-server-url:https://test.auth.ardc.edu.au/auth/}")
	private String kcAuthServerURL;

	@Value("${keycloak.realm:ARDC}")
	private String kcRealm;

	@Value("${keycloak.resource:igsn}")
	private String clientID;

	@Value("${keycloak.credentials.secret:secret}")
	private String clientSecret;

	@Autowired
	private Environment env;

	// Self-autowired reference for @Cachable
	@Resource
	private KeycloakService self;

	/**
	 * Get the Keycloak Access Token for the current request
	 * @param request the current HttpServletRequest
	 * @return AccessToken access token
	 */
	public AccessToken getAccessToken(@NotNull HttpServletRequest request) {
		logger.debug("Obtaining AccessToken.class for current request");
		KeycloakSecurityContext keycloakSecurityContext = (KeycloakSecurityContext) (request
				.getAttribute(KeycloakSecurityContext.class.getName()));

		logger.debug(String.format("Obtained keycloakSecurityContext: %s", keycloakSecurityContext));

		// if there is no security context for keycloak, then the user is not logged in
		if (keycloakSecurityContext == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You have to log in");
		}

		return keycloakSecurityContext.getToken();
	}

	/**
	 * Returns the OAuth2 Access Token as String for the current request
	 * @param request the current HttpServletRequest
	 * @return String access token
	 */
	public String getPlainAccessToken(@NotNull HttpServletRequest request) {
		logger.debug("Obtaining PlainAccessToken for current request");
		KeycloakSecurityContext keycloakSecurityContext = (KeycloakSecurityContext) (request
				.getAttribute(KeycloakSecurityContext.class.getName()));
		return keycloakSecurityContext.getTokenString();
	}

	/**
	 * Get the current logged in user UUID
	 * @param request current HttpServletRequest
	 * @return UUID of the current logged in user
	 */
	public UUID getUserUUID(HttpServletRequest request) {
		AccessToken kcToken = getAccessToken(request);
		String subject = kcToken.getSubject();

		return UUID.fromString(subject);
	}

	/**
	 * Returns a list of protected resources that are available to the current user
	 * @param accessToken the access token as string
	 * @return a list of resources
	 */
	public List<Permission> getAuthorizedResources(String accessToken) {
		logger.debug(String.format("Obtaining Authorized Resources for AccessToken: %s", accessToken));
		// to use the authzClient to obtain permission require a keycloak.json file
		// that duplicate the information that is already available in
		// application.properties
		// the Bean KeycloakConfigResolver does not work with AuthzClient yet

		// build a configuration out of the properties
		Configuration configuration = new Configuration();
		configuration.setAuthServerUrl(kcAuthServerURL);
		configuration.setRealm(kcRealm);
		configuration.setResource(clientID);
		Map<String, Object> credentials = new HashMap<>();
		credentials.put("secret", clientSecret);
		configuration.setCredentials(credentials);
		logger.debug("Built configuration set for AuthzClient");

		// use the authzClient with that configuration so it doesn't fall back to
		// keycloak.json
		try {
			AuthzClient authzClient = AuthzClient.create(configuration);
			logger.debug("Attempt Authz authorization");
			AuthorizationResponse authzResponse = authzClient.authorization(accessToken).authorize();
			String rpt = authzResponse.getToken();
			logger.debug("Obtained rpt. Introspecting...");
			TokenIntrospectionResponse requestingPartyToken = authzClient.protection()
					.introspectRequestingPartyToken(rpt);
			return requestingPartyToken.getPermissions();
		}
		catch (Exception e) {
			logger.error(String.format("KeyCloak Authorization error: %s Cause: %s", e.getMessage(), e.getCause()));
			// return a blank list if there's an authorization error
			return new ArrayList<>();
		}
	}

	public User getLoggedInUser(@NotNull HttpServletRequest request) {

		// if the user is already available in the request, take it from there
		User existing = (User) request.getAttribute(String.valueOf(User.class));
		if (existing != null) {
			logger.debug("Obtained User from request attributes");
			return existing;
		}

		logger.debug("Building currently logged in user for current HttpServletRequest request");
		AccessToken token = getAccessToken(request);
		logger.debug("Obtained access Token: " + token);

		User user = new User(UUID.fromString(token.getSubject()));
		user.setUsername(token.getPreferredUsername());
		user.setName(token.getName());
		user.setEmail(token.getEmail());
		user.setRoles(new ArrayList<>(token.getRealmAccess().getRoles()));

		// groups belongs to otherClaims
		logger.debug(
				String.format("Building groups for user based on otherClaims size: %s", token.getOtherClaims().size()));
		Map<String, Object> otherClaims = token.getOtherClaims();
		if (otherClaims.containsKey("groups")) {
			List<String> groups = new ArrayList<>();
			groups.addAll((Collection<? extends String>) otherClaims.get("groups"));
			logger.debug(String.format("Group size: %s", groups.size()));

			List<DataCenter> userDataCenters = new ArrayList<>();
			logger.debug("Building DataCenter instance for each group");
			for (String group : groups) {
				try {
					DataCenter dc = self.getDataCenterByGroupName(group);
					userDataCenters.add(dc);
				}
				catch (Exception e) {
					logger.error(String.format("Failed building data center for group %s ,Message: %s ,Cause: %s",
							group, e.getMessage(), e.getCause()));
				}
			}
			user.setDataCenters(userDataCenters);
		}

		// Allocations in authorizedResources
		List<Permission> permissions = getAuthorizedResources(getPlainAccessToken(request));
		logger.debug(String.format("Obtained permissions, length: %s", permissions.size()));
		List<Allocation> userPermissions = new ArrayList<>();
		for (Permission permission : permissions) {
			logger.debug("Building Allocation for permission: " + permission.getResourceId());
			try {
				Allocation allocation = self.getAllocationByResourceID(permission.getResourceId());

				// add scopes after (cached) allocations since they're from Permissions
				// instead of ResourceID
				List<Scope> scopes = permission.getScopes().stream().map(Scope::fromString)
						.collect(Collectors.toList());
				allocation.setScopes(scopes);
				userPermissions.add(allocation);
			}
			catch (Exception e) {
				logger.error(String.format("Failed obtaining Allocation for resourceid: %s ,Message: %s ,Cause: %s",
						permission.getResourceId(), e.getMessage(), e.getCause()));
			}
		}
		user.setAllocations(userPermissions);

		// passing the User along with the request, mainly for logging but can be used for
		// anything else
		request.setAttribute(String.valueOf(User.class), user);

		// user.setAllocations(permissions);
		return user;
	}

	/**
	 * @param name The name (path) of the Group
	 * @throws Exception normally when the environment is not properly set or (rarely)
	 * group name doesn't exist
	 * @return DataCenter representation of a keycloak Group
	 */
	@Cacheable("datacenters")
	public DataCenter getDataCenterByGroupName(String name) throws Exception {
		// todo cache
		logger.debug("Obtaining DataCenter for group: " + name);
		Keycloak keycloak = getAdminClient();
		GroupRepresentation group = keycloak.realm(kcRealm).getGroupByPath(name);
		logger.debug(String.format("Obtained GroupRepresentation %s", group));
		DataCenter dataCenter = new DataCenter(UUID.fromString(group.getId()));
		dataCenter.setName(group.getName());
		dataCenter.setPath(group.getPath());
		return dataCenter;
	}

	@Cacheable("users")
	public User getUserByUUID(UUID userId) throws Exception {
		Keycloak keycloak = getAdminClient();
		UserRepresentation resource = keycloak.realm(kcRealm).users().get(userId.toString()).toRepresentation();
		User user = new User(UUID.fromString(resource.getId()));
		user.setUsername(resource.getUsername());
		user.setEmail(resource.getEmail());
		return user;
	}

	/**
	 * @param datacenterId The UUID of the Group
	 * @throws Exception normally when the environment is not properly set or (rarely)
	 * group name doesn't exist
	 * @return DataCenter representation of a keycloak Group
	 */
	@Cacheable("datacenters")
	public DataCenter getDataCenterByUUID(UUID datacenterId) throws Exception {
		// todo cache
		logger.debug("Obtaining DataCenter for uuid: " + datacenterId);
		Keycloak keycloak = getAdminClient();
		List<GroupRepresentation> groups = keycloak.realm(kcRealm).groups().groups();
		List<RoleRepresentation> roles = keycloak.realm(kcRealm).roles().list();
		GroupRepresentation group = null;
		for (GroupRepresentation g  : groups) {
			if(g.getId().equals(datacenterId.toString())){
				group = g;
			}else{
				group = getSubgroupByID(g, datacenterId.toString());
			}
			if(group != null)
			{
				logger.debug(String.format("Obtained GroupRepresentation %s", group.getName()));
				DataCenter dataCenter = new DataCenter(UUID.fromString(group.getId()));
				dataCenter.setName(group.getName());
				dataCenter.setPath(group.getPath());
				return dataCenter;
			}
		}
		return null;
	}

	private GroupRepresentation getSubgroupByID(GroupRepresentation group, String groupId){
		if(group.getSubGroups().isEmpty()) {
			return null;
		}
		for(GroupRepresentation sg : group.getSubGroups()){
			if(sg.getId().equals(groupId)){
				return sg;
			}else{
				GroupRepresentation ssg = getSubgroupByID(sg, groupId);
				if(ssg != null){
					return ssg;
				}
			}
		}
		return null;
	}


	@Cacheable("allocations")
	public Allocation getAllocationByResourceID(String id) {
		// todo cache
		logger.debug("Obtaining Allocation for resourceID: " + id);
		AuthzClient authzClient = null;
		try {
			authzClient = getAuthzClient();
		}
		catch (Exception e) {
			logger.debug("Client not Authenticated: " + id);
			return null;
		}
		ResourceRepresentation resource = authzClient.protection().resource().findById(id);
		logger.debug(String.format("Obtained ResourceRepresentation id:%s %s: ", resource.getId(), resource.getId()));
		Allocation allocation = null;

		allocation = new Allocation(UUID.fromString(resource.getId()));

		allocation.setName(resource.getName());
		allocation.setType(resource.getType());
		Map<String, List<String>> attributes = resource.getAttributes();
		allocation.setStatus(attributes.containsKey("status") ? String.valueOf(attributes.get("status")) : null);
		allocation.setAttributes(attributes);

		// obtain groups attribute to determine Allocation<->DataCenter relationships
		if (attributes.containsKey("groups")) {
			List<DataCenter> dataCenters = new ArrayList<>();
			for (String groupName : attributes.get("groups")) {
				try {
					DataCenter dc = self.getDataCenterByGroupName(groupName);
					dataCenters.add(dc);
				}
				catch (Exception e) {
					logger.error(String.format("Failed building data center for group %s ,Message: %s ,Cause: %s",
							groupName, e.getMessage(), e.getCause()));
				}
			}
			allocation.setDataCenters(dataCenters);
		}

		return allocation;
	}

	/**
	 * @return an Admin Keycloak client
	 * @throws Exception when the environment is not properly set
	 */
	private Keycloak getAdminClient() throws Exception {
		String username = env.getProperty("keycloak-admin.username");
		String password = env.getProperty("keycloak-admin.password");
		if (username == null || password == null) {
			throw new Exception("Keycloak credentials is not properly configured");
		}
		Keycloak keycloak = KeycloakBuilder.builder().serverUrl(kcAuthServerURL).realm("master").username(username)
				.password(password).clientId("admin-cli")
				.resteasyClient(new ResteasyClientBuilder().connectionPoolSize(10).build()).build();

		return keycloak;
	}

	@NotNull
	private AuthzClient getAuthzClient() throws Exception {
		Configuration configuration = new Configuration();
		configuration.setAuthServerUrl(kcAuthServerURL);
		configuration.setRealm(kcRealm);
		configuration.setResource(clientID);
		Map<String, Object> credentials = new HashMap<>();
		credentials.put("secret", clientSecret);
		configuration.setCredentials(credentials);

		AuthzClient authzClient = AuthzClient.create(configuration);

		return authzClient;
	}

	/**
	 * @throws Exception normally when the environment is not properly set
	 * @return List of DataCenter representation of a keycloak Group
	 */
	@Cacheable("datacenters")
	public List<DataCenter> getDataCentres() throws Exception {
		// todo cache
		logger.debug("Obtaining DataCenters");
		Keycloak keycloak = getAdminClient();
		List<GroupRepresentation> groups = keycloak.realm(kcRealm).groups().groups();
		List<DataCenter> dataCenters = new ArrayList<DataCenter>();
		logger.debug(String.format("Obtained GroupRepresentation"));
		for (GroupRepresentation group : groups) {
			DataCenter dataCenter = new DataCenter(UUID.fromString(group.getId()));
			dataCenter.setName(group.getName());
			dataCenters.add(dataCenter);
		}
		return dataCenters;
	}

}
