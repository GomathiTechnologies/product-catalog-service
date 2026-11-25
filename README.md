# Product Catalog Service

Spring Boot service providing sample internet products. Requires Java 17 or later and Maven.

Run with `mvn spring-boot:run`, then request `GET http://localhost:8081/api/products`. Run tests with `mvn test`. The sample data is held in memory.

## Inspect PDF documents

From the service directory, run the standalone reader to print the text and metadata of each document produced from the PDFs in `src/main/rag-knowledge-base`:

```sh
mvn compile exec:java -Dexec.mainClass=com.example.productcatalogservice.PdfReader
```

To inspect another directory, append `-Dexec.args="/path/to/pdfs"`. The default `PagePdfDocumentReader` produces one document per PDF page. The utility normalizes whitespace, removes the demo corpus footer, and splits those documents with `TokenTextSplitter` (targeting 200 tokens per chunk), then prints each chunk's ID, metadata, and text. This inspection does not start the service or require an API key.

## Sample add-to-cart tool

`CartTool.addToCart` is a Spring AI `@Tool` that accepts a `productId` and returns a sample order. It can also be called over HTTP:

```sh
curl -X POST http://localhost:8081/api/cart/add \
  -H 'Content-Type: application/json' \
  -d '{"productId":"internet-500"}'
```

The response contains an order ID, the supplied product ID, a sample name and price, and a total. The name and price are fixed examples: the tool does not look up products, persist a cart, or place an order. Missing or blank product IDs return HTTP 400. The tool is registered with the Spring AI chat client; it does not require an MCP server.

## Embedding endpoint

Generate an embedding using the configured OpenAI model:

```sh
curl -X POST http://localhost:8081/ai/embedding \
  -H 'Content-Type: application/json' \
  -d '{"text":"Business internet"}'
```

The response contains `dimensions` and a `vector` array. The service logs the embedding dimensions; the vector is returned in the response. Set `OPENAI_API_KEY` before starting; `OPENAI_EMBEDDING_MODEL` overrides the default `text-embedding-3-small`. Missing or blank text returns HTTP 400, and a model failure returns HTTP 502.

## Sample chat endpoint

Send a prompt to the initial chat endpoint:

```sh
curl -X POST http://localhost:8081/ai/chat \
  -H 'Content-Type: application/json' \
  -d '{"prompt":"Tell me about your internet plans"}'
```

It returns JSON with a `response` field containing the model's reply. Missing or blank prompts return HTTP 400; an AI service failure returns HTTP 502. Set your API key in the environment before starting the service:

```sh
export OPENAI_API_KEY='your-api-key'
mvn spring-boot:run
```

Do not commit the key. The default model is `gpt-4.1-mini`; set `OPENAI_MODEL` to override it. Spring AI can invoke the registered cart and province tools automatically when the model requests them. Tool logs include only recognized sample province and product identifiers; other values are redacted. The cart operation returns a sample order and does not persist or purchase anything.

## Province-based sample plans

Get sample internet plans by Canadian province or territory name or two-letter code:

```sh
curl 'http://localhost:8081/dynamic/province/products?province=Ontario'
curl 'http://localhost:8081/dynamic/province/products?province=NU'
```

Every province and territory has three sample plans except Nunavut, which returns an empty `products` array with `No service available`. Names and prices are illustrative. Missing, blank, or unrecognized provinces return HTTP 400. The same operation is registered as the Spring AI tool `ProvinceProductsTool.getProvinceProducts` for `/ai/chat`.

## In-memory knowledge search

These endpoints are for local development. Start the service from its project directory with `OPENAI_API_KEY` set. Ingestion and non-empty searches call the configured embedding model and may incur API usage. The store is lost when the service stops; re-ingestion replaces all stored chunks after every embedding succeeds. Changing embedding models requires re-ingestion. Document IDs can change on re-ingestion. The PDF directory is relative to the service working directory.

```sh
# Check an empty store (or search it without making an embedding request).
curl http://localhost:8081/ai/knowledge/count
curl -X POST http://localhost:8081/ai/knowledge/search -H 'Content-Type: application/json' -d '{"query":"internet"}'

# Ingest, then repeat to verify that the count does not double.
curl -X POST http://localhost:8081/ai/knowledge/ingest
curl -X POST http://localhost:8081/ai/knowledge/ingest
curl http://localhost:8081/ai/knowledge/count

# Inspect all chunks, or replace DOCUMENT_ID with an ID from the response.
curl http://localhost:8081/ai/knowledge/documents
curl 'http://localhost:8081/ai/knowledge/documents?includeEmbedding=true'
curl 'http://localhost:8081/ai/knowledge/documents/DOCUMENT_ID'
curl 'http://localhost:8081/ai/knowledge/documents/DOCUMENT_ID?includeEmbedding=true'
curl -i 'http://localhost:8081/ai/knowledge/documents/invalid-id'

# Search examples. Thresholds filter raw cosine scores; tune them for your corpus.
curl -X POST http://localhost:8081/ai/knowledge/search -H 'Content-Type: application/json' -d '{"query":"Business internet plans"}'
curl -X POST http://localhost:8081/ai/knowledge/search -H 'Content-Type: application/json' -d '{"query":"Can I get a fixed IP address on fibre internet?","topK":3,"similarityThreshold":0.3}'
curl -X POST http://localhost:8081/ai/knowledge/search -H 'Content-Type: application/json' -d '{"query":"Free indoor access points"}'
curl -X POST http://localhost:8081/ai/knowledge/search -H 'Content-Type: application/json' -d '{"query":"Internet backup during an outage"}'
curl -X POST http://localhost:8081/ai/knowledge/search -H 'Content-Type: application/json' -d '{"query":"Installation charges"}'
curl -X POST http://localhost:8081/ai/knowledge/search -H 'Content-Type: application/json' -d '{"query":"An elephant in the forest","similarityThreshold":0.3}'

# Validation and clearing the store.
curl -i -X POST http://localhost:8081/ai/knowledge/search -H 'Content-Type: application/json' -d '{"query":"internet","topK":0}'
curl -i -X POST http://localhost:8081/ai/knowledge/search -H 'Content-Type: application/json' -d '{"query":"internet","similarityThreshold":2}'
curl -X DELETE http://localhost:8081/ai/knowledge/documents
curl http://localhost:8081/ai/knowledge/count
```
