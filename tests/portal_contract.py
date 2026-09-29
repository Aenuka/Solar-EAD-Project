"""Read the React bootstrap document or the JSON response used by React forms."""
import json
import re


def page_data(source):
    if source.lstrip().startswith("{"):
        return json.loads(source)
    match = re.search(r'<script id="portal-data" type="application/json">(.*?)</script>', source, re.S)
    if not match:
        raise AssertionError("Missing React bootstrap data")
    return json.loads(match.group(1))
