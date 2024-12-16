package home.smartland

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.server.ziohttp.ZioHttpServerOptions
import sttp.tapir.server.ServerEndpoint
import zio.*
import zio.http.Server
import zio.json.DeriveJsonCodec
import zio.json.JsonCodec

import scala.collection.mutable

object TapirDemo extends ZIOAppDefault {

  val simplestEndpoint = endpoint
    .tag("simple")
    .name("simple")
    .description("simplest endpoint possible")
    // ^^ for documentation
    .get
    .in("simple")
    .out(plainBody[String])
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

  val simpleServerProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default // configs CORS
    ).toHttp(simplestEndpoint)
  )

  // simulate a job board
  val db = mutable.Map[Long, Job](
    1L -> Job(1L, "Instructor", "https://rockthejvm.com", "Example"),
    2L -> Job(2L, "Scala Developer", "https://smartland.home", "Example")
  )

  // create
  val createEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("create")
    .description("Create a job")
    .in("jobs")
    .post
    .in(jsonBody[CreateJobRequest])
    .out(jsonBody[Job])
    .serverLogicSuccess(req =>
      ZIO.succeed{
        val newId = db.keys.max + 1
        val newJob = Job(newId, req.title, req.url, req.company)
        db += (newId -> newJob)
        newJob
    })

  // get by Id
  val getByIdEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("getById")
    .description("Get a job by id")
    .in("jobs" / path[Long]("id"))
    .get
    .out(jsonBody[Option[Job]])
    .serverLogicSuccess(id => ZIO.succeed(db.get(id)))

  val getAllEndpoints: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("name")
    .description("Get all jobs")
    .in("jobs")
    .get
    .out(jsonBody[List[Job]])
    .serverLogicSuccess(_ => ZIO.succeed(db.values.toList))


  val serverProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default
    ).toHttp(List(createEndpoint, getByIdEndpoint, getAllEndpoints))
  )

  def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = serverProgram.provide(
    Server.default
  )

}

case class Job(id: Long, title: String, url: String, company: String)
object Job {
  // import sttp.tapir.generic.auto.*
  given codec: JsonCodec[Job] = DeriveJsonCodec.gen[Job] // macro-based JSON codec
}

case class CreateJobRequest(title: String, url: String, company: String)
object CreateJobRequest {
  given codec: JsonCodec[CreateJobRequest] = DeriveJsonCodec.gen[CreateJobRequest]
}

