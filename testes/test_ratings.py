import requests
from conftest import BASE_URL, create_client, login, auth_headers


class TestCreateRating:
    def test_client_creates_rating(self, professional_data, client_data):
        payload = {
            "professionalId": professional_data["user"]["id"],
            "score": 5,
            "comment": "Ótimo profissional!",
        }
        r = requests.post(
            f"{BASE_URL}/ratings",
            json=payload,
            headers=client_data["headers"],
        )
        assert r.status_code == 201
        body = r.json()
        assert body["score"] == 5
        assert body["professionalId"] == professional_data["user"]["id"]
        assert body["clientId"] == client_data["user"]["id"]

    def test_professional_cannot_create_rating(self, professional_data):
        payload = {
            "professionalId": professional_data["user"]["id"],
            "score": 4,
            "comment": "Auto avaliação",
        }
        r = requests.post(
            f"{BASE_URL}/ratings",
            json=payload,
            headers=professional_data["headers"],
        )
        assert r.status_code in (400, 403)

    def test_create_rating_without_token(self, professional_data):
        payload = {
            "professionalId": professional_data["user"]["id"],
            "score": 3,
        }
        r = requests.post(f"{BASE_URL}/ratings", json=payload)
        assert r.status_code in (401, 403)

    def test_score_below_minimum(self, professional_data, client_data):
        payload = {
            "professionalId": professional_data["user"]["id"],
            "score": 0,
        }
        r = requests.post(
            f"{BASE_URL}/ratings",
            json=payload,
            headers=client_data["headers"],
        )
        assert r.status_code in (400, 422)

    def test_score_above_maximum(self, professional_data, client_data):
        payload = {
            "professionalId": professional_data["user"]["id"],
            "score": 6,
        }
        r = requests.post(
            f"{BASE_URL}/ratings",
            json=payload,
            headers=client_data["headers"],
        )
        assert r.status_code in (400, 422)

    def test_all_valid_scores(self, professional_data):
        for score in range(1, 6):
            new_client = create_client()
            token = login(new_client["email"])
            payload = {
                "professionalId": professional_data["user"]["id"],
                "score": score,
            }
            r = requests.post(
                f"{BASE_URL}/ratings",
                json=payload,
                headers=auth_headers(token),
            )
            assert r.status_code == 201, f"Score {score} falhou: {r.text}"


class TestGetRatings:
    def test_get_professional_ratings(self, professional_data, client_data):
        payload = {
            "professionalId": professional_data["user"]["id"],
            "score": 4,
        }
        requests.post(
            f"{BASE_URL}/ratings",
            json=payload,
            headers=client_data["headers"],
        )
        prof_id = professional_data["user"]["id"]
        r = requests.get(
            f"{BASE_URL}/ratings/professional/{prof_id}",
            headers=client_data["headers"],
        )
        assert r.status_code == 200
        ratings = r.json()
        assert isinstance(ratings, list)
        assert all(rating["professionalId"] == prof_id for rating in ratings)

    def test_get_my_ratings_as_client(self, professional_data, client_data):
        r = requests.get(
            f"{BASE_URL}/ratings/my-ratings",
            headers=client_data["headers"],
        )
        assert r.status_code == 200
        ratings = r.json()
        assert isinstance(ratings, list)
        assert all(rating["clientId"] == client_data["user"]["id"] for rating in ratings)

    def test_get_my_ratings_without_token(self):
        r = requests.get(f"{BASE_URL}/ratings/my-ratings")
        assert r.status_code in (401, 403)
