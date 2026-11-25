CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS knowledge_vectors (
    id text PRIMARY KEY,
    content text NOT NULL,
    metadata jsonb NOT NULL,
    embedding vector(1536) NOT NULL
);
