import pytest
import requests
from conftest import BASE_URL, unique_email, create_professional, create_client, login, auth_headers


class TestCreateUser:
    def test_create_professional(self):
        payload = {
            "name": "Maria Silva",
            "email": unique_email(),
            "phone": "11999990001",
            "role": "PROFESSIONAL",
            "password": "senha123",
            "professionId": 1,
            "bio": "Dentista experiente",
        }
        r = requests.post(f"{BASE_URL}/users", json=payload)
        assert r.status_code == 201
        body = r.json()
        assert body["name"] == payload["name"]
        assert body["email"] == payload["email"]
        assert body["role"] == "PROFESSIONAL"
        assert "id" in body

    def test_create_client(self):
        payload = {
            "name": "João Costa",
            "email": unique_email(),
            "phone": "11988880001",
            "role": "CLIENT",
            "password": "senha123",
            "taxId": "111.222.333-44",
            "preferredPaymentMethod": "PIX",
        }
        r = requests.post(f"{BASE_URL}/users", json=payload)
        assert r.status_code == 201
        body = r.json()
        assert body["role"] == "CLIENT"

    def test_create_user_duplicate_email(self):
        email = unique_email()
        payload = {
            "name": "User A",
            "email": email,
            "phone": "11900000001",
            "role": "CLIENT",
            "password": "senha123",
        }
        requests.post(f"{BASE_URL}/users", json=payload)
        r = requests.post(f"{BASE_URL}/users", json=payload)
        assert r.status_code in (400, 409, 500)


class TestLogin:
    def test_login_valid_credentials(self):
        data = create_client()
        r = requests.post(
            f"{BASE_URL}/users/login",
            json={"email": data["email"], "password": "senha123"},
        )
        assert r.status_code == 200
        body = r.json()
        assert "token" in body
        assert body["email"] == data["email"]

    def test_login_wrong_password(self):
        data = create_client()
        r = requests.post(
            f"{BASE_URL}/users/login",
            json={"email": data["email"], "password": "errada"},
        )
        assert r.status_code in (400, 401, 403)

    def test_login_nonexistent_email(self):
        r = requests.post(
            f"{BASE_URL}/users/login",
            json={"email": "naoexiste@test.com", "password": "senha123"},
        )
        assert r.status_code in (400, 401, 404)


class TestGetMe:
    def test_get_me_authenticated(self, client_data):
        r = requests.get(f"{BASE_URL}/users/me", headers=client_data["headers"])
        assert r.status_code == 200
        body = r.json()
        assert body["email"] == client_data["user"]["email"]

    def test_get_me_without_token(self):
        r = requests.get(f"{BASE_URL}/users/me")
        assert r.status_code in (401, 403)

    def test_get_me_invalid_token(self):
        r = requests.get(
            f"{BASE_URL}/users/me",
            headers={"Authorization": "Bearer token-invalido"},
        )
        assert r.status_code in (401, 403)
