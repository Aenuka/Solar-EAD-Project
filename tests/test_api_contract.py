"""Wire-contract regressions for the backend's move from controllers to Minimal APIs."""
import base64
import json
import os
import secrets
import unittest
import urllib.error
import urllib.parse
import urllib.request

from test_component1 import API, request


@unittest.skipUnless(API, "Run scripts/verify.py for isolated MongoDB/API tests")
class ApiContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        code, session, _ = request("POST", "auth/staff/login", {
            "username": os.environ["Bootstrap__Username"],
            "password": os.environ["Bootstrap__Password"],
        })
        assert code == 200, session
        cls.admin = session["accessToken"]
        cls.registration = {
            "nic": "19901238" + f"{secrets.randbelow(10000):04}",
            "fullName": "Contract Prosumer", "email": "contract@example.test",
            "phone": "+94771234567", "address": "25 Contract Road",
            "password": secrets.token_urlsafe(18),
        }
        code, cls.profile, cls.registration_headers = request("POST", "prosumers", cls.registration)
        assert code == 201, cls.profile
        code, cls.session, _ = request("POST", "auth/prosumers/login", {
            key: cls.registration[key] for key in ("nic", "password")
        })
        assert code == 200, cls.session
        cls.token = cls.session["accessToken"]

    def test_login_and_profile_field_names_and_jwt_claims(self):
        self.assertEqual({"accessToken", "expiresAt", "tokenType", "id", "fullName", "role"}, set(self.session))
        self.assertEqual("Bearer", self.session["tokenType"])
        self.assertEqual("Prosumer", self.session["role"])
        self.assertEqual(self.registration["nic"], self.session["id"])
        parts = self.token.split(".")
        self.assertEqual(3, len(parts))
        header, claims = [json.loads(base64.urlsafe_b64decode(part + "=" * (-len(part) % 4))) for part in parts[:2]]
        self.assertEqual({"alg": "HS256", "typ": "JWT"}, header)
        self.assertEqual({"sub", "name", "role", "sv", "jti", "iat", "nbf", "exp", "iss", "aud"}, set(claims))
        self.assertEqual(self.profile["nic"], claims["sub"])
        self.assertEqual(self.profile["fullName"], claims["name"])
        self.assertEqual("Prosumer", claims["role"])
        self.assertIsInstance(claims["sv"], str)
        self.assertIsInstance(claims["iat"], int)
        self.assertEqual("solar-microgrid-api", claims["iss"])
        self.assertEqual("solar-microgrid-clients", claims["aud"])
        self.assertEqual({"nic", "fullName", "email", "phone", "address", "status", "version", "createdAt",
                          "deactivationRequest", "recentEvents"}, set(self.profile))
        self.assertEqual("/api/v1/prosumers/me", urllib.parse.urlparse(self.registration_headers["Location"]).path)
        code, profile, _ = request("GET", "prosumers/me", token=self.token)
        self.assertEqual(200, code)
        self.assertEqual(self.profile, profile)

    def test_data_annotations_return_field_error_arrays(self):
        code, problem, headers = request("PATCH", "prosumers/me", {
            **{key: self.profile[key] for key in ("fullName", "email", "phone", "address", "version")},
            "fullName": " ", "email": "invalid-email",
        }, self.token)
        self.assertEqual(400, code)
        self.assertEqual(400, problem["status"])
        self.assertIn("application/problem+json", headers["Content-Type"])
        self.assertIn("traceId", problem)
        self.assertTrue(problem["errors"])
        self.assertTrue(all(isinstance(messages, list) and messages for messages in problem["errors"].values()))
        code, unchanged, _ = request("GET", "prosumers/me", token=self.token)
        self.assertEqual(200, code)
        self.assertEqual(self.profile, unchanged)

    def test_query_validation_on_each_endpoint_group(self):
        for path in ("staff-users?page=0", "staff-users?pageSize=101", "prosumers?search=" + "a" * 101,
                     "prosumers?status=999", "prosumers?requestStatus=999", "prosumers?page=abc",
                     "stations?page=0", "stations?pageSize=101", "stations?radiusKm=0",
                     "stations?latitude=91&longitude=79", "stations?activeOnly=invalid"):
            with self.subTest(path=path):
                code, problem, _ = request("GET", path, token=self.admin)
                self.assertEqual(400, code, problem)
                self.assertEqual(400, problem["status"])

    def test_missing_required_json_fields_and_invalid_enum_formats(self):
        profile_update = {key: self.profile[key] for key in ("fullName", "email", "phone", "address")}
        station = {"name": "Contract Station", "address": "25 Station Road", "longitude": 79.8,
                   "capacityKw": 50, "storageKwh": 100, "batterySlots": 4}
        staff = {"username": "contract_staff", "fullName": "Contract Staff", "email": "staff@example.test",
                 "password": secrets.token_urlsafe(18)}
        cases = [
            ("PATCH", "prosumers/me", profile_update, self.token),
            ("PATCH", "prosumers/me", {**profile_update, "version": "1"}, self.token),
            ("PATCH", "prosumers/me", {**profile_update, "version": 1, "nic": "199012399999"}, self.token),
            ("POST", "stations", station, self.admin),
            ("POST", "stations/missing/status", {"version": 1}, self.admin),
            ("POST", "stations/missing/slots", {"version": 1}, self.admin),
            ("POST", "staff-users", {**staff, "role": 0}, self.admin),
            ("POST", "staff-users", {**staff, "role": None}, self.admin),
            ("POST", "staff-users", {**staff, "role": "Invalid"}, self.admin),
        ]
        for method, path, body, token in cases:
            with self.subTest(path=path, body=body):
                code, problem, _ = request(method, path, body, token)
                self.assertEqual(400, code, problem)
                self.assertEqual(400, problem["status"])

    def test_malformed_json_uses_json_problem_responses(self):
        cases = [
            ("auth/staff/login", b'{"username":', "application/json", None, 400),
            ("auth/staff/login", b"null", "application/json", None, 400),
            ("auth/staff/login", b"username=test", "text/plain", None, 415),
            ("prosumers", b"nic=test", "text/plain", None, 415),
            ("staff-users", b"username=test", "text/plain", None, 401),
            ("staff-users", b"username=test", "text/plain", self.token, 403),
            ("staff-users", b"username=test", "text/plain", self.admin, 415),
        ]
        for path, body, content_type, token, expected in cases:
            with self.subTest(path=path, expected=expected):
                headers = {"Content-Type": content_type, "Accept": "application/json"}
                if token:
                    headers["Authorization"] = "Bearer " + token
                req = urllib.request.Request(API + path, data=body, headers=headers)
                with self.assertRaises(urllib.error.HTTPError) as caught:
                    urllib.request.urlopen(req, timeout=20)
                with caught.exception as response:
                    self.assertEqual(expected, response.status)
                    self.assertIn("application/problem+json", response.headers["Content-Type"])
                    problem = json.load(response)
                    self.assertEqual(expected, problem["status"])
                    self.assertIn("traceId", problem)
                    if expected == 400:
                        self.assertTrue(problem["errors"])
