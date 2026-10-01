import os
import unittest
from datetime import datetime, timedelta, timezone
import secrets
import time
import test_component1 as account_tests

request = account_tests.request
COLOMBO = timezone(timedelta(hours=5, minutes=30))

@unittest.skipUnless(account_tests.API, "Run scripts/verify.py")
class PasinduTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        code, auth, _ = request("POST", "auth/staff/login", {"username": os.environ["Bootstrap__Username"], "password": os.environ["Bootstrap__Password"]})
        assert code == 200
        cls.admin = auth["accessToken"]
        
        cls.operator_user = {"username": "op_" + secrets.token_hex(4), "fullName": "Grid Operator", "email": "op@example.test", "password": secrets.token_urlsafe(18), "role": "GridOperator"}
        code, _, _ = request("POST", "staff-users", cls.operator_user, cls.admin)
        assert code == 201
        
        code, auth, _ = request("POST", "auth/staff/login", {"username": cls.operator_user["username"], "password": cls.operator_user["password"]})
        assert code == 200
        cls.operator = auth["accessToken"]
        
        cls.prosumer_nic = "199011112222"
        pwd = secrets.token_urlsafe(18)
        code, _, _ = request("POST", "prosumers", {"nic": cls.prosumer_nic, "fullName": "Test Prosumer", "email": "tp@example.test", "phone": "+94771112222", "address": "10 Test Road", "password": pwd})
        if code != 201 and code != 409:
            assert False, "Failed to create prosumer"
            
        code, auth, _ = request("POST", "auth/prosumers/login", {"nic": cls.prosumer_nic, "password": pwd})
        assert code == 200
        cls.prosumer = auth["accessToken"]

    def create_station_and_slot(self, start_time):
        name = "Station " + secrets.token_hex(4)
        code, s, _ = request("POST", "stations", {"name": name, "address": "Address", "latitude": 6.9, "longitude": 79.8, "capacityKw": 50, "storageKwh": 100, "batterySlots": 4}, self.admin)
        assert code == 201, s
        
        code, _, _ = request("PUT", f"stations/{s['id']}/schedule", {"version": s["version"], "days": list(range(7)), "opensAt": "00:00", "closesAt": "23:59"}, self.operator)
        _, s, _ = request("GET", f"stations/{s['id']}", token=self.admin)
        
        code, result, _ = request("POST", f"stations/{s['id']}/slots", {"version": s["version"], "startsAt": start_time.isoformat(), "endsAt": (start_time + timedelta(hours=1)).isoformat(), "usableSlots": 2, "usableEnergyKwh": 40}, self.operator)
        self.assertEqual(200, code, result)
        _, s, _ = request("GET", f"stations/{s['id']}", token=self.admin)
        return s

    def test_pasindu_qr_and_dashboard_flow(self):
        now = datetime.now(COLOMBO)
        # Create and reserve a future window, then verify once it starts.
        start = now + timedelta(seconds=10)
        if start.hour == 23:
            self.skipTest("Avoid a window crossing the operating day's boundary")
        s = self.create_station_and_slot(start)
        slot_id = s["slots"][0]["id"]
        
        code, r, _ = request("POST", "reservations", {
            "prosumerNic": self.prosumer_nic,
            "stationId": s["id"],
            "slotId": slot_id,
            "reservationDate": start.isoformat(),
            "energyAmountKwh": 10,
            "tradingType": "Export"
        }, self.prosumer)
        self.assertEqual(200, code, r)
        res_id = r["id"]
        self.assertEqual("PENDING", r["status"])
        
        code, dash, _ = request("GET", "Reservations/dashboard", token=self.operator)
        self.assertEqual(200, code)
        self.assertTrue(any(p["id"] == res_id for p in dash["pendingReservations"]))
        
        code, r, _ = request("PATCH", f"reservations/{res_id}/approve", token=self.admin)
        self.assertEqual(200, code)
        
        code, qr, _ = request("GET", f"reservations/{res_id}/transaction", token=self.prosumer)
        self.assertEqual(200, code)
        token = qr["transactionToken"]
        time.sleep(max(0, (start - datetime.now(COLOMBO)).total_seconds()) + 0.1)
        
        code, verified, _ = request("GET", f"Reservations/verify?token={token}", token=self.operator)
        self.assertEqual(200, code, verified)
        
        code, completed, _ = request("PATCH", f"Reservations/{res_id}/complete", token=self.operator)
        self.assertEqual(200, code, completed)
        self.assertEqual("COMPLETED", completed["status"])
        
        code, dash, _ = request("GET", "Reservations/dashboard", token=self.operator)
        self.assertEqual(200, code)
        self.assertTrue(any(c["id"] == res_id for c in dash["recentCompletedReservations"]))

    def test_pasindu_qr_failures(self):
        code, err, _ = request("GET", "Reservations/verify?token=fake", token=self.operator)
        self.assertEqual(400, code)
        
        code, err, _ = request("GET", "Reservations/verify?token=fake", token=self.prosumer)
        self.assertEqual(403, code)

if __name__ == '__main__':
    unittest.main()
