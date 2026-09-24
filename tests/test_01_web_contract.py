"""MVC form/session regressions for the tutorial-style .NET refactor."""
import http.cookiejar
import os
import re
import secrets
import unittest
import urllib.parse
import urllib.request

from test_component1 import API, WEB, request


@unittest.skipUnless(API and WEB, "Run scripts/verify.py for isolated API/MVC tests")
class WebContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        code, session, _ = request("POST", "auth/staff/login", {
            "username": os.environ["Bootstrap__Username"],
            "password": os.environ["Bootstrap__Password"],
        })
        assert code == 200, session
        cls.admin = session["accessToken"]

    def setUp(self):
        self.cookies = http.cookiejar.CookieJar()
        self.browser = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))

    def get(self, path):
        with self.browser.open(WEB + path, timeout=20) as response:
            return response.read().decode(), response.url

    def post(self, path, data, html):
        token = re.search(r'name="__RequestVerificationToken"[^>]*value="([^"]+)"', html).group(1)
        body = urllib.parse.urlencode({**data, "__RequestVerificationToken": token}).encode()
        with self.browser.open(WEB + path, body, timeout=20) as response:
            return response.read().decode(), response.url

    def login(self, username=None, password=None):
        html, _ = self.get("/Account/Login")
        return self.post("/Account/Login", {
            "Username": username or os.environ["Bootstrap__Username"],
            "Password": password or os.environ["Bootstrap__Password"],
        }, html)

    def new_operator(self):
        credentials = {
            "username": "web_contract_" + secrets.token_hex(5),
            "fullName": "Portal Contract Operator", "email": "portal@example.test",
            "password": secrets.token_urlsafe(18), "role": "GridOperator",
        }
        code, staff, _ = request("POST", "staff-users", credentials, self.admin)
        self.assertEqual(201, code, staff)
        code, session, _ = request("POST", "auth/staff/login", {
            key: credentials[key] for key in ("username", "password")
        })
        self.assertEqual(200, code, session)
        return staff, credentials, session["accessToken"]

    def test_invalid_login_keeps_form_without_creating_session(self):
        html, url = self.login("missing_" + secrets.token_hex(4), "invalid-password")
        self.assertIn("/Account/Login", url)
        self.assertIn("Invalid credentials or account unavailable.", html)
        self.assertFalse(any(cookie.name == "SolarMicrogrid.Session" for cookie in self.cookies))
        _, url = self.get("/Staff")
        self.assertIn("/Account/Login", url)

    def test_invalid_staff_form_keeps_values_without_creating_account(self):
        self.login()
        code, before, _ = request("GET", "staff-users", token=self.admin)
        self.assertEqual(200, code)
        html, _ = self.get("/Staff/Create")
        html, url = self.post("/Staff/Create", {
            "Username": "form_invalid", "FullName": "Keep this name", "Email": "invalid-email",
            "Password": "short", "Role": "GridOperator",
        }, html)
        self.assertIn("/Staff/Create", url)
        self.assertIn('value="Keep this name"', html)
        self.assertIn("validation-summary-errors", html)
        code, after, _ = request("GET", "staff-users", token=self.admin)
        self.assertEqual(200, code)
        self.assertEqual(before["total"], after["total"])

    def test_empty_station_window_form_does_not_change_inventory(self):
        self.login()
        code, station, _ = request("POST", "stations", {
            "name": "Portal validation station", "address": "10 Portal Road",
            "latitude": 6.9, "longitude": 79.8, "capacityKw": 50, "storageKwh": 100, "batterySlots": 4,
        }, self.admin)
        self.assertEqual(201, code, station)
        html, _ = self.get("/Stations/Details/" + station["id"])
        html, url = self.post("/Stations/AddSlot/" + station["id"], {
            "Version": station["version"], "StartsAt": "", "EndsAt": "", "UsableSlots": "", "UsableEnergyKwh": "",
        }, html)
        self.assertIn("/Stations/Details/", url)
        self.assertIn('role="alert" class="notice error"', html)
        self.assertIn("required", html)
        code, after, _ = request("GET", "stations/" + station["id"], token=self.admin)
        self.assertEqual(200, code)
        self.assertEqual(station, after)

    def test_logout_clears_cookie_and_revokes_api_sessions(self):
        _, credentials, token = self.new_operator()
        html, _ = self.login(credentials["username"], credentials["password"])
        self.assertTrue(any(cookie.name == "SolarMicrogrid.Session" for cookie in self.cookies))
        _, url = self.post("/Account/Logout", {}, html)
        self.assertIn("/Account/Login", url)
        self.assertFalse(any(cookie.name == "SolarMicrogrid.Session" for cookie in self.cookies))
        self.assertEqual(401, request("GET", "auth/me", token=token)[0])
        _, url = self.get("/Stations")
        self.assertIn("/Account/Login", url)

    def test_revoked_account_rejects_existing_browser_cookie(self):
        staff, credentials, _ = self.new_operator()
        self.login(credentials["username"], credentials["password"])
        code, result, _ = request("PATCH", "staff-users/" + staff["id"], {
            "fullName": staff["fullName"], "email": staff["email"], "role": staff["role"],
            "status": "Inactive", "version": staff["version"],
        }, self.admin)
        self.assertEqual(200, code, result)
        _, url = self.get("/Stations")
        self.assertIn("/Account/Login", url)
        self.assertFalse(any(cookie.name == "SolarMicrogrid.Session" for cookie in self.cookies))
