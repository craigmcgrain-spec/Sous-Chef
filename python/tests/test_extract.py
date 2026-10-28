import json

import pytest

from conftest import fixture
from souschef.extract import ExtractionError, extract, temperatures_from_texts


def test_jsonld_fallback_normalizes_unknown_site():
    data = json.loads(extract(fixture("jsonld_unknown.html"), "https://example.com/ragu"))
    assert data["title"] == "Sunday Ragù"
    assert data["siteName"] == "Example Kitchen"
    assert data["prepMinutes"] == 15
    assert data["cookMinutes"] == 180
    assert data["totalMinutes"] == 195          # fallback: prep + cook
    assert data["ingredients"] == ["2 tbsp olive oil", "1kg beef shin"]
    assert data["steps"][:2] == ["Heat the oven to 160°C.", "Brown the beef."]
    assert data["steps"][-2:] == ["Rest for 10 minutes.", "Serve."]
    assert data["temperatures"][0] == {
        "valueC": 160, "valueF": 320, "raw": "160°C",
        "context": "Heat the oven to 160°C.", "stepIndex": 0,
    }
    assert {"author", "imageUrl"}.issubset(set(data["missing"]))


def test_blob_steps_are_split_and_numbering_stripped():
    data = json.loads(extract(fixture("blob_steps.html"), "https://example.com/x"))
    assert data["steps"] == ["Rest for 10 minutes.", "Serve."]


def test_empty_page_raises_code():
    with pytest.raises(ExtractionError) as err:
        extract("   ", "https://example.com/x")
    assert err.value.code == "EMPTY_PAGE"


def test_howto_section_flattens_steps():
    data = json.loads(extract(fixture("jsonld_unknown.html"), "https://example.com/x"))
    assert data["steps"][-2] == "Rest for 10 minutes."


def test_no_recipe_raises_code():
    with pytest.raises(ExtractionError) as err:
        extract(fixture("no_recipe.html"), "https://example.com/blog")
    assert err.value.code == "NO_RECIPE"


def test_supported_site_uses_scrapers_or_fallback():
    assert json.loads(extract(fixture("supported_site.html"), "https://www.allrecipes.com/recipe/1/x"))["title"] == "Test Cake"


def test_temperatures_ignore_grams_and_dedupe():
    temps = json.loads(temperatures_from_texts(["Add 180g flour.", "Bake at 180°C.", "Same oven: 180 C."]))
    assert [t["valueC"] for t in temps] == [180]
    assert temps[0]["stepIndex"] == 1


def test_temperature_variants():
    temps = json.loads(temperatures_from_texts(["Heat to 350 F.", "Gas mark 4.", "Fan 160°C."]))
    by_c = {t["valueC"] for t in temps}
    assert {177, 180}.issubset(by_c)
