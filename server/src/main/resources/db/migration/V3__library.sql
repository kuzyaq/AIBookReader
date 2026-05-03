CREATE TABLE library_books (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(512) NOT NULL,
    author VARCHAR(512) NOT NULL DEFAULT '',
    format VARCHAR(16) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_key VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending_upload',
    metadata_version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_library_books_user ON library_books(user_id);
CREATE INDEX idx_library_books_user_status ON library_books(user_id, status);

CREATE TABLE library_reading_progress (
    library_book_id UUID PRIMARY KEY REFERENCES library_books(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    locator_json TEXT,
    current_page_index INT NOT NULL DEFAULT 0,
    progress_fraction REAL,
    last_read_at_ms BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_library_progress_user ON library_reading_progress(user_id);

CREATE TABLE library_chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    library_book_id UUID NOT NULL REFERENCES library_books(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_message_id UUID NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    created_at_ms BIGINT NOT NULL,
    server_created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(library_book_id, client_message_id)
);

CREATE INDEX idx_library_chat_book ON library_chat_messages(library_book_id, server_created_at);
