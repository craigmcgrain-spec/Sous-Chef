"""Recipe extraction shared by the Android UI through Chaquopy."""

import json
import re

from bs4 import BeautifulSoup
from recipe_scrapers import scrape_html

_ERROR_PREFIX = "EXTRACTION_ERROR:"

_MISSING_FIELDS = (
    "author",
    "imageUrl",
    "servings",
    "prepMinutes",
    "cookMinutes",
    "totalMinutes",
)

_TEMPERATURE_RE = re.compile(r"(?<!\d)(\d{1,4})\s*(?:°\s*([CF])?|([CF]))(?![A-Za-z])", re.IGNORECASE)
_SPELLED_TEMPERATURE_RE = re.compile(
    r"(?<!\d)(\d{1,4})\s*degrees?\s*(?:(F|C)\b|(fahrenheit|celsius))?",
    re.IGNORECASE,
)
_GAS_MARK_RE = re.compile(r"gas\s*mark\s*(\d+)", re.IGNORECASE)
_GAS_MARK = {
    1: (140, 275),
    2: (150, 300),
    3: (170, 325),
    4: (180, 350),
    5: (190, 375),
    6: (200, 400),
    7: (210, 425),
    8: (220, 450),
    9: (230, 475),
}
_NUMBERING_RE = re.compile(r"^\s*(?:step\s*\d+|\d+)\s*[.):](?=\s|$)\s*", re.IGNORECASE)
_DURATION_RE = re.compile(r"^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?)?$", re.IGNORECASE)
_SENTENCE_END_RE = re.compile(r"[.!?](?=\s|$)")


class ExtractionError(Exception):
    def __init__(self, code):
        self.code = code
        super().__init__(f"{_ERROR_PREFIX}{code}")


def extract(html: str, url: str) -> str:
    if not html or not html.strip():
        raise ExtractionError("EMPTY_PAGE")

    data = _from_scraper(html, url)
    if not _is_recipe(data):
        try:
            data = _from_jsonld(html)
        except Exception as exc:
            raise ExtractionError("PARSE_FAILED") from exc
    if not _is_recipe(data):
        raise ExtractionError("NO_RECIPE")

    data["temperatures"] = _temperatures(data["steps"], data["ingredients"])
    data["missing"] = [field for field in _MISSING_FIELDS if data[field] is None]
    return json.dumps(data, ensure_ascii=False)


def temperatures_from_texts(items: list[str]) -> str:
    return json.dumps(_temperatures(items), ensure_ascii=False)


def _is_recipe(data):
    return bool(data and data["title"] and (data["ingredients"] or data["steps"]))


def _from_scraper(html, url):
    try:
        scraper = scrape_html(html, org_url=url)
    except Exception:
        return None

    prep = _minutes(_field(scraper, "prep_time"))
    cook = _minutes(_field(scraper, "cook_time"))
    return {
        "title": _text(_field(scraper, "title")),
        "author": _text(_field(scraper, "author")) or None,
        "siteName": _text(_field(scraper, "site_name")) or None,
        "imageUrl": _text(_field(scraper, "image")) or None,
        "servings": _text(_field(scraper, "yields")) or None,
        "prepMinutes": prep,
        "cookMinutes": cook,
        "totalMinutes": _total(prep, cook, _minutes(_field(scraper, "total_time"))),
        "ingredients": _string_list(_field(scraper, "ingredients")),
        "steps": _split_steps(_field(scraper, "instructions")),
    }


def _field(scraper, name):
    try:
        return getattr(scraper, name)()
    except Exception:
        return None


def _from_jsonld(html):
    soup = BeautifulSoup(html, "html.parser")
    recipe = _find_recipe(soup)
    if recipe is None:
        return None

    prep = _minutes(recipe.get("prepTime"))
    cook = _minutes(recipe.get("cookTime"))
    return {
        "title": _text(recipe.get("name")),
        "author": _name(recipe.get("author")),
        "siteName": _site_name(recipe, soup),
        "imageUrl": _image(recipe.get("image")),
        "servings": _servings(recipe.get("recipeYield") or recipe.get("yield")),
        "prepMinutes": prep,
        "cookMinutes": cook,
        "totalMinutes": _total(prep, cook, _minutes(recipe.get("totalTime"))),
        "ingredients": _string_list(recipe.get("recipeIngredient") or recipe.get("ingredients")),
        "steps": _instruction_texts(recipe.get("recipeInstructions") or recipe.get("instructions")),
    }


def _find_recipe(soup):
    for script in soup.find_all("script", attrs={"type": "application/ld+json"}):
        try:
            data = json.loads(script.string or script.get_text())
        except ValueError:
            continue
        for item in _walk(data):
            types = item.get("@type")
            if isinstance(types, str):
                types = [types]
            if types and "Recipe" in types:
                return item
    return None


def _walk(value):
    if isinstance(value, dict):
        yield value
        for child in value.values():
            yield from _walk(child)
    elif isinstance(value, list):
        for child in value:
            yield from _walk(child)


def _site_name(recipe, soup):
    meta = soup.find("meta", attrs={"property": "og:site_name"})
    if meta and meta.get("content"):
        return meta["content"].strip()
    for key in ("publisher", "sourceOrganization"):
        name = _name(recipe.get(key))
        if name:
            return name
    return None


def _name(value):
    if isinstance(value, list):
        return next((name for name in (_name(item) for item in value) if name), None)
    if isinstance(value, dict):
        value = value.get("name")
    text = _text(value)
    return text or None


def _image(value):
    if isinstance(value, list):
        return next((image for image in (_image(item) for item in value) if image), None)
    if isinstance(value, dict):
        value = value.get("url")
    text = _text(value)
    return text or None


def _servings(value):
    if isinstance(value, list):
        return _servings(value[0]) if value else None
    if isinstance(value, dict):
        return _servings(value.get("value") or value.get("name"))
    if isinstance(value, (int, float)):
        return str(int(value))
    return _text(value) or None


def _string_list(value):
    if isinstance(value, str):
        value = [value]
    if not isinstance(value, list):
        return []
    items = []
    for item in value:
        if isinstance(item, dict):
            item = item.get("text") or item.get("name")
        text = _text(item)
        if text:
            items.append(text)
    return items


def _instruction_texts(value):
    if isinstance(value, str):
        return _split_steps(value)
    if isinstance(value, list):
        steps = []
        for item in value:
            steps.extend(_instruction_texts(item))
        return steps
    if isinstance(value, dict):
        if value.get("text"):
            return _split_steps(value["text"])
        return _instruction_texts(value.get("itemListElement") or [])
    return []


def _split_steps(text):
    if not isinstance(text, str) or not text:
        return []
    lines = [" ".join(line.split()) for line in text.splitlines()]
    lines = [line for line in lines if line]
    if lines and all(_NUMBERING_RE.match(line) for line in lines):
        lines = [_NUMBERING_RE.sub("", line).strip() for line in lines]
    return [line for line in lines if line]


def _minutes(value):
    if value is None or isinstance(value, bool):
        return None
    if isinstance(value, (int, float)):
        return int(value)
    text = _text(value)
    if text.isdigit():
        return int(text)
    match = _DURATION_RE.match(text)
    if not match:
        return None
    days, hours, minutes = (int(part) if part else 0 for part in match.groups())
    return days * 1440 + hours * 60 + minutes


def _total(prep, cook, total):
    if total is not None:
        return total
    if prep is None and cook is None:
        return None
    return (prep or 0) + (cook or 0)


def _text(value):
    return " ".join(str(value).split()) if value is not None else ""


def _temperatures(texts, ingredient_texts=()):
    temperatures = []
    seen = set()
    for index, text in enumerate(texts):
        if not text:
            continue
        _scan_temperatures(temperatures, seen, text, index)
    for text in ingredient_texts:
        if not text:
            continue
        _scan_temperatures(temperatures, seen, text, None)
    return temperatures


def _scan_temperatures(temperatures, seen, text, step_index):
    for match in _TEMPERATURE_RE.finditer(text):
        _record_reading(
            temperatures, seen, int(match.group(1)),
            match.group(2) or match.group(3) or "C",
            match.group(0), text, match.start(), step_index,
        )
    # "350 degrees" defaults to Fahrenheit; recipes using just "180°" are Celsius.
    for match in _SPELLED_TEMPERATURE_RE.finditer(text):
        _record_reading(
            temperatures, seen, int(match.group(1)),
            match.group(2) or match.group(3) or "F",
            match.group(0), text, match.start(), step_index,
        )
    for match in _GAS_MARK_RE.finditer(text):
        values = _GAS_MARK.get(int(match.group(1)))
        if values is not None:
            _add_temperature(temperatures, seen, values[0], values[1], match.group(0), _sentence_at(text, match.start()), step_index)


def _record_reading(temperatures, seen, amount, unit, raw, text, position, step_index):
    if unit.upper().startswith("C"):
        _add_temperature(temperatures, seen, amount, round(amount * 9 / 5 + 32), raw, _sentence_at(text, position), step_index)
    else:
        _add_temperature(temperatures, seen, round((amount - 32) * 5 / 9), amount, raw, _sentence_at(text, position), step_index)


def _sentence_at(text, position):
    """The sentence containing [position], punctuation kept; full text if unsplit."""
    start = 0
    for match in _SENTENCE_END_RE.finditer(text):
        if position <= match.start():
            return text[start:match.end()].strip()
        start = match.end()
    return text[start:].strip()


def _add_temperature(temperatures, seen, value_c, value_f, raw, context, step_index):
    if value_c in seen:
        return
    seen.add(value_c)
    temperatures.append({
        "valueC": value_c,
        "valueF": value_f,
        "raw": raw,
        "context": context,
        "stepIndex": step_index,
    })
