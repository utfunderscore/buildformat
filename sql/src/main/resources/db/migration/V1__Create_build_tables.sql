CREATE TABLE IF NOT EXISTS build_meta
(
    id         SERIAL PRIMARY KEY,
    build_name VARCHAR(16) NOT NULL,
    created_at TIMESTAMP   NOT NULL
);

-- Create unique index on name
CREATE UNIQUE INDEX idx_build_meta_name ON build_meta (build_name);

-- Create build_version table
CREATE TABLE IF NOT EXISTS build_version
(
    id             SERIAL PRIMARY KEY,
    build_meta_id  INTEGER     NOT NULL,
    version_number INTEGER     NOT NULL,
    checksum       VARCHAR(64) NOT NULL,
    metadata       JSONB       NOT NULL,
    FOREIGN KEY (build_meta_id) REFERENCES build_meta (id)
);

-- Create unique index on metaId and versionNumber
CREATE UNIQUE INDEX idx_build_version_meta_id_version_number ON build_version (build_meta_id, version_number);

-- Create build_supported_formats table
CREATE TABLE IF NOT EXISTS build_supported_formats
(
    id               SERIAL PRIMARY KEY,
    build_version_id INTEGER     NOT NULL,
    format_name      VARCHAR(32) NOT NULL,
    FOREIGN KEY (build_version_id) REFERENCES build_version (id)
);

-- Create unique index on versionId and formatName
CREATE UNIQUE INDEX idx_build_supported_formats_version_id_format_name ON build_supported_formats (build_version_id, format_name);