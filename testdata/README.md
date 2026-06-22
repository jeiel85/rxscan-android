# Test Data Policy

Normal development uses synthetic data only.

Allowed:

- generated dispensing-bag layouts with invented names;
- API-shaped public-data fixtures that do not contain patient information;
- malformed synthetic text for parser and sanitizer tests.

Not allowed in Git:

- real prescription photos;
- OCR text copied from a real prescription;
- patient names, phone numbers, resident registration numbers, addresses, hospitals, or pharmacies from real records;
- consented de-identified corpus material.

Consented de-identified fixtures require a separate protected process and are
excluded from this repository by `.gitignore`.

