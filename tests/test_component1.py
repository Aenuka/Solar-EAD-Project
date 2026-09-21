"""Security and lifecycle integration checks; launched by scripts/verify.py."""
from concurrent.futures import ThreadPoolExecutor
import base64
import hashlib
import hmac
import http.cookiejar
import json
import os
import re
import secrets
import time
import unittest
import urllib.error
import urllib.parse
import urllib.request

API = os.environ.get("TEST_API_URL", "")
WEB = os.environ.get("TEST_WEB_URL", "")


def request(method, path, body=None, token=None):
    data = None if body is None else json.dumps(body).encode()
    headers = {"Accept": "application/json"}
    if data is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(API + path, data=data, headers=headers, method=method)
    try:
        response = urllib.request.urlopen(req, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        payload = response.read()
        return response.status, json.loads(payload) if payload else None, response.headers


@unittest.skipUnless(API, "Use python3 scripts/verify.py to start an isolated test environment")
class ComponentOneTests(unittest.TestCase):
    sequence = 1000

    @classmethod
    def setUpClass(cls):
        status, session, _ = request("POST", "auth/staff/login", {"username": os.environ["Bootstrap__Username"], "password": os.environ["Bootstrap__Password"]})
        if status != 200:
            raise AssertionError(f"Bootstrap login failed: {status} {session}")
        cls.admin = session["accessToken"]

    def new_prosumer(self, login=True):
        ComponentOneTests.sequence += 1
        nic = "19901230" + str(ComponentOneTests.sequence)
        body = {"nic": nic, "fullName": "Test Prosumer", "email": "solar@example.test", "phone": "+94771234567", "address": "10 Solar Road", "password": secrets.token_urlsafe(18)}
        status, profile, _ = request("POST", "prosumers", body)
        self.assertEqual(201, status, profile)
        token = None
        if login:
            status, session, _ = request("POST", "auth/prosumers/login", {"nic": nic, "password": body["password"]})
            self.assertEqual(200, status, session)
            token = session["accessToken"]
        return profile, token, body

    def new_staff(self, role="GridOperator"):
        body = {"username": "staff_" + secrets.token_hex(5), "fullName": "Test Staff", "email": "staff@example.test", "password": secrets.token_urlsafe(18), "role": role}
        status, user, _ = request("POST", "staff-users", body, self.admin)
        self.assertEqual(201, status, user)
        status, session, _ = request("POST", "auth/staff/login", {"username": body["username"], "password": body["password"]})
        self.assertEqual(200, status, session)
        return user, session["accessToken"], body

    def profile_update(self, profile, **overrides):
        return {**{field: profile[field] for field in ("fullName", "email", "phone", "address", "version")}, **overrides}

    def pending(self):
        profile, token, body = self.new_prosumer()
        status, pending, _ = request("POST", "prosumers/me/deactivation-requests", {"reason": "Moving to a new home", "version": profile["version"]}, token)
        self.assertEqual(201, status, pending)
        return pending, token, body

    def decision_path(self, profile):
        return f"prosumers/{profile['nic']}/deactivation-requests/{profile['deactivationRequest']['id']}/decision"

    def test_01_registration_contract_and_sensitive_fields(self):
        profile, token, _ = self.new_prosumer()
        self.assertEqual("Active", profile["status"])
        self.assertIsNone(profile["deactivationRequest"])
        self.assertIsInstance(profile["version"], int)
        for forbidden in ("password", "passwordHash", "securityVersion"):
            self.assertNotIn(forbidden, profile)
        status, mine, headers = request("GET", "prosumers/me", token=token)
        self.assertEqual(200, status)
        self.assertEqual(profile["nic"], mine["nic"])
        self.assertIn("no-store", headers["Cache-Control"])

    def test_02_legacy_nic_is_same_identity(self):
        profile, _, body = self.new_prosumer(login=False)
        legacy = profile["nic"][2:7] + profile["nic"][8:] + "v"
        status, session, _ = request("POST", "auth/prosumers/login", {"nic": legacy, "password": body["password"]})
        self.assertEqual(200, status, session)
        self.assertEqual(profile["nic"], session["id"])
        status, _, _ = request("POST", "prosumers", {**body, "nic": legacy.upper()})
        self.assertEqual(409, status)

    def test_03_parallel_duplicate_registration(self):
        _, _, body = self.new_prosumer(login=False)
        body["nic"] = "19901239" + str(ComponentOneTests.sequence)
        with ThreadPoolExecutor(max_workers=2) as pool:
            results = list(pool.map(lambda _: request("POST", "prosumers", body)[0], range(2)))
        self.assertEqual([201, 409], sorted(results))

    def test_04_server_validation_and_overposting(self):
        _, _, body = self.new_prosumer(login=False)
        for changes in ({"nic": "not-a-nic"}, {"nic": "199000012345"}, {"email": "bad"}, {"password": "short"}, {"role": "Backoffice"}, {"status": "Inactive"}, {"fullName": "   "}):
            with self.subTest(changes=list(changes)):
                status, error, _ = request("POST", "prosumers", {**body, **changes})
                self.assertEqual(400, status, error)
                self.assertEqual(400, error["status"])

    def test_05_invalid_credentials_are_generic(self):
        profile, _, _ = self.new_prosumer(login=False)
        first = request("POST", "auth/prosumers/login", {"nic": profile["nic"], "password": "incorrect-password"})
        second = request("POST", "auth/prosumers/login", {"nic": "199012399999", "password": "incorrect-password"})
        self.assertEqual(401, first[0]); self.assertEqual(401, second[0])
        self.assertEqual(first[1]["detail"], second[1]["detail"])

    def test_06_unauthenticated_access_is_rejected(self):
        for path in ("staff-users", "prosumers", "prosumers/me", "dashboard", "auth/me"):
            with self.subTest(path=path):
                status, error, headers = request("GET", path)
                self.assertEqual(401, status)
                self.assertEqual(401, error["status"])
                self.assertIn("Bearer", headers["WWW-Authenticate"])

    def test_07_prosumer_cannot_access_other_accounts_or_staff(self):
        _, token, _ = self.new_prosumer()
        other, _, _ = self.new_prosumer(login=False)
        for path in ("staff-users", "dashboard", "prosumers", "prosumers/" + other["nic"]):
            self.assertEqual(403, request("GET", path, token=token)[0])
        self.assertEqual(403, request("PATCH", "prosumers/" + other["nic"], self.profile_update(other), token)[0])

    def test_08_profile_update_and_stale_version(self):
        profile, token, _ = self.new_prosumer()
        body = self.profile_update(profile, fullName="Updated Prosumer")
        status, updated, _ = request("PATCH", "prosumers/me", body, token)
        self.assertEqual(200, status, updated)
        self.assertEqual("Updated Prosumer", updated["fullName"])
        self.assertEqual(profile["version"] + 1, updated["version"])
        self.assertEqual(409, request("PATCH", "prosumers/me", body, token)[0])
        self.assertEqual(400, request("PATCH", "prosumers/me", {**self.profile_update(updated), "nic": "199012399999"}, token)[0])

    def test_09_grid_operator_is_not_an_administrator(self):
        _, token, _ = self.new_staff()
        self.assertEqual(200, request("GET", "auth/me", token=token)[0])
        for path in ("staff-users", "prosumers", "dashboard", "prosumers/me"):
            self.assertEqual(403, request("GET", path, token=token)[0])

    def test_10_staff_role_change_revokes_existing_session(self):
        staff, token, credentials = self.new_staff()
        body = {"fullName": staff["fullName"], "email": staff["email"], "role": "Backoffice", "status": "Active", "version": staff["version"]}
        status, updated, _ = request("PATCH", "staff-users/" + staff["id"], body, self.admin)
        self.assertEqual(200, status, updated)
        self.assertEqual(401, request("GET", "auth/me", token=token)[0])
        status, session, _ = request("POST", "auth/staff/login", {"username": credentials["username"], "password": credentials["password"]})
        self.assertEqual(200, status); self.assertEqual("Backoffice", session["role"])
        self.assertEqual(200, request("GET", "staff-users", token=session["accessToken"])[0])

    def test_11_staff_deactivation_and_username_uniqueness(self):
        staff, token, credentials = self.new_staff()
        self.assertEqual(409, request("POST", "staff-users", {**credentials, "username": credentials["username"].upper()}, self.admin)[0])
        body = {"fullName": staff["fullName"], "email": staff["email"], "role": staff["role"], "status": "Inactive", "version": staff["version"]}
        self.assertEqual(200, request("PATCH", "staff-users/" + staff["id"], body, self.admin)[0])
        self.assertEqual(401, request("GET", "auth/me", token=token)[0])
        self.assertEqual(401, request("POST", "auth/staff/login", {"username": credentials["username"], "password": credentials["password"]})[0])

    def test_12_bootstrap_and_self_access_are_protected(self):
        status, staff, _ = request("GET", "staff-users/bootstrap", token=self.admin)
        self.assertEqual(200, status)
        body = {"fullName": staff["fullName"], "email": staff["email"], "role": "GridOperator", "status": "Inactive", "version": staff["version"]}
        self.assertEqual(403, request("PATCH", "staff-users/bootstrap", body, self.admin)[0])
        own, token, _ = self.new_staff("Backoffice")
        body.update({"fullName": own["fullName"], "email": own["email"], "version": own["version"]})
        self.assertEqual(403, request("PATCH", "staff-users/" + own["id"], body, token)[0])

    def test_13_pending_request_keeps_account_active_and_rejects_duplicate(self):
        profile, token, _ = self.pending()
        self.assertEqual("Active", profile["status"])
        self.assertEqual("Pending", profile["deactivationRequest"]["status"])
        self.assertEqual(200, request("GET", "prosumers/me", token=token)[0])
        self.assertEqual(409, request("POST", "prosumers/me/deactivation-requests", {"reason": "Another request", "version": profile["version"]}, token)[0])

    def test_14_approval_revokes_sessions_and_reactivation_requires_new_login(self):
        profile, token, body = self.pending()
        _, operator, _ = self.new_staff()
        decision = {"decision": "Approved", "note": "Approved after review", "version": profile["version"]}
        self.assertEqual(403, request("POST", self.decision_path(profile), decision, operator)[0])
        status, inactive, _ = request("POST", self.decision_path(profile), decision, self.admin)
        self.assertEqual(200, status, inactive)
        self.assertEqual("Inactive", inactive["status"]); self.assertEqual("Approved", inactive["deactivationRequest"]["status"])
        self.assertEqual(401, request("GET", "prosumers/me", token=token)[0])
        login = {"nic": profile["nic"], "password": body["password"]}
        self.assertEqual(401, request("POST", "auth/prosumers/login", login)[0])
        status, active, _ = request("POST", "prosumers/" + profile["nic"] + "/reactivation", {"note": "Member requested reactivation", "version": inactive["version"]}, self.admin)
        self.assertEqual(200, status, active); self.assertEqual("Active", active["status"])
        self.assertEqual(401, request("GET", "prosumers/me", token=token)[0])
        self.assertEqual(200, request("POST", "auth/prosumers/login", login)[0])

    def test_15_rejection_keeps_session_and_allows_a_new_request(self):
        profile, token, _ = self.pending()
        status, result, _ = request("POST", self.decision_path(profile), {"decision": "Rejected", "note": "Please contact Backoffice first", "version": profile["version"]}, self.admin)
        self.assertEqual(200, status, result); self.assertEqual("Active", result["status"])
        self.assertEqual(200, request("GET", "prosumers/me", token=token)[0])
        status, new, _ = request("POST", "prosumers/me/deactivation-requests", {"reason": "Discussed with Backoffice", "version": result["version"]}, token)
        self.assertEqual(201, status, new)
        self.assertNotEqual(profile["deactivationRequest"]["id"], new["deactivationRequest"]["id"])

    def test_16_competing_decisions_cannot_both_succeed(self):
        profile, _, _ = self.pending()
        def decide(value):
            return request("POST", self.decision_path(profile), {"decision": value, "note": "Concurrent review decision", "version": profile["version"]}, self.admin)
        with ThreadPoolExecutor(max_workers=2) as pool:
            results = list(pool.map(decide, ["Approved", "Rejected"]))
        self.assertEqual([200, 409], sorted(result[0] for result in results))
        winner = next(result[1] for result in results if result[0] == 200)
        status, stored, _ = request("GET", "prosumers/" + profile["nic"], token=self.admin)
        self.assertEqual(200, status); self.assertEqual(winner["status"], stored["status"])
        self.assertEqual(winner["deactivationRequest"], stored["deactivationRequest"])

    def test_17_logout_revokes_all_sessions(self):
        profile, first, credentials = self.new_prosumer()
        _, session, _ = request("POST", "auth/prosumers/login", {"nic": profile["nic"], "password": credentials["password"]})
        self.assertEqual(204, request("POST", "auth/logout", token=first)[0])
        self.assertEqual(401, request("GET", "auth/me", token=first)[0])
        self.assertEqual(401, request("GET", "auth/me", token=session["accessToken"])[0])

    def test_18_pagination_filters_and_invalid_values(self):
        profile, _, _ = self.pending()
        status, page, _ = request("GET", "prosumers?requestStatus=Pending&pageSize=1", token=self.admin)
        self.assertEqual(200, status); self.assertEqual(1, len(page["items"]))
        self.assertGreater(page["total"], 0)
        self.assertEqual("Pending", page["items"][0]["deactivationRequest"]["status"])
        for query in ("page=0", "pageSize=101", "status=Unknown", "requestStatus=Unknown"):
            self.assertEqual(400, request("GET", "prosumers?" + query, token=self.admin)[0])

    def test_19_forged_token_is_rejected(self):
        _, token, _ = self.new_prosumer()
        parts = token.split(".")
        parts[2] = ("a" if parts[2][0] != "a" else "b") + parts[2][1:]
        self.assertEqual(401, request("GET", "auth/me", token=".".join(parts))[0])

    def test_20_web_login_antiforgery_and_backoffice_pages(self):
        jar = http.cookiejar.CookieJar()
        browser = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
        with browser.open(WEB + "/Account/Login") as response:
            html = response.read().decode()
        token = re.search(r'name="__RequestVerificationToken"[^>]*value="([^"]+)"', html).group(1)
        form = {"Username": os.environ["Bootstrap__Username"], "Password": os.environ["Bootstrap__Password"]}
        with self.assertRaises(urllib.error.HTTPError) as rejected:
            browser.open(WEB + "/Account/Login", urllib.parse.urlencode(form).encode())
        self.assertEqual(400, rejected.exception.code)
        form["__RequestVerificationToken"] = token
        with browser.open(WEB + "/Account/Login", urllib.parse.urlencode(form).encode()) as response:
            html = response.read().decode()
            self.assertEqual(200, response.status)
            self.assertIn("Account overview", html)
            self.assertIn("frame-ancestors 'none'", response.headers["Content-Security-Policy"])
        for path in ("/Staff", "/Staff/Create", "/Staff/Edit/bootstrap", "/Prosumers", "/Prosumers?pending=true"):
            with browser.open(WEB + path) as response:
                self.assertEqual(200, response.status, path)
        profile, _, _ = self.new_prosumer(login=False)
        with browser.open(WEB + "/Prosumers/Details/" + profile["nic"]) as response:
            self.assertIn("Profile details", response.read().decode())

    def test_21_web_grid_operator_cannot_view_admin_pages(self):
        _, _, credentials = self.new_staff()
        browser = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        with browser.open(WEB + "/Account/Login") as response:
            html = response.read().decode()
        token = re.search(r'name="__RequestVerificationToken"[^>]*value="([^"]+)"', html).group(1)
        form = {"Username": credentials["username"], "Password": credentials["password"], "__RequestVerificationToken": token}
        with browser.open(WEB + "/Account/Login", urllib.parse.urlencode(form).encode()) as response:
            self.assertIn("Grid Operator access", response.read().decode())
        with self.assertRaises(urllib.error.HTTPError) as denied:
            browser.open(WEB + "/Staff")
        self.assertEqual(403, denied.exception.code)
        self.assertIn("Your role does not have access", denied.exception.read().decode())

    def test_22_jwt_issuer_audience_expiry_and_algorithm(self):
        _, token, _ = self.new_prosumer()
        encoded_header, encoded_payload, _ = token.split(".")
        claims = json.loads(base64.urlsafe_b64decode(encoded_payload + "=" * (-len(encoded_payload) % 4)))
        def encode(value):
            return base64.urlsafe_b64encode(json.dumps(value, separators=(",", ":")).encode()).decode().rstrip("=")
        for override in ({"iss": "wrong-issuer"}, {"aud": "wrong-audience"}, {"exp": int(time.time()) - 120}, {"nbf": int(time.time()) + 120}):
            with self.subTest(claim=list(override)[0]):
                message = encoded_header + "." + encode({**claims, **override})
                signature = base64.urlsafe_b64encode(hmac.new(os.environ["Jwt__SigningKey"].encode(), message.encode(), hashlib.sha256).digest()).decode().rstrip("=")
                self.assertEqual(401, request("GET", "auth/me", token=message + "." + signature)[0])
        unsigned = encode({"alg": "none", "typ": "JWT"}) + "." + encoded_payload + "."
        self.assertEqual(401, request("GET", "auth/me", token=unsigned)[0])

    def test_23_web_profile_and_lifecycle_forms(self):
        browser = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        def get(path):
            with browser.open(WEB + path) as response:
                return response.read().decode()
        def post(path, form, source):
            token = re.search(r'name="__RequestVerificationToken"[^>]*value="([^"]+)"', source).group(1)
            with browser.open(WEB + path, urllib.parse.urlencode({**form, "__RequestVerificationToken": token}).encode()) as response:
                return response.read().decode()
        login = get("/Account/Login")
        post("/Account/Login", {"Username": os.environ["Bootstrap__Username"], "Password": os.environ["Bootstrap__Password"]}, login)
        profile, _, _ = self.pending()
        detail_path = "/Prosumers/Details/" + profile["nic"]
        html = post(detail_path, {**self.profile_update(profile), "fullName": "Edited through MVC"}, get(detail_path))
        self.assertIn("Profile updated.", html)
        _, current, _ = request("GET", "prosumers/" + profile["nic"], token=self.admin)
        self.assertEqual("Edited through MVC", current["fullName"])
        html = post("/Prosumers/Decide/" + profile["nic"], {"RequestId": current["deactivationRequest"]["id"], "Version": current["version"], "Decision": "Approved", "Note": "Reviewed using the staff portal"}, html)
        self.assertIn("Deactivation request reviewed.", html)
        _, inactive, _ = request("GET", "prosumers/" + profile["nic"], token=self.admin)
        self.assertEqual("Inactive", inactive["status"])
        html = post("/Prosumers/Reactivate/" + profile["nic"], {"Version": inactive["version"], "Note": "Member is returning to the community"}, html)
        self.assertIn("Account reactivated.", html)
        _, active, _ = request("GET", "prosumers/" + profile["nic"], token=self.admin)
        self.assertEqual("Active", active["status"])

    def test_24_openapi_matches_android_wire_types(self):
        with urllib.request.urlopen(API.removesuffix("api/v1/") + "openapi/v1.json") as response:
            document = json.load(response)
        schemas = document["components"]["schemas"]
        self.assertEqual("string", schemas["AccountStatus"]["type"])
        self.assertEqual(["Active", "Inactive"], schemas["AccountStatus"]["enum"])
        self.assertEqual("integer", schemas["UpdateProfileRequest"]["properties"]["version"]["type"])
        self.assertEqual(12, schemas["RegisterProsumerRequest"]["properties"]["password"]["minLength"])
        self.assertIn("Bearer", document["components"]["securitySchemes"])
        self.assertIn("security", document["paths"]["/api/v1/prosumers/me"]["get"])
        self.assertNotIn("security", document["paths"]["/api/v1/auth/prosumers/login"]["post"])

    def test_25_web_staff_creation_and_update(self):
        browser = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        def get(path):
            with browser.open(WEB + path) as response:
                return response.read().decode()
        def post(path, form, source):
            token = re.search(r'name="__RequestVerificationToken"[^>]*value="([^"]+)"', source).group(1)
            with browser.open(WEB + path, urllib.parse.urlencode({**form, "__RequestVerificationToken": token}).encode()) as response:
                return response.read().decode()
        login = get("/Account/Login")
        post("/Account/Login", {"Username": os.environ["Bootstrap__Username"], "Password": os.environ["Bootstrap__Password"]}, login)
        username, password = "web_staff_" + secrets.token_hex(5), secrets.token_urlsafe(18)
        html = post("/Staff/Create", {"Username": username, "Password": password, "FullName": "Web-created Staff", "Email": "webstaff@example.test", "Role": "GridOperator"}, get("/Staff/Create"))
        self.assertIn("Staff account created.", html)
        status, session, _ = request("POST", "auth/staff/login", {"username": username, "password": password})
        self.assertEqual(200, status, session)
        _, staff, _ = request("GET", "staff-users/" + session["id"], token=self.admin)
        edit = "/Staff/Edit/" + staff["id"]
        html = post(edit, {"FullName": staff["fullName"], "Email": staff["email"], "Role": "GridOperator", "Status": "Inactive", "Version": staff["version"]}, get(edit))
        self.assertIn("Staff account updated.", html)
        self.assertEqual(401, request("GET", "auth/me", token=session["accessToken"])[0])

    def test_99_authentication_rate_limit(self):
        for _ in range(201):
            status, body, headers = request("POST", "auth/staff/login", {"username": "unknown", "password": "incorrect-password"})
            if status == 429:
                self.assertEqual(429, body["status"])
                self.assertIn("Retry-After", headers)
                return
            self.assertEqual(401, status)
        self.fail("Authentication rate limiter did not reject excessive attempts")
