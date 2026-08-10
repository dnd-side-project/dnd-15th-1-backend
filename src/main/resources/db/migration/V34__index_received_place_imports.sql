CREATE INDEX idx_place_imports_status_updated_at
    ON place_imports (status, updated_at, id);
