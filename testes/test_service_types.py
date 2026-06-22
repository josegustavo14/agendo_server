import requests
from conftest import BASE_URL


class TestCreateServiceType:
    def test_create_service_type(self, professional_data):
        payload = {"name": "Limpeza", "description": "Limpeza dental", "price": 200.00}
        r = requests.post(
            f"{BASE_URL}/service-types",
            json=payload,
            headers=professional_data["headers"],
        )
        assert r.status_code == 201
        body = r.json()
        assert body["name"] == payload["name"]
        assert float(body["price"]) == payload["price"]

    def test_create_service_type_without_token(self):
        payload = {"name": "Serviço X", "description": "Desc", "price": 100.00}
        r = requests.post(f"{BASE_URL}/service-types", json=payload)
        assert r.status_code in (401, 403)


class TestListServiceTypes:
    def test_list_own_service_types(self, professional_data, service_type):
        r = requests.get(
            f"{BASE_URL}/service-types",
            headers=professional_data["headers"],
        )
        assert r.status_code == 200
        services = r.json()
        assert isinstance(services, list)
        assert any(s["id"] == service_type["id"] for s in services)

    def test_list_service_types_without_token(self):
        r = requests.get(f"{BASE_URL}/service-types")
        assert r.status_code in (401, 403)

    def test_list_does_not_return_other_owners(self, client_data, service_type):
        r = requests.get(
            f"{BASE_URL}/service-types",
            headers=client_data["headers"],
        )
        assert r.status_code == 200
        services = r.json()
        assert not any(s["id"] == service_type["id"] for s in services)


class TestGetServiceTypeById:
    def test_get_own_service_type(self, professional_data, service_type):
        r = requests.get(
            f"{BASE_URL}/service-types/{service_type['id']}",
            headers=professional_data["headers"],
        )
        assert r.status_code == 200
        assert r.json()["id"] == service_type["id"]

    def test_get_nonexistent_service_type(self, professional_data):
        r = requests.get(
            f"{BASE_URL}/service-types/999999",
            headers=professional_data["headers"],
        )
        assert r.status_code == 404

    def test_get_other_owners_service_type_forbidden(self, client_data, service_type):
        r = requests.get(
            f"{BASE_URL}/service-types/{service_type['id']}",
            headers=client_data["headers"],
        )
        assert r.status_code in (403, 404)
