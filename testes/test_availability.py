import requests
from conftest import BASE_URL

WEEKLY_SCHEDULE = [
    {"dayOfWeek": "MONDAY", "startTime": "08:00", "endTime": "17:00", "slotDurationMinutes": 60},
    {"dayOfWeek": "TUESDAY", "startTime": "09:00", "endTime": "18:00", "slotDurationMinutes": 30},
    {"dayOfWeek": "WEDNESDAY", "startTime": "08:00", "endTime": "12:00", "slotDurationMinutes": 60},
]


class TestWeeklySchedule:
    def test_save_weekly_schedule(self, professional_data):
        payload = {"schedule": WEEKLY_SCHEDULE}
        r = requests.post(
            f"{BASE_URL}/availability/schedule",
            json=payload,
            headers=professional_data["headers"],
        )
        assert r.status_code == 200
        schedule = r.json()
        assert isinstance(schedule, list)
        days = [s["dayOfWeek"] for s in schedule]
        assert "MONDAY" in days

    def test_get_weekly_schedule(self, professional_data):
        requests.post(
            f"{BASE_URL}/availability/schedule",
            json={"schedule": WEEKLY_SCHEDULE},
            headers=professional_data["headers"],
        )
        r = requests.get(
            f"{BASE_URL}/availability/schedule",
            headers=professional_data["headers"],
        )
        assert r.status_code == 200
        assert isinstance(r.json(), list)

    def test_get_schedule_without_token(self):
        r = requests.get(f"{BASE_URL}/availability/schedule")
        assert r.status_code in (401, 403)

    def test_save_schedule_without_token(self):
        r = requests.post(
            f"{BASE_URL}/availability/schedule",
            json={"schedule": WEEKLY_SCHEDULE},
        )
        assert r.status_code in (401, 403)

    def test_delete_day_from_schedule(self, professional_data):
        requests.post(
            f"{BASE_URL}/availability/schedule",
            json={"schedule": WEEKLY_SCHEDULE},
            headers=professional_data["headers"],
        )
        r = requests.delete(
            f"{BASE_URL}/availability/schedule/WEDNESDAY",
            headers=professional_data["headers"],
        )
        assert r.status_code == 204

        get_r = requests.get(
            f"{BASE_URL}/availability/schedule",
            headers=professional_data["headers"],
        )
        days = [s["dayOfWeek"] for s in get_r.json()]
        assert "WEDNESDAY" not in days

    def test_delete_day_without_token(self):
        r = requests.delete(f"{BASE_URL}/availability/schedule/MONDAY")
        assert r.status_code in (401, 403)


class TestTimeSlots:
    def test_get_available_slots_public(self, professional_data):
        requests.post(
            f"{BASE_URL}/availability/schedule",
            json={"schedule": [
                {"dayOfWeek": "MONDAY", "startTime": "08:00", "endTime": "12:00", "slotDurationMinutes": 60}
            ]},
            headers=professional_data["headers"],
        )
        prof_id = professional_data["user"]["id"]
        r = requests.get(
            f"{BASE_URL}/availability/{prof_id}/slots",
            params={"date": "2026-11-30"},
        )
        assert r.status_code == 200
        slots = r.json()
        assert isinstance(slots, list)
        if slots:
            assert "time" in slots[0]
            assert "available" in slots[0]

    def test_get_slots_nonexistent_professional(self):
        r = requests.get(
            f"{BASE_URL}/availability/999999/slots",
            params={"date": "2026-11-30"},
        )
        assert r.status_code in (200, 404)

    def test_get_slots_day_without_schedule(self, professional_data):
        prof_id = professional_data["user"]["id"]
        r = requests.get(
            f"{BASE_URL}/availability/{prof_id}/slots",
            params={"date": "2026-11-29"},
        )
        assert r.status_code == 200
        assert r.json() == []
