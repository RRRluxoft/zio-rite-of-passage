package home.smartland

import zio.*

import scala.io.StdIn

object ZIORecap extends ZIOAppDefault {

  // "effects" == computation as values

  val meaningOfLife: ZIO[Any, Nothing, Int] = ZIO.succeed(42)
  val aFailure: ZIO[Any, String, Nothing] = ZIO.fail(s"Boom! Smth went wrong")
  // suspension / delay
  val aSuspension: ZIO[Any, Throwable, Int] = ZIO.suspend(meaningOfLife)

  val smallProgram = for {
    _    <- Console.printLine("what's your name?")
    name <- ZIO.succeed(StdIn.readLine())
    _    <- Console.printLine(s"Hi, $name!")
  } yield ()

  // error handling
  val anAttempt: ZIO[Any, Throwable, Int] = ZIO.attempt {
    val str: String = null
    str.length
  }

  val catchError: ZIO[Any, Nothing, Any] = anAttempt.catchAll { err => ZIO.succeed(s"Error: $err") }
  val catchSelective = anAttempt.catchSome {
    case error: RuntimeException => ZIO.succeed(s"Ignoring Runtime exception: $error")
    case _                       => ZIO.succeed(s"Ignoring everything else")
  }

  // fibers:
  val delayedValue = ZIO.sleep(1.second) *> Random.nextIntBetween(0, 100)
  val aPair = for {
    a <- delayedValue
    b <- delayedValue
  } yield (a, b) // 2 seconds
  val aPairPar = for {
    fibA <- delayedValue.fork
    fibB <- delayedValue.fork
    a <- fibA.join
    b <- fibB.join
  } yield (a, b) // 1 second

  val interruptedFiber = for {
    fib <- delayedValue.map(println).onInterrupt(ZIO.succeed(println("Interrupted!"))).fork
    _   <- ZIO.sleep(500.millis) *> ZIO.succeed(println("cancelling fiber")) *> fib.interrupt
    _   <- fib.join
  } yield ()

  val ignoredInterruption = for {
    fib <- ZIO.uninterruptible(delayedValue.map(println).onInterrupt(ZIO.succeed(println("Interrupted!")))).fork
    _ <- ZIO.sleep(500.millis) *> ZIO.succeed(println("cancelling fiber")) *> fib.interrupt
    _ <- fib.join
  } yield ()

  // many APIs on top of fibers
  val aPairPar_v2 = delayedValue.zipPar(delayedValue)
  val randomX10 = ZIO.collectPar((1 to 10).map(_ => delayedValue)) // traverse

  // dependencies
  case class User(name: String, email: String)
  class UserSubscription(emailService: EmailService, userDatabase: UserDatabase) {
    def subscribe(user: User): Task[Unit] = for {
      _ <- emailService.email(user)
      _ <- userDatabase.insert(user)
      _ <- ZIO.succeed((s"Subscribed ${user.name}"))
    } yield ()
  }
  object UserSubscription {
    val live: ZLayer[EmailService with UserDatabase, Nothing, UserSubscription] =
      ZLayer.fromFunction(
        (emailService: EmailService, userDatabase: UserDatabase) => new UserSubscription(emailService, userDatabase))
//    def apply(emailService: EmailService, userDatabase: UserDatabase): UserSubscription =
//      new UserSubscription(emailService, userDatabase)
  }

  class EmailService {
    def email(user: User): Task[Unit] = ZIO.succeed(s"Emailed $user")
  }
  object EmailService {
    val live: ZLayer[Any, Nothing, EmailService] =
      ZLayer.succeed(new EmailService())
  }

  class UserDatabase(connectionPool: ConnectionPool) {
    def insert(user: User): Task[Unit] = ZIO.succeed(s"inserted $user")
  }
  object UserDatabase {
    val live: ZLayer[ConnectionPool, Nothing, UserDatabase] =
      ZLayer.fromFunction(new UserDatabase(_))
  }

  class ConnectionPool(nConnections: Int) {
    def getConnection: Task[Connection] = ZIO.succeed(Connection())
  }
  object ConnectionPool {
    def live(nConnections: Int): ZLayer[Any, Nothing, ConnectionPool] =
      ZLayer.succeed(new ConnectionPool(nConnections))
  }

  case class Connection()

  def subscribe(user: User): ZIO[UserSubscription, Throwable, Unit] = for {
    sub <- ZIO.service[UserSubscription]
    _ <- sub.subscribe(user)
  } yield ()

  val program: ZIO[UserSubscription, Throwable, Unit] = for {
    _ <- subscribe(User("TomCat", "tomcat@smartland.home"))
    _ <- subscribe(User("WildCat", "wildcat@smartland.home"))
  } yield ()


  def run =
    program.provide( //(UserSubscription(new EmailService(), new UserDatabase(new ConnectionPool(10))))
      ConnectionPool.live(10),
      UserDatabase.live,
      EmailService.live,
      UserSubscription.live
    )
}
