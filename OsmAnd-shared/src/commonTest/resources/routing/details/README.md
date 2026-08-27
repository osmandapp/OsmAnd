# Route details fixtures

`android_route_details_schema_v1.json` is a hand-authored serialization schema fixture. It verifies
field names, numeric types, Android sentinel values, ordered duplicate route tags, and flattened
height samples.

It is not captured routing output and must not be used as evidence of behavioral parity. Android
parity is established later by differential tests that feed the same legacy route objects to the
legacy and shared backends.
