package lk.solar.microgrid.data;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ContractTest {
    @Test public void loginPreservesPasswordButTrimsNic() throws Exception {
        JSONObject body = RequestBodies.login(" 901234567v ", " password with spaces ");
        assertEquals("901234567v", body.getString("nic"));
        assertEquals(" password with spaces ", body.getString("password"));
        assertEquals(2, body.length());
    }
    @Test public void registrationDoesNotSupplyPrivileges() throws Exception {
        JSONObject body = RequestBodies.register("199012304567", " Solar User ", "solar@example.test", "+94771234567", "10 Solar Road", "long-password!");
        assertEquals(6, body.length());
        assertEquals("Solar User", body.getString("fullName"));
        assertFalse(body.has("role")); assertFalse(body.has("status")); assertFalse(body.has("passwordHash"));
    }
    @Test public void profileWritesCarryVersionAndNeverNicOrStatus() throws Exception {
        JSONObject body = RequestBodies.profile("User Name", "name@example.test", "+94771234567", "10 Solar Road", 7);
        assertEquals(7L, body.getLong("version")); assertEquals(5, body.length());
        assertFalse(body.has("nic")); assertFalse(body.has("status"));
    }
    @Test public void deactivationIsARequestNotAStatusChange() throws Exception {
        JSONObject body = RequestBodies.deactivate(" Moving home ", 4);
        assertEquals("Moving home", body.getString("reason")); assertEquals(4L, body.getLong("version")); assertEquals(2, body.length());
    }
    @Test public void parsesServerProfileAndPendingRequest() throws Exception {
        JSONObject body = new JSONObject("{\"nic\":\"199012304567\",\"fullName\":\"Solar User\",\"email\":\"solar@example.test\",\"phone\":\"+94771234567\",\"address\":\"10 Solar Road\",\"status\":\"Active\",\"version\":2,\"deactivationRequest\":{\"id\":\"request-1\",\"status\":\"Pending\",\"reason\":\"Moving home\",\"decisionNote\":null}}");
        Profile profile = new Profile(body);
        assertEquals("199012304567", profile.nic); assertEquals(2L, profile.version);
        assertEquals("Active", profile.status); assertEquals("Pending", profile.requestStatus); assertNull(profile.decisionNote);
    }
    @Test public void validationProblemDetailsAreReadable() throws Exception {
        JSONObject problem = new JSONObject("{\"title\":\"Validation failed\",\"errors\":{\"Email\":[\"Enter a valid email.\"]}}");
        assertEquals("Enter a valid email.", ApiClient.problemMessage(problem));
    }
}
