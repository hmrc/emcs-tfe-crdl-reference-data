# emcs-tfe-crdl-reference-data

This service is a fork of the [emcs-tfe-reference-data](https://github.com/hmrc/emcs-tfe-reference-data) service.

It reworks the service to use reference data from the Central Reference Data Library via the [crdl-cache](https://github.com/hmrc/crdl-cache/) service.

The purpose of this service is to act as an adapter for EMCS TFE services like [emcs-tfe-frontend](https://github.com/hmrc/emcs-tfe-frontend/) which previously used data produced by stored procedures in the CandE Oracle database.

It translates the flat key-value codelists of the [crdl-cache](https://github.com/hmrc/crdl-cache/) service into the composite domain objects used by the TFE frontends.

## Running the service

1. Make sure you run all the dependant services through the service manager:

```shell
 sm2 --start EMCS_TFE_CRDL
 ```

2. Stop the EMCS-TFE microservice from the service manager and run it locally:

```shell 
sm2 --stop EMCS_TFE_CRDL_REFERENCE_DATA
```

```shell 
sbt run
```
The service runs on port 8321 by default.

You will need a bearer token to invoke some of the API endpoints in this service. In order to generate a token:
1. Go to the auth-login-stub `http://localhost:9949/auth-login-stub/gg-sign-in` on the browser.
2. Set up the following: redirect url as `http://localhost:9949/auth-login-stub/gg-sign-in`, affinity group as `Organization` with the enrolment `HMRC-EMCS-ORG` provide a valid excise number and submit.
3. You will be able to copy the Bearer Token from the authToken section on the redirected page.


## API endpoints

<details>
<summary>Retrieve CN Code information

**`POST`** /oracle/cn-code-information</summary>

Retrieve CN Code information for a given list of Product Codes and CN Codes

**Request Body**: [CnInformationRequest Model](app/uk/gov/hmrc/emcstfereferencedata/models/request/CnInformationRequest.scala)

**Example request body**:

```json
{
  "items": [
    {
      "productCode": "B000",
      "cnCode": "22030001"
    },
    {
      "productCode": "S500",
      "cnCode": "10000000"
    }
  ]
}
```

### Responses

#### Success Response(s)

**Status**: 200 (OK)

**Body**: Key:value pair of String:[CnCodeInformation Model](app/uk/gov/hmrc/emcstfereferencedata/models/response/CnCodeInformation.scala)

**Example response body**:

```json
{
  "24029000": {
    "cnCodeDescription": "Cigars, cheroots, cigarillos and cigarettes not containing tobacco",
    "exciseProductCodeDescription": "Fine-cut tobacco for the rolling of cigarettes",
    "unitOfMeasureCode": 1
  }
}
```

#### Error Response(s)

**Status**: 500 (ISE)

**Body**: [ErrorResponse Model](app/uk/gov/hmrc/emcstfereferencedata/models/response/ErrorResponse.scala)

</details>

<details>
<summary>Retrieve particular packaging types

**`POST`** /oracle/packaging-types</summary>

Retrieve packaging type information for a list of packaging types

**Request Body**: JSON array of packaging types

**Example request body**:

```json
[
  "VP",
  "NE",
  "TO"
]
```

### Responses

#### Success Response(s)

**Status**: 200 (OK)

**Body**: Key:value pair of String:String

**Example response body**:

```json
{
  "NE": "Unpacked or unpackaged",
  "TO": "Tun",
  "VP": "Vacuum-packed"
}
```

#### Error Response(s)

**Status**: 500 (ISE)

**Body**: [ErrorResponse Model](app/uk/gov/hmrc/emcstfereferencedata/models/response/ErrorResponse.scala)

</details>

<details>
<summary>Retrieve all packaging types

**`GET`** /oracle/packaging-types</summary>

Retrieve all packaging type information. An optional `isCountable` boolean query parameter
can be provided to find `countable` packaging types.
The response is sorted by the description in ascending (A-Z) order.

### Responses

#### Success Response(s)

**Status**: 200 (OK)

**Body**: Key:value pair of String:String

**Example response body**:

```json
{
  "CR": "Crate",
  "FD": "Framed crate",
  "VA": "Vat"
}
```

#### Error Response(s)

**Status**: 500 (ISE)

**Body**: [ErrorResponse Model](app/uk/gov/hmrc/emcstfereferencedata/models/response/ErrorResponse.scala)

</details>

<details>
<summary>Retrieve all wine operations

**`GET`** /oracle/wine-operations</summary>

Retrieve all wine operations.

### Responses

#### Success Response(s)

**Status**: 200 (OK)

**Body**: Key:value pair of String:String

**Example response body**:

```json
{
  "0": "The product has undergone none of the following operations",
  "1": "The product has been enriched",
  "2": "The product has been acidified",
  "3": "The product has been de-acidified"
}
```

#### Error Response(s)

**Status**: 500 (ISE)

**Body**: [ErrorResponse Model](app/uk/gov/hmrc/emcstfereferencedata/models/response/ErrorResponse.scala)

</details>

<details>
<summary>Retrieve particular wine operations

**`POST`** /oracle/wine-operations</summary>

Retrieve wine operation information for a list of wine operations

**Request Body**: JSON array of wine operations

**Example request body**:

```json
[
  "4",
  "11",
  "9"
]
```

### Responses

#### Success Response(s)

**Status**: 200 (OK)

**Body**: Key:value pair of String:String

**Example response body**:

```json
{
  "4": "The product has been sweetened",
  "11": "The product has been partially dealcoholised",
  "9": "The product has been made using oak chips"
}
```

#### Error Response(s)

**Status**: 500 (ISE)

**Body**: [ErrorResponse Model](app/uk/gov/hmrc/emcstfereferencedata/models/response/ErrorResponse.scala)

</details>

<details>
<summary>Retrieve member states

**`GET`** /oracle/member-states</summary>

Retrieve member states list

### Responses

#### Success Response(s)

**Status**: 200 (OK)

**Body**: Array of [Country](app/uk/gov/hmrc/emcstfereferencedata/models/response/Country.scala) object

**Example response body**:

```json
[
  {
    "countryCode": "FR",
    "country": "France"
  },
  {
    "countryCode": "AT",
    "country": "Austria"
  }
]
```

#### Error Response(s)

**Status**: 500 (ISE)

**Body**: [ErrorResponse Model](app/uk/gov/hmrc/emcstfereferencedata/models/response/ErrorResponse.scala)

</details>

### All tests and checks
This is an sbt command alias specific to this project. It will run a scala format
check, run unit tests, run integration tests and produce a coverage report:
> `sbt runAllChecks`
