# Product Catalog Service

Spring Boot service providing sample internet products. Requires Java 17 or later and Maven.

Run with `mvn spring-boot:run`, then request `GET http://localhost:8081/api/products`. Run tests with `mvn test`. The sample data is held in memory.

## Sample add-to-cart tool

`CartTool.addToCart` is a Spring AI `@Tool` that accepts a `productId` and returns a sample order. It can also be called over HTTP:

```sh
curl -X POST http://localhost:8081/api/cart/add \
  -H 'Content-Type: application/json' \
  -d '{"productId":"internet-500"}'
```

The response contains an order ID, the supplied product ID, a sample name and price, and a total. The name and price are fixed examples: the tool does not look up products, persist a cart, or place an order. Missing or blank product IDs return HTTP 400. To use the tool with a model, register the `CartTool` bean with a Spring AI chat client; this project does not configure a model or MCP server.

## Sample chat endpoint

Send a prompt to the initial chat endpoint:

```sh
curl -X POST http://localhost:8081/ai/chat \
  -H 'Content-Type: application/json' \
  -d '{"prompt":"Tell me about your internet plans"}'
```

It returns JSON with a `response` field containing a sample reply. Missing or blank prompts return HTTP 400. This first version does not call an AI model or invoke tools.
