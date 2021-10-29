# The PADDS Stack

![Scala PADDS Logo (courtesy of https://logodesign.net)](scala-padds.png?raw=true)

An opinionated example stack using Protobuf, Akka, Docker, DynamoDB, and Scala.

## Proto Ecosystems Description

[Protobuf](https://developers.google.com/protocol-buffers) files are the core of all data models. 
These can be consumed by `rpc` models in Proto to make a definition of how a server should accept the models.
We can also just serialize the protobuf messages without an `rpc` object.
Backwards and forwards compatibility is a top concern for Proto and all libraries utilizing it.

From these primitives, we generate a variety of useful things:

- Proto => Linting and Backwards Compatibility (via [bufbuild](#1-protobuf-lintingbc-bufbuildbuf))


- Proto => Scala Models + Json DeSer (via [ScalaPB and scalapb-json4s](#2-scala-models-wjson-scalapb--json4s))


- Proto => Models for Other Langs (example: Typescript via [ts-proto](#5-typescript-and-other-lang-generation-ts-proto))


- Proto => OpenAPI YAML (via Google's [protoc-gen-openapi](#3-openapi-generation-gnosticprotoc-gen-openapi))
- OpenAPI YAML => OpenAPI HTML (via [redoc-cli](#4-html-generation-redoc-cli))
- OpenAPI YAML => AkkaHttp Routes + Scala Models + DeSer (via twilio's guardrail)


- Scala Models => Avro Schema + DeSer (via avro4s)

## Tools

### 1. Protobuf Linting/BC (bufbuild/buf) 

[Buf](https://buf.build/) is a project whose goal is to lint and verify that your Protobuf 
files don't have backwards-incompatible changes. Protobuf loses a lot of its benefits
if you don't adhere to its rules on versioning, and Buf is a way of enforcing those rules.

#### buf.yaml

In `src/main/protobuf` there's a `buf.yaml` file that dictates what rules to apply onto your `.proto` files.
It has a dependency on `buf.build/googleapis/googleapis`, which is a project ID for a 
[BSR](https://buf.build/product/bsr/) lock file that Buf will use 
to lint our Google proto depedencies. (Similar if depending on an in-house depedency) 

To refresh that lock file:

```bash
cd src/main/protobuf
buf mod update
cd -
```

#### buf.work.yaml

A top-level file that points to all protobuf directories in this project. Enables running buf commands
at the root of the project instead of inside `src/main/protobuf`.

#### Installation

```bash
#macOS
brew tap bufbuild/buf
brew install buf
```

#### Usage

Buf commands to lint and check breaking compatibility agains the previous Git tag in this repo:

```bash
buf lint
PREV_VER=$(git describe --abbrev=0)
buf breaking --against ".git#tag=${PREV_VER}"
```

### 2. Scala Models w/Json (ScalaPB / Json4s)

[ScalaPB](https://scalapb.github.io/) is a project to translate Protobuf into Scala Case Classes.
It uses [scalapb-json4s](https://github.com/scalapb/scalapb-json4s) to DeSer JSON models from the generated
case classes. 

JSON serialization is made automatic for single-line usage and Akka integration via 
[ProtoJsonProtocol.scala](src/main/scala/com/padds/example/json/ProtoJsonProtocol.scala).

#### Installation

Add plugins to [plugins.sbt](project/plugins.sbt) and dependencies to [build.sbt](build.sbt).

### 3. OpenAPI Generation (gnostic/protoc-gen-openapi)

[protoc-gen-openapi](https://github.com/google/gnostic/tree/master/apps/protoc-gen-openapi)
is for making OpenAPI yaml from the proto files. It needs to be pointed at the service files 
and will output a single yaml file of all combined endpoints with referenced objects.

#### Installation

```bash
# Step 0: Install go. https://golang.org/doc/install
go get github.com/google/gnostic
go install github.com/google/gnostic/apps/protoc-gen-openapi@latest
```

#### Usage

```bash
# One-time: you need to run these for ScalaPB to pull external protos the first time, 
# but they should stick around unless cleaned up, so it's not necessary every time
sbt compile 
mkdir -p src/main/resources/generated-openapi

PROTOC_IMPORT_PATH="src/main/protobuf"
OPENAPI_DESTINATION="src/main/resources/generated-openapi"
rm -rf ${OPENAPI_DESTINATION}
mkdir -p ${OPENAPI_DESTINATION}

protoc -Isrc/main/protobuf -Itarget/protobuf_external ${PROTOC_IMPORT_PATH}/**/service/*.proto --openapi_out=${OPENAPI_DESTINATION}

# OR
# If your shell doesn't have ** wildcard expansion:

EXPANDED_SERVICE_PROTO=$(find ${PROTOC_IMPORT_PATH} -regex '.*/service/.*.proto' | tr '\n' ' ')
protoc -Isrc/main/protobuf -Itarget/protobuf_external ${EXPANDED_SERVICE_PROTO} --openapi_out=${OPENAPI_DESTINATION}
```

### 4. HTML Generation (redoc-cli)

[redoc-cli](https://www.npmjs.com/package/redoc-cli) is used to generate HTML to view and navigate
your OpenAPI docs.

#### Installation

```bash
# Step 0: install npm. https://docs.npmjs.com/downloading-and-installing-node-js-and-npm
npm install -g redoc-cli
```

#### Usage

```bash
# Make sure you've generated the OpenAPI yaml first!
HTML_DESTINATION="src/main/resources/generated-html"
rm -rf ${HTML_DESTINATION}
mkdir -p ${HTML_DESTINATION}
redoc-cli bundle -o ${HTML_DESTINATION}/openapi.html src/main/resources/generated-openapi/*
```

### 5. Typescript and Other Lang Generation (ts-proto) 

[ts-proto](https://www.npmjs.com/package/ts-proto) is used to create 
Typescript and/or Javascript models from Protobuf files. This can be helpful when passing models
to a browser app without having to worry about serialization across different languages.

This can be a similar process for any language you want to interface with, the Proto community
has support for many different languages and formats.

#### Installation

```bash
# Step 0: install npm. https://docs.npmjs.com/downloading-and-installing-node-js-and-npm
npm install -g ts-proto
```

#### Usage

```bash
# One-time: you need to run these for ScalaPB to pull external protos the first time, 
# but they should stick around unless cleaned up, so it's not necessary every time
sbt compile 

PROTOC_IMPORT_PATH="./src/main/protobuf"
PROTOC_EXTERNAL_IMPORT_PATH="./target/protobuf_external"
TS_DESTINATION="./src/main/resources/generated-typescript"
rm -rf ${TS_DESTINATION}
mkdir -p ${TS_DESTINATION}
protoc --proto_path="${PROTOC_IMPORT_PATH}" --proto_path="${PROTOC_EXTERNAL_IMPORT_PATH}" --ts_proto_opt=esModuleInterop=true,outputEncodeMethods=false,outputJsonMethods=false,outputClientImpl=false,useOptionals=true,unrecognizedEnum=false --ts_proto_out="${TS_DESTINATION}" $(find ${PROTOC_IMPORT_PATH} -iname "*.proto")
```
