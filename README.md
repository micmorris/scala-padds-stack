# The PADDS Stack

![Scala PADDS Logo (courtesy of https://logodesign.net)](scala-padds.png?raw=true)

An opinionated example stack using Protobuf, Akka, Docker, DynamoDB, and Scala.

## Summary

## Proto Ecosystems

Protobuf files are the core of all data models. These can be consumed by `rpc` models in Proto to
make a definition of how a server should accept the models. Backwards and forwards compatibility is a
top concern for Proto and all libraries utilizing it.

From these primitives, we generate a variety of useful things:

- Proto => Linting and Backwards Compatibility (via bufbuild)


- Proto => Scala Models + Json DeSer (via ScalaPB and scalapb-json4s)


- Proto => OpenAPI YAML (via Google's protoc-gen-openapi)
- OpenAPI YAML => OpenAPI HTML (via redoc-cli)
- OpenAPI YAML => AkkaHttp Routes + Scala Models + DeSer (via twilio's guardrail)


- Scala Models => Avro Schema + DeSer (via avro4s)

## Tools

### bufbuild/buf