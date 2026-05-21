import requests
from conftest import BASE_URL


class TestProfessions:
    def test_list_professions_public(self):
        r = requests.get(f"{BASE_URL}/professions")
        assert r.status_code == 200
        body = r.json()
        assert isinstance(body, list)

    def test_list_professions_returns_id_and_name(self):
        r = requests.get(f"{BASE_URL}/professions")
        assert r.status_code == 200
        professions = r.json()
        if professions:
            assert "id" in professions[0]
            assert "name" in professions[0]
