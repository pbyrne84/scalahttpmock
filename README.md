# ScalaHttpMock

A scala based http mocking service with a netty backgound which tries to offer nicer errors
and easy switching from an expectation to a verification. Ideally
this should enable clearer boundary testing.

## Dependency setup in a project
All package name etc. will change before publishing

## Starting

### Future
```scala
NettyMockServer.createFutureVersion(port = 9999).start
```
#### Example future test
<https://github.com/pbyrne84/scalahttpmock/blob/master/modules/core/src/test/scala/com/github/pbyrne84/scalahttpmock/service/TestServiceFutureSpec.scala>


### Cats Effect
```scala
implicit val mockServiceExecutor: CENettyMockServiceExecutor = new CENettyMockServiceExecutor()
NettyMockServer.createIoMonadVersion(port = 9999).start
```
#### Example CE test
<https://github.com/pbyrne84/scalahttpmock/blob/master/modules/core/src/test/scala/com/github/pbyrne84/scalahttpmock/service/TestServiceCESpec.scala>


### ZIO 
```scala
import zio.interop.catz._
implicit val zIOMockServiceExecutor: ZIONettyMockServiceExecutor = new ZIONettyMockServiceExecutor()
NettyMockServer.createIoMonadVersion(port = 9999).start
```
#### Example Layer for a test
<https://github.com/pbyrne84/scalahttpmock/blob/master/modules/zio/src/main/scala/com/github/pbyrne84/scalahttpmock/zio/ZioNettyMockServer.scala>

#### Shared ZIO layer so startup is shared between any tests
<https://github.com/pbyrne84/scalahttpmock/blob/master/modules/zio/src/test/scala/com/github/pbyrne84/scalahttpmock/zio/ZIOBaseSpec.scala>

#### Example ZIO test using shared layering
<https://github.com/pbyrne84/scalahttpmock/blob/master/modules/zio/src/test/scala/com/github/pbyrne84/scalahttpmock/zio/TestServiceZioSpec.scala>



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

so a factory method can be used to create the basis for a project/test
and then only the minimum needs to be changed per scenario. As the
expectation and verify have the same interface it is easy to switch
from tight expectation to a loose expectation altered to a then
tight verification. For example an Any payload match for the request
expectation then switched to an exact match for the verification.
Sometimes too tight matching required in the expectation leads to
very hard to debug failures whereas a verification tends to be clear.

**withResponses** allows functionality similar to 
```
thenReturn()
.thenReturn()
.thenReturn()
```

For simple chaining for testing things like retries. The first 2 can fail and then the third
can succeed etc.

E.g.
```scala
import com.github.pbyrne84.scalahttpmock.expectation.matcher.{JsonContentEquals, PostMatcher}
import com.github.pbyrne84.scalahttpmock.expectation.{JsonResponse, ServiceExpectation}
import com.github.pbyrne84.scalahttpmock.expectation.matcher.HeaderEquals
import com.softwaremill.sttp.sttp
import com.softwaremill.sttp._
implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

val port = 9000
val service = MockServiceFactory.create(port)

val responseJson = """{"a":"1"}"""
val expectation = ServiceExpectation()
    .addHeader(HeaderEquals("a", "avalue"))
    .withMethod(PostMatcher())
    .withUri("/test/path".asUriEquals)
    .withResponse(JsonResponse(202, Some(responseJson))) // adds json header and allows for custom headers

service.addExpectation(expectation)

val uri = uri"http://localhost:$port/test/path"
val request = sttp
.post(uri)
.header("a", "avalue")

val response = request.send()

response.code shouldBe 202
response.unsafeBody shouldBe responseJson

service.verifyCall(expectation.withMethod(PostMatcher(JsonContentEquals("""{ "z":"26" }""")))) //fails here
```

fails with the message
```
The following expectation was matched 0 out of 1 times:-
ServiceExpectation[method="Post"](
  Headers : [ Equals("a", "avalue") ],
  Uri     : Uri equals "/test/path",
  Params  : Any,
  Body    : {
    "z" : "26"
  }
)
The following requests were made (1):-
[INVALID] SCORE:4.0/3.0 failed {CONTENT Equals Json}
  Method  - Matching     : Post
  Headers - Matching     : [ Equals("a", "avalue") ]
  Headers - Non matching : None
  Uri     - Matching     : Uri equals "/test/path"
  Params  - Matching     : None
  Params  - Non matching : None
  Content - Non matching : {
    "z" : "26"
  }
Request[method="POST", path="/test/path"](
  Uri     : "/test/path",
  Params  : [],
  Headers : [ ("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2"),
              ("Accept-Encoding", "gzip, deflate"),
              ("Connection", "keep-alive"),
              ("Host", "localhost:9000"),
              ("User-Agent", "Java/1.8.0_77"),
              ("a", "avalue") ],
  Body    : None
)

```


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
