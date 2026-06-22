PRAGMA foreign_keys = ON;

CREATE TABLE db_metadata (
    key TEXT PRIMARY KEY NOT NULL,
    value TEXT NOT NULL
);

CREATE TABLE source_snapshot (
    source_id TEXT NOT NULL,
    snapshot_id TEXT NOT NULL,
    fetched_at_utc TEXT NOT NULL,
    source_updated_at TEXT,
    endpoint TEXT NOT NULL,
    raw_sha256 TEXT NOT NULL,
    record_count INTEGER NOT NULL CHECK(record_count >= 0),
    builder_version TEXT NOT NULL,
    PRIMARY KEY (source_id, snapshot_id)
);

CREATE TABLE drug_product (
    item_code TEXT PRIMARY KEY NOT NULL,
    product_name TEXT NOT NULL,
    product_name_normalized TEXT NOT NULL,
    manufacturer_name TEXT,
    manufacturer_name_normalized TEXT,
    strength_value REAL,
    strength_unit TEXT,
    dosage_form TEXT,
    route TEXT,
    professional_class TEXT,
    status TEXT NOT NULL,
    approval_date TEXT,
    source_updated_at TEXT,
    source_id TEXT NOT NULL
);

CREATE INDEX idx_drug_product_name_normalized
ON drug_product(product_name_normalized);

CREATE INDEX idx_drug_product_manufacturer
ON drug_product(manufacturer_name_normalized);

CREATE TABLE drug_alias (
    item_code TEXT NOT NULL REFERENCES drug_product(item_code) ON DELETE CASCADE,
    alias TEXT NOT NULL,
    alias_normalized TEXT NOT NULL,
    alias_type TEXT NOT NULL,
    PRIMARY KEY(item_code, alias_normalized, alias_type)
);

CREATE INDEX idx_drug_alias_normalized
ON drug_alias(alias_normalized);

CREATE TABLE drug_identifier (
    identifier_type TEXT NOT NULL,
    identifier_value TEXT NOT NULL,
    item_code TEXT NOT NULL REFERENCES drug_product(item_code) ON DELETE CASCADE,
    valid_from TEXT,
    valid_to TEXT,
    source_id TEXT NOT NULL,
    PRIMARY KEY(identifier_type, identifier_value, item_code)
);

CREATE INDEX idx_drug_identifier_lookup
ON drug_identifier(identifier_type, identifier_value);

CREATE TABLE ingredient (
    ingredient_code TEXT PRIMARY KEY NOT NULL,
    ingredient_name TEXT NOT NULL,
    ingredient_name_normalized TEXT NOT NULL
);

CREATE TABLE drug_ingredient (
    item_code TEXT NOT NULL REFERENCES drug_product(item_code) ON DELETE CASCADE,
    ingredient_code TEXT NOT NULL REFERENCES ingredient(ingredient_code),
    amount_value REAL,
    amount_unit TEXT,
    PRIMARY KEY(item_code, ingredient_code)
);

CREATE TABLE easy_drug_info (
    item_code TEXT PRIMARY KEY NOT NULL REFERENCES drug_product(item_code) ON DELETE CASCADE,
    efficacy_html TEXT,
    use_method_html TEXT,
    warning_html TEXT,
    caution_html TEXT,
    interaction_html TEXT,
    adverse_effect_html TEXT,
    storage_html TEXT,
    open_date TEXT,
    update_date TEXT,
    content_sha256 TEXT NOT NULL,
    sanitizer_version TEXT NOT NULL,
    source_id TEXT NOT NULL
);

CREATE TABLE dur_rule (
    dur_rule_id TEXT PRIMARY KEY NOT NULL,
    dur_type TEXT NOT NULL,
    subject_ingredient_code TEXT,
    related_ingredient_code TEXT,
    item_code TEXT,
    dosage_form TEXT,
    notice_date TEXT,
    content_text TEXT NOT NULL,
    source_id TEXT NOT NULL
);

CREATE INDEX idx_dur_subject
ON dur_rule(subject_ingredient_code, dur_type);

CREATE INDEX idx_dur_related
ON dur_rule(related_ingredient_code, dur_type);

CREATE TABLE source_link (
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    label TEXT NOT NULL,
    url TEXT NOT NULL,
    source_id TEXT NOT NULL,
    PRIMARY KEY(entity_type, entity_id, label)
);

CREATE VIRTUAL TABLE drug_search_fts USING fts5(
    item_code UNINDEXED,
    product_name,
    aliases,
    ingredients,
    manufacturer,
    tokenize = 'unicode61'
);
