# ScalaHttpMock

A scala based http mocking service which tries to offer nicer errors
and easy switching from an expectation to a verification. Ideally
this should enable clearer boundary testing.

## Dependency setup in a project
All package name etc will change before publishing

## Starting

```scala
MockServiceFactory.create(9999) // will create a running test service on 9999
```


## Adding an expectation
```scala
mockService.addExpectation(ServiceExpectation())
```

Note the default response code is 501 not implemented as 404 can have
business rules tied to it such as auth failure.

## Verifying an expectation was called
```scala
mockService.verifyCall(ServiceExpectation())
```

### Expectations
Expectations are case classes with convenience methods to also help with
copying to a modified version.


For example the following are identical to one another
```scala
import com.github.pbyrne84.scalahttpmock.expectation.{JsonResponse, ServiceExpectation}
import com.github.pbyrne84.scalahttpmock.expectation.matcher._

val expectedHeader = HeaderEquals( "header_name", "header_value" )
val expectedHttpMethod = GetMatcher
val expectedUriPath = PathEquals( "/path/to/item" )
val expectedParam = ParamEquals( "a", "12345" )
val response = JsonResponse( 200, Some( "{}" ) )

ServiceExpectation(
  headerMatchers = List(expectedHeader),
  httpMethodMatcher = expectedHttpMethod,
  uriMatcher = expectedUriPath,
  paramMatchers = List(expectedParam),
  response = response
)

ServiceExpectation()
    .addHeader(expectedHeader) // there are also replace all and add many
    .withMethod(expectedHttpMethod)
    .withUri(expectedUriPath)
    .addParam(expectedParam) // there are also replace all and add many
    .withResponse(response)

```

so a factory method can be use to create the basis for a project/test
and then only the minimum needs to be changed per scenario. As the
expectation and verify have the same interface it is easy to switch
from tight expectation to loose expectation altered to a then
tight verification. For example an Any payload match for the request
expectation then switched to an exact match for the verification.
Sometimes too tight matching required in the expectation leads to
very hard to debug failures whereas a verification tends to be clear.

#### Expecting a payload
Payloads are bound to method matchers that accept payloads
* PostMatcher
* PutMatcher
* PatchMatcher

These all default to Any

Supported payloads matching is
* ContentEquals - string equals
* ContentMatches - regex matches
* JsonContentEquals - if valid json then equality should be formatting
  insensitive. If invalid json then it will behave like string equals.

## ServiceExpectation and scoring
The default ServiceExpectation will match anything though the ANY matchers
 have a low score so exact matches are boosted above the default when
it comes to expectations and responses etc. Scoring is a rough prototype
at the moment but its main purpose is to give the closest response and
order mismatches on failure with closed descending.

Though false positives are probably less than ideal and should probably be
handled by a strict option.

## TODO:
* Globally configure headers that are really cared about so noise can
  be reduced when showing errors. Also enforce a subset of headers to be
  matched if desired. Over loose matching can give false impressions about
  quality of testing.
* DSL - ("","').asHeaderEquals etc. Undecided on verbosity.
