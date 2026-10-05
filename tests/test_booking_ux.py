"""Booking labels and window editing against isolated MongoDB/API/web services."""
from datetime import datetime, timedelta, timezone
import http.cookiejar
import os
import secrets
import unittest
import urllib.parse
import urllib.request
from portal_contract import page_data
from test_component1 import API, WEB, request


@unittest.skipUnless(API and WEB, "Run scripts/verify.py for isolated booking tests")
class BookingUxTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        code, auth, _ = request("POST", "auth/staff/login", {
            "username": os.environ["Bootstrap__Username"], "password": os.environ["Bootstrap__Password"]})
        assert code == 200, auth
        cls.admin = auth["accessToken"]
        cls.nic = "19901238" + f"{secrets.randbelow(10000):04}"
        password = secrets.token_urlsafe(20)
        code, profile, _ = request("POST", "prosumers", {
            "nic": cls.nic, "fullName": "Booking UX Prosumer", "email": "booking@example.test",
            "phone": "+94771234567", "address": "10 Solar Road", "password": password})
        assert code == 201, profile
        code, auth, _ = request("POST", "auth/prosumers/login", {"nic": cls.nic, "password": password})
        assert code == 200, auth
        cls.prosumer = auth["accessToken"]

    def call(self, method, path, body=None, token=None, expected=200):
        code, data, _ = request(method, path, body, token or self.admin)
        self.assertEqual(expected, code, data)
        return data

    def setup_booking(self):
        station = self.call("POST", "stations", {"name": "Coastal Solar " + secrets.token_hex(4),
            "address": "10 Coast Road", "latitude": 6.9, "longitude": 79.8,
            "capacityKw": 50, "storageKwh": 100, "batterySlots": 4}, expected=201)
        start = (datetime.now(timezone(timedelta(hours=5, minutes=30))) + timedelta(days=2)).replace(hour=10, minute=0, second=0, microsecond=0)
        for day in (0, 1):
            window_start = start + timedelta(days=day)
            station = self.call("POST", f"stations/{station['id']}/slots", {"version": station["version"],
                "startsAt": window_start.isoformat(), "endsAt": (window_start + timedelta(hours=1)).isoformat(),
                "usableSlots": 1, "usableEnergyKwh": 20})
        booking = self.call("POST", "reservations", {"prosumerNic": self.nic, "stationId": station["id"],
            "slotId": station["slots"][0]["id"], "reservationDate": station["slots"][0]["startsAt"],
            "energyAmountKwh": 10, "tradingType": "DROP_OFF"}, self.prosumer)
        return station, booking

    def test_labels_follow_renames_and_inactive_stations_remain_readable(self):
        station, booking = self.setup_booking()
        self.assertEqual(station["name"], booking["stationName"])
        self.assertEqual(station["address"], booking["stationAddress"])
        self.assertEqual("Booking UX Prosumer", booking["prosumerName"])
        self.assertEqual(datetime.fromisoformat(station["slots"][0]["startsAt"]), datetime.fromisoformat(booking["slotStartsAt"]))
        self.assertEqual(datetime.fromisoformat(station["slots"][0]["endsAt"]), datetime.fromisoformat(booking["slotEndsAt"]))
        current = self.call("GET", "stations/" + station["id"])
        fields = ("name", "address", "latitude", "longitude", "capacityKw", "storageKwh", "batterySlots", "version")
        renamed = self.call("PATCH", "stations/" + station["id"], {
            **{k: current[k] for k in fields}, "name": "Renamed Coastal Solar"})
        pending = self.call("GET", "reservations/pending")
        self.assertEqual(renamed["name"], next(b for b in pending if b["id"] == booking["id"])["stationName"])
        self.call("PATCH", f"reservations/{booking['id']}/cancel", {"reason": "Changed plans"}, self.prosumer)
        current = self.call("GET", "stations/" + station["id"])
        self.call("POST", f"stations/{station['id']}/status", {"version": current["version"], "active": False})
        history = self.call("GET", "reservations/history?nic=" + self.nic, token=self.prosumer)
        self.assertEqual(renamed["name"], next(b for b in history if b["id"] == booking["id"])["stationName"])

        browser = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        browser.addheaders = [("Accept", "application/json")]
        with browser.open(WEB + "/Account/Login") as response:
            csrf = page_data(response.read().decode())["csrfToken"]
        credentials = urllib.parse.urlencode({"Username": os.environ["Bootstrap__Username"],
            "Password": os.environ["Bootstrap__Password"], "__RequestVerificationToken": csrf}).encode()
        browser.open(WEB + "/Account/Login", credentials).close()
        with browser.open(WEB + "/Bookings?stationId=" + station["id"]) as response:
            page = page_data(response.read().decode())
        self.assertEqual(1, len(page["model"]))
        self.assertEqual(renamed["name"], page["model"][0]["stationName"])
        choice = next(s for s in page["meta"]["Stations"] if s["id"] == station["id"])
        self.assertEqual(renamed["name"], choice["name"])
        self.assertFalse(choice["active"])

    def test_window_changes_move_inventory_and_rejected_changes_preserve_it(self):
        station, booking = self.setup_booking()
        original, target = station["slots"]
        update = {"slotId": target["id"], "reservationDate": target["startsAt"],
            "energyAmountKwh": 15, "tradingType": "DROP_OFF"}
        changed = self.call("PUT", f"reservations/{booking['id']}", update, self.prosumer)
        self.assertEqual(datetime.fromisoformat(target["startsAt"]), datetime.fromisoformat(changed["slotStartsAt"]))
        current = self.call("GET", "stations/" + station["id"])
        windows = {w["id"]: w for w in current["slots"]}
        self.assertEqual(1, windows[original["id"]]["availableSlots"])
        self.assertEqual(20, windows[original["id"]]["availableEnergyKwh"])
        self.assertEqual(0, windows[target["id"]]["availableSlots"])
        self.assertEqual(5, windows[target["id"]]["availableEnergyKwh"])
        self.assertEqual(1, len(windows[target["id"]]["allocations"]))
        self.call("PUT", f"reservations/{booking['id']}", {**update, "energyAmountKwh": 25}, self.prosumer, expected=400)
        self.assertEqual(current, self.call("GET", "stations/" + station["id"]))
        unchanged = next(b for b in self.call("GET", "reservations/history?nic=" + self.nic, token=self.prosumer)
            if b["id"] == booking["id"])
        self.assertEqual(15, unchanged["energyAmountKwh"])
        self.call("PATCH", f"reservations/{booking['id']}/cancel", {}, self.prosumer)
        released = self.call("GET", "stations/" + station["id"])
        self.assertEqual(20, next(w for w in released["slots"] if w["id"] == target["id"])["availableEnergyKwh"])
