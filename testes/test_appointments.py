import pytest
import requests
from conftest import BASE_URL, create_professional, create_client, login, auth_headers, unique_email


class TestCreateAppointment:
    def test_client_creates_appointment(self, professional_data, client_data, service_type):
        payload = {
            "professionalId": professional_data["user"]["id"],
            "clientId": client_data["user"]["id"],
            "serviceTypeIds": [service_type["id"]],
            "scheduleDate": "2026-12-15T14:00:00",
        }
        r = requests.post(
            f"{BASE_URL}/appointments",
            json=payload,
            headers=client_data["headers"],
        )
        assert r.status_code == 201
        body = r.json()
        assert body["status"] == "PENDING"
        assert body["professional"]["id"] == professional_data["user"]["id"]
        assert body["client"]["id"] == client_data["user"]["id"]

    def test_create_appointment_without_token(self, professional_data, client_data, service_type):
        payload = {
            "professionalId": professional_data["user"]["id"],
            "clientId": client_data["user"]["id"],
            "serviceTypeIds": [service_type["id"]],
            "scheduleDate": "2026-12-16T10:00:00",
        }
        r = requests.post(f"{BASE_URL}/appointments", json=payload)
        assert r.status_code in (401, 403)


class TestListAppointments:
    def test_list_appointments_as_client(self, client_data, appointment):
        r = requests.get(
            f"{BASE_URL}/appointments",
            headers=client_data["headers"],
        )
        assert r.status_code == 200
        assert isinstance(r.json(), list)

    def test_list_appointments_as_professional(self, professional_data, appointment):
        r = requests.get(
            f"{BASE_URL}/appointments",
            headers=professional_data["headers"],
        )
        assert r.status_code == 200
        assert isinstance(r.json(), list)

    def test_list_appointments_with_role_filter_professional(self, professional_data):
        r = requests.get(
            f"{BASE_URL}/appointments",
            params={"role": "professional"},
            headers=professional_data["headers"],
        )
        assert r.status_code == 200

    def test_list_appointments_with_role_filter_client(self, client_data):
        r = requests.get(
            f"{BASE_URL}/appointments",
            params={"role": "client"},
            headers=client_data["headers"],
        )
        assert r.status_code == 200

    def test_list_appointments_without_token(self):
        r = requests.get(f"{BASE_URL}/appointments")
        assert r.status_code in (401, 403)

    def test_list_active_appointments(self, client_data, appointment):
        r = requests.get(
            f"{BASE_URL}/appointments/active",
            headers=client_data["headers"],
        )
        assert r.status_code == 200
        for appt in r.json():
            assert appt["status"] in ("PENDING", "APPROVED")

    def test_list_archive_appointments(self, client_data):
        r = requests.get(
            f"{BASE_URL}/appointments/archive",
            headers=client_data["headers"],
        )
        assert r.status_code == 200
        for appt in r.json():
            assert appt["status"] in ("COMPLETED", "CANCELLED", "REJECTED")

    def test_list_professional_appointments(self, professional_data):
        r = requests.get(
            f"{BASE_URL}/appointments/professional",
            headers=professional_data["headers"],
        )
        assert r.status_code == 200


class TestGetAppointmentById:
    def test_get_appointment_by_id(self, client_data, appointment):
        r = requests.get(
            f"{BASE_URL}/appointments/{appointment['id']}",
            headers=client_data["headers"],
        )
        assert r.status_code == 200
        assert r.json()["id"] == appointment["id"]

    def test_get_nonexistent_appointment(self, client_data):
        r = requests.get(
            f"{BASE_URL}/appointments/999999",
            headers=client_data["headers"],
        )
        assert r.status_code == 404

    def test_get_appointment_unauthorized_user(self, professional_data, client_data, service_type):
        other_client_data = create_client()
        other_token = login(other_client_data["email"])
        payload = {
            "professionalId": professional_data["user"]["id"],
            "clientId": client_data["user"]["id"],
            "serviceTypeIds": [service_type["id"]],
            "scheduleDate": "2026-12-20T09:00:00",
        }
        create_r = requests.post(
            f"{BASE_URL}/appointments",
            json=payload,
            headers=client_data["headers"],
        )
        appt_id = create_r.json()["id"]
        r = requests.get(
            f"{BASE_URL}/appointments/{appt_id}",
            headers=auth_headers(other_token),
        )
        assert r.status_code in (403, 404)


class TestAppointmentTimeline:
    def test_get_timeline(self, client_data, appointment):
        r = requests.get(
            f"{BASE_URL}/appointments/{appointment['id']}/timeline",
            headers=client_data["headers"],
        )
        assert r.status_code == 200
        assert isinstance(r.json(), list)

    def test_get_timeline_nonexistent_appointment(self, client_data):
        r = requests.get(
            f"{BASE_URL}/appointments/999999/timeline",
            headers=client_data["headers"],
        )
        assert r.status_code == 404


class TestAppointmentLifecycle:
    """Tests the full status flow: PENDING -> APPROVED -> COMPLETED / CANCELLED"""

    def _create_fresh_appointment(self, professional_data, client_data, service_type, date):
        payload = {
            "professionalId": professional_data["user"]["id"],
            "clientId": client_data["user"]["id"],
            "serviceTypeIds": [service_type["id"]],
            "scheduleDate": date,
        }
        r = requests.post(
            f"{BASE_URL}/appointments",
            json=payload,
            headers=client_data["headers"],
        )
        assert r.status_code == 201
        return r.json()

    def test_professional_approves_appointment(self, professional_data, client_data, service_type):
        appt = self._create_fresh_appointment(
            professional_data, client_data, service_type, "2026-11-10T10:00:00"
        )
        r = requests.patch(
            f"{BASE_URL}/appointments/{appt['id']}/approve",
            headers=professional_data["headers"],
        )
        assert r.status_code == 200
        assert r.json()["status"] == "APPROVED"

    def test_client_cannot_approve_appointment(self, professional_data, client_data, service_type):
        appt = self._create_fresh_appointment(
            professional_data, client_data, service_type, "2026-11-11T10:00:00"
        )
        r = requests.patch(
            f"{BASE_URL}/appointments/{appt['id']}/approve",
            headers=client_data["headers"],
        )
        assert r.status_code in (400, 403)

    def test_professional_rejects_appointment(self, professional_data, client_data, service_type):
        appt = self._create_fresh_appointment(
            professional_data, client_data, service_type, "2026-11-12T10:00:00"
        )
        r = requests.patch(
            f"{BASE_URL}/appointments/{appt['id']}/reject",
            headers=professional_data["headers"],
        )
        assert r.status_code == 200
        assert r.json()["status"] == "REJECTED"

    def test_client_cannot_reject_appointment(self, professional_data, client_data, service_type):
        appt = self._create_fresh_appointment(
            professional_data, client_data, service_type, "2026-11-13T10:00:00"
        )
        r = requests.patch(
            f"{BASE_URL}/appointments/{appt['id']}/reject",
            headers=client_data["headers"],
        )
        assert r.status_code in (400, 403)

    def test_client_cancels_approved_appointment(self, professional_data, client_data, service_type):
        appt = self._create_fresh_appointment(
            professional_data, client_data, service_type, "2026-11-14T10:00:00"
        )
        requests.patch(
            f"{BASE_URL}/appointments/{appt['id']}/approve",
            headers=professional_data["headers"],
        )
        r = requests.patch(
            f"{BASE_URL}/appointments/{appt['id']}/cancel",
            headers=client_data["headers"],
        )
        assert r.status_code == 200
        assert r.json()["status"] == "CANCELLED"

    def test_cannot_cancel_pending_appointment(self, professional_data, client_data, service_type):
        appt = self._create_fresh_appointment(
            professional_data, client_data, service_type, "2026-11-15T10:00:00"
        )
        r = requests.patch(
            f"{BASE_URL}/appointments/{appt['id']}/cancel",
            headers=client_data["headers"],
        )
        assert r.status_code in (400, 403)

    def test_professional_completes_appointment(self, professional_data, client_data, service_type):
        appt = self._create_fresh_appointment(
            professional_data, client_data, service_type, "2026-11-16T10:00:00"
        )
        requests.patch(
            f"{BASE_URL}/appointments/{appt['id']}/approve",
            headers=professional_data["headers"],
        )
        r = requests.patch(
            f"{BASE_URL}/appointments/{appt['id']}/complete",
            headers=professional_data["headers"],
        )
        assert r.status_code == 200
        assert r.json()["status"] == "COMPLETED"

    def test_client_cannot_complete_appointment(self, professional_data, client_data, service_type):
        appt = self._create_fresh_appointment(
            professional_data, client_data, service_type, "2026-11-17T10:00:00"
        )
        requests.patch(
            f"{BASE_URL}/appointments/{appt['id']}/approve",
            headers=professional_data["headers"],
        )
        r = requests.patch(
            f"{BASE_URL}/appointments/{appt['id']}/complete",
            headers=client_data["headers"],
        )
        assert r.status_code in (400, 403)

    def test_cannot_approve_already_approved(self, professional_data, client_data, service_type):
        appt = self._create_fresh_appointment(
            professional_data, client_data, service_type, "2026-11-18T10:00:00"
        )
        requests.patch(
            f"{BASE_URL}/appointments/{appt['id']}/approve",
            headers=professional_data["headers"],
        )
        r = requests.patch(
            f"{BASE_URL}/appointments/{appt['id']}/approve",
            headers=professional_data["headers"],
        )
        assert r.status_code in (400, 409)
