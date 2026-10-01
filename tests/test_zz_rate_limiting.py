"""Run rate-limit exhaustion last so it does not invalidate other fixtures."""
import unittest
from test_component1 import API, request


@unittest.skipUnless(API, "Run scripts/verify.py for isolated integration tests")
class RateLimitingTests(unittest.TestCase):
    def test_99_authentication_rate_limit(self):
        for _ in range(201):
            status, body, headers = request("POST", "auth/staff/login", {"username": "unknown", "password": "incorrect-password"})
            if status == 429:
                self.assertEqual(429, body["status"])
                self.assertIn("Retry-After", headers)
                return
            self.assertEqual(401, status)
        self.fail("Authentication rate limiter did not reject excessive attempts")
