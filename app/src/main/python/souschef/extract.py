import platform

from recipe_scrapers.__version__ import __version__ as recipe_scrapers_version
import lxml.etree


def probe() -> str:
    return (
        f"python={platform.python_version()} "
        f"recipe_scrapers={recipe_scrapers_version} "
        f"lxml={lxml.etree.__version__}"
    )
