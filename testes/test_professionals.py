import requests
from conftest import BASE_URL, create_professional


class TestSearchProfessionals:
    def test_list_all_professionals(self):
        r = requests.get(f"{BASE_URL}/professionals")
        assert r.status_code == 200
        assert isinstance(r.json(), list)

    def test_search_by_name(self, professional_data):
        name = professional_data["user"]["name"]
        r = requests.get(f"{BASE_URL}/professionals", params={"name": name})
        assert r.status_code == 200
        results = r.json()
        assert any(p["name"] == name for p in results)

    def test_search_by_profession_id(self):
        r = requests.get(f"{BASE_URL}/professionals", params={"professionId": 1})
        assert r.status_code == 200
        assert isinstance(r.json(), list)

    def test_search_empty_name_returns_all(self):
        r = requests.get(f"{BASE_URL}/professionals", params={"name": ""})
        assert r.status_code == 200

    def test_search_nonexistent_name(self):
        r = requests.get(
            f"{BASE_URL}/professionals",
            params={"name": "xxxxnaoexistexxx"},
        )
        assert r.status_code == 200
        assert r.json() == []


class TestGetProfessionalById:
    def test_get_existing_professional(self, professional_data):
        prof_id = professional_data["user"]["id"]
        r = requests.get(f"{BASE_URL}/professionals/{prof_id}")
        assert r.status_code == 200
        body = r.json()
        assert body["id"] == prof_id

    def test_get_nonexistent_professional(self):
        r = requests.get(f"{BASE_URL}/professionals/999999")
        assert r.status_code == 404

    def test_professional_response_fields(self, professional_data):
        prof_id = professional_data["user"]["id"]
        r = requests.get(f"{BASE_URL}/professionals/{prof_id}")
        body = r.json()
        for field in ("id", "name", "professionName"):
            assert field in body


class TestProfessionalServices:
    def test_get_professional_services(self, professional_data, service_type):
        prof_id = professional_data["user"]["id"]
        r = requests.get(f"{BASE_URL}/professionals/{prof_id}/services")
        assert r.status_code == 200
        services = r.json()
        assert isinstance(services, list)
        assert any(s["id"] == service_type["id"] for s in services)

    def test_get_services_nonexistent_professional(self):
        r = requests.get(f"{BASE_URL}/professionals/999999/services")
        assert r.status_code == 404
