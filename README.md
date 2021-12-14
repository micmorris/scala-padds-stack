# The PADDS Stack

![Scala PADDS Logo (courtesy of https://logodesign.net)](scala-padds.png?raw=true)

An opinionated example stack using Protobuf, Akka, Docker, DynamoDB, and Scala.

## Why Use the Proto Ecosystem?

[Protobuf](https://developers.google.com/protocol-buffers) files can be the core of all data models. 
These can be consumed by `rpc` models in Proto to make a definition of how a server should accept the models. 
We can also just serialize the protobuf messages without an `rpc` object. 


[Backwards and forwards compatibility](https://yokota.blog/2021/08/26/understanding-protobuf-compatibility/) 
is a top concern for Proto and Google has detailed docs on 
[how to safely update message with a new field](https://developers.google.com/protocol-buffers/docs/proto3#updating). 

Proto3 also supports 
[canonical JSON encoding](https://developers.google.com/protocol-buffers/docs/proto3#json) 
meaning that regardless of the client you use to turn Proto into different models, 
they can still communicate with each other over JSON. 

From these primitives, we generate a variety of useful things:

- Proto => Linting and Backwards Compatibility (via [1. bufbuild](#1-protobuf-lintingbc-bufbuildbuf))


- Proto => Scala Models + Json DeSer (
  via [2. ScalaPB and scalapb-json4s](#2-scala-models-wjson-scalapb--json4s))
- Proto => Validations (via [3. Validation](#3-validation-scalapb-validate--protoc-gen-validate))


- Proto => OpenAPI YAML (via
  Google's [4. protoc-gen-openapi](#4-openapi-generation-gnosticprotoc-gen-openapi))
- OpenAPI YAML => OpenAPI HTML (via [5. redoc-cli](#5-html-generation-redoc-cli))


- Proto => Models for Other Langs (example: Typescript
  via [6. ts-proto](#6-typescript-and-other-lang-generation-ts-proto))


- Scala Models => Avro Schema + DeSer (via [7. avro4s](#7-avro-schema-creation-avro4s))

Finally, we package and deploy a running Scala service 
via [8. Docker](#8-deployment-docker-docker-for-desktop-and--docker-compose)

## Tools

### 0. Protobuf/Protoc

#### Installation

```bash
#macOS
brew install protobuf
```

### 1. Protobuf Linting/BC (bufbuild/buf)

[Buf](https://buf.build/) is a project whose goal is to lint and verify that your Protobuf files don't have
backwards-incompatible changes. Protobuf loses a lot of its benefits if you don't adhere to its rules on
versioning, and Buf is a way of enforcing those rules.

#### buf.yaml

In `src/main/protobuf` there's a `buf.yaml` file that dictates what rules to apply onto your `.proto` files.
It has a dependency on `buf.build/googleapis/googleapis`, which is a project ID for a
[BSR](https://buf.build/product/bsr/) lock file that Buf will use to lint our Google proto depedencies. (
Similar if depending on an in-house depedency)

To refresh that lock file:

```bash
cd src/main/protobuf
buf mod update
cd -
```

#### buf.work.yaml

A top-level file that points to all protobuf directories in this project. Enables running buf commands at the
root of the project instead of inside `src/main/protobuf`.

#### Installation

```bash
#macOS
brew install bufbuild/buf/buf
```

#### Usage

Buf commands to lint and check breaking compatibility agains the previous Git tag in this repo:

```bash
buf lint
PREV_VER=$(git describe --abbrev=0)
buf breaking --against ".git#tag=${PREV_VER}"
```

### 2. Scala Models w/Json (ScalaPB / Json4s)

[ScalaPB](https://scalapb.github.io/) is a project to translate Protobuf into Scala Case Classes. It
uses [scalapb-json4s](https://github.com/scalapb/scalapb-json4s) to DeSer JSON models from the generated case
classes.

JSON serialization is made automatic for single-line usage and Akka integration via
[ProtoJsonProtocol.scala](src/main/scala/com/padds/example/json/ProtoJsonProtocol.scala).

#### Installation

Add to [plugins.sbt](project/plugins.sbt):

```scala
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.3")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.5"
```

Add to [build.sbt](build.sbt):

```scala
import scalapb.compiler.Version.scalapbVersion

// Compile ScalaPB and pull external_protobuf content
Compile / PB.targets := Seq(
  scalapb
    .gen(flatPackage = true, javaConversions = false) -> (Compile / sourceManaged).value
)

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion % "protobuf",
  "com.thesamet.scalapb" %% "scalapb-json4s" % "0.11.1",
  "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % "2.5.0-2" % "protobuf",
  "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % "2.5.0-2"
)
```

### 3. Validation (ScalaPB-Validate / protoc-gen-validate)

COMING SOON!

### 4. OpenAPI Generation (gnostic/protoc-gen-openapi)

[protoc-gen-openapi](https://github.com/google/gnostic/tree/master/apps/protoc-gen-openapi)
is for making OpenAPI yaml from the proto files. It needs to be pointed at the service files and will output a
single yaml file of all combined endpoints with referenced objects.

#### Installation

```bash
# Step 0: Install go. https://golang.org/doc/install
go get github.com/google/gnostic
go install github.com/google/gnostic/apps/protoc-gen-openapi@latest
```

##### Known Bug

I've [opened an issue on gnostic](https://github.com/google/gnostic/issues/263), but in the meantime, you'll
need to make this change yourself locally during installation?

```bash
go get github.com/google/gnostic
vim $GOPATH/pkg/mod/github.com/google/gnostic@v0.5.6/apps/protoc-gen-openapi/generator/openapi-v3.go

# Modify this section, currently at line 589-602
#// Add the schema to the components.schema list.
#d.Components.Schemas.AdditionalProperties = append(d.Components.Schemas.AdditionalProperties,
#  &v3.NamedSchemaOrReference{
#    Name: string(message.Desc.Name()),
#    Value: &v3.SchemaOrReference{
#      Oneof: &v3.SchemaOrReference_Schema{
#        Schema: &v3.Schema{
#          Description: messageDescription,
#----------TODO: Add the below line!
#          Type: "object",
#          Properties:  definitionProperties,
#        },
#      },
#    },
#  },
#)

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

# If your shell has ** wildcard expansion:

protoc -Isrc/main/protobuf -Itarget/protobuf_external ${PROTOC_IMPORT_PATH}/**/service/*.proto --openapi_out=${OPENAPI_DESTINATION}

# OR
# If your shell DOESN'T have ** wildcard expansion:

EXPANDED_SERVICE_PROTO=$(find ${PROTOC_IMPORT_PATH} -regex '.*/service/.*.proto' | tr '\n' ' ')
protoc -Isrc/main/protobuf -Itarget/protobuf_external ${EXPANDED_SERVICE_PROTO} --openapi_out=${OPENAPI_DESTINATION}
```

### 5. HTML Generation (redoc-cli)

[redoc-cli](https://www.npmjs.com/package/redoc-cli) is used to generate HTML to view and navigate your
OpenAPI docs.

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

### 6. Typescript and Other Lang Generation (ts-proto)

[stephenh/ts-proto](https://github.com/stephenh/ts-proto) is used to create Typescript and/or Javascript
models from Protobuf files. This can be helpful when passing models to a browser app without having to worry
about serialization across different languages.

This can be a similar process for any language you want to interface with, the Proto community has support for
many different languages and formats.

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

### 7. Avro Schema Creation (avro4s)

COMING SOON!

### 8. Deployment (docker, Docker for Desktop, and  docker-compose)

COMING SOON!

## Deployment

```bash
sbt test docker:publishLocal

docker-compose down
docker-compose up
```

MORE COMING SOON!

### Example Requests

#### New Order

```bash
curl http://localhost:8080/v1/order/padding -H "Content-Type: application/json" \
-d '{"playerId":"1ce91e38-4601-4354-ad1b-2c5c1c70da1a","paddingId":"f766cfce-6edd-4e89-aa78-f3018212080f","qty":1}'
```

#### Get Order by ID

```bash
curl -X GET http://localhost:8080/v1/orderId/1ce91e38-4601-4354-ad1b-2c5c1c70da1a
```

#### Get Batch of Orders

```bash
curl http://localhost:8080/v1/order/lookup -H "Content-Type: application/json" \
-d '{"orderIds":["1ce91e38-4601-4354-ad1b-2c5c1c70da1a", "f766cfce-6edd-4e89-aa78-f3018212080f"]}'
```




