-- Create BuildMetaTable
CREATE TABLE build_meta
(
    id         SERIAL PRIMARY KEY,
    build_name VARCHAR(16) NOT NULL,
    created_at BIGINT      NOT NULL
);

-- Create unique index on name
CREATE UNIQUE INDEX idx_build_meta_name ON build_meta (build_name);

-- Create BuildVersionTable
CREATE TABLE build_version
(
    id             SERIAL PRIMARY KEY,
    build_meta_id  INTEGER      NOT NULL REFERENCES build_meta (id),
    description    VARCHAR(128) NOT NULL,
    version_number INTEGER      NOT NULL,
    checksum       VARCHAR(64)  NOT NULL
);

-- Create unique index on metaId and versionNumber
CREATE UNIQUE INDEX idx_build_version_meta_id_version_number ON build_version (build_meta_id, version_number);

-- Create BuildSupportedFormats
CREATE TABLE build_supported_formats
(
    id               SERIAL PRIMARY KEY,
    build_version_id INTEGER     NOT NULL REFERENCES build_version (id),
    format_name      VARCHAR(32) NOT NULL
);

-- Create unique index on versionId and formatName
CREATE UNIQUE INDEX idx_build_supported_formats_version_id_format_name ON build_supported_formats (build_version_id, format_name);

---

BEGIN ISOLATION LEVEL SERIALIZABLE;

-- Query 1: Try to get existing record
SELECT id
FROM build_meta
WHERE build_name = 'test';

-- If no results, then execute both queries
INSERT INTO build_meta (build_name, created_at)
VALUES ('test', '03')
ON CONFLICT (build_name)
    DO NOTHING;

SELECT id
FROM build_meta
WHERE build_name = 'test';

COMMIT;

INSERT INTO build_version (build_meta_id, description, checksum, version_number)
VALUES (42,
        'Hot‑fix: X',
        'e3b0c44298fc1c149afbf4c8996fb924...',
        COALESCE(
                (SELECT MAX(version_number) + 1
                 FROM build_version
                 WHERE build_meta_id = 42),
                1
        ));