# Task: Implement Recipient Selection

**Service**: Notification Service  
**Phase**: 8  
**Estimated Time**: 2 hours  

## Objective

Implement recipient selection logic based on notification preferences and organization membership.

## Prerequisites

- [x] Task 003: Email service implemented

## Scope

**Files to Create**:
- `services/notification-service/src/main/java/com/turaf/notification/service/RecipientService.java`
- `services/notification-service/src/main/java/com/turaf/notification/client/OrganizationServiceClient.java`
- `services/notification-service/src/main/java/com/turaf/notification/model/NotificationPreference.java`

## Implementation Details

### Recipient Service

```java
public class RecipientService {
    private final OrganizationServiceClient organizationClient;
    private final PreferenceService preferenceService;
    
    public List<String> getRecipients(String organizationId, String eventType) {
        // Get all organization members
        List<MemberDto> members = organizationClient.getMembers(organizationId);
        
        // Filter based on preferences
        return members.stream()
            .filter(member -> shouldNotify(member.getUserId(), eventType))
            .map(MemberDto::getEmail)
            .collect(Collectors.toList());
    }
    
    private boolean shouldNotify(String userId, String eventType) {
        NotificationPreference preference = preferenceService.getPreference(userId, eventType);
        return preference == null || preference.isEnabled();
    }
}
```

### Organization Service Client

```java
public class OrganizationServiceClient {
    private final String baseUrl;
    private final HttpClient httpClient;
    
    public List<MemberDto> getMembers(String organizationId) {
        String url = baseUrl + "/api/v1/organizations/" + organizationId + "/members";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("X-Organization-Id", organizationId)
            .GET()
            .build();
        
        try {
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(
                response.body(),
                new TypeReference<List<MemberDto>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch members", e);
        }
    }
}
```

## Acceptance Criteria

- [ ] Recipient selection works
- [ ] Preferences respected
- [ ] Organization members fetched
- [ ] Error handling implemented
- [ ] Unit tests pass

## Testing Requirements

**Unit Tests**:
- Test recipient selection
- Test preference filtering
- Test member fetching

**Test Files to Create**:
- `RecipientServiceTest.java`

## References

- Specification: `specs/notification-service.md` (Recipient Selection section)
- Related Tasks: 007-add-idempotency
