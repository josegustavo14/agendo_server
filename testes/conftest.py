import os
import pytest
import requests
import uuid

BASE_URL = os.environ.get("AGENDO_BASE_URL", "http://localhost:9090")


def unique_email():
    return f"test_{uuid.uuid4().hex[:8]}@test.com"


def create_professional(profession_id=1):
    payload = {
        "name": "Prof Teste",
        "email": unique_email(),
        "phone": "11999990001",
        "role": "PROFESSIONAL",
        "password": "senha123",
        "professionId": profession_id,
        "bio": "Bio de teste",
    }
    r = requests.post(f"{BASE_URL}/users", json=payload)
    assert r.status_code == 201, r.text
    return r.json()


def create_client():
    payload = {
        "name": "Cliente Teste",
        "email": unique_email(),
        "phone": "11999990002",
        "role": "CLIENT",
        "password": "senha123",
        "taxId": "123.456.789-00",
        "preferredPaymentMethod": "PIX",
    }
    r = requests.post(f"{BASE_URL}/users", json=payload)
    assert r.status_code == 201, r.text
    return r.json()


def login(email, password="senha123"):
    r = requests.post(f"{BASE_URL}/users/login", json={"email": email, "password": password})
    assert r.status_code == 200, r.text
    return r.json()["token"]


def auth_headers(token):
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture(scope="session")
def professional_data():
    data = create_professional()
    token = login(data["email"])
    return {"user": data, "token": token, "headers": auth_headers(token)}


@pytest.fixture(scope="session")
def client_data():
    data = create_client()
    token = login(data["email"])
    return {"user": data, "token": token, "headers": auth_headers(token)}


@pytest.fixture(scope="session")
def service_type(professional_data):
    payload = {"name": "Consulta", "description": "Consulta geral", "price": 150.00}
    r = requests.post(
        f"{BASE_URL}/service-types",
        json=payload,
        headers=professional_data["headers"],
    )
    assert r.status_code == 201, r.text
    return r.json()


@pytest.fixture(scope="session")
def appointment(professional_data, client_data, service_type):
    payload = {
        "professionalId": professional_data["user"]["id"],
        "clientId": client_data["user"]["id"],
        "serviceTypeIds": [service_type["id"]],
        "scheduleDate": "2026-12-01T10:00:00",
    }
    r = requests.post(
        f"{BASE_URL}/appointments",
        json=payload,
        headers=client_data["headers"],
    )
    assert r.status_code == 201, r.text
    return r.json()
