package com.github.pbyrne84.scalahttpmock.zio

import com.github.pbyrne84.scalahttpmock.service.executor.RunningMockServerWithOperations
import com.github.pbyrne84.scalahttpmock.service.request.FreePort
import com.github.pbyrne84.scalahttpmock.zio.ZIOBaseSpec.SharedDeps
import zio.test.ZIOSpec
import zio.{Task, ZLayer}

object ZIOBaseSpec {

  protected val port: Int = FreePort.calculate
  type SharedDeps = RunningMockServerWithOperations[Task]

  val sharedLayer: ZLayer[Any, Throwable, SharedDeps] = ZLayer.make[SharedDeps](
    ZioNettyMockServer.layer(port)
  )

}
abstract class ZIOBaseSpec extends ZIOSpec[SharedDeps] {
  protected val port: Int = ZIOBaseSpec.port

  override def bootstrap: ZLayer[Any, Throwable, SharedDeps] = ZIOBaseSpec.sharedLayer
}
