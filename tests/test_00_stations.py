"""Station inventory integration tests; run before the account suite exhausts the auth limiter."""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
from portal_contract import page_data

import http.cookiejar
import os
import secrets
import time
import unittest
import urllib.parse
import urllib.request
import test_component1 as account_tests

request = account_tests.request
COLOMBO = timezone(timedelta(hours=5, minutes=30))


@unittest.skipUnless(account_tests.API, "Run scripts/verify.py for isolated MongoDB/API/web tests")
class StationTests(unittest.TestCase):
    @classmethod
    # Signs in test roles and creates a prosumer for station permission checks. *****
    def setUpClass(cls):
        code, auth, _ = request("POST", "auth/staff/login", {"username": os.environ["Bootstrap__Username"], "password": os.environ["Bootstrap__Password"]})
        assert code == 200, auth
        cls.admin = auth["accessToken"]
        cls.operator_user = {"username": "station_ops_" + secrets.token_hex(4), "fullName": "Station Operator", "email": "ops@example.test", "password": secrets.token_urlsafe(18), "role": "GridOperator"}
        code, result, _ = request("POST", "staff-users", cls.operator_user, cls.admin)
        assert code == 201, result
        code, result, _ = request("POST", "auth/staff/login", {k: cls.operator_user[k] for k in ("username", "password")})
        assert code == 200, result
        cls.operator = result["accessToken"]
        password = secrets.token_urlsafe(18)
        code, result, _ = request("POST", "prosumers", {"nic": "199012399999", "fullName": "Map Prosumer", "email": "map@example.test", "phone": "+94771234567", "address": "20 Solar Road", "password": password})
        assert code == 201, result
        code, result, _ = request("POST", "auth/prosumers/login", {"nic": "199012399999", "password": password})
        assert code == 200, result
        cls.prosumer = result["accessToken"]

    # Sends an API request and checks its expected HTTP status. *****
    def call(self, method, path, body=None, token=None, expected=200):
        code, data, _ = request(method, path, body, token or self.admin)
        self.assertEqual(expected, code, data)
        return data

    # Creates a station fixture with realistic Colombo coordinates and capacity. *****
    def station(self):
        return self.call("POST", "stations", {"name": "Test Solar " + secrets.token_hex(4), "address": "10 Station Road, Colombo", "latitude": 6.9271, "longitude": 79.8612, "capacityKw": 50, "storageKwh": 100, "batterySlots": 4}, expected=201)

    # Adds a future energy window to a station fixture. *****
    def window(self, s, start=None, slots=2):
        start = start or (datetime.now(COLOMBO) + timedelta(days=2)).replace(hour=10, minute=0, second=0, microsecond=0)
        return self.call("POST", f"stations/{s['id']}/slots", {"version": s["version"], "startsAt": start.isoformat(), "endsAt": (start + timedelta(hours=1)).isoformat(), "usableSlots": slots, "usableEnergyKwh": 40}, self.operator)

    # Reserves capacity in the fixture's first energy window. *****
    def reserve(self, s, booking="booking-1", expected=200, slots=1):
        return self.call("POST", f"stations/{s['id']}/slots/{s['slots'][0]['id']}/allocations", {"version": s["version"], "bookingId": booking, "slots": slots, "energyKwh": 10}, self.operator, expected)

    # Checks role permissions and rejects invalid station input. *****
    def test_permissions_and_validation(self):
        self.assertEqual(401, request("GET", "stations")[0])
        s = self.station()
        body = {k: s[k] for k in ("name", "address", "latitude", "longitude", "capacityKw", "storageKwh", "batterySlots")}
        self.call("POST", "stations", body, self.operator, 403)
        self.call("POST", "stations", {**body, "latitude": 91}, expected=400)
        self.call("POST", "stations", {**body, "capacityKw": -1}, expected=400)
        self.call("POST", "stations", {**body, "active": False}, expected=400)
        self.call("POST", f"stations/{s['id']}/status", {"active": False, "version": s["version"]}, self.operator, 403)
        self.call("POST", f"stations/{s['id']}/slots", {}, self.prosumer, 403)
        self.call("GET", "stations?latitude=6", token=self.prosumer, expected=400)
        self.call("GET", "stations?latitude=99&longitude=79", token=self.prosumer, expected=400)

    # Ensures stale station versions cannot overwrite newer edits. *****
    def test_update_and_stale_version(self):
        s = self.station()
        update = {k: s[k] for k in ("name", "address", "latitude", "longitude", "capacityKw", "storageKwh", "batterySlots", "version")}
        updated = self.call("PATCH", f"stations/{s['id']}", {**update, "name": "Renamed Station"})
        self.assertEqual(s["id"], updated["id"])
        self.call("PATCH", f"stations/{s['id']}", update, expected=409)

    # Blocks deactivation with reservations and makes repeated cancellation safe. *****
    def test_deactivation_reservations_and_idempotent_cancel(self):
        s = self.window(self.station())
        original = s
        s = self.reserve(s)
        repeated = self.reserve(original)
        self.assertEqual(s["version"], repeated["version"])
        self.assertEqual(1, s["slots"][0]["availableSlots"])
        self.call("POST", f"stations/{s['id']}/status", {"active": False, "version": s["version"]}, expected=409)
        self.call("PUT", f"stations/{s['id']}/slots/{s['slots'][0]['id']}/availability", {"version": s["version"], "usableSlots": 0, "usableEnergyKwh": 0}, self.operator, 409)
        path = f"stations/{s['id']}/slots/{s['slots'][0]['id']}/allocations/booking-1/cancel"
        cancelled = self.call("POST", path, {"version": s["version"]}, self.operator)
        repeated = self.call("POST", path, {"version": s["version"]}, self.operator)
        self.assertEqual(cancelled["version"], repeated["version"])
        self.assertEqual(2, cancelled["slots"][0]["availableSlots"])
        inactive = self.call("POST", f"stations/{s['id']}/status", {"active": False, "version": cancelled["version"]})
        self.call("GET", f"stations/{s['id']}", token=self.prosumer, expected=404)
        self.reserve(inactive, "another", 409)
        active = self.call("POST", f"stations/{s['id']}/status", {"active": True, "version": inactive["version"]})
        self.assertTrue(active["active"])

    # Ensures concurrent writes cannot overbook or deactivate a reserved station. *****
    def test_concurrent_allocations_and_deactivation(self):
        s = self.window(self.station(), slots=1)
        path = f"stations/{s['id']}/slots/{s['slots'][0]['id']}/allocations"
        with ThreadPoolExecutor(2) as pool:
            codes = list(pool.map(lambda booking: request("POST", path, {"version": s["version"], "bookingId": booking, "slots": 1, "energyKwh": 10}, self.operator)[0], ["race-a", "race-b"]))
        self.assertEqual([200, 409], sorted(codes))
        fresh = self.call("GET", f"stations/{s['id']}")
        self.assertEqual(0, fresh["slots"][0]["availableSlots"])
        s = self.window(self.station())
        with ThreadPoolExecutor(2) as pool:
            reserve = pool.submit(request, "POST", f"stations/{s['id']}/slots/{s['slots'][0]['id']}/allocations", {"version": s["version"], "bookingId": "status-race", "slots": 1, "energyKwh": 10}, self.operator)
            deactivate = pool.submit(request, "POST", f"stations/{s['id']}/status", {"version": s["version"], "active": False}, self.admin)
            self.assertEqual([200, 409], sorted([reserve.result()[0], deactivate.result()[0]]))
        fresh = self.call("GET", f"stations/{s['id']}")
        self.assertFalse(not fresh["active"] and fresh["activeReservations"] > 0)

    # Rejects invalid schedules, overlapping windows, and excess capacity. *****
    def test_schedule_overlap_capacity_and_archive(self):
        s = self.station()
        path = f"stations/{s['id']}/schedule"
        self.call("PUT", path, {"version": s["version"], "days": [], "opensAt": "08:00", "closesAt": "18:00"}, self.operator, 400)
        self.call("PUT", path, {"version": s["version"], "days": [7], "opensAt": "08:00", "closesAt": "18:00"}, self.operator, 400)
        self.call("PUT", path, {"version": s["version"], "days": [1], "opensAt": "18:00", "closesAt": "08:00"}, self.operator, 400)
        s = self.window(s)
        slot = s["slots"][0]
        self.call("POST", f"stations/{s['id']}/slots", {"version": s["version"], "startsAt": slot["startsAt"], "endsAt": slot["endsAt"], "usableSlots": 1, "usableEnergyKwh": 1}, self.operator, 409)
        self.call("PUT", path, {"version": s["version"], "days": list(range(7)), "opensAt": "12:00", "closesAt": "18:00"}, self.operator, 409)
        self.call("PUT", f"stations/{s['id']}/slots/{slot['id']}/availability", {"version": s["version"], "usableSlots": 5, "usableEnergyKwh": 40}, self.operator, 409)
        self.call("PUT", f"stations/{s['id']}/slots/{slot['id']}/availability", {"version": s["version"], "usableSlots": 2, "usableEnergyKwh": 51}, self.operator, 409)
        archived = self.call("POST", f"stations/{s['id']}/slots/{slot['id']}/archive", {"version": s["version"]}, self.operator)
        self.assertEqual([], archived["slots"])

    # Filters nearby stations and hides staff allocation details from prosumers. *****
    def test_nearby_visibility_and_private_allocation_data(self):
        s = self.reserve(self.window(self.station()))
        result = self.call("GET", "stations?latitude=6.9271&longitude=79.8612&radiusKm=1&pageSize=100", token=self.prosumer)
        found = next(item for item in result["items"] if item["id"] == s["id"])
        self.assertLess(found["distanceKm"], 0.01)
        detail = self.call("GET", f"stations/{s['id']}", token=self.prosumer)
        self.assertEqual([], detail["slots"][0]["allocations"])
        far = self.call("GET", "stations?latitude=0&longitude=0&radiusKm=1", token=self.prosumer)
        self.assertEqual(0, far["total"])

    # Rejects reservations starting outside the next seven days. *****
    def test_seven_day_limit(self):
        start = (datetime.now(COLOMBO) + timedelta(days=8)).replace(hour=10, minute=0, second=0, microsecond=0)
        s = self.window(self.station(), start)
        self.reserve(s, expected=409)

    # Enforces transfer completion timing and 12-hour cancellation notice. *****
    def test_completion_and_twelve_hour_cancellation(self):
        s = self.station()
        s = self.call("PUT", f"stations/{s['id']}/schedule", {"version": s["version"], "days": list(range(7)), "opensAt": "00:00", "closesAt": "23:59"}, self.operator)
        start = datetime.now(COLOMBO) + timedelta(seconds=5)
        if start.hour == 23: self.skipTest("Avoid a window crossing the operating day's boundary")
        s = self.reserve(self.window(s, start))
        path = f"stations/{s['id']}/slots/{s['slots'][0]['id']}/allocations/booking-1"
        self.call("POST", path + "/cancel", {"version": s["version"]}, self.operator, 409)
        self.call("POST", path + "/complete", {"version": s["version"]}, self.operator, 409)
        time.sleep(max(0, (start - datetime.now(COLOMBO)).total_seconds()) + 0.1)
        completed = self.call("POST", path + "/complete", {"version": s["version"]}, self.operator)
        self.assertEqual(0, completed["activeReservations"])
        self.assertEqual(1, completed["slots"][0]["availableSlots"])
        self.call("POST", f"stations/{s['id']}/status", {"active": False, "version": completed["version"]})

    # Checks station web forms and Grid Operator access to staff actions. *****
    def test_web_forms_and_operator_access(self):
        browser = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        # Fetches a staff portal page with the current authenticated session. *****
        def get(path):
            with browser.open(account_tests.WEB + path) as response: return response.read().decode()
        # Posts a station form while preserving the portal's antiforgery data. *****
        def post(path, data, html):
            token = page_data(html)["csrfToken"]
            with browser.open(account_tests.WEB + path, urllib.parse.urlencode({**data, "__RequestVerificationToken": token}, doseq=True).encode()) as response:
                return response.read().decode(), response.url
        post("/Account/Login", {"Username": os.environ["Bootstrap__Username"], "Password": os.environ["Bootstrap__Password"]}, get("/Account/Login"))
        html, url = post("/Stations/Create", {"Name": "Web Solar Station", "Address": "10 Test Station Road", "Latitude": 6.9, "Longitude": 79.8, "CapacityKw": 50, "StorageKwh": 100, "BatterySlots": 4}, get("/Stations/Create"))
        self.assertIn("Station created", html)
        station_id = url.rsplit("/", 1)[-1]
        s = self.call("GET", "stations/" + station_id)
        html, _ = post("/Stations/Schedule/" + station_id, {"Version": s["version"], "Days": list(range(7)), "OpensAt": "07:00", "ClosesAt": "19:00"}, html)
        self.assertIn("Station updated", html)
        s = self.call("GET", "stations/" + station_id)
        start = (datetime.now(COLOMBO) + timedelta(days=2)).replace(hour=9, minute=0, second=0, microsecond=0)
        html, _ = post("/Stations/AddSlot/" + station_id, {"Version": s["version"], "StartsAt": start.strftime("%Y-%m-%dT%H:%M"), "EndsAt": (start + timedelta(hours=1)).strftime("%Y-%m-%dT%H:%M"), "UsableSlots": 4, "UsableEnergyKwh": 40}, html)
        self.assertIn("Station updated", html)
        s = self.call("GET", "stations/" + station_id)
        self.assertEqual(start, datetime.fromisoformat(s["slots"][0]["startsAt"]))
        html, _ = post("/Stations/AddSlot/" + station_id, {"Version": s["version"], "StartsAt": start.strftime("%Y-%m-%dT%H:%M"), "EndsAt": (start + timedelta(hours=1)).strftime("%Y-%m-%dT%H:%M"), "UsableSlots": 4, "UsableEnergyKwh": 40}, html)
        self.assertIn('Station time windows cannot overlap.', html)
        unchanged = self.call("GET", "stations/" + station_id)
        self.assertEqual(s["version"], unchanged["version"])
        self.assertEqual(1, len(unchanged["slots"]))
        late_start = start.replace(hour=22)
        html, _ = post("/Stations/AddSlot/" + station_id, {"Version": s["version"], "StartsAt": late_start.strftime("%Y-%m-%dT%H:%M"), "EndsAt": late_start.replace(hour=23).strftime("%Y-%m-%dT%H:%M"), "UsableSlots": 4, "UsableEnergyKwh": 40}, html)
        self.assertIn('The slot must fall within', html)
        self.assertIn("operating days and hours (Asia/Colombo)", html)
        html, _ = post("/Stations/Availability/" + station_id + "?slotId=" + s["slots"][0]["id"], {"Version": s["version"], "UsableSlots": 4, "UsableEnergyKwh": 60}, html)
        self.assertIn('Usable energy exceeds', html)
        self.assertIn("50 kW for 1 hour(s) permits at most 50 kWh.", html)
        unchanged = self.call("GET", "stations/" + station_id)
        self.assertEqual(s["version"], unchanged["version"])
        self.assertEqual(s["slots"], unchanged["slots"])
        browser = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        post("/Account/Login", {"Username": self.operator_user["username"], "Password": self.operator_user["password"]}, get("/Account/Login"))
        html = get("/Stations/Details/" + station_id)
        self.assertEqual("GridOperator", page_data(html)["user"]["role"])
        self.assertEqual(s["schedule"], page_data(html)["model"]["schedule"])
        self.assertEqual("Details", page_data(html)["page"])
        html, _ = post("/Stations/Availability/" + station_id + "?slotId=" + s["slots"][0]["id"], {"Version": s["version"], "UsableSlots": 3, "UsableEnergyKwh": 30}, html)
        self.assertIn("Station updated", html)
        updated = self.call("GET", "stations/" + station_id)
        self.assertEqual(3, updated["slots"][0]["availableSlots"])
        with self.assertRaises(urllib.error.HTTPError) as error: get("/Stations/Create")
        self.assertEqual(403, error.exception.code)
        error.exception.close()
