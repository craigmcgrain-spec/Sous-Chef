# Vendored jstyleson 0.0.2

`recipe-scrapers` -> `extruct` -> `jstyleson`. jstyleson is published only as
an sdist on PyPI, and Chaquopy 17 installs index packages with
`--only-binary`, so it cannot be resolved from the index. It is a single-file,
pure-Python, MIT-licensed module, so the sdist contents are vendored here and
installed as a local path from `app/build.gradle.kts`.

Upstream: https://github.com/linjackson78/jstyleson (0.0.2), MIT licence in
`LICENSE`. Remove this directory if jstyleson publishes a wheel.
