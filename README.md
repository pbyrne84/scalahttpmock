# ScalaHttpMock

A scala based http mocking service which tries to offer nicer errors
and easy switching from an expectation to a verification. Ideally
this should enable clearer boundary testing.

## Dependency setup in a project
All package name etc will change before publishing

## Starting

```scala
ServiceFactory.create(9999) // will create a running test service on 8888
```


## Adding an expectation
```scala
testService.addExpectation(ServiceExpectation())
```

## Verifying an expectation was called
```scala
testService.verifyCall(ServiceExpectation())
```

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
* Make it human friendly when a request does not match an expectation.

